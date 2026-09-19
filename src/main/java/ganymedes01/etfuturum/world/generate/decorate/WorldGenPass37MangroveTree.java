package ganymedes01.etfuturum.world.generate.decorate;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockModernSapling;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

import java.util.Random;

/** Manual/bonemeal-only Mangrove tree used by Pass 37. Never registered as a biome feature. */
public final class WorldGenPass37MangroveTree extends WorldGenAbstractTree {
    public WorldGenPass37MangroveTree(boolean notify) { super(notify); }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        if (!ModBlocks.MANGROVE_LOG.isEnabled() || !ModBlocks.LEAVES.isEnabled()) return false;
        // 1.21.11 TreeGrower.MANGROVE selects the tall configured feature 85% of the time.
        // Keep that selection ratio while using a bounded 1.7-compatible manual generator.
        boolean tall = random.nextFloat() < 0.85F;
        int height = tall ? 9 + random.nextInt(5) : 6 + random.nextInt(4);
        if (y < 2 || y + height + 3 >= 256) return false;

        for (int yy = y; yy <= y + height + 2; yy++) {
            int radius = yy >= y + height - 2 ? 3 : 1;
            for (int xx = x - radius; xx <= x + radius; xx++) {
                for (int zz = z - radius; zz <= z + radius; zz++) {
                    Block here = world.getBlock(xx, yy, zz);
                    if (here != Blocks.air && !here.isLeaves(world, xx, yy, zz)
                            && !here.isReplaceable(world, xx, yy, zz)) return false;
                }
            }
        }

        Block soil = world.getBlock(x, y - 1, z);
        if (soil != Blocks.grass && soil != Blocks.dirt && soil != Blocks.clay
                && soil != Blocks.farmland && soil != ModBlocks.MUD.get()) return false;

        // Modern mangroves expose roots around the trunk. Keep this bounded to blocks already
        // present in EFR; if ecology is disabled the tree still grows without manufacturing roots.
        if (ModBlocks.MANGROVE_ROOTS.isEnabled()) {
            final int[][] rootOffsets = {{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] off : rootOffsets) {
                if (random.nextBoolean()) {
                    int rx = x + off[0], rz = z + off[1];
                    if (world.getBlock(rx, y, rz).isReplaceable(world, rx, y, rz))
                        setBlockAndNotifyAdequately(world, rx, y, rz, ModBlocks.MANGROVE_ROOTS.get(), 0);
                }
            }
        }

        for (int dy = 0; dy < height; dy++)
            setBlockAndNotifyAdequately(world, x, y + dy, z, ModBlocks.MANGROVE_LOG.get(), 0);

        int crownY = y + height;
        for (int dy = -2; dy <= 2; dy++) {
            int radius = dy == 2 ? 1 : dy == -2 ? 2 : 3;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && random.nextInt(3) == 0) continue;
                    int px = x + dx, py = crownY + dy, pz = z + dz;
                    Block old = world.getBlock(px, py, pz);
                    if (old == Blocks.air || old.isLeaves(world, px, py, pz) || old.isReplaceable(world, px, py, pz))
                        setBlockAndNotifyAdequately(world, px, py, pz, ModBlocks.LEAVES.get(), 0);
                }
            }
        }

        // Modern configured Mangrove/Tall Mangrove features attach hanging propagules to
        // leaves with probability 0.14, assigning a random age 0..4. This decorator remains
        // strictly inside manually grown trees; it is never registered as biome generation.
        if (ModBlocks.SAPLING.isEnabled()) {
            for (int dy = -2; dy <= 2; dy++) {
                int radius = dy == 2 ? 1 : dy == -2 ? 2 : 3;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        int px = x + dx, py = crownY + dy, pz = z + dz;
                        if (world.getBlock(px, py, pz) != ModBlocks.LEAVES.get()
                                || (world.getBlockMetadata(px, py, pz) & 3) != 0
                                || !world.isAirBlock(px, py - 1, pz) || random.nextFloat() >= 0.14F) continue;
                        BlockModernSapling.placeHangingPropagule(world, px, py - 1, pz, random.nextInt(5));
                    }
                }
            }
        }
        return true;
    }
}
