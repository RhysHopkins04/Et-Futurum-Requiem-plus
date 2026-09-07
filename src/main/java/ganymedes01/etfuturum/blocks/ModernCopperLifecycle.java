package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.Random;

/** Pass 36 bridge for modern lightning cleaning across EFR's shared IDegradable copper contract. */
public final class ModernCopperLifecycle {

    private ModernCopperLifecycle() { }

    /**
     * Minecraft Java 1.21.11 lightning cleaning:
     * - an unwaxed struck copper block is reset to its first oxidation stage;
     * - waxed copper is not itself cleaned;
     * - either kind of copper strike starts 3..5 short random walks which scrape nearby unwaxed
     *   copper by one stage at a time.
     *
     * This deliberately operates through IDegradable so mature EFR copper and Pass-36 parity
     * families count as one neighbourhood rather than creating a second copper framework.
     */
    public static void cleanCopperOnLightningStrike(World world, int x, int y, int z, Random random) {
        if (world == null || world.isRemote || !ConfigBlocksItems.enableModernMapParityBlocks) return;
        Block struck = world.getBlock(x, y, z);
        if (!(struck instanceof IDegradable)) return;

        IDegradable degradable = (IDegradable) struck;
        int worldMeta = world.getBlockMetadata(x, y, z);
        int copperMeta = degradable.getCopperMeta(world, x, y, z, worldMeta);
        if (copperMeta < 0 || copperMeta > 15) return;

        // Waxed copper starts the nearby cleaning walks but remains waxed itself.
        if (copperMeta < 8 && degradable.countTowardsDegredation(copperMeta, world, x, y, z)) {
            int firstMeta = copperMeta - (copperMeta % 4);
            if (firstMeta != copperMeta) {
                Block first = degradable.getCopperBlockFromMeta(firstMeta);
                if (first != null) {
                    degradable.setCopperBlock(first,
                            degradable.getFinalCopperMeta(world, x, y, z, firstMeta, worldMeta),
                            world, x, y, z);
                }
            }
        }

        int walks = random.nextInt(3) + 3;
        for (int walk = 0; walk < walks; walk++) {
            int steps = random.nextInt(8) + 1;
            randomWalkClean(world, x, y, z, steps, random);
        }
    }

    private static void randomWalkClean(World world, int startX, int startY, int startZ,
            int steps, Random random) {
        int x = startX;
        int y = startY;
        int z = startZ;
        for (int step = 0; step < steps; step++) {
            int[] next = randomStepClean(world, x, y, z, random);
            if (next == null) return;
            x = next[0];
            y = next[1];
            z = next[2];
        }
    }

    /** Mirrors BlockPos.randomInCube(..., 10, pos, 1): ten random candidates in the 3x3x3 cube. */
    private static int[] randomStepClean(World world, int x, int y, int z, Random random) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int nx = x + random.nextInt(3) - 1;
            int ny = y + random.nextInt(3) - 1;
            int nz = z + random.nextInt(3) - 1;
            Block block = world.getBlock(nx, ny, nz);
            if (!(block instanceof IDegradable)) continue;

            IDegradable degradable = (IDegradable) block;
            int worldMeta = world.getBlockMetadata(nx, ny, nz);
            int copperMeta = degradable.getCopperMeta(world, nx, ny, nz, worldMeta);
            if (copperMeta < 0 || copperMeta >= 8
                    || !degradable.countTowardsDegredation(copperMeta, world, nx, ny, nz)) continue;

            if ((copperMeta % 4) > 0) {
                int previousMeta = copperMeta - 1;
                Block previous = degradable.getCopperBlockFromMeta(previousMeta);
                if (previous != null) {
                    degradable.setCopperBlock(previous,
                            degradable.getFinalCopperMeta(world, nx, ny, nz, previousMeta, worldMeta),
                            world, nx, ny, nz);
                }
            }
            // Even fully-clean copper is a valid walk node in modern Java.
            return new int[]{nx, ny, nz};
        }
        return null;
    }
}
