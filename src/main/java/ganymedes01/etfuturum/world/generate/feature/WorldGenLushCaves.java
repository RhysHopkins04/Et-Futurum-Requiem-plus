package ganymedes01.etfuturum.world.generate.feature;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockBigDripleaf;
import ganymedes01.etfuturum.blocks.BlockSmallDripleaf;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenAzaleaTree;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenCaveVines;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.Facing;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 1.7.10 adaptation of the modern Lush Caves underground biome feature set.
 *
 * <p>1.7.10 has no 3D biome container, so this deliberately does not register a surface biome.
 * Instead, deterministic seed-based underground regions decorate already-carved Overworld cave
 * systems. Surface Azalea Trees mark those regions and grow a rooted-dirt system down toward a
 * real cave cavity below them. All writes are kept inside the currently populated chunk so this
 * generator does not trigger neighbouring chunk loads during population.</p>
 */
public class WorldGenLushCaves {

    private static final long REGION_SALT = 0x4C55534843415645L; // "LUSHCAVE"
    private static final long DECORATION_SALT = 0x4C5553484445434FL; // "LUSHDECO"
    private static final int CHUNK_SIZE = 16;
    private static final int INNER_MIN = 1;
    private static final int INNER_MAX = 14;

    private final WorldGenAzaleaTree azaleaTree = new WorldGenAzaleaTree(false);
    private final WorldGenCaveVines caveVines = ModBlocks.CAVE_VINE.isEnabled()
            ? new WorldGenCaveVines(ModBlocks.CAVE_VINE.get())
            : null;

    /** Decorates one already-generated Overworld chunk if it belongs to a lush region. */
    public boolean generateChunk(World world, int chunkX, int chunkZ) {
        if (world.provider.dimensionId != 0 || ConfigMapCompatibility.isEnabled() || !ConfigWorld.lushCavesWorldgen) {
            return false;
        }

        RegionAnchor anchor = findRegionAnchor(world, chunkX, chunkZ);
        if (anchor == null) {
            return false;
        }

        Random rand = new Random(mixSeed(world.getSeed() ^ DECORATION_SALT, chunkX, chunkZ,
                anchor.chunkX, anchor.chunkZ));
        boolean changed = false;

        // The marker/root pass runs only in the deterministic anchor chunk. It is intentionally
        // done before cave decoration so the cave detector sees the original carved surfaces.
        if (chunkX == anchor.chunkX && chunkZ == anchor.chunkZ) {
            changed |= generateSurfaceMarkerAndRoots(world, rand, chunkX, chunkZ);
        }

        changed |= decorateMossFloors(world, rand, chunkX, chunkZ, 52);
        changed |= decorateMossCeilings(world, rand, chunkX, chunkZ, 38);
        changed |= decorateCaveVines(world, rand, chunkX, chunkZ, 72);
        changed |= decorateSporeBlossoms(world, rand, chunkX, chunkZ, 14);
        changed |= decorateClassicVines(world, rand, chunkX, chunkZ, 16);
        changed |= decorateClayAndDripleaf(world, rand, chunkX, chunkZ, 10);
        return changed;
    }

    /**
     * Locates a deterministic region anchor without reading neighbouring chunks. Every chunk can
     * independently derive the same nearby anchor from only the world seed and chunk coordinates.
     */
    private RegionAnchor findRegionAnchor(World world, int chunkX, int chunkZ) {
        int radius = Math.max(1, ConfigWorld.lushCaveRegionRadiusChunks);
        RegionAnchor best = null;
        int bestDistance = Integer.MAX_VALUE;
        long bestTie = Long.MAX_VALUE;

        for (int ax = chunkX - radius; ax <= chunkX + radius; ax++) {
            for (int az = chunkZ - radius; az <= chunkZ + radius; az++) {
                int dx = ax - chunkX;
                int dz = az - chunkZ;
                int distance = dx * dx + dz * dz;
                if (distance > radius * radius || !isRegionAnchor(world.getSeed(), ax, az)) {
                    continue;
                }

                long tie = mixSeed(world.getSeed() ^ REGION_SALT, ax, az, 0, 0);
                if (best == null || distance < bestDistance || (distance == bestDistance && tie < bestTie)) {
                    best = new RegionAnchor(ax, az);
                    bestDistance = distance;
                    bestTie = tie;
                }
            }
        }
        return best;
    }

    private boolean isRegionAnchor(long worldSeed, int chunkX, int chunkZ) {
        int rarity = Math.max(1, ConfigWorld.lushCaveRarity);
        Random regionRand = new Random(mixSeed(worldSeed ^ REGION_SALT, chunkX, chunkZ, chunkZ, chunkX));
        return regionRand.nextInt(rarity) == 0;
    }

    private boolean generateSurfaceMarkerAndRoots(World world, Random rand, int chunkX, int chunkZ) {
        CavePoint cave = findMarkerCave(world, rand, chunkX, chunkZ);
        if (cave == null) {
            return false;
        }

        int surfaceY = world.getHeightValue(cave.x, cave.z);
        if (surfaceY <= cave.ceilingY + 5 || surfaceY >= world.getHeight() - 8) {
            return false;
        }

        Block ground = world.getBlock(cave.x, surfaceY - 1, cave.z);
        if (!canAzaleaTreeGrowOn(ground) || !world.isAirBlock(cave.x, surfaceY, cave.z)) {
            return false;
        }

        if (!azaleaTree.generate(world, rand, cave.x, surfaceY, cave.z)) {
            return false;
        }
        azaleaTree.func_150524_b(world, rand, cave.x, surfaceY, cave.z);
        generateRootSystem(world, rand, cave.x, surfaceY - 1, cave.z, cave.ceilingY, chunkX, chunkZ);
        return true;
    }

    /** Finds a proper enclosed cave column under the safe inner part of the anchor chunk. */
    private CavePoint findMarkerCave(World world, Random rand, int chunkX, int chunkZ) {
        int minY = effectiveMinY();
        int maxY = effectiveMaxY(world);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        // Randomise scan order while keeping candidate X/Z safely far enough from chunk borders
        // for the Azalea canopy and the radius-3 root system.
        int start = rand.nextInt(64);
        CavePoint best = null;
        for (int i = 0; i < 64; i++) {
            int index = (start + i) & 63;
            int x = baseX + 4 + (index & 7);
            int z = baseZ + 4 + ((index >> 3) & 7);
            CavePoint point = findHighestEnclosedCave(world, x, z, minY, maxY);
            if (point != null && (best == null || point.ceilingY > best.ceilingY)) {
                best = point;
            }
        }
        return best;
    }

    private CavePoint findHighestEnclosedCave(World world, int x, int z, int minY, int maxY) {
        int y = maxY;
        while (y >= minY) {
            if (!world.isAirBlock(x, y, z)) {
                y--;
                continue;
            }

            int top = y;
            while (top + 1 <= maxY + 12 && top + 1 < world.getHeight() - 2 && world.isAirBlock(x, top + 1, z)) {
                top++;
            }
            int bottom = y;
            while (bottom - 1 >= minY - 1 && world.isAirBlock(x, bottom - 1, z)) {
                bottom--;
            }

            int height = top - bottom + 1;
            Block floor = world.getBlock(x, bottom - 1, z);
            Block ceiling = world.getBlock(x, top + 1, z);
            if (height >= 3 && isLushGroundReplaceable(floor) && isRootReplaceable(ceiling)) {
                return new CavePoint(x, bottom, z, top + 1);
            }
            y = bottom - 1;
        }
        return null;
    }

    /**
     * Emulates the modern rooted_azalea_tree root-system feature: a long rooted-dirt column with
     * small lateral attempts and hanging roots where the column meets open cave space.
     */
    private void generateRootSystem(World world, Random rand, int startX, int startY, int startZ,
                                    int targetCeilingY, int chunkX, int chunkZ) {
        int x = startX;
        int z = startZ;
        int floorY = Math.max(targetCeilingY, startY - 100);

        for (int y = startY; y >= floorY; y--) {
            if (y > targetCeilingY + 4 && rand.nextInt(6) == 0) {
                x += rand.nextInt(3) - 1;
                z += rand.nextInt(3) - 1;
                x = clampToChunkInner(x, chunkX, 3);
                z = clampToChunkInner(z, chunkZ, 3);
            } else if (y <= targetCeilingY + 4) {
                x += Integer.compare(startX, x);
                z += Integer.compare(startZ, z);
            }

            Block block = world.getBlock(x, y, z);
            if (isRootReplaceable(block)) {
                setRootedDirt(world, x, y, z);
            } else if (world.isAirBlock(x, y, z)) {
                placeHangingRootIfPossible(world, x, y, z);
            }

            // Modern root_system uses 20 root placement attempts in radius 3. Spreading those
            // attempts through the vertical column produces the same irregular rooted silhouette.
            if (rand.nextInt(5) == 0) {
                int rx = clampToChunkInner(x + rand.nextInt(7) - 3, chunkX, 1);
                int rz = clampToChunkInner(z + rand.nextInt(7) - 3, chunkZ, 1);
                int ry = y + rand.nextInt(3) - 1;
                if (ry > 1 && ry < world.getHeight() - 1 && isRootReplaceable(world.getBlock(rx, ry, rz))) {
                    setRootedDirt(world, rx, ry, rz);
                    if (rand.nextBoolean()) {
                        placeHangingRootIfPossible(world, rx, ry - 1, rz);
                    }
                }
            }
        }

        // Concentrate hanging-root attempts around the actual cave ceiling, matching the modern
        // radius-3 / vertical-span-2 root-system settings.
        if (ModBlocks.HANGING_ROOTS.isEnabled()) {
            for (int i = 0; i < 20; i++) {
                int rx = clampToChunkInner(startX + rand.nextInt(7) - 3, chunkX, 1);
                int rz = clampToChunkInner(startZ + rand.nextInt(7) - 3, chunkZ, 1);
                int ry = targetCeilingY - rand.nextInt(2);
                placeHangingRootIfPossible(world, rx, ry, rz);
            }
        }
    }

    private boolean decorateMossFloors(World world, Random rand, int chunkX, int chunkZ, int attempts) {
        if (!ModBlocks.MOSS_BLOCK.isEnabled()) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < attempts; i++) {
            int x = randomInnerX(rand, chunkX, 2);
            int z = randomInnerZ(rand, chunkZ, 2);
            int airY = findFloorAir(world, rand, x, z);
            if (airY == Integer.MIN_VALUE) {
                continue;
            }
            changed |= growMossFloorPatch(world, rand, x, airY, z, chunkX, chunkZ);
        }
        return changed;
    }

    private boolean growMossFloorPatch(World world, Random rand, int x, int airY, int z, int chunkX, int chunkZ) {
        boolean changed = false;
        int radius = 1 + rand.nextInt(2);
        for (int ox = -radius; ox <= radius; ox++) {
            for (int oz = -radius; oz <= radius; oz++) {
                if (ox * ox + oz * oz > radius * radius + 1) {
                    continue;
                }
                int px = x + ox;
                int pz = z + oz;
                if (!isInsideChunkInner(px, pz, chunkX, chunkZ, 1)) {
                    continue;
                }
                int py = findFloorNear(world, px, airY, pz, 3);
                if (py == Integer.MIN_VALUE) {
                    continue;
                }
                Block floor = world.getBlock(px, py - 1, pz);
                if (isMossReplaceable(floor) && rand.nextFloat() < 0.84F) {
                    world.setBlock(px, py - 1, pz, ModBlocks.MOSS_BLOCK.get(), 0, 2);
                    changed = true;
                }
                if (world.getBlock(px, py - 1, pz) == ModBlocks.MOSS_BLOCK.get() && rand.nextFloat() < 0.60F) {
                    changed |= placeLushVegetation(world, rand, px, py, pz);
                }
            }
        }
        return changed;
    }

    private boolean decorateMossCeilings(World world, Random rand, int chunkX, int chunkZ, int attempts) {
        if (!ModBlocks.MOSS_BLOCK.isEnabled()) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < attempts; i++) {
            int x = randomInnerX(rand, chunkX, 2);
            int z = randomInnerZ(rand, chunkZ, 2);
            int airY = findCeilingAir(world, rand, x, z);
            if (airY == Integer.MIN_VALUE) {
                continue;
            }
            int radius = 1 + rand.nextInt(2);
            for (int ox = -radius; ox <= radius; ox++) {
                for (int oz = -radius; oz <= radius; oz++) {
                    if (ox * ox + oz * oz > radius * radius + 1) {
                        continue;
                    }
                    int px = x + ox;
                    int pz = z + oz;
                    if (!isInsideChunkInner(px, pz, chunkX, chunkZ, 1)) {
                        continue;
                    }
                    int py = findCeilingNear(world, px, airY, pz, 3);
                    if (py == Integer.MIN_VALUE) {
                        continue;
                    }
                    Block ceiling = world.getBlock(px, py + 1, pz);
                    if (isMossReplaceable(ceiling) && rand.nextFloat() < 0.78F) {
                        world.setBlock(px, py + 1, pz, ModBlocks.MOSS_BLOCK.get(), 0, 2);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private boolean decorateCaveVines(World world, Random rand, int chunkX, int chunkZ, int attempts) {
        if (caveVines == null) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < attempts; i++) {
            int x = randomInnerX(rand, chunkX, 1);
            int z = randomInnerZ(rand, chunkZ, 1);
            int y = findCeilingAir(world, rand, x, z);
            if (y != Integer.MIN_VALUE && rand.nextFloat() < 0.72F) {
                changed |= caveVines.generate(world, rand, x, y, z);
            }
        }
        return changed;
    }

    private boolean decorateSporeBlossoms(World world, Random rand, int chunkX, int chunkZ, int attempts) {
        if (!ModBlocks.SPORE_BLOSSOM.isEnabled()) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < attempts; i++) {
            int x = randomInnerX(rand, chunkX, 1);
            int z = randomInnerZ(rand, chunkZ, 1);
            int y = findCeilingAir(world, rand, x, z);
            if (y != Integer.MIN_VALUE && rand.nextFloat() < 0.48F
                    && ModBlocks.SPORE_BLOSSOM.get().canPlaceBlockAt(world, x, y, z)) {
                world.setBlock(x, y, z, ModBlocks.SPORE_BLOSSOM.get(), 0, 2);
                changed = true;
            }
        }
        return changed;
    }

    private boolean decorateClassicVines(World world, Random rand, int chunkX, int chunkZ, int attempts) {
        boolean changed = false;
        for (int i = 0; i < attempts; i++) {
            int x = randomInnerX(rand, chunkX, 1);
            int z = randomInnerZ(rand, chunkZ, 1);
            int y = randomCaveY(rand, world);
            if (!world.isAirBlock(x, y, z)) {
                continue;
            }
            for (int side = 2; side <= 5; side++) {
                if (!Blocks.vine.canPlaceBlockOnSide(world, x, y, z, side)) {
                    continue;
                }
                int meta = 1 << Direction.facingToDirection[Facing.oppositeSide[side]];
                int length = 1 + rand.nextInt(4);
                for (int dy = 0; dy < length && y - dy > 1 && world.isAirBlock(x, y - dy, z); dy++) {
                    world.setBlock(x, y - dy, z, Blocks.vine, meta, 2);
                    changed = true;
                }
                break;
            }
        }
        return changed;
    }

    /** Adds both dry clay/dripleaf patches and safely recessed one-block water pools. */
    private boolean decorateClayAndDripleaf(World world, Random rand, int chunkX, int chunkZ, int attempts) {
        boolean changed = false;
        for (int i = 0; i < attempts; i++) {
            int x = randomInnerX(rand, chunkX, 4);
            int z = randomInnerZ(rand, chunkZ, 4);
            int airY = findFloorAir(world, rand, x, z);
            if (airY == Integer.MIN_VALUE) {
                continue;
            }

            int radius = 2 + rand.nextInt(3);
            boolean waterPool = rand.nextBoolean();
            changed |= makeClayPatch(world, rand, x, airY, z, radius, waterPool, chunkX, chunkZ);
        }
        return changed;
    }

    private boolean makeClayPatch(World world, Random rand, int x, int airY, int z, int radius,
                                  boolean waterPool, int chunkX, int chunkZ) {
        boolean changed = false;
        int groundY = airY - 1;

        for (int ox = -radius; ox <= radius; ox++) {
            for (int oz = -radius; oz <= radius; oz++) {
                if (ox * ox + oz * oz > radius * radius + 1) {
                    continue;
                }
                int px = x + ox;
                int pz = z + oz;
                if (!isInsideChunkInner(px, pz, chunkX, chunkZ, 1)
                        || !world.isAirBlock(px, groundY + 1, pz)) {
                    continue;
                }
                Block ground = world.getBlock(px, groundY, pz);
                if (!isLushGroundReplaceable(ground)) {
                    continue;
                }
                world.setBlock(px, groundY, pz, Blocks.clay, 0, 2);
                if (groundY > 1 && isLushGroundReplaceable(world.getBlock(px, groundY - 1, pz)) && rand.nextFloat() < 0.80F) {
                    world.setBlock(px, groundY - 1, pz, Blocks.clay, 0, 2);
                }
                changed = true;
            }
        }

        // In 1.7.10 Dripleaf cannot truly waterlog. Recess a small source-water basin one block
        // into an otherwise solid clay floor so cave water does not spill through the whole cavern;
        // Dripleaf is then placed on the dry clay rim, preserving the modern visual relationship.
        if (waterPool && canMakeRecessedPool(world, x, groundY, z)) {
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    if (Math.abs(ox) == 1 && Math.abs(oz) == 1) {
                        continue;
                    }
                    world.setBlock(x + ox, groundY - 1, z + oz, Blocks.clay, 0, 2);
                    world.setBlock(x + ox, groundY, z + oz, Blocks.water, 0, 2);
                    changed = true;
                }
            }
        }

        int dripleafAttempts = 2 + rand.nextInt(4);
        for (int i = 0; i < dripleafAttempts; i++) {
            int px = x + rand.nextInt(radius * 2 + 1) - radius;
            int pz = z + rand.nextInt(radius * 2 + 1) - radius;
            if (!isInsideChunkInner(px, pz, chunkX, chunkZ, 1) || world.getBlock(px, groundY, pz) != Blocks.clay) {
                continue;
            }
            if (rand.nextBoolean()) {
                changed |= placeSmallDripleaf(world, rand, px, groundY + 1, pz);
            } else {
                changed |= placeBigDripleaf(world, rand, px, groundY + 1, pz);
            }
        }
        return changed;
    }

    private boolean canMakeRecessedPool(World world, int x, int groundY, int z) {
        if (groundY <= 2) {
            return false;
        }
        for (int ox = -2; ox <= 2; ox++) {
            for (int oz = -2; oz <= 2; oz++) {
                if (Math.abs(ox) == 2 && Math.abs(oz) == 2) {
                    continue;
                }
                Block ground = world.getBlock(x + ox, groundY, z + oz);
                if (!isLushGroundReplaceable(ground) && ground != Blocks.clay) {
                    return false;
                }
                if (!world.isAirBlock(x + ox, groundY + 1, z + oz)) {
                    return false;
                }
                if (world.isAirBlock(x + ox, groundY - 1, z + oz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean placeSmallDripleaf(World world, Random rand, int x, int y, int z) {
        if (!ModBlocks.SMALL_DRIPLEAF.isEnabled() || y + 1 >= world.getHeight()
                || !world.isAirBlock(x, y, z) || !world.isAirBlock(x, y + 1, z)) {
            return false;
        }
        int facing = rand.nextInt(4);
        world.setBlock(x, y, z, ModBlocks.SMALL_DRIPLEAF.get(), BlockSmallDripleaf.makeMeta(facing, false), 2);
        world.setBlock(x, y + 1, z, ModBlocks.SMALL_DRIPLEAF.get(), BlockSmallDripleaf.makeMeta(facing, true), 2);
        return true;
    }

    private boolean placeBigDripleaf(World world, Random rand, int x, int y, int z) {
        if (!ModBlocks.BIG_DRIPLEAF.isEnabled() || !ModBlocks.BIG_DRIPLEAF_STEM.isEnabled()) {
            return false;
        }
        int desiredHeight = 1 + rand.nextInt(5); // modern configured feature: 0-4 stems + one top
        int height = 0;
        while (height < desiredHeight && y + height < world.getHeight() - 1 && world.isAirBlock(x, y + height, z)) {
            height++;
        }
        if (height <= 0) {
            return false;
        }

        int facing = rand.nextInt(4);
        for (int i = 0; i < height - 1; i++) {
            world.setBlock(x, y + i, z, ModBlocks.BIG_DRIPLEAF_STEM.get(), facing, 2);
        }
        world.setBlock(x, y + height - 1, z, ModBlocks.BIG_DRIPLEAF.get(),
                BlockBigDripleaf.makeMeta(facing, BlockBigDripleaf.TILT_NONE), 2);
        return true;
    }

    private boolean placeLushVegetation(World world, Random rand, int x, int y, int z) {
        if (!world.isAirBlock(x, y, z)) {
            return false;
        }

        // Modern moss_vegetation weights: short grass 50, tall grass 10, moss carpet 25,
        // azalea 7, flowering azalea 4.
        int roll = rand.nextInt(96);
        if (roll < 50) {
            if (Blocks.tallgrass.canBlockStay(world, x, y, z)) {
                world.setBlock(x, y, z, Blocks.tallgrass, 1, 2);
                return true;
            }
        } else if (roll < 60) {
            if (y + 1 < world.getHeight() && world.isAirBlock(x, y + 1, z)
                    && Blocks.double_plant.canBlockStay(world, x, y, z)) {
                Blocks.double_plant.func_149889_c(world, x, y, z, 2, 2);
                return true;
            }
        } else if (roll < 85 && ModBlocks.MOSS_CARPET.isEnabled()) {
            if (ModBlocks.MOSS_CARPET.get().canBlockStay(world, x, y, z)) {
                world.setBlock(x, y, z, ModBlocks.MOSS_CARPET.get(), 0, 2);
                return true;
            }
        } else if (ModBlocks.AZALEA.isEnabled()) {
            int meta = roll < 92 ? 0 : 1;
            if (ModBlocks.AZALEA.get().canBlockStay(world, x, y, z)) {
                world.setBlock(x, y, z, ModBlocks.AZALEA.get(), meta, 2);
                return true;
            }
        }
        return false;
    }

    private int findFloorAir(World world, Random rand, int x, int z) {
        int y = randomCaveY(rand, world);
        if (!world.isAirBlock(x, y, z)) {
            return Integer.MIN_VALUE;
        }
        for (int step = 0; step <= 12 && y - step > 1; step++) {
            int py = y - step;
            if (!world.isAirBlock(x, py, z)) {
                return Integer.MIN_VALUE;
            }
            Block below = world.getBlock(x, py - 1, z);
            if (!below.isAir(world, x, py - 1, z)) {
                return isLushGroundReplaceable(below) ? py : Integer.MIN_VALUE;
            }
        }
        return Integer.MIN_VALUE;
    }

    private int findCeilingAir(World world, Random rand, int x, int z) {
        int y = randomCaveY(rand, world);
        if (!world.isAirBlock(x, y, z)) {
            return Integer.MIN_VALUE;
        }
        for (int step = 0; step <= 12 && y + step < world.getHeight() - 1; step++) {
            int py = y + step;
            if (!world.isAirBlock(x, py, z)) {
                return Integer.MIN_VALUE;
            }
            Block above = world.getBlock(x, py + 1, z);
            if (!above.isAir(world, x, py + 1, z)) {
                return isRootReplaceable(above) || above == ModBlocks.MOSS_BLOCK.get() ? py : Integer.MIN_VALUE;
            }
        }
        return Integer.MIN_VALUE;
    }

    private int findFloorNear(World world, int x, int y, int z, int range) {
        for (int py = Math.min(effectiveMaxY(world), y + range); py >= Math.max(effectiveMinY(), y - range); py--) {
            if (world.isAirBlock(x, py, z) && isLushGroundReplaceable(world.getBlock(x, py - 1, z))) {
                return py;
            }
        }
        return Integer.MIN_VALUE;
    }

    private int findCeilingNear(World world, int x, int y, int z, int range) {
        for (int py = Math.max(effectiveMinY(), y - range); py <= Math.min(effectiveMaxY(world), y + range); py++) {
            Block above = world.getBlock(x, py + 1, z);
            if (world.isAirBlock(x, py, z) && (isRootReplaceable(above) || above == ModBlocks.MOSS_BLOCK.get())) {
                return py;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static boolean isMossReplaceable(Block block) {
        if (block == Blocks.stone || block == Blocks.dirt || block == Blocks.grass) {
            return true;
        }
        if (ModBlocks.STONE.isEnabled() && block == ModBlocks.STONE.get()) {
            return true;
        }
        if (ModBlocks.DEEPSLATE.isEnabled() && block == ModBlocks.DEEPSLATE.get()) {
            return true;
        }
        return ModBlocks.TUFF.isEnabled() && block == ModBlocks.TUFF.get();
    }

    private static boolean isLushGroundReplaceable(Block block) {
        return isMossReplaceable(block)
                || block == Blocks.clay
                || block == Blocks.gravel
                || block == Blocks.sand
                || (ModBlocks.MOSS_BLOCK.isEnabled() && block == ModBlocks.MOSS_BLOCK.get());
    }

    private static boolean isRootReplaceable(Block block) {
        return isLushGroundReplaceable(block)
                || block == Blocks.hardened_clay
                || block == Blocks.stained_hardened_clay
                || block == Blocks.mycelium
                || (ModBlocks.ROOTED_DIRT.isEnabled() && block == ModBlocks.ROOTED_DIRT.get());
    }

    private static boolean canAzaleaTreeGrowOn(Block block) {
        return block == Blocks.dirt
                || block == Blocks.grass
                || block == Blocks.mycelium
                || block == Blocks.sand
                || block == Blocks.hardened_clay
                || block == Blocks.stained_hardened_clay
                || block == Blocks.snow
                || (ModBlocks.COARSE_DIRT.isEnabled() && block == ModBlocks.COARSE_DIRT.get())
                || (ModBlocks.MOSS_BLOCK.isEnabled() && block == ModBlocks.MOSS_BLOCK.get())
                || (ModBlocks.ROOTED_DIRT.isEnabled() && block == ModBlocks.ROOTED_DIRT.get());
    }

    private static void setRootedDirt(World world, int x, int y, int z) {
        if (ModBlocks.ROOTED_DIRT.isEnabled()) {
            world.setBlock(x, y, z, ModBlocks.ROOTED_DIRT.get(), 0, 2);
        }
    }

    private static void placeHangingRootIfPossible(World world, int x, int y, int z) {
        if (!ModBlocks.HANGING_ROOTS.isEnabled() || y <= 0 || y >= world.getHeight() - 1 || !world.isAirBlock(x, y, z)) {
            return;
        }
        Block above = world.getBlock(x, y + 1, z);
        if (above == ModBlocks.ROOTED_DIRT.get() || above == ModBlocks.MOSS_BLOCK.get()) {
            world.setBlock(x, y, z, ModBlocks.HANGING_ROOTS.get(), 0, 2);
        }
    }

    private int randomCaveY(Random rand, World world) {
        int min = effectiveMinY();
        int max = effectiveMaxY(world);
        return min + rand.nextInt(Math.max(1, max - min + 1));
    }

    private int effectiveMinY() {
        return Math.max(4, Math.min(ConfigWorld.lushCaveMinY, ConfigWorld.lushCaveMaxY));
    }

    private int effectiveMaxY(World world) {
        return Math.min(world.getHeight() - 8, Math.max(ConfigWorld.lushCaveMinY, ConfigWorld.lushCaveMaxY));
    }

    private static int randomInnerX(Random rand, int chunkX, int margin) {
        int localMin = Math.max(INNER_MIN, margin);
        int localMax = Math.min(INNER_MAX, 15 - margin);
        return (chunkX << 4) + localMin + rand.nextInt(localMax - localMin + 1);
    }

    private static int randomInnerZ(Random rand, int chunkZ, int margin) {
        int localMin = Math.max(INNER_MIN, margin);
        int localMax = Math.min(INNER_MAX, 15 - margin);
        return (chunkZ << 4) + localMin + rand.nextInt(localMax - localMin + 1);
    }

    private static boolean isInsideChunkInner(int x, int z, int chunkX, int chunkZ, int margin) {
        int minX = (chunkX << 4) + margin;
        int maxX = (chunkX << 4) + CHUNK_SIZE - 1 - margin;
        int minZ = (chunkZ << 4) + margin;
        int maxZ = (chunkZ << 4) + CHUNK_SIZE - 1 - margin;
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private static int clampToChunkInner(int coord, int chunk, int margin) {
        int min = (chunk << 4) + margin;
        int max = (chunk << 4) + CHUNK_SIZE - 1 - margin;
        return Math.max(min, Math.min(max, coord));
    }

    private static long mixSeed(long seed, int a, int b, int c, int d) {
        long mixed = seed;
        mixed ^= (long) a * 341873128712L;
        mixed ^= (long) b * 132897987541L;
        mixed ^= (long) c * 42317861L;
        mixed ^= (long) d * 374761393L;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return mixed;
    }

    private static final class RegionAnchor {
        final int chunkX;
        final int chunkZ;

        RegionAnchor(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }

    private static final class CavePoint {
        final int x;
        final int floorAirY;
        final int z;
        final int ceilingY;

        CavePoint(int x, int floorAirY, int z, int ceilingY) {
            this.x = x;
            this.floorAirY = floorAirY;
            this.z = z;
            this.ceilingY = ceilingY;
        }
    }
}
