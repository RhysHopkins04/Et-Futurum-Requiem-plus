package ganymedes01.etfuturum.world.generate.feature;

import java.util.Random;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockPointedDripstone;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import ganymedes01.etfuturum.world.generate.terrain.ModernOverworldCaveRegionSource;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * P008e-c chamber-biased surface-driven Dripstone Cave decorator for the modern Overworld.
 *
 * <p>Unlike the unfinished upstream generator, this never infers cave biome ownership from the 2D
 * surface biome or legacy surface-biome climate values. It queries the same independent 3D cave-region
 * field used by P008d Lush Caves, scans surfaces that actually exist, and decorates only DRIPSTONE
 * volumes. This keeps the 384-high translated cave geometry and aquifers authoritative.</p>
 */
public final class WorldGenDripstoneCaves {

    private static final int INNER_MIN = 1;
    private static final int INNER_MAX = 14;

    private static final long SURFACE_SALT = 0x4452495053555246L; // "DRIPSURF"
    private static final long POINT_SALT = 0x44524950504F494EL;   // "DRIPPOIN"
    private static final long LARGE_SALT = 0x445249504C415247L;   // "DRIPLARG"
    private static final long WALL_SALT = 0x4452495057414C4CL;    // "DRIPWALL"
    private static final long BLOCK_PATCH_SALT = 0x44524950424C4F43L; // "DRIPBLOC"
    private static final long WATER_SALT = 0x4452495057415445L;      // "DRIPWATE"

    // Ignore the weakest ownership fringe when decorating. Region ownership itself is already
    // concentrated by P008e-b, and this extra floor prevents isolated Dripstone Block speckles.
    private static final double MIN_DECORATION_STRENGTH = 0.015D;

    // P008e-c: pointed formations belong primarily in real chambers, not cramped spaghetti/noodle
    // passages. Dripstone Block may still lightly crust a small tunnel, but stalactites/stalagmites
    // and the heaviest wall takeover require useful vertical + lateral room.
    private static final int MIN_POINTED_CAVITY_HEIGHT = 5;
    private static final int MIN_LARGE_CAVITY_HEIGHT = 12;
    private static final int CHAMBER_PROBE_RADIUS = 5;
    private static final double MIN_POINTED_CHAMBER_FACTOR = 0.28D;
    private static final double MIN_LARGE_CHAMBER_FACTOR = 0.50D;

    private ModernOverworldCaveRegionSource regionSource;
    private long regionSeed = Long.MIN_VALUE;
    private int lushMin;
    private int lushMax;
    private int dripMin;
    private int dripMax;

    public boolean generateChunk(World world, int chunkX, int chunkZ) {
        if (world == null || world.provider == null || world.provider.dimensionId != 0
                || !ConfigWorld.modernOverworldGeneration || !ConfigWorld.dripstoneCavesWorldgen
                || ConfigMapCompatibility.isEnabled() || !ModBlocks.DRIPSTONE_BLOCK.isEnabled()
                || !ModBlocks.POINTED_DRIPSTONE.isEnabled()) {
            return false;
        }

        ModernOverworldCaveRegionSource source = regionSource(world);
        if (!chunkHasDripstoneRegion(source, chunkX, chunkZ)) {
            return false;
        }

        // Water basins are placed before surface/formation decoration so later pointed-dripstone
        // placement cannot turn a valid contained basin into an unsafe source-water edge.
        boolean changed = decorateWaterBasins(world, source, chunkX, chunkZ);
        changed |= decorateSurfaces(world, source, chunkX, chunkZ);
        changed |= decorateLargeFormations(world, source, chunkX, chunkZ);
        return changed;
    }

    /** Adds sparse, safely recessed floor-water basins inside true Dripstone regions. */
    private boolean decorateWaterBasins(World world, ModernOverworldCaveRegionSource source,
            int chunkX, int chunkZ) {
        long chunkSeed = mix64(world.getSeed() ^ WATER_SALT
                ^ ((long) chunkX * 341873128712L) ^ ((long) chunkZ * 132897987541L));
        // Modern Dripstone Caves can contain water pools, but they are an accent rather than an
        // aquifer-sized defining feature.  Roughly one in six eligible chunks gets a basin attempt.
        if ((chunkSeed & Long.MAX_VALUE) % 6L != 0L) {
            return false;
        }

        Random rand = new Random(chunkSeed);
        int x = (chunkX << 4) + 4 + rand.nextInt(8);
        int z = (chunkZ << 4) + 4 + rand.nextInt(8);
        Cavity cavity = findBestCavity(world, source, x, z);
        if (cavity == null || cavity.height() < 6 || cavity.strength < 0.025D
                || cavity.chamberFactor < 0.24D) {
            return false;
        }

        int radiusX = 2 + rand.nextInt(3);
        int radiusZ = 2 + rand.nextInt(3);
        boolean changed = false;
        for (int ox = -radiusX; ox <= radiusX; ox++) {
            for (int oz = -radiusZ; oz <= radiusZ; oz++) {
                int px = x + ox;
                int pz = z + oz;
                if (!isInsideChunkInner(px, pz, chunkX, chunkZ, 2)) {
                    continue;
                }
                double nx = ox / Math.max(1.0D, radiusX);
                double nz = oz / Math.max(1.0D, radiusZ);
                double edge = 0.72D + coordinateChance(world.getSeed() ^ WATER_SALT, px, cavity.floorAirY, pz) * 0.36D;
                if (nx * nx + nz * nz > edge) {
                    continue;
                }

                int airY = findFloorNear(world, source, px, cavity.floorAirY, pz, 3);
                if (airY == Integer.MIN_VALUE || !canCarveWaterCell(world, source, px, airY, pz)) {
                    continue;
                }
                int groundY = airY - 1;
                Block below = world.getBlock(px, groundY - 1, pz);
                if (isReplaceableStone(below)) {
                    world.setBlock(px, groundY - 1, pz, ModBlocks.DRIPSTONE_BLOCK.get(), 0, 2);
                }
                world.setBlock(px, groundY, pz, Blocks.water, 0, 2);
                changed = true;
            }
        }
        return changed;
    }

    private int findFloorNear(World world, ModernOverworldCaveRegionSource source,
            int x, int referenceAirY, int z, int radius) {
        for (int delta = 0; delta <= radius; delta++) {
            int down = referenceAirY - delta;
            if (isDripstoneFloorAir(world, source, x, down, z)) {
                return down;
            }
            if (delta > 0) {
                int up = referenceAirY + delta;
                if (isDripstoneFloorAir(world, source, x, up, z)) {
                    return up;
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    private boolean isDripstoneFloorAir(World world, ModernOverworldCaveRegionSource source,
            int x, int airY, int z) {
        return airY > 2 && airY < world.getActualHeight() - 2
                && world.isAirBlock(x, airY, z)
                && world.isAirBlock(x, airY + 1, z)
                && source.isDripstone(x, airY, z)
                && isReplaceableStone(world.getBlock(x, airY - 1, z));
    }

    private boolean canCarveWaterCell(World world, ModernOverworldCaveRegionSource source,
            int x, int airY, int z) {
        int groundY = airY - 1;
        if (!source.isDripstone(x, airY, z) || !world.isAirBlock(x, airY, z)
                || !isReplaceableStone(world.getBlock(x, groundY, z))) {
            return false;
        }
        Block below = world.getBlock(x, groundY - 1, z);
        if (below.isAir(world, x, groundY - 1, z) || below.getMaterial().isLiquid()) {
            return false;
        }
        final int[] dx = {1, -1, 0, 0};
        final int[] dz = {0, 0, 1, -1};
        for (int i = 0; i < 4; i++) {
            int sx = x + dx[i];
            int sz = z + dz[i];
            Block side = world.getBlock(sx, groundY, sz);
            if (side.isAir(world, sx, groundY, sz)) {
                return false;
            }
            if (side.getMaterial().isLiquid() && side != Blocks.water && side != Blocks.flowing_water) {
                return false;
            }
        }
        return true;
    }

    private boolean isInsideChunkInner(int x, int z, int chunkX, int chunkZ, int margin) {
        int minX = (chunkX << 4) + margin;
        int minZ = (chunkZ << 4) + margin;
        return x >= minX && x <= (chunkX << 4) + 15 - margin
                && z >= minZ && z <= (chunkZ << 4) + 15 - margin;
    }

    private boolean decorateSurfaces(World world, ModernOverworldCaveRegionSource source, int chunkX, int chunkZ) {
        final int baseX = chunkX << 4;
        final int baseZ = chunkZ << 4;
        final int minY = WorldHeightCompat.modernToPhysicalY(Math.max(WorldHeightCompat.MODERN_MIN_Y,
                Math.min(ConfigWorld.modernDripstoneCaveMinY, ConfigWorld.modernDripstoneCaveMaxY)));
        final int maxY = Math.min(world.getActualHeight() - 2,
                WorldHeightCompat.modernToPhysicalY(Math.min(WorldHeightCompat.MODERN_MAX_Y,
                        Math.max(ConfigWorld.modernDripstoneCaveMinY, ConfigWorld.modernDripstoneCaveMaxY))));
        final long seed = world.getSeed();
        final BlockPointedDripstone pointed = (BlockPointedDripstone) ModBlocks.POINTED_DRIPSTONE.get();
        boolean changed = false;

        for (int localX = INNER_MIN; localX <= INNER_MAX; localX++) {
            int x = baseX + localX;
            for (int localZ = INNER_MIN; localZ <= INNER_MAX; localZ++) {
                int z = baseZ + localZ;
                for (int y = Math.max(2, minY); y <= maxY; y++) {
                    if (!world.isAirBlock(x, y, z) || !source.isDripstone(x, y, z)) {
                        continue;
                    }

                    double strength = source.dripstoneStrength(x, y, z);
                    if (strength < MIN_DECORATION_STRENGTH) {
                        continue;
                    }
                    double core = coreFactor(strength);
                    Block floor = world.getBlock(x, y - 1, z);
                    Block ceiling = world.getBlock(x, y + 1, z);
                    boolean floorSurface = isReplaceableStone(floor);
                    boolean ceilingSurface = isReplaceableStone(ceiling);
                    boolean wallSurface = hasReplaceableWall(world, x, y, z);
                    if (!floorSurface && !ceilingSurface && !wallSurface) {
                        continue;
                    }

                    // Only pay for the openness probe where there is an exposed cave surface or a
                    // possible wall. A low chamber factor means a short/narrow tunnel; those retain
                    // occasional Dripstone Block crust but are intentionally poor pointed-dripstone
                    // habitat. Large caverns rise rapidly toward 1.0.
                    double chamber = chamberFactor(world, x, y, z);
                    double patch = coordinateChance(seed ^ BLOCK_PATCH_SALT, x >> 2, y >> 2, z >> 2);
                    double chamberBlockBias = 0.58D + chamber * 0.52D;

                    if (floorSurface) {
                        double roll = coordinateChance(seed ^ SURFACE_SALT, x, y - 1, z);
                        double chance = clamp01((0.30D + core * 0.54D + (patch > 0.66D ? 0.18D : 0.0D))
                                * chamberBlockBias);
                        if (roll < chance) {
                            world.setBlock(x, y - 1, z, ModBlocks.DRIPSTONE_BLOCK.get(), 0, 2);
                            changed = true;

                            int clearance = contiguousAirHeight(world, x, y, z, 18);
                            if (clearance >= MIN_POINTED_CAVITY_HEIGHT && chamber >= MIN_POINTED_CHAMBER_FACTOR) {
                                double pointRoll = coordinateChance(seed ^ POINT_SALT, x, y, z);
                                double pointChance = 0.03D + core * 0.23D + chamber * 0.40D;
                                if (pointRoll < pointChance) {
                                    int upwardClearance = countAir(world, x, y, z, 1, 12);
                                    int height = chooseHeight(seed ^ POINT_SALT, x, y, z,
                                            upwardClearance, core * (0.70D + chamber * 0.30D), false);
                                    if (height > 0) {
                                        growSupportPatch(world, source, x, y - 1, z, false,
                                                chamber > 0.76D ? 2 : 1);
                                        placeColumn(world, source, pointed, x, y, z, true, height);
                                    }
                                }
                            }
                        }
                    }

                    if (ceilingSurface && world.isAirBlock(x, y, z)) {
                        double roll = coordinateChance(seed ^ (SURFACE_SALT + 0x101L), x, y + 1, z);
                        double chance = clamp01((0.32D + core * 0.54D + (patch > 0.64D ? 0.19D : 0.0D))
                                * chamberBlockBias);
                        if (roll < chance) {
                            world.setBlock(x, y + 1, z, ModBlocks.DRIPSTONE_BLOCK.get(), 0, 2);
                            changed = true;

                            int clearance = contiguousAirHeight(world, x, y, z, 18);
                            if (clearance >= MIN_POINTED_CAVITY_HEIGHT && chamber >= MIN_POINTED_CHAMBER_FACTOR) {
                                double pointRoll = coordinateChance(seed ^ (POINT_SALT + 0x202L), x, y, z);
                                double pointChance = 0.04D + core * 0.24D + chamber * 0.42D;
                                if (pointRoll < pointChance) {
                                    int downwardClearance = countAir(world, x, y, z, -1, 12);
                                    int height = chooseHeight(seed ^ (POINT_SALT + 0x202L), x, y, z,
                                            downwardClearance, core * (0.70D + chamber * 0.30D), true);
                                    if (height > 0) {
                                        growSupportPatch(world, source, x, y + 1, z, true,
                                                chamber > 0.76D ? 2 : 1);
                                        placeColumn(world, source, pointed, x, y, z, false, height);
                                    }
                                }
                            }
                        }
                    }

                    // P008e-c increases actual Dripstone Block identity in roomy chambers. Wall
                    // patches are multi-block and chamber-weighted; cramped passages no longer get
                    // the same wall takeover as a broad cavern.
                    if (wallSurface && chamber > 0.12D && coordinateChance(seed ^ WALL_SALT, x, y, z)
                            < clamp01(0.08D + core * 0.38D + chamber * 0.38D
                                    + (patch > 0.70D ? 0.12D : 0.0D))) {
                        changed |= decorateWallPatch(world, source, seed, x, y, z, chamber);
                    }
                }
            }
        }
        return changed;
    }

    private boolean hasReplaceableWall(World world, int x, int y, int z) {
        return isReplaceableStone(world.getBlock(x + 1, y, z))
                || isReplaceableStone(world.getBlock(x - 1, y, z))
                || isReplaceableStone(world.getBlock(x, y, z + 1))
                || isReplaceableStone(world.getBlock(x, y, z - 1));
    }

    private boolean decorateWallPatch(World world, ModernOverworldCaveRegionSource source, long seed,
            int x, int y, int z, double chamber) {
        final int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int start = (int) ((mix64(seed ^ ((long) x * 341873128712L) ^ ((long) y * 132897987541L)
                ^ ((long) z * 42317861L)) & Long.MAX_VALUE) % 4L);
        int radius = chamber > 0.78D ? 2 : (chamber > 0.44D ? 1 : 0);

        for (int i = 0; i < 4; i++) {
            int[] dir = dirs[(start + i) & 3];
            int tangentX = dir[1];
            int tangentZ = dir[0];
            boolean foundWall = false;
            boolean changed = false;

            for (int dy = -1; dy <= 1; dy++) {
                for (int tangent = -radius; tangent <= radius; tangent++) {
                    int airX = x + tangentX * tangent;
                    int airY = y + dy;
                    int airZ = z + tangentZ * tangent;
                    int wallX = airX + dir[0];
                    int wallZ = airZ + dir[1];
                    if (!world.isAirBlock(airX, airY, airZ) || !source.isDripstone(airX, airY, airZ)
                            || !isReplaceableStone(world.getBlock(wallX, airY, wallZ))) {
                        continue;
                    }
                    foundWall = true;
                    double keep = 0.62D + chamber * 0.30D;
                    if (coordinateChance(seed ^ WALL_SALT ^ 0x50415443484CL, wallX, airY, wallZ) < keep) {
                        world.setBlock(wallX, airY, wallZ, ModBlocks.DRIPSTONE_BLOCK.get(), 0, 2);
                        changed = true;
                    }
                }
            }
            if (foundWall) {
                return changed;
            }
        }
        return false;
    }

    /** Adds broad block-built stalagmites/stalactites only where a real roomy chamber supports them. */
    private boolean decorateLargeFormations(World world, ModernOverworldCaveRegionSource source,
            int chunkX, int chunkZ) {
        long chunkSeed = mix64(world.getSeed() ^ LARGE_SALT
                ^ ((long) chunkX * 341873128712L) ^ ((long) chunkZ * 132897987541L));
        Random rand = new Random(chunkSeed);
        // More search attempts, but a substantially stricter chamber gate. This moves formation
        // density out of low tunnels and into the large/vertical spaces where modern Dripstone reads best.
        int attempts = 2 + rand.nextInt(2);
        boolean changed = false;
        BlockPointedDripstone pointed = (BlockPointedDripstone) ModBlocks.POINTED_DRIPSTONE.get();

        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = (chunkX << 4) + 3 + rand.nextInt(10);
            int z = (chunkZ << 4) + 3 + rand.nextInt(10);
            Cavity cavity = findBestCavity(world, source, x, z);
            if (cavity == null || cavity.height() < MIN_LARGE_CAVITY_HEIGHT
                    || cavity.strength < 0.045D || cavity.chamberFactor < MIN_LARGE_CHAMBER_FACTOR) {
                continue;
            }

            double core = coreFactor(cavity.strength);
            if (rand.nextDouble() > clamp01(0.16D + core * 0.42D + cavity.chamberFactor * 0.34D)) {
                continue;
            }

            int available = cavity.height();
            int maxSide = Math.max(5, Math.min(18, available / 2));
            int floorHeight = 5 + rand.nextInt(Math.max(1, maxSide - 4));
            int ceilHeight = 5 + rand.nextInt(Math.max(1, maxSide - 4));

            // Tall/core chambers occasionally produce spectacular opposed formations. Open chambers
            // receive broader block-built bases than P008e-b while the pointed ends remain narrow.
            if (available >= 16 && core > 0.52D && cavity.chamberFactor > 0.66D && rand.nextInt(4) == 0) {
                floorHeight = Math.max(6, available / 2);
                ceilHeight = Math.max(6, available - floorHeight);
            } else if (floorHeight + ceilHeight > available - 2) {
                int overflow = floorHeight + ceilHeight - (available - 2);
                if (floorHeight >= ceilHeight) {
                    floorHeight = Math.max(4, floorHeight - overflow);
                } else {
                    ceilHeight = Math.max(4, ceilHeight - overflow);
                }
            }

            int opennessBonus = cavity.chamberFactor > 0.78D ? 2 : (cavity.chamberFactor > 0.62D ? 1 : 0);
            int floorRadius = Math.min(5, 2 + floorHeight / 5 + opennessBonus + (core > 0.75D ? 1 : 0));
            int ceilRadius = Math.min(5, 2 + ceilHeight / 5 + opennessBonus + (core > 0.75D ? 1 : 0));
            changed |= placeLargeCone(world, source, pointed, x, cavity.floorAirY, z, true,
                    floorHeight, floorRadius);
            changed |= placeLargeCone(world, source, pointed, x, cavity.ceilingAirY, z, false,
                    ceilHeight, ceilRadius);

            // Satellites are likewise chamber-gated so a nearby narrow offshoot does not suddenly
            // fill with needles just because it touches a large Dripstone cavern.
            int satellites = 2 + rand.nextInt(3);
            for (int i = 0; i < satellites; i++) {
                int sx = x + rand.nextInt(11) - 5;
                int sz = z + rand.nextInt(11) - 5;
                Cavity side = findCavityNear(world, source, sx, sz, cavity.floorAirY, 7);
                if (side == null || side.height() < 6 || side.chamberFactor < 0.30D) {
                    continue;
                }
                if (rand.nextBoolean()) {
                    if (isReplaceableStone(world.getBlock(sx, side.floorAirY - 1, sz))) {
                        growSupportPatch(world, source, sx, side.floorAirY - 1, sz, false,
                                side.chamberFactor > 0.65D ? 2 : 1);
                        placeColumn(world, source, pointed, sx, side.floorAirY, sz, true,
                                Math.min(6, 2 + rand.nextInt(Math.max(1, Math.min(5, side.height() - 2)))));
                    }
                } else if (isReplaceableStone(world.getBlock(sx, side.ceilingAirY + 1, sz))) {
                    growSupportPatch(world, source, sx, side.ceilingAirY + 1, sz, true,
                            side.chamberFactor > 0.65D ? 2 : 1);
                    placeColumn(world, source, pointed, sx, side.ceilingAirY, sz, false,
                            Math.min(6, 2 + rand.nextInt(Math.max(1, Math.min(5, side.height() - 2)))));
                }
            }
        }
        return changed;
    }

    private boolean placeLargeCone(World world, ModernOverworldCaveRegionSource source,
            BlockPointedDripstone pointed, int x, int startAirY, int z, boolean pointingUp,
            int targetHeight, int baseRadius) {
        int step = pointingUp ? 1 : -1;
        int requestedBlockLayers = Math.max(1, targetHeight - 2);
        int builtCenterLayers = 0;
        boolean changed = false;

        for (int layer = 0; layer < requestedBlockLayers; layer++) {
            int y = startAirY + layer * step;
            if (y <= 1 || y >= world.getActualHeight() - 1) {
                break;
            }

            // The central spine is authoritative. If the Dripstone region ends, another feature has
            // opened the support below/above, or this layer is no longer air, stop the whole cone here.
            // P008e-c used the originally requested height for its pointed cap even when the block-built
            // cone was truncated, which could leave an apparently floating stalagmite/stalactite tip.
            int centerSupportY = y - step;
            if (!world.isAirBlock(x, y, z) || !source.isDripstone(x, y, z)
                    || world.isAirBlock(x, centerSupportY, z)
                    || world.getBlock(x, centerSupportY, z).getMaterial().isLiquid()) {
                break;
            }

            double taper = 1.0D - layer / Math.max(1.0D, requestedBlockLayers - 0.25D);
            int radius = Math.max(0, (int) Math.floor(baseRadius * taper + 0.35D));
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius + 1) {
                        continue;
                    }
                    int px = x + dx;
                    int pz = z + dz;
                    if (!world.isAirBlock(px, y, pz) || !source.isDripstone(px, y, pz)) {
                        continue;
                    }
                    int supportY = y - step;
                    if (world.isAirBlock(px, supportY, pz)
                            || world.getBlock(px, supportY, pz).getMaterial().isLiquid()) {
                        continue;
                    }
                    world.setBlock(px, y, pz, ModBlocks.DRIPSTONE_BLOCK.get(), 0, 2);
                    changed = true;
                }
            }
            builtCenterLayers++;
        }

        // Attach the pointed cap to the actual connected cone, never to the cone's requested end.
        if (builtCenterLayers > 0) {
            int pointedStartY = startAirY + builtCenterLayers * step;
            int pointedLength = Math.min(3, Math.max(1, targetHeight - builtCenterLayers));
            placeColumn(world, source, pointed, x, pointedStartY, z, pointingUp, pointedLength);
        }
        return changed;
    }

    private void growSupportPatch(World world, ModernOverworldCaveRegionSource source,
            int x, int supportY, int z, boolean ceiling, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + 1) {
                    continue;
                }
                int px = x + dx;
                int pz = z + dz;
                int airY = supportY + (ceiling ? -1 : 1);
                if (source.isDripstone(px, airY, pz) && isReplaceableStone(world.getBlock(px, supportY, pz))) {
                    world.setBlock(px, supportY, pz, ModBlocks.DRIPSTONE_BLOCK.get(), 0, 2);
                }
            }
        }
    }

    private Cavity findBestCavity(World world, ModernOverworldCaveRegionSource source, int x, int z) {
        int minLogical = Math.max(WorldHeightCompat.MODERN_MIN_Y,
                Math.min(ConfigWorld.modernDripstoneCaveMinY, ConfigWorld.modernDripstoneCaveMaxY));
        int maxLogical = Math.min(WorldHeightCompat.MODERN_MAX_Y,
                Math.max(ConfigWorld.modernDripstoneCaveMinY, ConfigWorld.modernDripstoneCaveMaxY));
        int minY = Math.max(2, WorldHeightCompat.modernToPhysicalY(minLogical));
        int maxY = Math.min(world.getActualHeight() - 3, WorldHeightCompat.modernToPhysicalY(maxLogical));
        Cavity best = null;
        int y = minY;
        while (y <= maxY) {
            if (!world.isAirBlock(x, y, z)) {
                y++;
                continue;
            }
            int start = y;
            while (y <= maxY && world.isAirBlock(x, y, z)) {
                y++;
            }
            int end = y - 1;
            if (end - start + 1 < 4 || !isReplaceableStone(world.getBlock(x, start - 1, z))
                    || !isReplaceableStone(world.getBlock(x, end + 1, z))) {
                continue;
            }
            int mid = (start + end) >>> 1;
            if (!source.isDripstone(x, mid, z)) {
                continue;
            }
            double strength = source.dripstoneStrength(x, mid, z);
            double chamber = chamberFactor(world, x, mid, z);
            Cavity candidate = new Cavity(start, end, strength, chamber);
            if (best == null || candidate.formationScore() > best.formationScore()) {
                best = candidate;
            }
        }
        return best;
    }

    private Cavity findCavityNear(World world, ModernOverworldCaveRegionSource source,
            int x, int z, int referenceY, int verticalSearch) {
        int lo = Math.max(2, referenceY - verticalSearch);
        int hi = Math.min(world.getActualHeight() - 3, referenceY + verticalSearch + 24);
        for (int y = lo; y <= hi; y++) {
            if (!world.isAirBlock(x, y, z)) {
                continue;
            }
            int start = y;
            while (y <= hi && world.isAirBlock(x, y, z)) {
                y++;
            }
            int end = y - 1;
            int mid = (start + end) >>> 1;
            if (end - start + 1 >= 4 && source.isDripstone(x, mid, z)
                    && isReplaceableStone(world.getBlock(x, start - 1, z))
                    && isReplaceableStone(world.getBlock(x, end + 1, z))) {
                return new Cavity(start, end, source.dripstoneStrength(x, mid, z),
                        chamberFactor(world, x, mid, z));
            }
        }
        return null;
    }

    private void placeColumn(World world, ModernOverworldCaveRegionSource source,
            BlockPointedDripstone pointed, int x, int startY, int z, boolean pointingUp, int requestedLength) {
        int step = pointingUp ? 1 : -1;

        // Worldgen uses notification flag 2, so vanilla neighbour updates cannot be relied upon to
        // clean up a bad generated base. Require a real supporting face before placing even the first
        // segment; this makes floating generated columns impossible even if a cone/patch was clipped.
        int supportY = startY - step;
        ForgeDirection supportFace = pointingUp ? ForgeDirection.UP : ForgeDirection.DOWN;
        if (supportY < 0 || supportY >= world.getActualHeight()
                || !world.isSideSolid(x, supportY, z, supportFace)) {
            return;
        }

        int length = 0;
        for (int i = 0; i < requestedLength; i++) {
            int y = startY + i * step;
            // Recheck region ownership for every segment. P008e only checked the origin, allowing a
            // tall formation to continue vertically into an adjacent Lush volume.
            if (y <= 0 || y >= world.getActualHeight() - 1 || !world.isAirBlock(x, y, z)
                    || !source.isDripstone(x, y, z)) {
                break;
            }
            length++;
        }
        if (length <= 0) {
            return;
        }
        for (int i = 0; i < length; i++) {
            int y = startY + i * step;
            int meta = BlockPointedDripstone.metadataForGeneratedColumn(pointingUp, i, length);
            world.setBlock(x, y, z, pointed, meta, 2);
        }
        pointed.refreshColumn(world, x, startY, z);
    }

    /**
     * Returns 0..1 describing whether this air position belongs to a useful chamber rather than a
     * cramped tunnel. Vertical height is authoritative; horizontal openness only strengthens an
     * already-tall space so a long two-block-high tube never masquerades as a cavern.
     */
    private double chamberFactor(World world, int x, int y, int z) {
        int vertical = contiguousAirHeight(world, x, y, z, 18);
        double verticalT = clamp01((vertical - 3.0D) / 9.0D);
        verticalT = verticalT * verticalT * (3.0D - 2.0D * verticalT);
        if (verticalT <= 0.0D) {
            return 0.0D;
        }

        final int[][] dirs = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        int openSteps = 0;
        for (int[] dir : dirs) {
            for (int step = 1; step <= CHAMBER_PROBE_RADIUS; step++) {
                int px = x + dir[0] * step;
                int pz = z + dir[1] * step;
                if (!world.isAirBlock(px, y, pz)) {
                    break;
                }
                openSteps++;
            }
        }
        double lateral = openSteps / (double) (dirs.length * CHAMBER_PROBE_RADIUS);
        return clamp01(verticalT * (0.70D + lateral * 0.30D));
    }

    private int contiguousAirHeight(World world, int x, int y, int z, int max) {
        if (!world.isAirBlock(x, y, z)) {
            return 0;
        }
        int count = 1;
        for (int step = 1; step < max; step++) {
            int py = y + step;
            if (py >= world.getActualHeight() - 1 || !world.isAirBlock(x, py, z)) break;
            count++;
        }
        for (int step = 1; step < max; step++) {
            int py = y - step;
            if (py <= 0 || !world.isAirBlock(x, py, z)) break;
            count++;
        }
        return count;
    }

    private static double clamp01(double value) {
        return value < 0.0D ? 0.0D : (value > 1.0D ? 1.0D : value);
    }

    private int chooseHeight(long seed, int x, int y, int z, int clearance, double core, boolean ceiling) {
        if (clearance <= 0) {
            return 0;
        }
        double roll = coordinateChance(seed ^ (ceiling ? 0xCE11L : 0xF100L), x, y, z);
        int target = 1;
        if (roll < 0.58D + core * 0.12D) target++;
        if (roll < 0.28D + core * 0.22D) target++;
        if (roll < 0.11D + core * 0.18D) target += 1 + (int) Math.floor(core * 2.0D);
        return Math.min(Math.min(clearance, 8), target);
    }

    private int countAir(World world, int x, int startY, int z, int step, int max) {
        int count = 0;
        for (int i = 0; i < max; i++) {
            int y = startY + i * step;
            if (y <= 0 || y >= world.getActualHeight() - 1 || !world.isAirBlock(x, y, z)) {
                break;
            }
            count++;
        }
        return count;
    }

    private boolean chunkHasDripstoneRegion(ModernOverworldCaveRegionSource source, int chunkX, int chunkZ) {
        int minLogical = Math.max(WorldHeightCompat.MODERN_MIN_Y,
                Math.min(ConfigWorld.modernDripstoneCaveMinY, ConfigWorld.modernDripstoneCaveMaxY));
        int maxLogical = Math.min(WorldHeightCompat.MODERN_MAX_Y,
                Math.max(ConfigWorld.modernDripstoneCaveMinY, ConfigWorld.modernDripstoneCaveMaxY));
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        for (int logicalY = minLogical + 8; logicalY <= maxLogical; logicalY += 16) {
            int physicalY = WorldHeightCompat.modernToPhysicalY(logicalY);
            for (int localX = 2; localX <= 14; localX += 4) {
                for (int localZ = 2; localZ <= 14; localZ += 4) {
                    double strength = source.dripstoneStrength(baseX + localX, physicalY, baseZ + localZ);
                    if (strength >= MIN_DECORATION_STRENGTH
                            && !source.isLush(baseX + localX, physicalY, baseZ + localZ)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private ModernOverworldCaveRegionSource regionSource(World world) {
        long seed = world.getSeed();
        int currentLushMin = Math.min(ConfigWorld.modernLushCaveMinY, ConfigWorld.modernLushCaveMaxY);
        int currentLushMax = Math.max(ConfigWorld.modernLushCaveMinY, ConfigWorld.modernLushCaveMaxY);
        int currentDripMin = Math.min(ConfigWorld.modernDripstoneCaveMinY, ConfigWorld.modernDripstoneCaveMaxY);
        int currentDripMax = Math.max(ConfigWorld.modernDripstoneCaveMinY, ConfigWorld.modernDripstoneCaveMaxY);
        if (regionSource == null || regionSeed != seed || lushMin != currentLushMin || lushMax != currentLushMax
                || dripMin != currentDripMin || dripMax != currentDripMax) {
            regionSource = new ModernOverworldCaveRegionSource(seed, currentLushMin, currentLushMax,
                    currentDripMin, currentDripMax);
            regionSeed = seed;
            lushMin = currentLushMin;
            lushMax = currentLushMax;
            dripMin = currentDripMin;
            dripMax = currentDripMax;
        }
        return regionSource;
    }

    private boolean isReplaceableStone(Block block) {
        if (block == Blocks.stone || block == ModBlocks.DRIPSTONE_BLOCK.get()) {
            return true;
        }
        if (ModBlocks.DEEPSLATE.isEnabled() && block == ModBlocks.DEEPSLATE.get()) {
            return true;
        }
        if (ModBlocks.TUFF.isEnabled() && block == ModBlocks.TUFF.get()) {
            return true;
        }
        return ModBlocks.STONE.isEnabled() && block == ModBlocks.STONE.get();
    }

    private static double coreFactor(double strength) {
        if (strength <= 0.0D) return 0.0D;
        double t = strength / 0.22D;
        t = t < 0.0D ? 0.0D : (t > 1.0D ? 1.0D : t);
        return t * t * (3.0D - 2.0D * t);
    }

    private static double coordinateChance(long seed, int x, int y, int z) {
        long mixed = seed;
        mixed ^= (long) x * 341873128712L;
        mixed ^= (long) y * 132897987541L;
        mixed ^= (long) z * 42317861L;
        mixed = mix64(mixed);
        return ((mixed >>> 11) & ((1L << 53) - 1L)) / (double) (1L << 53);
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private static final class Cavity {
        final int floorAirY;
        final int ceilingAirY;
        final double strength;
        final double chamberFactor;

        Cavity(int floorAirY, int ceilingAirY, double strength, double chamberFactor) {
            this.floorAirY = floorAirY;
            this.ceilingAirY = ceilingAirY;
            this.strength = strength;
            this.chamberFactor = chamberFactor;
        }

        int height() {
            return ceilingAirY - floorAirY + 1;
        }

        double formationScore() {
            return height() * (0.70D + chamberFactor * 0.55D) + strength * 8.0D;
        }
    }
}
