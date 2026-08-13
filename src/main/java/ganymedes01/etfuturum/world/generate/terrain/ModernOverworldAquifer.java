package ganymedes01.etfuturum.world.generate.terrain;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;

/**
 * P008b-c: bounded local aquifers plus a modern-style deep lava shelf.
 *
 * <p>The contained P008b-a water basins remain unchanged. P008b-c moves the characteristic bottom
 * lava lakes out of rare Voronoi ownership and into the cave floor itself: carved space below
 * logical Y-54 is lava by default, so lake footprint follows cave geometry just as the 1.21
 * reference world does. A much rarer secondary lava-site branch creates occasional higher lava
 * pockets without reintroducing P008b's enormous lava-owned regions.</p>
 */
final class ModernOverworldAquifer {

    static final int NO_FLUID_LEVEL = Integer.MIN_VALUE / 4;

    private static final int CELL_SIZE = 96;
    private static final double SITE_JITTER = 0.28D;
    private static final double WET_SITE_THRESHOLD = 0.55D;
    private static final int DEEP_LAVA_SHELF_MAX_Y = -55;
    private static final double LAVA_SITE_CHANCE = 0.100D;
    private static final int LAVA_POD_LEVEL_MIN = -36;
    private static final int LAVA_POD_LEVEL_MAX = -24;
    private static final int LAVA_POD_BOTTOM_MIN = -53;
    private static final double PRESSURE_BARRIER_WIDTH = 0.11D;
    private static final int PRESSURE_LEVEL_DIFFERENCE = 8;
    private static final double BASIN_SHELL_WIDTH = 4.5D;

    private static final long SALT_SITE_X = 0x243F6A8885A308D3L;
    private static final long SALT_SITE_Z = 0x13198A2E03707344L;
    private static final long SALT_WETNESS = 0xA4093822299F31D0L;
    private static final long SALT_LEVEL = 0x082EFA98EC4E6C89L;
    private static final long SALT_LAVA = 0xBE5466CF34E90C6CL;
    private static final long SALT_RADIUS = 0xC0AC29B7C97C50DDL;
    private static final long SALT_DEPTH = 0x3F84D5B5B5470917L;

    private final long seed;
    private final ModernOverworldTerrainGenerator terrain;

    ModernOverworldAquifer(long seed, ModernOverworldTerrainGenerator terrain) {
        this.seed = seed;
        this.terrain = terrain;
    }

    Column sampleColumn(int worldX, int worldZ, boolean amplified) {
        final int baseCellX = Math.floorDiv(worldX, CELL_SIZE);
        final int baseCellZ = Math.floorDiv(worldZ, CELL_SIZE);

        Site nearest = null;
        Site second = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        double secondDistanceSquared = Double.POSITIVE_INFINITY;

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                final Site site = createSite(baseCellX + dx, baseCellZ + dz, amplified);
                final double deltaX = worldX - site.x;
                final double deltaZ = worldZ - site.z;
                final double distanceSquared = deltaX * deltaX + deltaZ * deltaZ;

                if (distanceSquared < nearestDistanceSquared) {
                    second = nearest;
                    secondDistanceSquared = nearestDistanceSquared;
                    nearest = site;
                    nearestDistanceSquared = distanceSquared;
                } else if (distanceSquared < secondDistanceSquared) {
                    second = site;
                    secondDistanceSquared = distanceSquared;
                }
            }
        }

        if (nearest == null) {
            throw new IllegalStateException("Aquifer site search produced no nearest site");
        }
        if (second == null) {
            second = nearest;
            secondDistanceSquared = nearestDistanceSquared;
        }

        final double nearestDistance = Math.sqrt(nearestDistanceSquared);
        final double secondDistance = Math.sqrt(secondDistanceSquared);
        final double distanceGap = (secondDistance - nearestDistance) / CELL_SIZE;
        return new Column(nearest, second, nearestDistance, secondDistance, distanceGap);
    }

    Decision resolve(Column column, int physicalY) {
        final int logicalY = WorldHeightCompat.physicalToModernY(physicalY);

        // The 1.21 reference world shows the lowest carved band behaving as a true lava shelf:
        // almost every cavity below Y=-54 is lava, with the footprint controlled by cave geometry
        // rather than by a sparse aquifer site. This produces broad bottom lakes while leaving the
        // rest of the underground mostly dry.
        if (logicalY <= DEEP_LAVA_SHELF_MAX_Y) {
            return Decision.LAVA;
        }

        if (needsPressureBarrier(column, logicalY)) {
            return Decision.PRESERVE;
        }

        final Site site = column.primary;
        if (!site.wet || site.fluidLevelLogicalY == NO_FLUID_LEVEL) {
            return Decision.AIR;
        }

        // A wet site no longer owns its full Voronoi cell. Keep the source region bounded so a
        // single site cannot waterlog/lava-log an entire cheese-cave complex.
        if (column.primaryDistance >= site.radius) {
            return Decision.AIR;
        }

        // Preserve a thin irregular-looking rock shell at the horizontal edge of the basin while
        // fluid pressure is relevant. This prevents static 1.7 source blocks from presenting as
        // exposed vertical sheets against dry cave space.
        if (column.primaryDistance >= site.radius - BASIN_SHELL_WIDTH
                && logicalY >= site.bottomLogicalY
                && logicalY <= site.fluidLevelLogicalY + 1) {
            return Decision.PRESERVE;
        }

        // Give each basin a real floor instead of flooding every carved block below its level down
        // to bedrock. The one-block preserved floor also stops source blocks from hanging above a
        // dry lower cave and later pouring through it when block updates occur.
        if (logicalY == site.bottomLogicalY) {
            return Decision.PRESERVE;
        }
        if (logicalY < site.bottomLogicalY || logicalY > site.fluidLevelLogicalY) {
            return Decision.AIR;
        }

        return site.lava ? Decision.LAVA : Decision.WATER;
    }

    private boolean needsPressureBarrier(Column column, int logicalY) {
        if (column.distanceGap >= PRESSURE_BARRIER_WIDTH) {
            return false;
        }

        final Site a = column.primary;
        final Site b = column.secondary;
        if (a == null || b == null) {
            return false;
        }

        final boolean aActive = isFluidActiveAt(a, column.primaryDistance, logicalY);
        final boolean bActive = isFluidActiveAt(b, column.secondaryDistance, logicalY);
        if (!aActive && !bActive) {
            return false;
        }

        if (aActive != bActive) {
            return true;
        }
        if (a.lava != b.lava) {
            return true;
        }
        return Math.abs(a.fluidLevelLogicalY - b.fluidLevelLogicalY) >= PRESSURE_LEVEL_DIFFERENCE;
    }

    private static boolean isFluidActiveAt(Site site, double distance, int logicalY) {
        return site != null
                && site.wet
                && distance < site.radius
                && logicalY > site.bottomLogicalY
                && logicalY <= site.fluidLevelLogicalY;
    }

    private Site createSite(int cellX, int cellZ, boolean amplified) {
        final long cellHash = mix64(seed ^ ((long) cellX * 341873128712L) ^ ((long) cellZ * 132897987541L));
        final double jitterX = (unit(cellHash ^ SALT_SITE_X) * 2.0D - 1.0D) * SITE_JITTER;
        final double jitterZ = (unit(cellHash ^ SALT_SITE_Z) * 2.0D - 1.0D) * SITE_JITTER;
        final int siteX = cellX * CELL_SIZE + CELL_SIZE / 2 + (int) Math.round(jitterX * CELL_SIZE);
        final int siteZ = cellZ * CELL_SIZE + CELL_SIZE / 2 + (int) Math.round(jitterZ * CELL_SIZE);

        final double wetness = unit(cellHash ^ SALT_WETNESS);
        if (wetness < WET_SITE_THRESHOLD) {
            return new Site(siteX, siteZ, false, false, NO_FLUID_LEVEL, NO_FLUID_LEVEL, 0.0D);
        }

        final int surfaceLogicalY = terrain.sampleSurfaceLogicalY(siteX, siteZ, amplified);
        final boolean lava = unit(cellHash ^ SALT_LAVA) < LAVA_SITE_CHANCE;
        final double levelRoll = unit(cellHash ^ SALT_LEVEL);
        final double radiusRoll = unit(cellHash ^ SALT_RADIUS);
        final double depthRoll = unit(cellHash ^ SALT_DEPTH);

        final int fluidLevel;
        final int depth;
        final double radius;
        if (lava) {
            // Secondary lava aquifers are intentionally much rarer than the bottom shelf. They form
            // occasional pods/lakes above Y=-54, matching the reference world's isolated mid-depth
            // lava bodies without making whole cave regions lava-owned.
            fluidLevel = LAVA_POD_LEVEL_MIN
                    + (int) Math.floor(levelRoll * (LAVA_POD_LEVEL_MAX - LAVA_POD_LEVEL_MIN + 1));
            depth = 8 + (int) Math.floor(depthRoll * 9.0D);         // 8..16 blocks
            radius = 18.0D + radiusRoll * 14.0D;                    // 18..32 blocks
        } else {
            int waterLevel = -28 + (int) Math.floor(levelRoll * 61.0D); // logical -28..32
            waterLevel = (waterLevel / 4) * 4;
            waterLevel = Math.min(waterLevel, surfaceLogicalY - 12);
            waterLevel = Math.max(-44, waterLevel);
            fluidLevel = waterLevel;
            depth = 18 + (int) Math.floor(depthRoll * 17.0D);      // 18..34 blocks
            radius = 34.0D + radiusRoll * 20.0D;                   // 34..54 blocks
        }

        final int bottom = lava
                ? Math.max(LAVA_POD_BOTTOM_MIN, fluidLevel - depth)
                : Math.max(-56, fluidLevel - depth);
        return new Site(siteX, siteZ, true, lava, fluidLevel, bottom, radius);
    }

    private static double unit(long value) {
        final long mixed = mix64(value);
        return (mixed >>> 11) * 0x1.0p-53;
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value;
    }

    enum Decision {
        PRESERVE,
        AIR,
        WATER,
        LAVA
    }

    static final class Column {
        final Site primary;
        final Site secondary;
        final double primaryDistance;
        final double secondaryDistance;
        final double distanceGap;
        private Column(Site primary, Site secondary, double primaryDistance, double secondaryDistance,
                double distanceGap) {
            this.primary = primary;
            this.secondary = secondary;
            this.primaryDistance = primaryDistance;
            this.secondaryDistance = secondaryDistance;
            this.distanceGap = distanceGap;
        }
    }

    static final class Site {
        final int x;
        final int z;
        final boolean wet;
        final boolean lava;
        final int fluidLevelLogicalY;
        final int bottomLogicalY;
        final double radius;

        private Site(int x, int z, boolean wet, boolean lava, int fluidLevelLogicalY,
                int bottomLogicalY, double radius) {
            this.x = x;
            this.z = z;
            this.wet = wet;
            this.lava = lava;
            this.fluidLevelLogicalY = fluidLevelLogicalY;
            this.bottomLogicalY = bottomLogicalY;
            this.radius = radius;
        }
    }
}
