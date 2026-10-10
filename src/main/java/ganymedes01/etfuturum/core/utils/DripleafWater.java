package ganymedes01.etfuturum.core.utils;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockBigDripleaf;
import ganymedes01.etfuturum.blocks.BlockBigDripleafStem;
import ganymedes01.etfuturum.blocks.BlockSmallDripleaf;
import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Waterlogged dripleaf state with no per-block tile entities. */
public final class DripleafWater {
    private DripleafWater() { }

    /** A narrow target predicate: only the three EFR dripleaf block classes. */
    public static boolean isDripleaf(IBlockAccess world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block instanceof BlockSmallDripleaf || block instanceof BlockBigDripleafStem
                || block instanceof BlockBigDripleaf;
    }

    public static boolean contains(IBlockAccess world, int x, int y, int z) {
        Block b = world.getBlock(x,y,z);
        int meta = world.getBlockMetadata(x,y,z);
        if (b instanceof BlockSmallDripleaf) return (meta & BlockSmallDripleaf.WATERLOGGED_BIT) != 0;
        if (b instanceof BlockBigDripleafStem) return (meta & BlockBigDripleafStem.WATERLOGGED_BIT) != 0;
        return b instanceof BlockBigDripleaf && ((BlockBigDripleaf)b).isWaterloggedVariant();
    }

    public static boolean fill(World world, int x, int y, int z) {
        if (world.provider.isHellWorld || contains(world,x,y,z)) return false;
        Block block = world.getBlock(x,y,z);
        int meta=world.getBlockMetadata(x,y,z);
        if (block instanceof BlockSmallDripleaf)
            return world.setBlockMetadataWithNotify(x,y,z,meta | BlockSmallDripleaf.WATERLOGGED_BIT,3);
        if (block instanceof BlockBigDripleafStem)
            return world.setBlockMetadataWithNotify(x,y,z,meta | BlockBigDripleafStem.WATERLOGGED_BIT,3);
        if (block == ModBlocks.BIG_DRIPLEAF.get() && ModBlocks.BIG_DRIPLEAF_WET.isEnabled())
            return world.setBlock(x,y,z,ModBlocks.BIG_DRIPLEAF_WET.get(),meta,3);
        return false;
    }

    public static boolean drain(World world, int x, int y, int z) {
        if (!contains(world,x,y,z)) return false;
        Block block = world.getBlock(x,y,z);
        int meta=world.getBlockMetadata(x,y,z);
        if (block instanceof BlockSmallDripleaf)
            return world.setBlockMetadataWithNotify(x,y,z,meta & ~BlockSmallDripleaf.WATERLOGGED_BIT,3);
        if (block instanceof BlockBigDripleafStem)
            return world.setBlockMetadataWithNotify(x,y,z,meta & ~BlockBigDripleafStem.WATERLOGGED_BIT,3);
        if (block == ModBlocks.BIG_DRIPLEAF_WET.get())
            return world.setBlock(x,y,z,ModBlocks.BIG_DRIPLEAF.get(),meta,3);
        return false;
    }
}
