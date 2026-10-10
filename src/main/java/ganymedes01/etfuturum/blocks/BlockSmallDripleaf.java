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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Two-block small dripleaf. Metadata layout: bits 0-1 facing, bit 2 upper half.
 *
 * Natural Lush Cave placement is handled by WorldGenLushCaves; this class supplies the
 * placeable two-block plant and vanilla-style growth bridge into the big-dripleaf family.
 */
public class BlockSmallDripleaf extends Block implements IGrowable {

    public static final int UPPER_BIT = 4;
    public static final int WATERLOGGED_BIT = 8;
    private static final ThreadLocal<Boolean> CONVERTING = new ThreadLocal<Boolean>();

    private IIcon leafTop;
    private IIcon leafSide;
    private IIcon stemTop;
    private IIcon stemBottom;

    public BlockSmallDripleaf() {
        super(Material.plants);
        setBlockName(Utils.getUnlocalisedName("small_dripleaf"));
        setBlockTextureName("small_dripleaf_top");
        Utils.setBlockSound(this, ModSounds.soundSmallDripleaf);
        setHardness(0.0F);
        setCreativeTab(EtFuturum.creativeTabBlocks);
        setBlockBounds(0.125F, 0.0F, 0.125F, 0.875F, 1.0F, 0.875F);
    }

    public static boolean isUpper(int meta) {
        return (meta & UPPER_BIT) != 0;
    }

    public static int getFacing(int meta) {
        return meta & 3;
    }

    public static int makeMeta(int facing, boolean upper) {
        return (facing & 3) | (upper ? UPPER_BIT : 0);
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() { return 1; }

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
        return RenderIDs.SMALL_DRIPLEAF;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        if (isUpper(world.getBlockMetadata(x, y, z))) {
            // The upper section includes leaf plates spanning near the block edges.
            setBlockBounds(1F / 16F, 0, 1F / 16F, 15F / 16F, 15F / 16F, 15F / 16F);
        } else {
            // The lower section is just two crossed stem planes, ~0.31 blocks wide.
            setBlockBounds(5.5F / 16F, 0, 5.5F / 16F, 10.5F / 16F, 1, 10.5F / 16F);
        }
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getSelectedBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return y < world.getHeight() - 1
                && isAirOrSourceWater(world, x, y, z)
                && isAirOrSourceWater(world, x, y + 1, z)
                && isValidSmallDripleafGround(world, x, y - 1, z, isSourceWater(world, x, y, z));
    }

    /** Called before the item replaces the source block, so the original water is still observable. */
    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side,
                              float hitX, float hitY, float hitZ, int metadata) {
        return (metadata & 7) | (isSourceWater(world, x, y, z) ? WATERLOGGED_BIT : 0);
    }

    private static boolean isSourceWater(IBlockAccess world, int x, int y, int z) {
        Block b = world.getBlock(x, y, z);
        return (b == Blocks.water || b == Blocks.flowing_water)
                && (world.getBlockMetadata(x, y, z) & 15) == 0;
    }

    private static boolean isAirOrSourceWater(IBlockAccess world, int x, int y, int z) {
        return world.isAirBlock(x, y, z) || isSourceWater(world, x, y, z);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (world.isRemote) {
            return;
        }

        int yawQuadrant = MathHelper.floor_double((placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        int facing = (yawQuadrant + 2) & 3;
        int lowerWater = world.getBlockMetadata(x, y, z) & WATERLOGGED_BIT;
        boolean upperWater = isSourceWater(world, x, y + 1, z);
        world.setBlockMetadataWithNotify(x, y, z, makeMeta(facing, false) | lowerWater, 2);
        if (isAirOrSourceWater(world, x, y + 1, z)) {
            world.setBlock(x, y + 1, z, this,
                    makeMeta(facing, true) | (upperWater ? WATERLOGGED_BIT : 0), 3);
        }
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        if (isUpper(meta)) {
            Block below = world.getBlock(x, y - 1, z);
            return below == this && !isUpper(world.getBlockMetadata(x, y - 1, z));
        }

        Block above = world.getBlock(x, y + 1, z);
        return isValidSmallDripleafGround(world, x, y - 1, z, (meta & WATERLOGGED_BIT) != 0)
                && above == this
                && isUpper(world.getBlockMetadata(x, y + 1, z));
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!world.isRemote && !canBlockStay(world, x, y, z)) {
            world.func_147480_a(x, y, z, true);
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block oldBlock, int oldMeta) {
        if (!world.isRemote && !Boolean.TRUE.equals(CONVERTING.get())) {
            int otherY = isUpper(oldMeta) ? y - 1 : y + 1;
            if (world.getBlock(x, otherY, z) == this) {
                world.setBlockToAir(x, otherY, z);
            }
        }
        super.breakBlock(world, x, y, z, oldBlock, oldMeta);
    }

    /**
     * Modern 1.21 small-dripleaf survival: clay and moss support it dry; on
     * dirt-like substrates its lower half must occupy source water.
     * Unlike a generic Material.ground check, this does not accidentally allow
     * stone/sand/gravel, which are not part of the modern placeable tag.
     */
    private boolean isValidSmallDripleafGround(IBlockAccess world, int x, int y, int z,
                                                boolean lowerIsWet) {
        Block ground = world.getBlock(x, y, z);
        if (ground == Blocks.clay || (ModBlocks.MOSS_BLOCK.isEnabled() && ground == ModBlocks.MOSS_BLOCK.get()))
            return true;
        if (!lowerIsWet) return false;
        return ground == Blocks.dirt || ground == Blocks.grass || ground == Blocks.farmland
                || ground == Blocks.mycelium
                || (ModBlocks.ROOTED_DIRT.isEnabled() && ground == ModBlocks.ROOTED_DIRT.get())
                || (ModBlocks.MUD.isEnabled() && ground == ModBlocks.MUD.get())
                || (ModBlocks.MUDDY_MANGROVE_ROOTS.isEnabled() && ground == ModBlocks.MUDDY_MANGROVE_ROOTS.get())
                || ground == ganymedes01.etfuturum.ModernMapParityBlocks.PALE_MOSS_BLOCK.get();
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
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        return new ItemStack(this, 1, 0);
    }

    /** MCP: canFertilize */
    @Override
    public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
        int lowerY = isUpper(world.getBlockMetadata(x, y, z)) ? y - 1 : y;
        return world.getBlock(x, lowerY, z) == this
                && world.getBlock(x, lowerY + 1, z) == this
                && ModBlocks.BIG_DRIPLEAF.isEnabled()
                && ModBlocks.BIG_DRIPLEAF_STEM.isEnabled();
    }

    /** MCP: shouldFertilize */
    @Override
    public boolean func_149852_a(World world, Random random, int x, int y, int z) {
        return true;
    }

    /** MCP: fertilize */
    @Override
    public void func_149853_b(World world, Random random, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        int lowerY = isUpper(meta) ? y - 1 : y;
        if (world.getBlock(x, lowerY, z) != this || world.getBlock(x, lowerY + 1, z) != this) {
            return;
        }

        int facing = getFacing(world.getBlockMetadata(x, lowerY, z));
        int desiredHeight = 2 + random.nextInt(4);
        int height = 2;
        for (int i = 2; i < desiredHeight; i++) {
            if (!isAirOrSourceWater(world, x, lowerY + i, z)) {
                break;
            }
            height++;
        }

        // Capture the original wet states *before* replacing the lower half.
        // breakBlock removes the original upper half, otherwise losing its water flag.
        boolean[] water = new boolean[height];
        for (int i = 0; i < height; i++) {
            int atY = lowerY + i;
            water[i] = world.getBlock(x, atY, z) == this
                    ? (world.getBlockMetadata(x, atY, z) & WATERLOGGED_BIT) != 0
                    : isSourceWater(world, x, atY, z);
        }
        // Replacing the lower small-dripleaf half normally removes its companion.
        // During intentional conversion keep both original halves available until
        // their wet states have been consumed, and publish only the complete column.
        CONVERTING.set(Boolean.TRUE);
        try {
            for (int i = 0; i < height - 1; i++) {
                world.setBlock(x, lowerY + i, z, ModBlocks.BIG_DRIPLEAF_STEM.get(),
                        facing | (water[i] ? BlockBigDripleafStem.WATERLOGGED_BIT : 0), 2);
            }
            world.setBlock(x, lowerY + height - 1, z,
                    water[height - 1] ? ModBlocks.BIG_DRIPLEAF_WET.get() : ModBlocks.BIG_DRIPLEAF.get(),
                    facing, 2);
        } finally {
            CONVERTING.remove();
        }
        for (int i = 0; i < height; i++) {
            int atY = lowerY + i;
            world.markBlockForUpdate(x, atY, z);
            world.notifyBlocksOfNeighborChange(x, atY, z, world.getBlock(x, atY, z));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        leafTop = reg.registerIcon("small_dripleaf_top");
        leafSide = reg.registerIcon("small_dripleaf_side");
        stemTop = reg.registerIcon("small_dripleaf_stem_top");
        stemBottom = reg.registerIcon("small_dripleaf_stem_bottom");
        blockIcon = leafTop;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getLeafTopIcon() {
        return leafTop;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getLeafSideIcon() {
        return leafSide;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getStemIcon(boolean upper) {
        return upper ? stemTop : stemBottom;
    }
}
