package ganymedes01.etfuturum.core.handlers;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.core.utils.DripleafWater;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.ModernPowderSnowBucket;
import ganymedes01.etfuturum.core.utils.PowderSnowPhysics;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;

public final class ModernFluidEventHandler {
    @SubscribeEvent public void living(LivingUpdateEvent e) { PowderSnowPhysics.tick(e.entityLiving); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void bucket(FillBucketEvent e) {
        if (e.target == null || e.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || e.current == null) return;
        int x=e.target.blockX,y=e.target.blockY,z=e.target.blockZ;
        if (!e.world.canMineBlock(e.entityPlayer,x,y,z) || !e.entityPlayer.canPlayerEdit(x,y,z,e.target.sideHit,e.current)) return;
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
            } else if (ModernMapParityBlocks.containsWater(e.world,x,y,z)) {
                if (!e.world.isRemote) ModernMapParityBlocks.drainContainedWater(e.world,x,y,z);
                e.result=new ItemStack(Items.water_bucket);
                e.setResult(Event.Result.ALLOW);
            }
        } else if (e.current.getItem()==Items.water_bucket) {
            // Do not replace an already-wet aquatic block with vanilla water;
            // consume the bucket normally and preserve the existing source.
            if (ModernMapParityBlocks.containsWater(e.world,x,y,z)
                    || ModernMapParityBlocks.fillContainedWater(e.world,x,y,z)) {
                e.result=new ItemStack(Items.bucket);
                e.setResult(Event.Result.ALLOW);
                if (!e.world.isRemote) e.world.playSoundEffect(x+0.5D,y+0.5D,z+0.5D,
                        Tags.MC_ASSET_VER + ":item.bucket.empty",1.0F,1.0F);
            }
        }
    }
}
