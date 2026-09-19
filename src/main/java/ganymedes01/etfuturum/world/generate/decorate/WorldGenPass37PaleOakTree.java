package ganymedes01.etfuturum.world.generate.decorate;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenCanopyTree;

/**
 * Manual/bonemeal-only Pale Oak generator. 1.21.11 uses the DarkOakTrunkPlacer and
 * DarkOakFoliagePlacer for PALE_OAK_BONEMEAL, so reuse 1.7's canopy-tree geometry while
 * substituting only the authored Pale Oak log/leaves identities.
 */
public final class WorldGenPass37PaleOakTree extends WorldGenCanopyTree {
    public WorldGenPass37PaleOakTree(boolean notify) { super(notify); }

    @Override
    protected void setBlockAndNotifyAdequately(World world, int x, int y, int z, Block block, int meta) {
        if (block == Blocks.log2 && (meta & 3) == 1 && ModernMapParityBlocks.PALE_OAK_LOG.get() != null) {
            int axis = (meta & 12) == 4 ? 1 : (meta & 12) == 8 ? 2 : 0;
            super.setBlockAndNotifyAdequately(world, x, y, z, ModernMapParityBlocks.PALE_OAK_LOG.get(), axis);
            return;
        }
        if (block == Blocks.leaves2 && (meta & 3) == 1 && ModernMapParityBlocks.PALE_OAK_LEAVES.get() != null) {
            super.setBlockAndNotifyAdequately(world, x, y, z, ModernMapParityBlocks.PALE_OAK_LEAVES.get(), 8);
            return;
        }
        super.setBlockAndNotifyAdequately(world, x, y, z, block, meta);
    }
}
