package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.client.particle.CustomParticles;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/** Ceiling-hanging lush-cave flower with modern-style ambient spores. */
public class BlockSporeBlossom extends Block {

    private IIcon baseIcon;
    private IIcon flowerIcon;

    public BlockSporeBlossom() {
        super(Material.plants);
        setHardness(0.0F);
        setResistance(0.0F);
        setBlockName(Utils.getUnlocalisedName("spore_blossom"));
        setBlockTextureName("spore_blossom");
        setCreativeTab(EtFuturum.creativeTabBlocks);
        Utils.setBlockSound(this, ModSounds.soundSporeBlossom);
        setBlockBounds(0.0625F, 0.40F, 0.0625F, 0.9375F, 1.0F, 0.9375F);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return RenderIDs.SPORE_BLOSSOM;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return super.canPlaceBlockAt(world, x, y, z) && canHangFrom(world, x, y + 1, z);
    }

    public boolean canBlockStay(World world, int x, int y, int z) {
        return canHangFrom(world, x, y + 1, z);
    }

    private static boolean canHangFrom(World world, int x, int y, int z) {
        Block above = world.getBlock(x, y, z);
        return !above.isLeaves(world, x, y, z)
                && above.isSideSolid(world, x, y, z, ForgeDirection.DOWN);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
        // The modern block emits a direct falling spore and also seeds drifting particles
        // around the surrounding air. The 10-block X/Z spread produces the familiar large
        // local cloud without introducing a server-side ticker.
        if (rand.nextInt(4) == 0) {
            CustomParticles.spawnSporeBlossomParticle(world,
                    x + 0.5D + (rand.nextDouble() - 0.5D) * 0.35D,
                    y + 0.45D,
                    z + 0.5D + (rand.nextDouble() - 0.5D) * 0.35D,
                    true);
        }

        if (rand.nextInt(2) == 0) {
            double px = x + 0.5D + (rand.nextDouble() - 0.5D) * 20.0D;
            double py = y - rand.nextDouble() * 10.0D;
            double pz = z + 0.5D + (rand.nextDouble() - 0.5D) * 20.0D;
            if (world.isAirBlock((int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz))) {
                CustomParticles.spawnSporeBlossomParticle(world, px, py, pz, false);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        flowerIcon = reg.registerIcon("spore_blossom");
        baseIcon = reg.registerIcon("spore_blossom_base");
        blockIcon = flowerIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return flowerIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getFlowerIcon() {
        return flowerIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBaseIcon() {
        return baseIcon;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }
}
