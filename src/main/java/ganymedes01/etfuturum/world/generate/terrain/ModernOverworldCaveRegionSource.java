package ganymedes01.etfuturum.world.generate.terrain;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;

/**
 * Deterministic 3D underground-region source for the Plus modern Overworld.
 *
 * <p>Minecraft 1.7.10 stores only a 2D biome byte per surface column, so modern cave biomes cannot
 * be represented by changing {@code Chunk#getBiomeArray()}. This source supplies an independent
 * seed-stable (x,y,z) region field that world-generation features can query without changing the
 * surface biome map. P008c activates {@link RegionType#LUSH}; P008e activates
 * {@link RegionType#DRIPSTONE} using a separate broad 3D field.</p>
 *
 * <p>Lush ownership is evaluated first so P008e cannot move or erode already-validated P008d-b
 * Lush regions. Dripstone therefore occupies coherent portions of the remaining normal cave space.
 * Both fields use broad trilinear value-noise volumes rather than per-chunk random anchors, avoiding
 * square chunk boundaries, tiny speckles and vertically infinite biome columns.</p>
 */
public final class ModernOverworldCaveRegionSource {

    public enum RegionType {
        NORMAL,
        LUSH,
        DRIPSTONE
    }

    private static final long SALT_WETNESS = 0x4C55534857455431L; // "LUSHWET1"
    private static final long SALT_REGION = 0x4C55534852454731L;  // "LUSHREG1"
    private static final long SALT_DRIPSTONE = 0x4452495053544F4EL; // "DRIPSTON"
    private static final long SALT_DRIP_DETAIL = 0x4452495044455431L; // "DRIPDET1"
    private static final long OCTAVE_SALT = 0x9E3779B97F4A7C15L;

    // Lush field -- retained byte-for-byte in scale/threshold behavior from P008d-b.
    private static final double WET_XZ_SCALE = 1.0D / 256.0D;
    private static final double WET_Y_SCALE = 1.0D / 128.0D;
    private static final double REGION_XZ_SCALE = 1.0D / 160.0D;
    private static final double REGION_Y_SCALE = 1.0D / 96.0D;
    private static final double BASE_LUSH_THRESHOLD = 0.24D;

    // P008e-b: Dripstone remains much rarer than the first P008e pass.  The broad
    // horizontal scale keeps a successful region substantial, while the stronger threshold and
    // tighter vertical detail stop neighbouring regions from joining into a near-continuous layer.
    private static final double DRIP_XZ_SCALE = 1.0D / 288.0D;
    private static final double DRIP_Y_SCALE = 1.0D / 128.0D;
    private static final double DRIP_DETAIL_XZ_SCALE = 1.0D / 176.0D;
    private static final double DRIP_DETAIL_Y_SCALE = 1.0D / 80.0D;
    private static final double BASE_DRIPSTONE_THRESHOLD = 0.38D;
    // P008e-b: discard the weak outer fringe entirely, then amplify the surviving core strength.
    // This trades widespread deepslate "confetti" for fewer coherent volumes that can decorate
    // decisively once found.
    private static final double DRIPSTONE_CORE_MARGIN = 0.030D;
    private static final double DRIPSTONE_STRENGTH_GAIN = 4.0D;

    private final long seed;
    private final int minLushLogicalY;
    private final int maxLushLogicalY;
    private final int minDripstoneLogicalY;
    private final int maxDripstoneLogicalY;

    /**
     * Backwards-compatible P008c constructor. Dripstone receives the P008e default vertical band;
     * callers that own Dripstone worldgen should use the explicit six-argument constructor.
     */
    public ModernOverworldCaveRegionSource(long seed, int minLushLogicalY, int maxLushLogicalY) {
        this(seed, minLushLogicalY, maxLushLogicalY, -64, 96);
    }

    public ModernOverworldCaveRegionSource(long seed, int minLushLogicalY, int maxLushLogicalY,
            int minDripstoneLogicalY, int maxDripstoneLogicalY) {
        this.seed = seed;
        this.minLushLogicalY = Math.max(WorldHeightCompat.MODERN_MIN_Y,
                Math.min(minLushLogicalY, maxLushLogicalY));
        this.maxLushLogicalY = Math.min(WorldHeightCompat.MODERN_MAX_Y,
                Math.max(minLushLogicalY, maxLushLogicalY));
        this.minDripstoneLogicalY = Math.max(WorldHeightCompat.MODERN_MIN_Y,
                Math.min(minDripstoneLogicalY, maxDripstoneLogicalY));
        this.maxDripstoneLogicalY = Math.min(WorldHeightCompat.MODERN_MAX_Y,
                Math.max(minDripstoneLogicalY, maxDripstoneLogicalY));
    }

    public RegionType sample(int worldX, int physicalY, int worldZ) {
        if (lushStrength(worldX, physicalY, worldZ) > 0.0D) {
            return RegionType.LUSH;
        }
        if (dripstoneStrength(worldX, physicalY, worldZ) > 0.0D) {
            return RegionType.DRIPSTONE;
        }
        return RegionType.NORMAL;
    }

    public boolean isLush(int worldX, int physicalY, int worldZ) {
        return lushStrength(worldX, physicalY, worldZ) > 0.0D;
    }

    public boolean isDripstone(int worldX, int physicalY, int worldZ) {
        return lushStrength(worldX, physicalY, worldZ) <= 0.0D
                && dripstoneStrength(worldX, physicalY, worldZ) > 0.0D;
    }

    /**
     * Positive values belong to the Lush Cave region; negative values are normal cave space.
     * Keeping the signed strength available gives decorators a stable edge/falloff signal.
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

    /**
     * Signed P008e Dripstone ownership. This deliberately has a broad mid-underground preference
     * rather than reading the 2D surface biome. Lush ownership remains authoritative in overlaps.
     */
    public double dripstoneStrength(int worldX, int physicalY, int worldZ) {
        final int logicalY = WorldHeightCompat.physicalToModernY(physicalY);
        if (logicalY <= minDripstoneLogicalY || logicalY >= maxDripstoneLogicalY) {
            return -1.0D;
        }

        final double broad = fractalValueNoise(seed ^ SALT_DRIPSTONE,
                worldX, logicalY, worldZ, DRIP_XZ_SCALE, DRIP_Y_SCALE);
        final double detail = fractalValueNoise(seed ^ SALT_DRIP_DETAIL,
                worldX, logicalY, worldZ, DRIP_DETAIL_XZ_SCALE, DRIP_DETAIL_Y_SCALE);

        // The supplied Java 1.21 reference around the saved player position is dominated by
        // Dripstone through roughly logical -32..47 and rapidly becomes rare outside that band.
        // P008e-a therefore applies a much stronger vertical penalty than the initial pass.  The
        // configured min/max remain authoritative hard bounds, but even an old P008e config still
        // receives this parity taper instead of decorating from the world floor to near-surface.
        final double centerPenalty;
        if (logicalY < -24) {
            centerPenalty = smoothstep(0.0D, 1.0D,
                    (-24.0D - logicalY) / Math.max(1.0D, -24.0D - minDripstoneLogicalY)) * 0.30D;
        } else if (logicalY > 32) {
            centerPenalty = smoothstep(0.0D, 1.0D,
                    (logicalY - 32.0D) / Math.max(1.0D, maxDripstoneLogicalY - 32.0D)) * 0.34D;
        } else {
            centerPenalty = 0.0D;
        }

        final double score = broad * 0.80D + detail * 0.20D;
        final double rawStrength = score - (BASE_DRIPSTONE_THRESHOLD + centerPenalty);
        return (rawStrength - DRIPSTONE_CORE_MARGIN) * DRIPSTONE_STRENGTH_GAIN;
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
