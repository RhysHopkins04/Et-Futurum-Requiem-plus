package ganymedes01.etfuturum;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.ModernAssetResourcePack;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class ModernPowderSnowBucket extends Item {
    private static Item bucket;
    private ModernPowderSnowBucket() { setMaxStackSize(1); setContainerItem(Items.bucket); setUnlocalizedName(Tags.MOD_ID+".powder_snow_bucket"); setCreativeTab(EtFuturum.creativeTabItems); }
    public static void init() {
        if (ModernMapParityBlocks.POWDER_SNOW.get()==null) return;
        bucket=GameRegistry.findItem(Tags.MOD_ID,"powder_snow_bucket");
        if (bucket==null) { bucket=new ModernPowderSnowBucket(); GameRegistry.registerItem(bucket,"powder_snow_bucket"); }
    }
    public static Item get() { return bucket; }
    @Override public ItemStack onItemRightClick(ItemStack stack,World world,EntityPlayer player) {
        MovingObjectPosition hit=getMovingObjectPositionFromPlayer(world,player,false);
        if (hit==null || hit.typeOfHit!=MovingObjectPosition.MovingObjectType.BLOCK) return stack;
        int x=hit.blockX,y=hit.blockY,z=hit.blockZ;
        if (!world.getBlock(x,y,z).isReplaceable(world,x,y,z)) {
            ForgeDirection d=ForgeDirection.getOrientation(hit.sideHit); x+=d.offsetX; y+=d.offsetY; z+=d.offsetZ;
        }
        if (y<0 || y>255 || !world.canMineBlock(player,hit.blockX,hit.blockY,hit.blockZ)
                || !player.canPlayerEdit(x,y,z,hit.sideHit,stack) || !world.getBlock(x,y,z).isReplaceable(world,x,y,z)) return stack;
        if (!world.isRemote && !world.setBlock(x,y,z,ModernMapParityBlocks.POWDER_SNOW.get(),0,3)) return stack;
        world.playSoundEffect(x+0.5,y+0.5,z+0.5,Tags.MC_ASSET_VER+":item.bucket.empty_powder_snow",1,1);
        return player.capabilities.isCreativeMode ? stack : new ItemStack(Items.bucket);
    }
    @Override @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        String synthetic = "modern_item/powder_snow_bucket";
        ModernAssetResourcePack.registerDynamicAlias(
                "textures/items/" + synthetic + ".png",
                "textures/item/powder_snow_bucket.png");
        itemIcon = register.registerIcon("minecraft:" + synthetic);
    }
}
