package ganymedes01.etfuturum.world.generate.terrain;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.NoiseGeneratorSimplex;

import java.util.Random;

/**
 * P007 base-terrain bridge for the Plus modern Overworld path.
 *
 * <p>This is intentionally the terrain/surface half of the modern generator rather than a one-shot
 * port of every 1.18+ world-generation subsystem. It reproduces the important modern terrain
 * architecture in the 1.7.10 engine: terrain shape is driven by broad continentalness, erosion
 * and ridge/peak fields independently of the legacy 2D biome ID map; logical sea level
 * remains Y63 (physical Y127); the world extends down through logical Y-64; and mountains can use
 * the expanded upper range. P008b owns modern noise-cave geometry and local aquifers; modern feature distributions
 * remain separate follow-up stages.</p>
 *
 * <p>The implementation is an original legacy-engine adaptation. It deliberately uses the existing
 * 1.7.10 biome surface pass after translating physical Y64..319 back into a temporary logical
 * Y0..255 window. That preserves biome-specific top/filler behaviour (including mesa/badlands
 * surfaces) without scattering +64 constants through every vanilla biome class.</p>
 */
public final class ModernOverworldTerrainGenerator {

    private static final int SURFACE_WINDOW_MIN_PHYSICAL_Y = WorldHeightCompat.PHYSICAL_ZERO_Y;
    private static final int SURFACE_WINDOW_HEIGHT = WorldHeightCompat.LEGACY_HEIGHT;
    private static final int SURFACE_WINDOW_MAX_PHYSICAL_Y = SURFACE_WINDOW_MIN_PHYSICAL_Y + SURFACE_WINDOW_HEIGHT - 1;

    // Keep the P007 surface within the translated vanilla surface window. Modern mountains still
    // reach the 220-250 logical range here; P008+ can add density overhangs without changing this
    // coordinate contract.
    private static final int MIN_TERRAIN_Y = 8;
    private static final int MAX_TERRAIN_Y = 250;

    private static final long SALT_CONTINENT = 0x4F9939F508L;
    private static final long SALT_EROSION = 0x1EF1565BD5L;
    private static final long SALT_WEIRDNESS = 0x6A09E667F3L;
    private static final long SALT_JAGGED = 0xBB67AE8584L;
    private static final long SALT_DETAIL = 0x3C6EF372FEL;

    private final long seed;
    private final FractalNoise continentalness;
    private final FractalNoise erosion;
    private final FractalNoise weirdness;
    private final FractalNoise jaggedness;
    private final FractalNoise detail;

    public ModernOverworldTerrainGenerator(long seed) {
        this.seed = seed;
        this.continentalness = new FractalNoise(seed ^ SALT_CONTINENT, 4, 0.5D);
        this.erosion = new FractalNoise(seed ^ SALT_EROSION, 4, 0.5D);
        this.weirdness = new FractalNoise(seed ^ SALT_WEIRDNESS, 4, 0.5D);
        this.jaggedness = new FractalNoise(seed ^ SALT_JAGGED, 3, 0.5D);
        this.detail = new FractalNoise(seed ^ SALT_DETAIL, 3, 0.5D);
    }

    /** Fill a 16x384 chunk block buffer with modern-coordinate base terrain and sea water. */
    public void generateBaseTerrain(World world, int chunkX, int chunkZ, Block[] blocks) {
        final int height = blocks.length / 256;
        if (height != WorldHeightCompat.EXTENDED_HEIGHT) {
            throw new IllegalArgumentException("Modern Overworld terrain requires a 16x16x384 block buffer; got height " + height);
        }

        final int originX = chunkX << 4;
        final int originZ = chunkZ << 4;
        final boolean amplified = world.getWorldInfo().getTerrainType() == WorldType.AMPLIFIED;

        for (int localX = 0; localX < 16; ++localX) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                final int worldX = originX + localX;
                final int worldZ = originZ + localZ;
                final int surfaceY = sampleSurfaceLogicalY(worldX, worldZ, amplified);
                final int columnBase = (localX * 16 + localZ) * height;

                for (int physicalY = 0; physicalY < height; ++physicalY) {
                    final int logicalY = WorldHeightCompat.physicalToModernY(physicalY);
                    Block block = null;

                    if (isBedrock(worldX, physicalY, worldZ)) {
                        block = Blocks.bedrock;
                    } else if (logicalY <= surfaceY) {
                        block = baseRock(worldX, logicalY, worldZ);
                    } else if (logicalY <= WorldHeightCompat.MODERN_SEA_LEVEL) {
                        block = Blocks.water;
                    }

                    blocks[columnBase + physicalY] = block;
                }
            }
        }
    }

    /**
     * Run legacy biome-specific surface rules in a translated logical Y0..255 window and copy the
     * result back to physical Y64..319. This keeps vanilla/EFR biome surface customisations while
     * retaining the modern sea-level mapping.
     */
    public void applyTranslatedBiomeSurface(World world, Random rand, int chunkX, int chunkZ,
            Block[] blocks, byte[] metadata, BiomeGenBase[] biomes, double[] surfaceNoise) {
        final int physicalHeight = blocks.length / 256;
        if (physicalHeight != WorldHeightCompat.EXTENDED_HEIGHT) {
            throw new IllegalArgumentException("Modern surface pass requires a 384-high block buffer");
        }

        final Block[] logicalBlocks = new Block[256 * SURFACE_WINDOW_HEIGHT];
        final byte[] logicalMetadata = new byte[logicalBlocks.length];

        // Vanilla's surface routine writes a rough bedrock floor at its local Y0..4. In the Plus
        // translated window those slots are physical Y64..68 (modern Y0..4), so preserve them and
        // restore them after the surface pass. The real modern bedrock floor is physical Y0..4.
        final Block[] preservedZeroBand = new Block[256 * 5];
        final byte[] preservedZeroBandMeta = new byte[preservedZeroBand.length];

        for (int localX = 0; localX < 16; ++localX) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                final int physicalColumn = localX * 16 + localZ;
                // BiomeGenBase.genBiomeTerrain internally indexes its temporary block buffer as
                // (localZ * 16 + localX). Keep the translated surface window in that convention
                // and explicitly transpose at the bridge instead of letting surface materials
                // silently apply to the opposite X/Z column inside every chunk.
                final int surfaceColumn = localZ * 16 + localX;
                final int physicalBase = physicalColumn * physicalHeight;
                final int logicalBase = surfaceColumn * SURFACE_WINDOW_HEIGHT;

                for (int logicalY = 0; logicalY < SURFACE_WINDOW_HEIGHT; ++logicalY) {
                    final int physicalY = SURFACE_WINDOW_MIN_PHYSICAL_Y + logicalY;
                    logicalBlocks[logicalBase + logicalY] = blocks[physicalBase + physicalY];
                    logicalMetadata[logicalBase + logicalY] = metadata[physicalBase + physicalY];
                }

                for (int y = 0; y < 5; ++y) {
                    preservedZeroBand[surfaceColumn * 5 + y] = blocks[physicalBase + SURFACE_WINDOW_MIN_PHYSICAL_Y + y];
                    preservedZeroBandMeta[surfaceColumn * 5 + y] = metadata[physicalBase + SURFACE_WINDOW_MIN_PHYSICAL_Y + y];
                }
            }
        }

        final int originX = chunkX << 4;
        final int originZ = chunkZ << 4;
        for (int localX = 0; localX < 16; ++localX) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                final int biomeIndex = localX + localZ * 16;
                final BiomeGenBase biome = biomes[biomeIndex];
                biome.genTerrainBlocks(world, rand, logicalBlocks, logicalMetadata,
                        originX + localX, originZ + localZ, surfaceNoise[biomeIndex]);
            }
        }

        for (int localX = 0; localX < 16; ++localX) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                final int physicalColumn = localX * 16 + localZ;
                // BiomeGenBase.genBiomeTerrain internally indexes its temporary block buffer as
                // (localZ * 16 + localX). Keep the translated surface window in that convention
                // and explicitly transpose at the bridge instead of letting surface materials
                // silently apply to the opposite X/Z column inside every chunk.
                final int surfaceColumn = localZ * 16 + localX;
                final int physicalBase = physicalColumn * physicalHeight;
                final int logicalBase = surfaceColumn * SURFACE_WINDOW_HEIGHT;

                for (int logicalY = 0; logicalY < SURFACE_WINDOW_HEIGHT; ++logicalY) {
                    final int physicalY = SURFACE_WINDOW_MIN_PHYSICAL_Y + logicalY;
                    blocks[physicalBase + physicalY] = logicalBlocks[logicalBase + logicalY];
                    metadata[physicalBase + physicalY] = logicalMetadata[logicalBase + logicalY];
                }

                for (int y = 0; y < 5; ++y) {
                    blocks[physicalBase + SURFACE_WINDOW_MIN_PHYSICAL_Y + y] = preservedZeroBand[surfaceColumn * 5 + y];
                    metadata[physicalBase + SURFACE_WINDOW_MIN_PHYSICAL_Y + y] = preservedZeroBandMeta[surfaceColumn * 5 + y];
                }
            }
        }
    }

    public int sampleSurfaceLogicalY(int x, int z, boolean amplified) {
        // Terrain shape must be continuous and independent of the legacy 1.7.10 biome ID map.
        // P007 originally applied hard OCEAN/RIVER/SWAMP/BEACH height clamps from the old 2D
        // GenLayer biome layout. Those discrete biome edges cut sea-level trenches through hills
        // and mountains. Modern terrain owns elevation first; biome placement is a later stage.
        final double continent = continentalness.sample(x, z, 1.0D / 2048.0D);
        final double erosionValue = erosion.sample(x, z, 1.0D / 896.0D);
        final double weirdnessValue = weirdness.sample(x, z, 1.0D / 704.0D);
        final double jaggedValue = jaggedness.sample(x, z, 1.0D / 320.0D);
        final double detailValue = detail.sample(x, z, 1.0D / 112.0D);

        double surface = continentalBaseHeight(continent);
        final double peaksValleys = peaksAndValleys(weirdnessValue);
        final double inland = smoothstep(-0.08D, 0.32D, continent);
        final double lowErosion = 1.0D - smoothstep(-0.55D, 0.45D, erosionValue);
        final double mountain = mountainStrength(continent, erosionValue, weirdnessValue);
        final double valley = inland * lowErosion * smoothstep(0.18D, 0.95D, -peaksValleys);

        // Low erosion + positive ridge/peak values produce the large mountain ranges. Rare jagged
        // positive noise pushes the tallest peaks into the modern 220-250 logical-Y region.
        surface += mountain * (52.0D + 116.0D * mountain);
        surface += mountain * Math.max(0.0D, jaggedValue) * 34.0D;
        surface -= valley * 24.0D;

        final double roughness = 1.0D - smoothstep(-0.35D, 0.75D, erosionValue);

        /*
         * Never switch detail amplitude with a hard coast boolean. P007d still used
         * `continent < -0.04`, and crossing that one value could change the same adjacent
         * seabed by 4-5 blocks even though every noise field itself was continuous. Blend the
         * quieter ocean-floor detail into the inland roughness over a broad continental band.
         */
        final double inlandDetailAmplitude = 4.0D + roughness * 7.0D + mountain * 9.0D;
        final double landDetailBlend = smoothstep(-0.16D, 0.10D, continent);
        final double detailAmplitude = lerp(3.0D, inlandDetailAmplitude, landDetailBlend);
        surface += detailValue * detailAmplitude;

        if (amplified && surface > WorldHeightCompat.MODERN_SEA_LEVEL) {
            surface = WorldHeightCompat.MODERN_SEA_LEVEL
                    + (surface - WorldHeightCompat.MODERN_SEA_LEVEL) * 1.55D;
        }

        return (int) Math.round(clamp(surface, MIN_TERRAIN_Y, MAX_TERRAIN_Y));
    }


    /** Physical-space surface block Y for structure/biome consumers that share the P006 +64 mapping. */
    public int sampleSurfacePhysicalY(int x, int z, boolean amplified) {
        return WorldHeightCompat.modernToPhysicalY(sampleSurfaceLogicalY(x, z, amplified));
    }

    public double sampleContinentalness(int x, int z) {
        return continentalness.sample(x, z, 1.0D / 2048.0D);
    }

    public double sampleErosion(int x, int z) {
        return erosion.sample(x, z, 1.0D / 896.0D);
    }

    public double sampleWeirdness(int x, int z) {
        return weirdness.sample(x, z, 1.0D / 704.0D);
    }

    /**
     * Shared terrain-shape signal for biome placement. This is deliberately the exact mountain
     * factor used by {@link #sampleSurfaceLogicalY}; biome ecology must follow the broad ridge/
     * erosion landform rather than turning on only at an arbitrary surface-Y or local-slope contour.
     */
    public double sampleMountainStrength(int x, int z) {
        final double continent = sampleContinentalness(x, z);
        final double erosionValue = sampleErosion(x, z);
        final double weirdnessValue = sampleWeirdness(x, z);
        return mountainStrength(continent, erosionValue, weirdnessValue);
    }

    private static double mountainStrength(double continent, double erosionValue, double weirdnessValue) {
        final double inland = smoothstep(-0.08D, 0.32D, continent);
        final double lowErosion = 1.0D - smoothstep(-0.55D, 0.45D, erosionValue);
        final double peak = smoothstep(0.08D, 0.92D, peaksAndValleys(weirdnessValue));
        return inland * lowErosion * peak;
    }

    private static double continentalBaseHeight(double continent) {
        // Broad piecewise spline: deep ocean -> ocean shelf -> coast -> inland -> high inland.
        if (continent < -0.62D) {
            return lerp(18.0D, 30.0D, inverseLerp(-1.0D, -0.62D, continent));
        }
        if (continent < -0.35D) {
            return lerp(30.0D, 43.0D, inverseLerp(-0.62D, -0.35D, continent));
        }
        if (continent < -0.16D) {
            return lerp(43.0D, 56.0D, inverseLerp(-0.35D, -0.16D, continent));
        }
        if (continent < -0.04D) {
            return lerp(56.0D, 63.0D, inverseLerp(-0.16D, -0.04D, continent));
        }
        if (continent < 0.18D) {
            return lerp(63.0D, 72.0D, inverseLerp(-0.04D, 0.18D, continent));
        }
        if (continent < 0.48D) {
            return lerp(72.0D, 84.0D, inverseLerp(0.18D, 0.48D, continent));
        }
        if (continent < 0.72D) {
            return lerp(84.0D, 96.0D, inverseLerp(0.48D, 0.72D, continent));
        }
        return lerp(96.0D, 108.0D, inverseLerp(0.72D, 1.0D, continent));
    }

    private Block baseRock(int x, int logicalY, int z) {
        final Block deepslate = ModBlocks.DEEPSLATE.isEnabled() ? ModBlocks.DEEPSLATE.get() : Blocks.stone;
        if (logicalY <= 0) {
            return deepslate;
        }
        if (logicalY >= 8 || deepslate == Blocks.stone) {
            return Blocks.stone;
        }

        // Modern deepslate transitions gradually through logical Y0..8 rather than ending on a
        // perfectly flat boundary. Hashing keeps the transition deterministic and chunk-order safe.
        final double deepslateChance = (8.0D - logicalY) / 8.0D;
        return unitHash(x, logicalY, z, seed ^ 0xA54FF53A5FL) < deepslateChance ? deepslate : Blocks.stone;
    }

    private boolean isBedrock(int x, int physicalY, int z) {
        if (physicalY < 0 || physicalY > 4) {
            return false;
        }
        final double chance = (5.0D - physicalY) / 5.0D;
        return unitHash(x, physicalY, z, seed ^ 0x510E527FADEL) < chance;
    }

    private static double peaksAndValleys(double weirdness) {
        return -(Math.abs(Math.abs(weirdness) - 0.6666666666666666D) - 0.3333333333333333D) * 3.0D;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge0 == edge1) {
            return value < edge0 ? 0.0D : 1.0D;
        }
        final double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    private static double inverseLerp(double a, double b, double value) {
        return clamp((value - a) / (b - a), 0.0D, 1.0D);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static double unitHash(int x, int y, int z, long salt) {
        long value = salt;
        value ^= (long) x * 0x632BE59BD9B4E019L;
        value ^= (long) y * 0x9E3779B97F4A7C15L;
        value ^= (long) z * 0x94D049BB133111EBL;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (double) (value >>> 11) * 0x1.0p-53;
    }

    private static final class FractalNoise {
        private final NoiseGeneratorSimplex[] octaves;
        private final double persistence;
        private final double normalizer;

        private FractalNoise(long seed, int octaveCount, double persistence) {
            this.persistence = persistence;
            this.octaves = new NoiseGeneratorSimplex[octaveCount];
            final Random random = new Random(seed);
            double amplitude = 1.0D;
            double sum = 0.0D;
            for (int i = 0; i < octaveCount; ++i) {
                this.octaves[i] = new NoiseGeneratorSimplex(random);
                sum += amplitude;
                amplitude *= persistence;
            }
            this.normalizer = sum;
        }

        private double sample(double x, double z, double baseScale) {
            double frequency = baseScale;
            double amplitude = 1.0D;
            double value = 0.0D;
            for (NoiseGeneratorSimplex octave : octaves) {
                value += octave.func_151605_a(x * frequency, z * frequency) * amplitude;
                frequency *= 2.0D;
                amplitude *= persistence;
            }
            return clamp(value / normalizer, -1.0D, 1.0D);
        }
    }
}
