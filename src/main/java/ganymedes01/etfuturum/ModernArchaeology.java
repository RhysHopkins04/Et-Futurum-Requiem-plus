package ganymedes01.etfuturum;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.ModernAssetResourcePack;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/** The bounded 1.20 archaeology tool used by the map-parity suspicious blocks. */
public final class ModernArchaeology {
    private static Item brush;
    private ModernArchaeology() { }

    public static void init() {
        Item existing = GameRegistry.findItem(Tags.MOD_ID, "brush");
        if (existing != null) { brush = existing; return; }
        brush = new BrushItem();
        GameRegistry.registerItem(brush, "brush");
    }
    public static Item getBrush() { return brush; }

    private static final class BrushItem extends Item {
        BrushItem() {
            setUnlocalizedName(Tags.MOD_ID + ".brush");
            setCreativeTab(EtFuturum.creativeTabItems);
            setMaxStackSize(1);
            setMaxDamage(64);
        }
        @Override public int getMaxItemUseDuration(ItemStack stack) { return 72000; }
        @Override public EnumAction getItemUseAction(ItemStack stack) { return EnumAction.none; }
        @Override public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
            MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, false);
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                    && ModernMapParityBlocks.isSuspiciousBlock(world.getBlock(hit.blockX, hit.blockY, hit.blockZ))) {
                player.setItemInUse(stack, getMaxItemUseDuration(stack));
            }
            return stack;
        }
        @Override public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
            if (player.worldObj.isRemote || (getMaxItemUseDuration(stack) - count) % 10 != 0) return;
            MovingObjectPosition hit = getMovingObjectPositionFromPlayer(player.worldObj, player, false);
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
            net.minecraft.tileentity.TileEntity tile = player.worldObj.getTileEntity(hit.blockX, hit.blockY, hit.blockZ);
            if (!(tile instanceof ModernMapParityBlocks.ParityBrushableTileEntity)) return;
            if (((ModernMapParityBlocks.ParityBrushableTileEntity) tile).brush(player)) {
                stack.damageItem(1, player);
                player.stopUsingItem();
            }
        }
        @Override @SideOnly(Side.CLIENT)
        public void registerIcons(IIconRegister register) {
            String synthetic = "modern_item/brush";
            ModernAssetResourcePack.registerDynamicAlias("textures/items/" + synthetic + ".png", "textures/item/brush.png");
            itemIcon = register.registerIcon("minecraft:" + synthetic);
        }
    }
}
