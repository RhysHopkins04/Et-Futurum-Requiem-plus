package ganymedes01.etfuturum.core.handlers;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.core.utils.DripleafWater;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.ModernPowderSnowBucket;
import ganymedes01.etfuturum.api.spectator.SpectatorUtils;
import ganymedes01.etfuturum.core.utils.PowderSnowPhysics;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.lang.reflect.Field;

public final class ModernFluidEventHandler {
    private static final Field BUKKIT_INTERACTION = findBukkitInteractionField();

    private static Field findBukkitInteractionField() {
        try {
            // Crucible carries the original Bukkit decision separately from the Forge fields.
            return PlayerInteractEvent.class.getField("cb");
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    public static boolean isItemUseAllowed(PlayerInteractEvent event) {
        if (event.useItem == Event.Result.DENY) return false;
        if (BUKKIT_INTERACTION == null) return true;
        try {
            Object bukkitEvent = BUKKIT_INTERACTION.get(event);
            if (bukkitEvent == null) return true;
            Object result = BUKKIT_INTERACTION.getType().getMethod("useItemInHand").invoke(bukkitEvent);
            return result instanceof Enum<?> && !"DENY".equals(((Enum<?>) result).name());
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    @SubscribeEvent public void living(LivingUpdateEvent e) { PowderSnowPhysics.tick(e.entityLiving); }

    public static boolean canUseOnHost(EntityPlayer player, World world, ItemStack stack, int x, int y, int z) {
        if (stack == null || player.isSneaking() || SpectatorUtils.isSpectator(player)) return false;
        if (stack.getItem() != Items.bucket && stack.getItem() != Items.water_bucket) return false;
        Block host = world.getBlock(x, y, z);
        if (NativeWaterlogging.creativeBucketOnly(host) && !player.capabilities.isCreativeMode) return false;
        if (stack.getItem() == Items.bucket) return NativeWaterlogging.contains(world, x, y, z);
        return !world.provider.isHellWorld && NativeWaterlogging.canHold(host, world, x, y, z);
    }

    public static boolean useOnHost(EntityPlayer player, World world, ItemStack stack,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (!canUseOnHost(player, world, stack, x, y, z)) return false;
        if (world.isRemote) return true; // The normal block-interaction packet still reaches the server.

        MovingObjectPosition target = new MovingObjectPosition(x, y, z, side,
                Vec3.createVectorHelper(x + hitX, y + hitY, z + hitZ));
        FillBucketEvent event = new FillBucketEvent(player, stack, world, target);
        if (!MinecraftForge.EVENT_BUS.post(event) && event.getResult() == Event.Result.ALLOW
                && event.result != null && !player.capabilities.isCreativeMode) {
            if (--stack.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, event.result);
            else if (!player.inventory.addItemStackToInventory(event.result))
                player.dropPlayerItemWithRandomChoice(event.result, false);
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public void bucket(FillBucketEvent e) {
        if (e.target == null || e.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || e.current == null) return;
        int x=e.target.blockX,y=e.target.blockY,z=e.target.blockZ;
        if (!e.world.canMineBlock(e.entityPlayer,x,y,z) || !e.entityPlayer.canPlayerEdit(x,y,z,e.target.sideHit,e.current)) return;
        if (NativeWaterlogging.creativeBucketOnly(e.world.getBlock(x, y, z))
                && !e.entityPlayer.capabilities.isCreativeMode) return;
        // Buckets must act on the *clicked* dripleaf, not the adjacent cell.
        // Modern Java allows a full bucket to empty into an already-wet block
        // without replacing the plant (MC-127110, fixed in 1.19.3).
        if (DripleafWater.isDripleaf(e.world, x, y, z)) {
            boolean wet = DripleafWater.contains(e.world, x, y, z);
            if (e.current.getItem() == Items.water_bucket) {
                if (!wet && !e.world.isRemote && !DripleafWater.fill(e.world, x, y, z)) return;
                if (!e.world.isRemote) e.world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D,
                        Tags.MC_ASSET_VER + ":item.bucket.empty", 1.0F, 1.0F);
                e.result = new ItemStack(Items.bucket);
                e.setResult(Event.Result.ALLOW);
                return;
            }
            if (e.current.getItem() == Items.bucket && wet) {
                if (!e.world.isRemote) {
                    if (!DripleafWater.drain(e.world, x, y, z)) return;
                    e.world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D,
                            Tags.MC_ASSET_VER + ":item.bucket.fill", 1.0F, 1.0F);
                }
                e.result = new ItemStack(Items.water_bucket);
                e.setResult(Event.Result.ALLOW);
                return;
            }
        }
        if (e.current.getItem()==Items.bucket) {
            if (e.world.getBlock(x,y,z)==ModernMapParityBlocks.POWDER_SNOW.get()) {
                if (!e.world.isRemote) e.world.setBlockToAir(x,y,z);
                e.result=new ItemStack(ModernPowderSnowBucket.get());
                e.setResult(Event.Result.ALLOW);
                e.world.playSoundEffect(x+0.5,y+0.5,z+0.5,"minecraft_1.21.11:item.bucket.fill_powder_snow",1,1);
            } else if (NativeWaterlogging.drain(e.world, x, y, z)) {
                e.result = new ItemStack(Items.water_bucket);
                e.setResult(Event.Result.ALLOW);
                if (!e.world.isRemote) e.world.playSoundEffect(x + .5, y + .5, z + .5,
                        Tags.MC_ASSET_VER + ":item.bucket.fill", 1, 1);
            } else if (ModernMapParityBlocks.drainContainedWater(e.world,x,y,z)) {
                e.result=new ItemStack(Items.water_bucket);
                e.setResult(Event.Result.ALLOW);
            }
        } else if (e.current.getItem()==Items.water_bucket) {
            if (e.entityPlayer.isSneaking()
                    && NativeWaterlogging.canHold(e.world.getBlock(x, y, z), e.world, x, y, z)) return;
            // Do not replace an already-wet aquatic block with vanilla water;
            // consume the bucket normally and preserve the existing source.
            if (NativeWaterlogging.fill(e.world, x, y, z)
                    || ModernMapParityBlocks.containsWater(e.world,x,y,z)
                    || ModernMapParityBlocks.fillContainedWater(e.world,x,y,z)) {
                e.result=new ItemStack(Items.bucket);
                e.setResult(Event.Result.ALLOW);
                if (!e.world.isRemote) e.world.playSoundEffect(x+0.5D,y+0.5D,z+0.5D,
                        Tags.MC_ASSET_VER + ":item.bucket.empty",1.0F,1.0F);
            }
        }
    }
}
