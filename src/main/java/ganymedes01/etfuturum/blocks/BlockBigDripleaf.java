package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Big dripleaf top block. Bits 0-1 store facing and bits 2-3 store the tilt state.
 *
 * Tilt timing follows the modern four-state progression:
 * upright -> unstable -> partial -> full -> upright. UNSTABLE shares the upright model,
 * PARTIAL is -22.5 degrees, FULL is -45 degrees and non-solid. Redstone holds/resets
 * normal entity tilts, while projectile impacts still force FULL.
 */
public class BlockBigDripleaf extends Block implements IGrowable {

    public static final int TILT_NONE = 0;
    public static final int TILT_UNSTABLE = 1;
    public static final int TILT_PARTIAL = 2;
    public static final int TILT_FULL = 3;

    private IIcon topIcon;
    private IIcon sideIcon;
    private IIcon tipIcon;
    private IIcon stemIcon;

    public BlockBigDripleaf() {
        super(Material.plants);
        setBlockName(Utils.getUnlocalisedName("big_dripleaf"));
        setBlockTextureName("big_dripleaf_top");
        Utils.setBlockSound(this, ModSounds.soundBigDripleaf);
        setHardness(0.1F);
        setCreativeTab(EtFuturum.creativeTabBlocks);
        setTickRandomly(false);
        setBlockBounds(0.0F, 0.75F, 0.0F, 1.0F, 0.9375F, 1.0F);
    }

    public static int getFacing(int meta) {
        return meta & 3;
    }

    public static int getTilt(int meta) {
        return (meta >> 2) & 3;
    }

    public static int makeMeta(int facing, int tilt) {
        return (facing & 3) | ((tilt & 3) << 2);
    }

    public static boolean isValidBigDripleafGround(IBlockAccess world, int x, int y, int z) {
        Block ground = world.getBlock(x, y, z);
        Material material = ground.getMaterial();
        return ground == Blocks.dirt
                || ground == Blocks.grass
                || ground == Blocks.farmland
                || ground == Blocks.clay
                || material == Material.ground
                || material == Material.grass
                || material == Material.clay
                || (ModBlocks.MOSS_BLOCK.isEnabled() && ground == ModBlocks.MOSS_BLOCK.get())
                || (ModBlocks.ROOTED_DIRT.isEnabled() && ground == ModBlocks.ROOTED_DIRT.get());
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
        return RenderIDs.BIG_DRIPLEAF;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        // Convert the legacy yaw quadrant (south, west, north, east) to the modern
        // blockstate order used by the renderer (north, east, south, west).
        int yawQuadrant = MathHelper.floor_double((placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        int facing = (yawQuadrant + 2) & 3;
        world.setBlockMetadataWithNotify(x, y, z, makeMeta(facing, TILT_NONE), 2);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        Block below = world.getBlock(x, y - 1, z);
        return below == ModBlocks.BIG_DRIPLEAF_STEM.get() || isValidBigDripleafGround(world, x, y - 1, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!world.isRemote && !canBlockStay(world, x, y, z)) {
            // Neighbor-driven collapse mirrors modern update-shape behaviour: the unsupported
            // top disappears rather than creating an extra drop above a stem the player broke.
            world.setBlockToAir(x, y, z);
            return;
        }

        if (!world.isRemote && world.isBlockIndirectlyGettingPowered(x, y, z)) {
            int meta = world.getBlockMetadata(x, y, z);
            if (getTilt(meta) != TILT_NONE) {
                world.setBlockMetadataWithNotify(x, y, z, makeMeta(getFacing(meta), TILT_NONE), 3);
            }
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        int tilt = getTilt(world.getBlockMetadata(x, y, z));
        // The partial state is the visible warning stage. Vanilla does not drop the
        // entity until the FULL state, so keep a platform until that transition.
        if (tilt == TILT_FULL) {
            return null;
        }
        return AxisAlignedBB.getBoundingBox(x, y + 0.75D, z, x + 1.0D, y + 0.9375D, z + 1.0D);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
    }

    private void startTilting(World world, int x, int y, int z, Entity entity) {
        if (world.isRemote) {
            return;
        }

        int meta = world.getBlockMetadata(x, y, z);
        int facing = getFacing(meta);

        // Modern Java Edition explicitly lets projectiles force a full tilt even when
        // the leaf is redstone-powered. Check this before the redstone hold rule.
        if (entity instanceof IProjectile) {
            if (getTilt(meta) != TILT_FULL) {
                world.setBlockMetadataWithNotify(x, y, z, makeMeta(facing, TILT_FULL), 3);
                world.scheduleBlockUpdate(x, y, z, this, 100);
            }
            return;
        }

        if (world.isBlockIndirectlyGettingPowered(x, y, z)) {
            return;
        }

        if (entity instanceof EntityLivingBase && getTilt(meta) == TILT_NONE) {
            // UNSTABLE intentionally still renders upright. After 10 ticks it visibly
            // reaches PARTIAL, then after another 10 ticks it reaches FULL/non-solid.
            world.setBlockMetadataWithNotify(x, y, z, makeMeta(facing, TILT_UNSTABLE), 3);
            world.scheduleBlockUpdate(x, y, z, this, 10);
        }
    }

    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
        startTilting(world, x, y, z, entity);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        startTilting(world, x, y, z, entity);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote || world.getBlock(x, y, z) != this) {
            return;
        }

        int meta = world.getBlockMetadata(x, y, z);
        int facing = getFacing(meta);
        int tilt = getTilt(meta);

        if (world.isBlockIndirectlyGettingPowered(x, y, z)) {
            if (tilt != TILT_NONE) {
                world.setBlockMetadataWithNotify(x, y, z, makeMeta(facing, TILT_NONE), 3);
            }
            return;
        }

        if (tilt == TILT_UNSTABLE) {
            world.setBlockMetadataWithNotify(x, y, z, makeMeta(facing, TILT_PARTIAL), 3);
            world.scheduleBlockUpdate(x, y, z, this, 10);
        } else if (tilt == TILT_PARTIAL) {
            world.setBlockMetadataWithNotify(x, y, z, makeMeta(facing, TILT_FULL), 3);
            world.scheduleBlockUpdate(x, y, z, this, 100);
        } else if (tilt == TILT_FULL) {
            world.setBlockMetadataWithNotify(x, y, z, makeMeta(facing, TILT_NONE), 3);
        }
    }

    public void growOne(World world, int x, int y, int z) {
        if (world.isRemote || !world.isAirBlock(x, y + 1, z)) {
            return;
        }

        int meta = world.getBlockMetadata(x, y, z);
        int facing = getFacing(meta);
        world.setBlock(x, y, z, ModBlocks.BIG_DRIPLEAF_STEM.get(), facing, 3);
        world.setBlock(x, y + 1, z, this, makeMeta(facing, TILT_NONE), 3);
    }

    /** MCP: canFertilize */
    @Override
    public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
        return ModBlocks.BIG_DRIPLEAF_STEM.isEnabled() && y < world.getHeight() - 1 && world.isAirBlock(x, y + 1, z);
    }

    /** MCP: shouldFertilize */
    @Override
    public boolean func_149852_a(World world, Random random, int x, int y, int z) {
        return true;
    }

    /** MCP: fertilize */
    @Override
    public void func_149853_b(World world, Random random, int x, int y, int z) {
        growOne(world, x, y, z);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        topIcon = reg.registerIcon("big_dripleaf_top");
        sideIcon = reg.registerIcon("big_dripleaf_side");
        tipIcon = reg.registerIcon("big_dripleaf_tip");
        stemIcon = reg.registerIcon("big_dripleaf_stem");
        blockIcon = topIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getTopIcon() {
        return topIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getSideIcon() {
        return sideIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getTipIcon() {
        return tipIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getStemIcon() {
        return stemIcon;
    }
}
