package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge;
import ganymedes01.etfuturum.core.utils.PowderSnowPhysics;
import ganymedes01.etfuturum.lib.RenderIDs;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import java.util.List;
import java.util.Random;

/** Java 1.21.11 powder snow: entity-context collision; never a generic solid snow cube. */
public final class BlockPowderSnow extends Block {
    public BlockPowderSnow() { super(Material.snow); setHardness(0.25F); setStepSound(soundTypeSnow); }
    @Override public int getRenderType() { return RenderIDs.MODERN_MAP_PARITY; }
    @Override public boolean isOpaqueCube() { return false; }
    @Override public boolean renderAsNormalBlock() { return false; }
    @Override public boolean isReplaceable(IBlockAccess world,int x,int y,int z) { return false; }
    @Override public AxisAlignedBB getCollisionBoundingBoxFromPool(World w,int x,int y,int z) { return null; }
    @Override public void addCollisionBoxesToList(World w,int x,int y,int z,AxisAlignedBB mask,List list,Entity e) {
        if (e == null) return;
        double top = e.fallDistance > 2.5F ? 0.9D : e instanceof EntityFallingBlock ? 1.0D
                : PowderSnowPhysics.canWalk(e) && !e.isSneaking() && e.boundingBox.minY >= y+1.0D-1.0E-5D ? 1.0D : 0.0D;
        if (top > 0) { AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x,y,z,x+1,y+top,z+1); if (box.intersectsWith(mask)) list.add(box); }
    }
    @Override public void onFallenUpon(World w,int x,int y,int z,Entity e,float distance) {
        if (distance >= 4 && e instanceof net.minecraft.entity.EntityLivingBase)
            e.playSound(distance < 7 ? "game.neutral.hurt.fall.small" : "game.neutral.hurt.fall.big",1,1);
    }
    @Override public void onEntityCollidedWithBlock(World w,int x,int y,int z,Entity e) {
        if (e.isBurning()) {
            if (!w.isRemote && (e instanceof net.minecraft.entity.player.EntityPlayer || w.getGameRules().getGameRuleBooleanValue("mobGriefing")))
                w.setBlockToAir(x,y,z);
            e.extinguish();
        }
        if (w.isRemote && (e.prevPosX != e.posX || e.prevPosZ != e.posZ) && w.rand.nextBoolean())
            w.spawnParticle("snowshovel",e.posX,y+1,e.posZ,(w.rand.nextDouble()-0.5D)/6,0.05D,(w.rand.nextDouble()-0.5D)/6);
    }
    @Override public Item getItemDropped(int meta,Random random,int fortune) { return null; }
    @Override protected boolean canSilkHarvest() { return false; }
    @Override @SideOnly(Side.CLIENT) public void registerBlockIcons(IIconRegister r) { blockIcon=ModernJsonModelBridge.getFallbackIcon(ModernJsonModelBridge.prepare(ModernMapParityBlocks.POWDER_SNOW,r)); }
    @Override @SideOnly(Side.CLIENT) public IIcon getIcon(int side,int meta) { return blockIcon; }
}
