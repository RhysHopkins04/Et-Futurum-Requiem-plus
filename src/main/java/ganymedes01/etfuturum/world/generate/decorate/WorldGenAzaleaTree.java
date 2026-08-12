package ganymedes01.etfuturum.world.generate.decorate;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Java 1.7.10 backport of the modern azalea-tree configured feature.
 *
 * Modern azalea trees use oak logs, a short bending trunk, irregular azalea foliage
 * with a 3:1 normal-to-flowering leaf ratio, and rooted dirt at the tree base.
 * This generator intentionally provides only the tree feature itself; natural lush-cave
 * placement and the long root-system marker are handled by the later lush-cave worldgen pass.
 */
public class WorldGenAzaleaTree extends WorldGenAbstractTree {

    public WorldGenAzaleaTree(boolean notify) {
        super(notify);
    }

    @Override
    public boolean generate(World world, Random rand, int x, int y, int z) {
        int height = 4 + rand.nextInt(3); // modern base 4 + up to 2
        int bendLength = 1 + rand.nextInt(2);
        int bendStart = Math.max(3, height - bendLength);
        int direction = rand.nextInt(4);
        int dx = direction == 1 ? 1 : direction == 3 ? -1 : 0;
        int dz = direction == 0 ? -1 : direction == 2 ? 1 : 0;

        List<int[]> trunk = buildTrunk(x, y, z, height, bendStart, dx, dz);
        int[] top = trunk.get(trunk.size() - 1);

        if (!hasRoom(world, trunk, top[0], top[1], top[2])) {
            return false;
        }

        Block rooted = ModBlocks.ROOTED_DIRT.isEnabled() ? ModBlocks.ROOTED_DIRT.get() : Blocks.dirt;
        setBlockAndNotifyAdequately(world, x, y - 1, z, rooted, 0);

        // The modern configured feature explicitly supplies oak_log[axis=y]. The trunk
        // may wander horizontally near its crown, but each placed log stays vertical.
        for (int[] pos : trunk) {
            setBlockAndNotifyAdequately(world, pos[0], pos[1], pos[2], Blocks.log, 0);
        }

        generateFoliage(world, rand, top[0], top[1], top[2]);
        return true;
    }

    private static List<int[]> buildTrunk(int x, int y, int z, int height, int bendStart, int dx, int dz) {
        List<int[]> result = new ArrayList<>();
        int tx = x;
        int tz = z;
        for (int i = 0; i < height; i++) {
            if (i >= bendStart) {
                tx += dx;
                tz += dz;
            }
            result.add(new int[]{tx, y + i, tz});
        }
        return result;
    }

    private boolean hasRoom(World world, List<int[]> trunk, int topX, int topY, int topZ) {
        for (int[] pos : trunk) {
            if (pos[1] < 1 || pos[1] >= world.getHeight() - 2 || !isTreeReplaceable(world, pos[0], pos[1], pos[2])) {
                return false;
            }
        }

        // The modern random-spread foliage placer has radius 3 and height 2.
        for (int dy = -1; dy <= 1; dy++) {
            int radius = dy == 1 ? 2 : 3;
            for (int ox = -radius; ox <= radius; ox++) {
                for (int oz = -radius; oz <= radius; oz++) {
                    if (ox * ox + oz * oz > radius * radius + 1) {
                        continue;
                    }
                    if (!isLeafReplaceable(world, topX + ox, topY + dy, topZ + oz)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isTreeReplaceable(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block.isAir(world, x, y, z)
                || block.isLeaves(world, x, y, z)
                || block.isReplaceable(world, x, y, z)
                || block == ModBlocks.AZALEA.get();
    }

    private static boolean isLeafReplaceable(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block.isAir(world, x, y, z)
                || block.isLeaves(world, x, y, z)
                || block.isReplaceable(world, x, y, z);
    }

    private void generateFoliage(World world, Random rand, int x, int y, int z) {
        generateLeafLayer(world, rand, x, y - 1, z, 3, 0.22F);
        generateLeafLayer(world, rand, x, y, z, 3, 0.30F);
        generateLeafLayer(world, rand, x, y + 1, z, 2, 0.35F);

        // A few interior attempts emulate the modern random-spread foliage placer's
        // irregular fill without forcing a perfect sphere.
        for (int i = 0; i < 18; i++) {
            int ox = rand.nextInt(5) - 2;
            int oz = rand.nextInt(5) - 2;
            int oy = rand.nextInt(3) - 1;
            if (ox * ox + oz * oz <= 6) {
                placeLeaf(world, rand, x + ox, y + oy, z + oz);
            }
        }
    }

    private void generateLeafLayer(World world, Random rand, int x, int y, int z, int radius, float edgeHoleChance) {
        for (int ox = -radius; ox <= radius; ox++) {
            for (int oz = -radius; oz <= radius; oz++) {
                int dist = ox * ox + oz * oz;
                if (dist > radius * radius + 1) {
                    continue;
                }
                boolean edge = Math.abs(ox) == radius || Math.abs(oz) == radius || dist >= radius * radius;
                if (edge && rand.nextFloat() < edgeHoleChance) {
                    continue;
                }
                placeLeaf(world, rand, x + ox, y, z + oz);
            }
        }
    }

    private void placeLeaf(World world, Random rand, int x, int y, int z) {
        if (!isLeafReplaceable(world, x, y, z)) {
            return;
        }
        int meta = rand.nextInt(4) == 0 ? 1 : 0; // modern weighted provider: 3 normal : 1 flowering
        setBlockAndNotifyAdequately(world, x, y, z, ModBlocks.AZALEA_LEAVES.get(), meta);
    }
}
