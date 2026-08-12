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
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return y < world.getHeight() - 1
                && world.isAirBlock(x, y + 1, z)
                && isValidSmallDripleafGround(world, x, y - 1, z);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (world.isRemote) {
            return;
        }

        int yawQuadrant = MathHelper.floor_double((placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        int facing = (yawQuadrant + 2) & 3;
        world.setBlockMetadataWithNotify(x, y, z, makeMeta(facing, false), 2);
        if (world.isAirBlock(x, y + 1, z)) {
            world.setBlock(x, y + 1, z, this, makeMeta(facing, true), 3);
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
        return isValidSmallDripleafGround(world, x, y - 1, z)
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
        if (!world.isRemote) {
            int otherY = isUpper(oldMeta) ? y - 1 : y + 1;
            if (world.getBlock(x, otherY, z) == this) {
                world.setBlockToAir(x, otherY, z);
            }
        }
        super.breakBlock(world, x, y, z, oldBlock, oldMeta);
    }

    private boolean isValidSmallDripleafGround(IBlockAccess world, int x, int y, int z) {
        Block ground = world.getBlock(x, y, z);
        return ground == Blocks.clay || (ModBlocks.MOSS_BLOCK.isEnabled() && ground == ModBlocks.MOSS_BLOCK.get());
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
            if (!world.isAirBlock(x, lowerY + i, z)) {
                break;
            }
            height++;
        }

        // Replacing the lower half removes the old upper half through breakBlock, after which the
        // complete big-dripleaf column can be installed bottom-up.
        for (int i = 0; i < height - 1; i++) {
            world.setBlock(x, lowerY + i, z, ModBlocks.BIG_DRIPLEAF_STEM.get(), facing, 3);
        }
        world.setBlock(x, lowerY + height - 1, z, ModBlocks.BIG_DRIPLEAF.get(), facing, 3);
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
