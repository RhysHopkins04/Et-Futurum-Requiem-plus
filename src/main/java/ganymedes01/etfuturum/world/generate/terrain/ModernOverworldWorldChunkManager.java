package ganymedes01.etfuturum.world.generate.terrain;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.gen.NoiseGeneratorSimplex;

import java.util.List;
import java.util.Random;

/**
 * P007g surface-biome source for the Plus modern Overworld path.
 *
 * <p>P007e made terrain/coast geometry continuous, but its biome source still classified every X/Z
 * column independently from smooth climate noises. A nearest-climate boundary could therefore cut
 * one- or two-column Plains/Taiga/Forest fragments through an otherwise coherent region, while a
 * second set of per-column coast decisions independently chose Ocean/Beach/Stone Beach. Runtime
 * testing exposed exactly those topology failures.</p>
 *
 * <p>P007f keeps terrain shape completely independent and replaces that resolver with two separate
 * clean-room layers:</p>
 * <ol>
 *     <li>a deterministic jittered macro-climate lattice which samples broad climate/terrain
 *     parameters once per regional site and selects land ecology from parameter ranges; and</li>
 *     <li>a continuous continental topology pass which decides true offshore water versus shore
 *     versus inland water without treating every local depression as an ocean or beach.</li>
 * </ol>
 *
 * <p>The lattice deliberately does not recreate the old 1.7 GenLayer stack. Minecraft 1.7.10 still
 * receives one 2D surface biome ID per column, but those IDs now come from coherent multi-parameter
 * regions. P007g keeps those regions intact while blending only a narrow band around the two nearest
 * regional sites. Where a hot/arid region meets a temperate one, that band becomes a Savanna ecotone
 * instead of a single hard Desert-to-grass seam. Ocean water also requires a short deterministic
 * surface-water path toward lower continentalness, so a closed depression cut into an otherwise real
 * beach no longer inherits Ocean just because the broad continental field is coastal.</p>
 */
public final class ModernOverworldWorldChunkManager extends WorldChunkManager {

    private static final long SALT_TEMPERATURE = 0x243F6A8885A308D3L;
    private static final long SALT_HUMIDITY = 0x13198A2E03707344L;
    private static final long SALT_VARIANT = 0xA4093822299F31D0L;
    private static final long SALT_CELL_JITTER_X = 0x082EFA98EC4E6C89L;
    private static final long SALT_CELL_JITTER_Z = 0x452821E638D01377L;

    /* Broad climate fields. LARGE_BIOMES scales the regional lattice rather than changing terrain. */
    private static final double TEMPERATURE_SCALE = 1.0D / 4096.0D;
    private static final double HUMIDITY_SCALE = 1.0D / 3584.0D;
    private static final double VARIANT_SCALE = 1.0D / 2048.0D;
    private static final double CLIMATE_RANGE = 1.70D;

    /*
     * One macro site per 64x64 region is intentionally much coarser than the final 1-block biome
     * array. The bounded jitter gives organic Voronoi-like boundaries while guaranteeing that a
     * single climate decision owns a meaningful area instead of a one-column sliver.
     */
    private static final int NORMAL_CLIMATE_CELL_SIZE = 64;
    private static final int LARGE_CLIMATE_CELL_SIZE = 256;
    private static final double CLIMATE_CELL_JITTER = 0.22D;
    private static final double CLIMATE_TRANSITION_WIDTH_FRACTION = 0.30D;
    private static final double ECOTONE_SECONDARY_INFLUENCE_MIN = 0.16D;

    /*
     * Continental topology is intentionally separate from land-biome selection. Water below sea
     * level on the ocean/coast side of the broad continental transition is ocean; a depression on
     * the inland side keeps its land biome (future hydrology can later own lake/river biomes).
     * Shore material is only permitted in the true continental transition band and never rings an
     * arbitrary inland pond.
     */
    private static final double OCEAN_CORE_CONTINENT_MAX = -0.060D;
    private static final double COAST_TOPOLOGY_CONTINENT_MAX = 0.14D;
    private static final double SHORE_LAND_CONTINENT_MAX = 0.020D;
    private static final double DEEP_OCEAN_CONTINENT_MAX = -0.34D;
    private static final int COAST_TOPOLOGY_FAR_RADIUS = 192;
    private static final int COAST_TOPOLOGY_FAR_DIAGONAL = 136;
    private static final int DEEP_OCEAN_DEPTH = 18;
    private static final int OPEN_WATER_NEAR_RADIUS = 12;
    private static final int OPEN_WATER_FAR_RADIUS = 40;
    private static final double OPEN_WATER_CONTINENT_DROP_MIN = 0.010D;
    private static final double STONY_SHORE_RUGGEDNESS_MIN = 0.30D;
    private static final double STONY_SHORE_TEMPERATURE_MAX = 0.12D;

    /* Parameter-space weights. Distances are zero while a sample lies inside a target span. */
    private static final double WEIGHT_TEMPERATURE = 3.20D;
    private static final double WEIGHT_HUMIDITY = 2.15D;
    private static final double WEIGHT_CONTINENT = 0.70D;
    private static final double WEIGHT_EROSION = 0.50D;
    private static final double WEIGHT_WEIRDNESS = 0.34D;
    private static final double WEIGHT_VARIANT = 0.38D;
    private static final double WEIGHT_MOUNTAIN = 2.40D;

    private static final ParameterSpan ANY = new ParameterSpan(-1.0D, 1.0D);

    /*
     * Clean-room surface parameter table. Specific regional variants precede their broader parent
     * climates so an in-range specialised target wins a zero-distance tie. Most ordinary land
     * targets stop accepting very strong mountain signal; arid climates intentionally do not, so a
     * desert/savanna ridge does not turn into a temperate Extreme Hills stripe.
     */
    private static final ClimateTarget[] LAND_TARGETS = new ClimateTarget[] {
            target(BiomeGenBase.mesa,
                    span(0.50D, 1.0D), span(-1.0D, -0.28D), span(0.16D, 1.0D), ANY, ANY,
                    span(0.38D, 1.0D), ANY),
            target(BiomeGenBase.roofedForest,
                    span(-0.10D, 0.34D), span(0.48D, 1.0D), span(0.16D, 1.0D), ANY, ANY,
                    span(0.30D, 1.0D), span(-1.0D, 0.20D)),
            target(BiomeGenBase.birchForest,
                    span(-0.12D, 0.40D), span(0.24D, 0.78D), span(0.08D, 1.0D), ANY, ANY,
                    span(-1.0D, -0.30D), span(-1.0D, 0.30D)),
            target(BiomeGenBase.swampland,
                    span(-0.02D, 0.50D), span(0.58D, 1.0D), span(0.02D, 0.30D),
                    span(0.02D, 1.0D), ANY, ANY, span(-1.0D, 0.12D)),

            target(BiomeGenBase.desert,
                    span(0.48D, 1.0D), span(-1.0D, -0.18D), ANY, ANY, ANY, ANY, ANY),
            target(BiomeGenBase.savanna,
                    span(0.34D, 1.0D), span(-0.32D, 0.30D), ANY, ANY, ANY, ANY, ANY),

            target(BiomeGenBase.iceMountains,
                    span(-1.0D, -0.30D), ANY, span(0.04D, 1.0D), ANY, ANY, ANY,
                    span(0.22D, 1.0D)),
            target(BiomeGenBase.extremeHillsPlus,
                    span(-0.30D, 0.46D), span(-0.18D, 1.0D), span(0.06D, 1.0D),
                    span(-1.0D, 0.18D), ANY, span(0.14D, 1.0D), span(0.42D, 1.0D)),
            target(BiomeGenBase.extremeHills,
                    span(-0.30D, 0.48D), ANY, span(0.04D, 1.0D), ANY, ANY, ANY,
                    span(0.22D, 1.0D)),

            target(BiomeGenBase.coldTaiga,
                    span(-0.72D, -0.30D), span(0.08D, 1.0D), span(0.0D, 1.0D), ANY, ANY, ANY,
                    span(-1.0D, 0.34D)),
            target(BiomeGenBase.icePlains,
                    span(-1.0D, -0.30D), ANY, ANY, ANY, ANY, ANY, span(-1.0D, 0.36D)),
            target(BiomeGenBase.taiga,
                    span(-0.44D, -0.10D), span(-0.08D, 0.74D), span(0.0D, 1.0D), ANY, ANY, ANY,
                    span(-1.0D, 0.34D)),
            target(BiomeGenBase.jungle,
                    span(0.46D, 1.0D), span(0.38D, 1.0D), span(0.08D, 1.0D), ANY, ANY, ANY,
                    span(-1.0D, 0.32D)),
            target(BiomeGenBase.forest,
                    span(-0.16D, 0.44D), span(0.16D, 0.80D), span(0.02D, 1.0D), ANY, ANY, ANY,
                    span(-1.0D, 0.34D)),
            target(BiomeGenBase.plains,
                    span(-0.20D, 0.40D), span(-1.0D, 0.30D), ANY, ANY, ANY, ANY,
                    span(-1.0D, 0.38D))
    };

    private final long seed;
    private final ModernOverworldTerrainGenerator terrain;
    private final FractalNoise temperature;
    private final FractalNoise humidity;
    private final FractalNoise variant;
    private final boolean amplified;
    private final int climateCellSize;

    public ModernOverworldWorldChunkManager(World world) {
        this(world.getSeed(),
                world.getWorldInfo().getTerrainType() == WorldType.AMPLIFIED,
                world.getWorldInfo().getTerrainType() == WorldType.LARGE_BIOMES);
    }

    /* Package-private deterministic constructor used by offline diagnostics without a live World. */
    ModernOverworldWorldChunkManager(long seed, boolean amplified, boolean largeBiomes) {
        super();
        this.seed = seed;
        this.terrain = new ModernOverworldTerrainGenerator(seed);
        this.temperature = new FractalNoise(seed ^ SALT_TEMPERATURE, 3, 0.42D);
        this.humidity = new FractalNoise(seed ^ SALT_HUMIDITY, 3, 0.42D);
        this.variant = new FractalNoise(seed ^ SALT_VARIANT, 2, 0.48D);
        this.amplified = amplified;
        this.climateCellSize = largeBiomes ? LARGE_CLIMATE_CELL_SIZE : NORMAL_CLIMATE_CELL_SIZE;
    }

    @Override
    public BiomeGenBase getBiomeGenAt(int x, int z) {
        return resolveBiome(x, z);
    }

    @Override
    public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] reuse, int x, int z, int width, int length) {
        reuse = ensureCapacity(reuse, width * length);
        for (int dz = 0; dz < length; ++dz) {
            for (int dx = 0; dx < width; ++dx) {
                // This legacy method operates on the coarse 1:4 biome-generation grid.
                reuse[dx + dz * width] = resolveBiome((x + dx) << 2, (z + dz) << 2);
            }
        }
        return reuse;
    }

    @Override
    public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase[] reuse, int x, int z, int width, int length) {
        return getBiomeGenAt(reuse, x, z, width, length, false);
    }

    @Override
    public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] reuse, int x, int z, int width, int length, boolean cacheFlag) {
        reuse = ensureCapacity(reuse, width * length);
        for (int dz = 0; dz < length; ++dz) {
            for (int dx = 0; dx < width; ++dx) {
                reuse[dx + dz * width] = resolveBiome(x + dx, z + dz);
            }
        }
        return reuse;
    }

    @Override
    public float[] getRainfall(float[] reuse, int x, int z, int width, int length) {
        if (reuse == null || reuse.length < width * length) {
            reuse = new float[width * length];
        }
        for (int dz = 0; dz < length; ++dz) {
            for (int dx = 0; dx < width; ++dx) {
                float rainfall = (float) resolveBiome(x + dx, z + dz).getIntRainfall() / 65536.0F;
                reuse[dx + dz * width] = rainfall > 1.0F ? 1.0F : rainfall;
            }
        }
        return reuse;
    }

    @Override
    public boolean areBiomesViable(int x, int z, int radius, List<BiomeGenBase> allowed) {
        final int minX = x - radius;
        final int maxX = x + radius;
        final int minZ = z - radius;
        final int maxZ = z + radius;
        for (int sampleZ = minZ; sampleZ <= maxZ; sampleZ += 4) {
            for (int sampleX = minX; sampleX <= maxX; sampleX += 4) {
                if (!allowed.contains(resolveBiome(sampleX, sampleZ))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ChunkPosition findBiomePosition(int x, int z, int radius, List<BiomeGenBase> allowed, Random random) {
        ChunkPosition selected = null;
        int matches = 0;
        for (int sampleZ = z - radius; sampleZ <= z + radius; sampleZ += 4) {
            for (int sampleX = x - radius; sampleX <= x + radius; sampleX += 4) {
                if (allowed.contains(resolveBiome(sampleX, sampleZ))) {
                    if (selected == null || random.nextInt(matches + 1) == 0) {
                        selected = new ChunkPosition(sampleX, 0, sampleZ);
                    }
                    ++matches;
                }
            }
        }
        return selected;
    }

    private BiomeGenBase resolveBiome(int x, int z) {
        final int surface = terrain.sampleSurfaceLogicalY(x, z, amplified);
        final double localContinent = terrain.sampleContinentalness(x, z);
        final int sea = WorldHeightCompat.MODERN_SEA_LEVEL;
        final ClimateResolution climateResolution = sampleMacroClimate(x, z);
        final ClimatePoint climate = climateResolution.blended;
        final BiomeGenBase inlandBiome = selectTransitionLandBiome(climateResolution);

        /*
         * Offshore topology owns submerged columns throughout the continental coast transition. A
         * low local detail depression farther inland intentionally keeps its land biome so future
         * rivers/lakes can be introduced without turning every pond into Ocean today.
         */
        final boolean coastalTopology = isCoastalTopology(x, z, localContinent);
        if (surface < sea && coastalTopology && isOpenCoastalWater(x, z, localContinent)) {
            if (isFrozenClimate(climate, inlandBiome)) {
                return BiomeGenBase.frozenOcean;
            }
            return localContinent <= DEEP_OCEAN_CONTINENT_MAX || surface <= sea - DEEP_OCEAN_DEPTH
                    ? BiomeGenBase.deepOcean
                    : BiomeGenBase.ocean;
        }

        /*
         * Shore is a topology band, not an adjacency-to-water rule. Once a dry column belongs to
         * the real coast topology, do not reintroduce a surface-height contour that can leave a
         * one-block Plains/Taiga/Desert pinhole inside the shore. Rugged macro climate chooses a
         * stony shore where appropriate.
         */
        if (surface >= sea
                && coastalTopology
                && localContinent <= SHORE_LAND_CONTINENT_MAX) {
            return selectShoreBiome(inlandBiome, climate);
        }

        return inlandBiome;
    }


    /**
     * Approximate open-ocean connectivity from the broad continental field rather than from the
     * presence of water alone. Core ocean is unconditional. Transitional lowlands must see an
     * ocean-core sample within the coast radius, preventing an isolated inland depression from
     * acquiring Ocean/Beach semantics while still allowing near-shore submerged columns to stop
     * inheriting Forest/Taiga/Desert IDs.
     */
    private boolean isCoastalTopology(int x, int z, double localContinent) {
        if (localContinent <= OCEAN_CORE_CONTINENT_MAX) {
            return true;
        }
        if (localContinent > COAST_TOPOLOGY_CONTINENT_MAX) {
            return false;
        }

        final int far = COAST_TOPOLOGY_FAR_RADIUS;
        final int diagonal = COAST_TOPOLOGY_FAR_DIAGONAL;
        return hasOceanCorridor(x, z, far, 0)
                || hasOceanCorridor(x, z, -far, 0)
                || hasOceanCorridor(x, z, 0, far)
                || hasOceanCorridor(x, z, 0, -far)
                || hasOceanCorridor(x, z, diagonal, diagonal)
                || hasOceanCorridor(x, z, diagonal, -diagonal)
                || hasOceanCorridor(x, z, -diagonal, diagonal)
                || hasOceanCorridor(x, z, -diagonal, -diagonal);
    }

    /**
     * A distant ocean-core sample is not enough by itself: an inland depression must not see
     * "through" a higher continental ridge and become Ocean merely because the sea happens to be
     * within the search radius. Requiring the broad-field midpoint to remain inside the coast
     * transition gives the topology test a simple deterministic continental corridor without
     * coupling it to individual water blocks or terrain-height adjacency.
     */
    private boolean hasOceanCorridor(int x, int z, int offsetX, int offsetZ) {
        if (terrain.sampleContinentalness(x + offsetX, z + offsetZ) > OCEAN_CORE_CONTINENT_MAX) {
            return false;
        }
        return terrain.sampleContinentalness(x + offsetX / 2, z + offsetZ / 2)
                <= COAST_TOPOLOGY_CONTINENT_MAX;
    }

    /**
     * A submerged transitional coast column must have actual surface-water continuity toward lower
     * continentalness before it becomes Ocean. Core-ocean continentalness remains unconditional.
     * This specifically prevents an enclosed beach-side pool from alternating Beach/Ocean/Beach
     * simply because the broad coast topology passes underneath it.
     */
    private boolean isOpenCoastalWater(int x, int z, double localContinent) {
        if (localContinent <= OCEAN_CORE_CONTINENT_MAX) {
            return true;
        }
        if (localContinent > COAST_TOPOLOGY_CONTINENT_MAX) {
            return false;
        }

        return hasOpenWaterRay(x, z, localContinent, 1, 0)
                || hasOpenWaterRay(x, z, localContinent, -1, 0)
                || hasOpenWaterRay(x, z, localContinent, 0, 1)
                || hasOpenWaterRay(x, z, localContinent, 0, -1);
    }

    private boolean hasOpenWaterRay(int x, int z, double localContinent, int directionX, int directionZ) {
        final int sea = WorldHeightCompat.MODERN_SEA_LEVEL;
        if (terrain.sampleSurfaceLogicalY(
                x + directionX * OPEN_WATER_NEAR_RADIUS,
                z + directionZ * OPEN_WATER_NEAR_RADIUS,
                amplified) >= sea
                || terrain.sampleSurfaceLogicalY(
                        x + directionX * OPEN_WATER_FAR_RADIUS,
                        z + directionZ * OPEN_WATER_FAR_RADIUS,
                        amplified) >= sea) {
            return false;
        }

        final double farContinent = terrain.sampleContinentalness(
                x + directionX * OPEN_WATER_FAR_RADIUS,
                z + directionZ * OPEN_WATER_FAR_RADIUS);
        return farContinent <= OCEAN_CORE_CONTINENT_MAX
                || farContinent <= localContinent - OPEN_WATER_CONTINENT_DROP_MIN;
    }

    private ClimateResolution sampleMacroClimate(int x, int z) {
        final ClimateSiteSelection selection = selectClimateSite(x, z);
        final ClimatePoint primary = sampleClimateAtSite(selection.primary);

        /*
         * The nearest-site lattice remains the authority away from a boundary. Only the narrow
         * region where the first- and second-nearest sites are similarly distant receives a smooth
         * interpolation. Because the secondary contribution reaches exactly 0.5 at the Voronoi
         * boundary, swapping primary/secondary on the other side remains continuous rather than
         * recreating the P007f hard climate-value jump. Interior columns retain P007f's single-site
         * sampling cost; the second climate site is sampled only inside the transition band.
         */
        final double transitionWidth = climateCellSize * CLIMATE_TRANSITION_WIDTH_FRACTION;
        final double distanceGap = Math.max(0.0D,
                Math.sqrt(selection.secondary.distanceSquared) - Math.sqrt(selection.primary.distanceSquared));
        if (distanceGap >= transitionWidth) {
            return new ClimateResolution(primary, primary, primary, 0.0D);
        }

        final ClimatePoint secondary = sampleClimateAtSite(selection.secondary);
        final double edgeDistance = clamp(distanceGap / transitionWidth, 0.0D, 1.0D);
        final double secondaryInfluence = 0.5D * (1.0D - smoothstep01(edgeDistance));
        final ClimatePoint blended = ClimatePoint.lerp(primary, secondary, secondaryInfluence);
        return new ClimateResolution(primary, secondary, blended, secondaryInfluence);
    }

    private ClimatePoint sampleClimateAtSite(ClimateSite site) {
        final int sampleX = site.sampleX;
        final int sampleZ = site.sampleZ;
        return new ClimatePoint(
                climateValue(temperature.sample(sampleX, sampleZ, TEMPERATURE_SCALE)),
                climateValue(humidity.sample(sampleX, sampleZ, HUMIDITY_SCALE)),
                terrain.sampleContinentalness(sampleX, sampleZ),
                terrain.sampleErosion(sampleX, sampleZ),
                terrain.sampleWeirdness(sampleX, sampleZ),
                variant.sample(sampleX, sampleZ, VARIANT_SCALE),
                terrain.sampleMountainStrength(sampleX, sampleZ));
    }

    /** Find the two nearest deterministically jittered macro-climate sites around this block column. */
    private ClimateSiteSelection selectClimateSite(int x, int z) {
        final int baseCellX = Math.floorDiv(x, climateCellSize);
        final int baseCellZ = Math.floorDiv(z, climateCellSize);
        ClimateSite primary = null;
        ClimateSite secondary = null;

        for (int dz = -1; dz <= 1; ++dz) {
            for (int dx = -1; dx <= 1; ++dx) {
                final int cellX = baseCellX + dx;
                final int cellZ = baseCellZ + dz;
                final double centerX = ((double) cellX + 0.5D) * climateCellSize
                        + cellJitter(cellX, cellZ, SALT_CELL_JITTER_X) * climateCellSize * CLIMATE_CELL_JITTER;
                final double centerZ = ((double) cellZ + 0.5D) * climateCellSize
                        + cellJitter(cellX, cellZ, SALT_CELL_JITTER_Z) * climateCellSize * CLIMATE_CELL_JITTER;
                final double deltaX = x - centerX;
                final double deltaZ = z - centerZ;
                final ClimateSite candidate = new ClimateSite(
                        (int) Math.round(centerX),
                        (int) Math.round(centerZ),
                        deltaX * deltaX + deltaZ * deltaZ);

                if (primary == null || candidate.distanceSquared < primary.distanceSquared) {
                    secondary = primary;
                    primary = candidate;
                } else if (secondary == null || candidate.distanceSquared < secondary.distanceSquared) {
                    secondary = candidate;
                }
            }
        }

        return new ClimateSiteSelection(primary, secondary);
    }

    private double cellJitter(int cellX, int cellZ, long salt) {
        long value = seed ^ salt;
        value ^= (long) cellX * 0x632BE59BD9B4E019L;
        value ^= (long) cellZ * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return ((double) (value >>> 11) * 0x1.0p-53) * 2.0D - 1.0D;
    }

    private static BiomeGenBase selectParameterizedLandBiome(ClimatePoint climate) {
        ClimateTarget selected = LAND_TARGETS[0];
        double bestScore = selected.distance(climate);
        for (int i = 1; i < LAND_TARGETS.length; ++i) {
            final ClimateTarget candidate = LAND_TARGETS[i];
            final double score = candidate.distance(climate);
            if (score < bestScore) {
                selected = candidate;
                bestScore = score;
            }
        }
        return selected.biome;
    }

    private static BiomeGenBase selectTransitionLandBiome(ClimateResolution resolution) {
        final BiomeGenBase blendedBiome = selectParameterizedLandBiome(resolution.blended);
        if (resolution.secondaryInfluence < ECOTONE_SECONDARY_INFLUENCE_MIN) {
            return blendedBiome;
        }

        final BiomeGenBase primaryBiome = selectParameterizedLandBiome(resolution.primary);
        final BiomeGenBase secondaryBiome = selectParameterizedLandBiome(resolution.secondary);
        if (primaryBiome == secondaryBiome) {
            return blendedBiome;
        }

        /*
         * Sand-to-grass is the visually harsh legacy surface seam reported after P007f. Treat the
         * narrow boundary between an arid macro region and a non-arid, non-frozen region as a dry
         * grassland ecotone. This is regional endpoint logic, not a per-column height/temperature
         * threshold: Desert/Mesa retain their interior and the neighbouring macro biome retains its
         * interior, while only their shared transition receives Savanna surface/ecology.
         */
        if (isAridBiome(primaryBiome) != isAridBiome(secondaryBiome)
                && !isFrozenLandBiome(primaryBiome)
                && !isFrozenLandBiome(secondaryBiome)) {
            return BiomeGenBase.savanna;
        }

        return blendedBiome;
    }

    private static BiomeGenBase selectShoreBiome(BiomeGenBase inlandBiome, ClimatePoint climate) {
        // Cold shoreline wins before rugged shoreline so Stone Beach cannot punch warm-looking gaps
        // through a snowy coast. The same macro temperature also controls Frozen Ocean offshore.
        if (isFrozenClimate(climate, inlandBiome)) {
            return BiomeGenBase.coldBeach;
        }

        // Hot/arid coasts stay warm even where the ridge signal is rugged; Stone Beach in 1.7 has
        // its own climate semantics and must not manufacture snowy shoreline beside Desert/Mesa.
        if (isAridBiome(inlandBiome)) {
            return BiomeGenBase.beach;
        }

        // Broad regional ruggedness, not a four-block local slope threshold, chooses stony shore.
        final double ruggedness = Math.max(0.0D, -climate.erosion) * 0.65D
                + climate.mountain * 0.75D
                + Math.max(0.0D, Math.abs(climate.weirdness) - 0.55D) * 0.25D;
        /*
         * The legacy 1.7 Stone Beach biome is intrinsically cool. Restrict it to a cool macro
         * climate as well as rugged terrain so its legacy snow/temperature semantics cannot create
         * a visually cold stony patch inside an otherwise warm coast.
         */
        return ruggedness > STONY_SHORE_RUGGEDNESS_MIN
                && climate.temperature <= STONY_SHORE_TEMPERATURE_MAX
                        ? BiomeGenBase.stoneBeach
                        : BiomeGenBase.beach;
    }

    private static boolean isFrozenClimate(ClimatePoint climate, BiomeGenBase inlandBiome) {
        return climate.temperature <= -0.30D
                || inlandBiome == BiomeGenBase.icePlains
                || inlandBiome == BiomeGenBase.iceMountains
                || inlandBiome == BiomeGenBase.coldTaiga;
    }

    private static boolean isFrozenLandBiome(BiomeGenBase biome) {
        return biome == BiomeGenBase.icePlains
                || biome == BiomeGenBase.iceMountains
                || biome == BiomeGenBase.coldTaiga;
    }

    private static boolean isAridBiome(BiomeGenBase biome) {
        return biome == BiomeGenBase.desert
                || biome == BiomeGenBase.savanna
                || biome == BiomeGenBase.mesa;
    }

    private static ClimateTarget target(BiomeGenBase biome,
            ParameterSpan temperature, ParameterSpan humidity, ParameterSpan continent,
            ParameterSpan erosion, ParameterSpan weirdness, ParameterSpan variant,
            ParameterSpan mountain) {
        return new ClimateTarget(biome, temperature, humidity, continent, erosion, weirdness, variant, mountain);
    }

    private static ParameterSpan span(double min, double max) {
        return new ParameterSpan(min, max);
    }

    private static double climateValue(double raw) {
        return clamp(raw * CLIMATE_RANGE, -1.0D, 1.0D);
    }

    private static BiomeGenBase[] ensureCapacity(BiomeGenBase[] reuse, int size) {
        return reuse == null || reuse.length < size ? new BiomeGenBase[size] : reuse;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smoothstep01(double value) {
        final double t = clamp(value, 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : value > max ? max : value;
    }

    private static final class ClimateSite {
        private final int sampleX;
        private final int sampleZ;
        private final double distanceSquared;

        private ClimateSite(int sampleX, int sampleZ, double distanceSquared) {
            this.sampleX = sampleX;
            this.sampleZ = sampleZ;
            this.distanceSquared = distanceSquared;
        }
    }

    private static final class ClimateSiteSelection {
        private final ClimateSite primary;
        private final ClimateSite secondary;

        private ClimateSiteSelection(ClimateSite primary, ClimateSite secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }
    }

    private static final class ClimateResolution {
        private final ClimatePoint primary;
        private final ClimatePoint secondary;
        private final ClimatePoint blended;
        private final double secondaryInfluence;

        private ClimateResolution(ClimatePoint primary, ClimatePoint secondary, ClimatePoint blended,
                double secondaryInfluence) {
            this.primary = primary;
            this.secondary = secondary;
            this.blended = blended;
            this.secondaryInfluence = secondaryInfluence;
        }
    }

    private static final class ClimatePoint {
        private final double temperature;
        private final double humidity;
        private final double continent;
        private final double erosion;
        private final double weirdness;
        private final double variant;
        private final double mountain;

        private ClimatePoint(double temperature, double humidity, double continent, double erosion,
                double weirdness, double variant, double mountain) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.continent = continent;
            this.erosion = erosion;
            this.weirdness = weirdness;
            this.variant = variant;
            this.mountain = mountain;
        }

        private static ClimatePoint lerp(ClimatePoint a, ClimatePoint b, double t) {
            return new ClimatePoint(
                    ModernOverworldWorldChunkManager.lerp(a.temperature, b.temperature, t),
                    ModernOverworldWorldChunkManager.lerp(a.humidity, b.humidity, t),
                    ModernOverworldWorldChunkManager.lerp(a.continent, b.continent, t),
                    ModernOverworldWorldChunkManager.lerp(a.erosion, b.erosion, t),
                    ModernOverworldWorldChunkManager.lerp(a.weirdness, b.weirdness, t),
                    ModernOverworldWorldChunkManager.lerp(a.variant, b.variant, t),
                    ModernOverworldWorldChunkManager.lerp(a.mountain, b.mountain, t));
        }
    }

    private static final class ParameterSpan {
        private final double min;
        private final double max;

        private ParameterSpan(double min, double max) {
            this.min = min;
            this.max = max;
        }

        private double distanceSquared(double value) {
            if (value < min) {
                final double delta = min - value;
                return delta * delta;
            }
            if (value > max) {
                final double delta = value - max;
                return delta * delta;
            }
            return 0.0D;
        }
    }

    private static final class ClimateTarget {
        private final BiomeGenBase biome;
        private final ParameterSpan temperature;
        private final ParameterSpan humidity;
        private final ParameterSpan continent;
        private final ParameterSpan erosion;
        private final ParameterSpan weirdness;
        private final ParameterSpan variant;
        private final ParameterSpan mountain;

        private ClimateTarget(BiomeGenBase biome, ParameterSpan temperature, ParameterSpan humidity,
                ParameterSpan continent, ParameterSpan erosion, ParameterSpan weirdness,
                ParameterSpan variant, ParameterSpan mountain) {
            this.biome = biome;
            this.temperature = temperature;
            this.humidity = humidity;
            this.continent = continent;
            this.erosion = erosion;
            this.weirdness = weirdness;
            this.variant = variant;
            this.mountain = mountain;
        }

        private double distance(ClimatePoint point) {
            return temperature.distanceSquared(point.temperature) * WEIGHT_TEMPERATURE
                    + humidity.distanceSquared(point.humidity) * WEIGHT_HUMIDITY
                    + continent.distanceSquared(point.continent) * WEIGHT_CONTINENT
                    + erosion.distanceSquared(point.erosion) * WEIGHT_EROSION
                    + weirdness.distanceSquared(point.weirdness) * WEIGHT_WEIRDNESS
                    + variant.distanceSquared(point.variant) * WEIGHT_VARIANT
                    + mountain.distanceSquared(point.mountain) * WEIGHT_MOUNTAIN;
        }
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
