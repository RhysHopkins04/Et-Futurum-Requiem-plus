package ganymedes01.etfuturum.world.generate.terrain;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;

/**
 * Deterministic 3D underground-region source for the Plus modern Overworld.
 *
 * <p>Minecraft 1.7.10 stores only a 2D biome byte per surface column, so modern cave biomes cannot
 * be represented by changing {@code Chunk#getBiomeArray()}. This source supplies an independent
 * seed-stable (x,y,z) region field that world-generation features can query without changing the
 * surface biome map. P008c activates {@link RegionType#LUSH}; {@link RegionType#DRIPSTONE} is
 * reserved here so the next cave-biome stage can share the exact same ownership contract.</p>
 *
 * <p>The field deliberately uses broad trilinear value-noise volumes rather than per-chunk random
 * anchors. That prevents square chunk boundaries, tiny speckles and vertically infinite columns.
 * The vertical preference is calibrated against a sampled Java 1.21 reference world: Lush Cave
 * ownership is strongest around logical Y -16..16, becomes uncommon above Y48, and is only rare
 * near the deep floor.</p>
 */
public final class ModernOverworldCaveRegionSource {

    public enum RegionType {
        NORMAL,
        LUSH,
        DRIPSTONE
    }

    private static final long SALT_WETNESS = 0x4C55534857455431L; // "LUSHWET1"
    private static final long SALT_REGION = 0x4C55534852454731L;  // "LUSHREG1"
    private static final long OCTAVE_SALT = 0x9E3779B97F4A7C15L;

    // Broad enough to form cave-biome volumes spanning multiple chunks while retaining irregular
    // vertical boundaries. The second octave adds shape without producing one-block speckling.
    private static final double WET_XZ_SCALE = 1.0D / 256.0D;
    private static final double WET_Y_SCALE = 1.0D / 128.0D;
    private static final double REGION_XZ_SCALE = 1.0D / 160.0D;
    private static final double REGION_Y_SCALE = 1.0D / 96.0D;

    private static final double BASE_LUSH_THRESHOLD = 0.24D;

    private final long seed;
    private final int minLushLogicalY;
    private final int maxLushLogicalY;

    public ModernOverworldCaveRegionSource(long seed, int minLushLogicalY, int maxLushLogicalY) {
        this.seed = seed;
        this.minLushLogicalY = Math.max(WorldHeightCompat.MODERN_MIN_Y,
                Math.min(minLushLogicalY, maxLushLogicalY));
        this.maxLushLogicalY = Math.min(WorldHeightCompat.MODERN_MAX_Y,
                Math.max(minLushLogicalY, maxLushLogicalY));
    }

    public RegionType sample(int worldX, int physicalY, int worldZ) {
        return lushStrength(worldX, physicalY, worldZ) > 0.0D ? RegionType.LUSH : RegionType.NORMAL;
    }

    public boolean isLush(int worldX, int physicalY, int worldZ) {
        return sample(worldX, physicalY, worldZ) == RegionType.LUSH;
    }

    /**
     * Positive values belong to the Lush Cave region; negative values are normal cave space.
     * Keeping the signed strength available gives later decorators a stable edge/falloff signal.
     */
    public double lushStrength(int worldX, int physicalY, int worldZ) {
        final int logicalY = WorldHeightCompat.physicalToModernY(physicalY);
        if (logicalY <= minLushLogicalY || logicalY >= maxLushLogicalY) {
            return -1.0D;
        }

        final double wetness = fractalValueNoise(seed ^ SALT_WETNESS,
                worldX, logicalY, worldZ, WET_XZ_SCALE, WET_Y_SCALE);
        final double regional = fractalValueNoise(seed ^ SALT_REGION,
                worldX, logicalY, worldZ, REGION_XZ_SCALE, REGION_Y_SCALE);

        // The reference distribution peaks around the lower-middle underground and falls away
        // much faster above Y16 than a simple symmetric vertical band. Convert that into a smooth
        // threshold penalty instead of hard horizontal layers.
        final double verticalPenalty;
        if (logicalY < -16) {
            verticalPenalty = smoothstep(0.0D, 1.0D,
                    (-16.0D - logicalY) / Math.max(1.0D, -16.0D - minLushLogicalY)) * 0.28D;
        } else {
            verticalPenalty = smoothstep(0.0D, 1.0D,
                    (logicalY + 16.0D) / Math.max(1.0D, maxLushLogicalY + 16.0D)) * 0.22D;
        }

        final double score = wetness * 0.70D + regional * 0.30D;
        return score - (BASE_LUSH_THRESHOLD + verticalPenalty);
    }

    private static double fractalValueNoise(long noiseSeed, double x, double y, double z,
            double xzScale, double yScale) {
        final double broad = valueNoise(noiseSeed, x, y, z, xzScale, yScale);
        final double detail = valueNoise(noiseSeed ^ OCTAVE_SALT, x, y, z,
                xzScale * 2.0D, yScale * 2.0D);
        return broad * 0.68D + detail * 0.32D;
    }

    private static double valueNoise(long noiseSeed, double x, double y, double z,
            double xzScale, double yScale) {
        final double sx = x * xzScale;
        final double sy = y * yScale;
        final double sz = z * xzScale;

        final int ix = floor(sx);
        final int iy = floor(sy);
        final int iz = floor(sz);
        final double tx = smoothstep(0.0D, 1.0D, sx - ix);
        final double ty = smoothstep(0.0D, 1.0D, sy - iy);
        final double tz = smoothstep(0.0D, 1.0D, sz - iz);

        final double x00 = lerp(hashValue(noiseSeed, ix, iy, iz),
                hashValue(noiseSeed, ix + 1, iy, iz), tx);
        final double x10 = lerp(hashValue(noiseSeed, ix, iy + 1, iz),
                hashValue(noiseSeed, ix + 1, iy + 1, iz), tx);
        final double x01 = lerp(hashValue(noiseSeed, ix, iy, iz + 1),
                hashValue(noiseSeed, ix + 1, iy, iz + 1), tx);
        final double x11 = lerp(hashValue(noiseSeed, ix, iy + 1, iz + 1),
                hashValue(noiseSeed, ix + 1, iy + 1, iz + 1), tx);

        final double y0 = lerp(x00, x10, ty);
        final double y1 = lerp(x01, x11, ty);
        return lerp(y0, y1, tz);
    }

    private static double hashValue(long noiseSeed, int x, int y, int z) {
        long mixed = noiseSeed;
        mixed ^= (long) x * 341873128712L;
        mixed ^= (long) y * 132897987541L;
        mixed ^= (long) z * 42317861L;
        mixed = mix64(mixed);
        final long mantissa = (mixed >>> 11) & ((1L << 53) - 1L);
        return (mantissa / (double) (1L << 53)) * 2.0D - 1.0D;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private static int floor(double value) {
        final int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge0 == edge1) {
            return value < edge0 ? 0.0D : 1.0D;
        }
        double t = (value - edge0) / (edge1 - edge0);
        t = t < 0.0D ? 0.0D : (t > 1.0D ? 1.0D : t);
        return t * t * (3.0D - 2.0D * t);
    }
}
