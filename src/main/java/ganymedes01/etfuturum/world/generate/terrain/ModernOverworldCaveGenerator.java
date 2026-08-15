package ganymedes01.etfuturum.world.generate.terrain;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.NoiseGeneratorImproved;

import java.util.Arrays;
import java.util.Random;

/**
 * P008b-c: 384-height-safe modern Overworld noise caves with regional density variation.
 *
 * <p>P008b-c keeps the P008b-b floor fade and contained aquifers, but adds a broad 3D rarity field
 * so cave-rich and cave-poor regions alternate instead of every explored area joining one nearly
 * continuous network. Cheese noise is broadened to form fewer, larger chambers; spaghetti/noodle
 * connectors are slightly rarer outside cave-rich regions. A separate translated canyon carver
 * supplies the long ravine-shaped cuts that the noise-only foundation intentionally lacked.</p>
 */
public final class ModernOverworldCaveGenerator extends MapGenBase {

    private static final int HEIGHT = WorldHeightCompat.EXTENDED_HEIGHT;
    private static final int GRID_STEP = 4;
    private static final int X_SAMPLES = 16 / GRID_STEP + 1;
    private static final int Z_SAMPLES = 16 / GRID_STEP + 1;
    private static final int Y_SAMPLES = HEIGHT / GRID_STEP + 1;
    private static final int SAMPLE_COUNT = X_SAMPLES * Y_SAMPLES * Z_SAMPLES;
    private static final int BLOCK_COUNT = 256 * HEIGHT;

    private static final int MIN_CARVE_PHYSICAL_Y = 1;
    private static final int DEEP_FLOOR_FADE_END_PHYSICAL_Y = 1;
    private static final int DEEP_FLOOR_FADE_START_PHYSICAL_Y = 18;
    private static final int MIN_SURFACE_ROOF = 7;
    private static final int TUNNEL_SURFACE_ROOF = 9;
    private static final int CHEESE_SURFACE_FADE = 28;
    private static final int ENTRANCE_DEPTH = 18;

    private static final byte MASK_CHEESE = 1;
    private static final byte MASK_TUNNEL = 2;
    private static final byte MASK_NOODLE = 4;
    private static final byte MASK_ENTRANCE = 8;

    private static final long SALT_CHEESE = 0x6A09E667F3BCC909L;
    private static final long SALT_SPAGHETTI_A = 0xBB67AE8584CAA73BL;
    private static final long SALT_SPAGHETTI_B = 0x3C6EF372FE94F82BL;
    private static final long SALT_SPAGHETTI_WIDTH = 0xA54FF53A5F1D36F1L;
    private static final long SALT_LONG_TUNNEL_A = 0x4C4F4E4754554E41L; // "LONGTUNA"
    private static final long SALT_LONG_TUNNEL_B = 0x4C4F4E4754554E42L; // "LONGTUNB"
    private static final long SALT_NOODLE_A = 0x510E527FADE682D1L;
    private static final long SALT_NOODLE_B = 0x9B05688C2B3E6C1FL;
    private static final long SALT_NOODLE_TOGGLE = 0x1F83D9ABFB41BD6BL;
    private static final long SALT_PILLAR_A = 0x5BE0CD19137E2179L;
    private static final long SALT_PILLAR_B = 0xCBBB9D5DC1059ED8L;
    private static final long SALT_ENTRANCE = 0x629A292A367CD507L;
    private static final long SALT_REGION = 0xD1310BA698DFB5ACL;

    private final ModernOverworldTerrainGenerator terrain;
    private final ModernOverworldAquifer aquifer;
    private final FractalNoise3D cheeseNoise;
    private final FractalNoise3D spaghettiNoiseA;
    private final FractalNoise3D spaghettiNoiseB;
    private final FractalNoise3D spaghettiWidthNoise;
    private final FractalNoise3D longTunnelNoiseA;
    private final FractalNoise3D longTunnelNoiseB;
    private final FractalNoise3D noodleNoiseA;
    private final FractalNoise3D noodleNoiseB;
    private final FractalNoise3D noodleToggleNoise;
    private final FractalNoise3D pillarNoiseA;
    private final FractalNoise3D pillarNoiseB;
    private final FractalNoise3D entranceNoise;
    private final FractalNoise3D regionNoise;

    private double[] cheeseField = new double[SAMPLE_COUNT];
    private double[] spaghettiFieldA = new double[SAMPLE_COUNT];
    private double[] spaghettiFieldB = new double[SAMPLE_COUNT];
    private double[] spaghettiWidthField = new double[SAMPLE_COUNT];
    private double[] longTunnelFieldA = new double[SAMPLE_COUNT];
    private double[] longTunnelFieldB = new double[SAMPLE_COUNT];
    private double[] noodleFieldA = new double[SAMPLE_COUNT];
    private double[] noodleFieldB = new double[SAMPLE_COUNT];
    private double[] noodleToggleField = new double[SAMPLE_COUNT];
    private double[] pillarFieldA = new double[SAMPLE_COUNT];
    private double[] pillarFieldB = new double[SAMPLE_COUNT];
    private double[] entranceField = new double[SAMPLE_COUNT];
    private double[] regionField = new double[SAMPLE_COUNT];
    private final byte[] carveMask = new byte[BLOCK_COUNT];

    public ModernOverworldCaveGenerator(long seed) {
        this.terrain = new ModernOverworldTerrainGenerator(seed);
        this.aquifer = new ModernOverworldAquifer(seed, terrain);
        this.cheeseNoise = new FractalNoise3D(seed ^ SALT_CHEESE, 4, 0.50D);
        this.spaghettiNoiseA = new FractalNoise3D(seed ^ SALT_SPAGHETTI_A, 3, 0.50D);
        this.spaghettiNoiseB = new FractalNoise3D(seed ^ SALT_SPAGHETTI_B, 3, 0.50D);
        this.spaghettiWidthNoise = new FractalNoise3D(seed ^ SALT_SPAGHETTI_WIDTH, 2, 0.50D);
        this.longTunnelNoiseA = new FractalNoise3D(seed ^ SALT_LONG_TUNNEL_A, 3, 0.52D);
        this.longTunnelNoiseB = new FractalNoise3D(seed ^ SALT_LONG_TUNNEL_B, 3, 0.52D);
        this.noodleNoiseA = new FractalNoise3D(seed ^ SALT_NOODLE_A, 2, 0.50D);
        this.noodleNoiseB = new FractalNoise3D(seed ^ SALT_NOODLE_B, 2, 0.50D);
        this.noodleToggleNoise = new FractalNoise3D(seed ^ SALT_NOODLE_TOGGLE, 2, 0.50D);
        this.pillarNoiseA = new FractalNoise3D(seed ^ SALT_PILLAR_A, 2, 0.50D);
        this.pillarNoiseB = new FractalNoise3D(seed ^ SALT_PILLAR_B, 2, 0.50D);
        this.entranceNoise = new FractalNoise3D(seed ^ SALT_ENTRANCE, 2, 0.50D);
        this.regionNoise = new FractalNoise3D(seed ^ SALT_REGION, 2, 0.55D);
    }

    @Override
    public void func_151539_a(IChunkProvider provider, World world, int chunkX, int chunkZ, Block[] blocks) {
        if (world == null || world.provider == null || world.provider.dimensionId != 0) {
            return;
        }
        if (blocks == null || blocks.length != BLOCK_COUNT) {
            throw new IllegalArgumentException("Modern Overworld caves require a 16x16x384 block buffer");
        }

        final boolean amplified = world.getWorldInfo().getTerrainType() == WorldType.AMPLIFIED;
        carveChunk(chunkX, chunkZ, blocks, amplified);
    }

    /** Package-private deterministic entry point used by offline P008 diagnostics. */
    int carveChunk(int chunkX, int chunkZ, Block[] blocks, boolean amplified) {
        fillNoiseFields(chunkX, chunkZ);
        Arrays.fill(carveMask, (byte) 0);

        markCaveGeometry(chunkX, chunkZ, amplified);
        dilateTraversablePassages();
        return applyCarvingAndAquifers(chunkX, chunkZ, blocks, amplified);
    }

    private void markCaveGeometry(int chunkX, int chunkZ, boolean amplified) {
        for (int localX = 0; localX < 16; ++localX) {
            final int worldX = chunkX * 16 + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                final int worldZ = chunkZ * 16 + localZ;
                final int surfaceY = terrain.sampleSurfacePhysicalY(worldX, worldZ, amplified);
                final int surfaceLogicalY = WorldHeightCompat.physicalToModernY(surfaceY);
                final boolean surfaceWaterColumn = surfaceLogicalY < WorldHeightCompat.MODERN_SEA_LEVEL - 1;
                final int columnBase = (localX * 16 + localZ) * HEIGHT;
                final int maxCarveY = Math.min(HEIGHT - 2, surfaceY);

                for (int physicalY = MIN_CARVE_PHYSICAL_Y; physicalY <= maxCarveY; ++physicalY) {
                    final int surfaceGap = surfaceY - physicalY;
                    final int logicalY = WorldHeightCompat.physicalToModernY(physicalY);
                    final double cheese = sampleField(cheeseField, localX, physicalY, localZ);
                    final double spaghettiA = sampleField(spaghettiFieldA, localX, physicalY, localZ);
                    final double spaghettiB = sampleField(spaghettiFieldB, localX, physicalY, localZ);
                    final double spaghettiControl = sampleField(spaghettiWidthField, localX, physicalY, localZ);
                    final double longTunnelA = sampleField(longTunnelFieldA, localX, physicalY, localZ);
                    final double longTunnelB = sampleField(longTunnelFieldB, localX, physicalY, localZ);
                    final double entrance = sampleField(entranceField, localX, physicalY, localZ);
                    final double region = sampleField(regionField, localX, physicalY, localZ);

                    boolean carveCheese = surfaceGap >= MIN_SURFACE_ROOF
                            && isCheeseCave(cheese, spaghettiControl, region, logicalY, physicalY, surfaceGap);
                    if (carveCheese) {
                        final double pillarA = sampleField(pillarFieldA, localX, physicalY, localZ);
                        final double pillarB = sampleField(pillarFieldB, localX, physicalY, localZ);
                        if (isCavernPillar(pillarA, pillarB, logicalY)) {
                            carveCheese = false;
                        }
                    }

                    final boolean normalTunnel = surfaceGap >= TUNNEL_SURFACE_ROOF;
                    // Do not punch through an existing water surface in this stage. Direct ocean/lake
                    // mouths leave static 1.7 source blocks suspended over freshly carved air unless a
                    // later hydrology pass owns the required fluid updates. Land mouths remain enabled.
                    final boolean surfaceEntrance = !surfaceWaterColumn
                            && surfaceGap >= 0 && surfaceGap < ENTRANCE_DEPTH
                            && isSurfaceEntrance(entrance, spaghettiA, spaghettiB, spaghettiControl, surfaceGap);
                    final boolean carveSpaghetti = (normalTunnel || surfaceEntrance)
                            && isSpaghettiCave(spaghettiA, spaghettiB, spaghettiControl, region, logicalY, physicalY);
                    // P009: a second, much broader anisotropic field supplies the long sloping
                    // tube systems that were underrepresented by the compact P008 spaghetti noise.
                    // It stays underground and fades at the floor, but reaches deep deepslate space.
                    final boolean carveLongTunnel = normalTunnel
                            && logicalY >= -56 && logicalY <= 112
                            && isLongTunnel(longTunnelA, longTunnelB, spaghettiControl, region, logicalY, physicalY);

                    boolean carveNoodle = false;
                    if (!carveCheese && !carveSpaghetti && logicalY <= 48 && surfaceGap >= TUNNEL_SURFACE_ROOF) {
                        final double noodleToggle = sampleField(noodleToggleField, localX, physicalY, localZ);
                        if (noodleToggle > 0.30D && region > -0.28D) {
                            final double noodleA = sampleField(noodleFieldA, localX, physicalY, localZ);
                            final double noodleB = sampleField(noodleFieldB, localX, physicalY, localZ);
                            carveNoodle = isNoodleCave(noodleA, noodleB, noodleToggle, region, logicalY, physicalY);
                        }
                    }

                    byte mask = 0;
                    if (carveCheese) {
                        mask |= MASK_CHEESE;
                    }
                    if (carveSpaghetti || carveLongTunnel) {
                        mask |= MASK_TUNNEL;
                    }
                    if (carveNoodle) {
                        mask |= MASK_NOODLE;
                    }
                    if (surfaceEntrance && carveSpaghetti) {
                        mask |= MASK_ENTRANCE;
                    }
                    if (mask != 0) {
                        carveMask[columnBase + physicalY] |= mask;
                    }
                }
            }
        }
    }

    /**
     * P008a could leave attractive spectator-visible slits that were only one block high in actual
     * play. Expand only tunnel/noodle geometry upward by one voxel so their narrowest useful parts
     * remain at least two blocks tall without globally inflating cheese caverns.
     */
    private void dilateTraversablePassages() {
        for (int localX = 0; localX < 16; ++localX) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                final int columnBase = (localX * 16 + localZ) * HEIGHT;
                for (int physicalY = HEIGHT - 3; physicalY >= MIN_CARVE_PHYSICAL_Y; --physicalY) {
                    final byte mask = carveMask[columnBase + physicalY];
                    if ((mask & (MASK_TUNNEL | MASK_NOODLE)) == 0) {
                        continue;
                    }
                    final byte expansion = (byte) (mask & (MASK_TUNNEL | MASK_NOODLE | MASK_ENTRANCE));
                    carveMask[columnBase + physicalY + 1] |= expansion;
                }
            }
        }
    }

    private int applyCarvingAndAquifers(int chunkX, int chunkZ, Block[] blocks, boolean amplified) {
        int carved = 0;
        for (int localX = 0; localX < 16; ++localX) {
            final int worldX = chunkX * 16 + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                final int worldZ = chunkZ * 16 + localZ;
                final int surfaceY = terrain.sampleSurfacePhysicalY(worldX, worldZ, amplified);
                final int columnBase = (localX * 16 + localZ) * HEIGHT;
                final ModernOverworldAquifer.Column aquiferColumn = aquifer.sampleColumn(worldX, worldZ, amplified);
                final boolean looseSurfaceCover = hasLooseSurfaceCover(blocks, columnBase, surfaceY);

                for (int physicalY = MIN_CARVE_PHYSICAL_Y; physicalY < HEIGHT - 1; ++physicalY) {
                    final byte mask = carveMask[columnBase + physicalY];
                    if (mask == 0) {
                        continue;
                    }

                    final int index = columnBase + physicalY;
                    final boolean entrance = (mask & MASK_ENTRANCE) != 0;
                    if (entrance && looseSurfaceCover) {
                        continue;
                    }
                    final Block block = blocks[index];
                    if (!(isCarvable(block) || (entrance && isSurfaceCarvable(block)))) {
                        continue;
                    }

                    final ModernOverworldAquifer.Decision decision = aquifer.resolve(aquiferColumn, physicalY);
                    if (decision == ModernOverworldAquifer.Decision.PRESERVE) {
                        continue;
                    }

                    if (decision == ModernOverworldAquifer.Decision.WATER) {
                        blocks[index] = Blocks.water;
                    } else if (decision == ModernOverworldAquifer.Decision.LAVA) {
                        blocks[index] = Blocks.lava;
                    } else {
                        blocks[index] = null;
                    }
                    ++carved;
                }
            }
        }
        return carved;
    }

    private void fillNoiseFields(int chunkX, int chunkZ) {
        final int startX = chunkX * 16;
        final int startZ = chunkZ * 16;

        cheeseField = cheeseNoise.fill(cheeseField, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                124.0D, 92.0D, 124.0D);
        spaghettiFieldA = spaghettiNoiseA.fill(spaghettiFieldA, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                58.0D, 42.0D, 58.0D);
        spaghettiFieldB = spaghettiNoiseB.fill(spaghettiFieldB, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                58.0D, 42.0D, 58.0D);
        spaghettiWidthField = spaghettiWidthNoise.fill(spaghettiWidthField, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                176.0D, 128.0D, 176.0D);
        longTunnelFieldA = longTunnelNoiseA.fill(longTunnelFieldA, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                108.0D, 156.0D, 108.0D);
        longTunnelFieldB = longTunnelNoiseB.fill(longTunnelFieldB, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                108.0D, 156.0D, 108.0D);
        noodleFieldA = noodleNoiseA.fill(noodleFieldA, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                30.0D, 24.0D, 30.0D);
        noodleFieldB = noodleNoiseB.fill(noodleFieldB, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                30.0D, 24.0D, 30.0D);
        noodleToggleField = noodleToggleNoise.fill(noodleToggleField, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                152.0D, 112.0D, 152.0D);
        pillarFieldA = pillarNoiseA.fill(pillarFieldA, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                52.0D, 196.0D, 52.0D);
        pillarFieldB = pillarNoiseB.fill(pillarFieldB, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                52.0D, 196.0D, 52.0D);
        entranceField = entranceNoise.fill(entranceField, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                224.0D, 112.0D, 224.0D);
        regionField = regionNoise.fill(regionField, startX, 0, startZ,
                X_SAMPLES, Y_SAMPLES, Z_SAMPLES, GRID_STEP, GRID_STEP, GRID_STEP,
                336.0D, 208.0D, 336.0D);
    }

    private static boolean isCheeseCave(double cheese, double control, double region, int logicalY, int physicalY, int surfaceGap) {
        // Positive regional density produces cave-rich zones; negative density deliberately creates
        // quiet rock between them. The non-linear poor-region penalty is what breaks the nearly
        // world-spanning P008b-b cave component without simply lowering cave volume everywhere.
        final double regionClamped = clamp(region, -1.0D, 1.0D);
        double threshold = 0.505D - clamp(control, -1.0D, 1.0D) * 0.050D - regionClamped * 0.085D;
        if (regionClamped < -0.22D) {
            threshold += smoothstep(-0.22D, -0.78D, regionClamped) * 0.075D;
        }

        if (logicalY > 48) {
            threshold += smoothstep(48.0D, 176.0D, logicalY) * 0.19D;
        }

        // P009 cave-finale: P008's broad caverns were visually biased toward the upper underground.
        // Give deep deepslate a bounded cheese-pocket bonus without defeating the bedrock density slide.
        // The regional multiplier keeps these as occasional large pockets rather than a continuous layer.
        if (logicalY < 24) {
            final double deep = clamp((24.0D - logicalY) / 72.0D, 0.0D, 1.0D);
            final double richRegion = 0.55D + (regionClamped + 1.0D) * 0.225D;
            threshold -= deep * 0.070D * richRegion;
        }

        // Modern caves approach the seeded bedrock floor through a density slide rather than
        // terminating every cave family against one hard Y plane. Keep Y=-54 (physical 10)
        // reachable by sufficiently strong cheese density for large deep lava lakes, but rapidly
        // close cave volume as the full-bedrock Y=-64 floor is approached.
        final double deepFloorScale = deepFloorScale(physicalY);
        threshold += (1.0D - deepFloorScale) * 0.18D;

        if (surfaceGap < CHEESE_SURFACE_FADE) {
            threshold += (1.0D - (double) (surfaceGap - MIN_SURFACE_ROOF)
                    / (CHEESE_SURFACE_FADE - MIN_SURFACE_ROOF)) * 0.23D;
        }

        return cheese > threshold;
    }

    private static boolean isSpaghettiCave(double a, double b, double control, double region, int logicalY, int physicalY) {
        double width = 0.060D + (clamp(control, -1.0D, 1.0D) + 1.0D) * 0.014D;
        width *= 0.82D + (clamp(region, -1.0D, 1.0D) + 1.0D) * 0.16D;
        if (logicalY > 96) {
            width *= 1.0D - smoothstep(96.0D, 208.0D, logicalY) * 0.38D;
        }
        width *= deepFloorScale(physicalY);
        return width > 0.0D && Math.max(Math.abs(a), Math.abs(b)) < width;
    }

    private static boolean isLongTunnel(double a, double b, double control, double region, int logicalY, int physicalY) {
        double width = 0.042D + (clamp(control, -1.0D, 1.0D) + 1.0D) * 0.010D;
        width *= 0.86D + (clamp(region, -1.0D, 1.0D) + 1.0D) * 0.11D;
        if (logicalY < 8) {
            width *= 1.0D + clamp((8.0D - logicalY) / 64.0D, 0.0D, 1.0D) * 0.18D;
        } else if (logicalY > 72) {
            width *= 1.0D - smoothstep(72.0D, 112.0D, logicalY) * 0.45D;
        }
        width *= deepFloorScale(physicalY);
        return width > 0.0D && Math.max(Math.abs(a), Math.abs(b)) < width;
    }

    private static boolean isNoodleCave(double a, double b, double toggle, double region, int logicalY, int physicalY) {
        double width = 0.040D + clamp((toggle - 0.30D) * 0.018D, 0.0D, 0.018D);
        width *= 0.88D + (clamp(region, -1.0D, 1.0D) + 1.0D) * 0.10D;
        if (logicalY > 20) {
            width *= 1.0D - smoothstep(20.0D, 56.0D, logicalY) * 0.28D;
        }
        width *= deepFloorScale(physicalY);
        return width > 0.0D && Math.max(Math.abs(a), Math.abs(b)) < width;
    }

    private static double deepFloorScale(int physicalY) {
        return smoothstep(DEEP_FLOOR_FADE_END_PHYSICAL_Y, DEEP_FLOOR_FADE_START_PHYSICAL_Y, physicalY);
    }

    private static boolean isCavernPillar(double a, double b, int logicalY) {
        if (logicalY < -48 || logicalY > 128) {
            return false;
        }
        // P009: deep large pockets should retain substantial freestanding supports too.
        // P008 made pillars slightly more common high up; reverse that bias while keeping them sparse.
        final double deep = clamp((48.0D - logicalY) / 96.0D, 0.0D, 1.0D);
        final double width = 0.098D + deep * 0.027D;
        return Math.max(Math.abs(a), Math.abs(b)) < width;
    }

    private static boolean isSurfaceEntrance(double entrance, double spaghettiA, double spaghettiB,
            double control, int surfaceGap) {
        final double depth = clamp(surfaceGap / (double) ENTRANCE_DEPTH, 0.0D, 1.0D);
        final double required = 0.41D + (1.0D - depth) * 0.070D;
        if (Math.abs(entrance) < required) {
            return false;
        }

        // Require the same tunnel field to actually be approaching the surface. The looser width
        // only selects the mouth region; isSpaghettiCave still owns the final geometry decision.
        final double mouthWidth = 0.10D + (clamp(control, -1.0D, 1.0D) + 1.0D) * 0.018D;
        return Math.max(Math.abs(spaghettiA), Math.abs(spaghettiB)) < mouthWidth;
    }

    private static boolean isCarvable(Block block) {
        return block == Blocks.stone || (block != null && block.getMaterial() == Material.rock);
    }

    private static boolean isSurfaceCarvable(Block block) {
        if (block == null || block == Blocks.air || block == Blocks.bedrock) {
            return false;
        }
        final Material material = block.getMaterial();
        return material == Material.rock
                || material == Material.ground
                || material == Material.grass
                || material == Material.clay
                || material == Material.snow
                || material == Material.craftedSnow;
    }


    private static boolean hasLooseSurfaceCover(Block[] blocks, int columnBase, int surfaceY) {
        final int minY = Math.max(MIN_CARVE_PHYSICAL_Y, surfaceY - 5);
        final int maxY = Math.min(HEIGHT - 1, surfaceY + 2);
        for (int y = minY; y <= maxY; ++y) {
            final Block block = blocks[columnBase + y];
            if (block != null && block.getMaterial() == Material.sand) {
                return true;
            }
        }
        return false;
    }

    private static double sampleField(double[] field, int localX, int physicalY, int localZ) {
        final int cellX = Math.min(localX / GRID_STEP, X_SAMPLES - 2);
        final int cellY = Math.min(physicalY / GRID_STEP, Y_SAMPLES - 2);
        final int cellZ = Math.min(localZ / GRID_STEP, Z_SAMPLES - 2);
        final double fracX = (localX - cellX * GRID_STEP) / (double) GRID_STEP;
        final double fracY = (physicalY - cellY * GRID_STEP) / (double) GRID_STEP;
        final double fracZ = (localZ - cellZ * GRID_STEP) / (double) GRID_STEP;

        final double x0z0 = lerp(field[index(cellX, cellY, cellZ)], field[index(cellX + 1, cellY, cellZ)], fracX);
        final double x0z1 = lerp(field[index(cellX, cellY, cellZ + 1)], field[index(cellX + 1, cellY, cellZ + 1)], fracX);
        final double x1z0 = lerp(field[index(cellX, cellY + 1, cellZ)], field[index(cellX + 1, cellY + 1, cellZ)], fracX);
        final double x1z1 = lerp(field[index(cellX, cellY + 1, cellZ + 1)], field[index(cellX + 1, cellY + 1, cellZ + 1)], fracX);
        final double z0 = lerp(x0z0, x0z1, fracZ);
        final double z1 = lerp(x1z0, x1z1, fracZ);
        return lerp(z0, z1, fracY);
    }

    private static int index(int sampleX, int sampleY, int sampleZ) {
        return (sampleX * Z_SAMPLES + sampleZ) * Y_SAMPLES + sampleY;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        final double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    /**
     * Small deterministic 3D fBm wrapper around vanilla 1.7.10's improved-noise primitive.
     * Frequencies are expressed in block-space wavelengths so neighbouring chunk sample lattices
     * share the same world-coordinate phase exactly.
     */
    private static final class FractalNoise3D {
        private final NoiseGeneratorImproved[] octaves;
        private final double persistence;
        private final double amplitudeNormalizer;

        private FractalNoise3D(long seed, int octaveCount, double persistence) {
            this.persistence = persistence;
            this.octaves = new NoiseGeneratorImproved[octaveCount];
            final Random random = new Random(seed);
            double amplitude = 1.0D;
            double sum = 0.0D;
            for (int i = 0; i < octaveCount; ++i) {
                this.octaves[i] = new NoiseGeneratorImproved(random);
                sum += amplitude;
                amplitude *= persistence;
            }
            this.amplitudeNormalizer = sum;
        }

        private double[] fill(double[] target, int startX, int startY, int startZ,
                int sizeX, int sizeY, int sizeZ, int stepX, int stepY, int stepZ,
                double wavelengthX, double wavelengthY, double wavelengthZ) {
            final int required = sizeX * sizeY * sizeZ;
            if (target == null || target.length != required) {
                target = new double[required];
            } else {
                Arrays.fill(target, 0.0D);
            }

            double frequency = 1.0D;
            double amplitude = 1.0D;
            for (NoiseGeneratorImproved octave : octaves) {
                final double scaleX = frequency / wavelengthX;
                final double scaleY = frequency / wavelengthY;
                final double scaleZ = frequency / wavelengthZ;
                octave.populateNoiseArray(target,
                        startX * scaleX, startY * scaleY, startZ * scaleZ,
                        sizeX, sizeY, sizeZ,
                        stepX * scaleX, stepY * scaleY, stepZ * scaleZ,
                        1.0D / amplitude);
                frequency *= 2.0D;
                amplitude *= persistence;
            }

            for (int i = 0; i < target.length; ++i) {
                target[i] /= amplitudeNormalizer;
            }
            return target;
        }
    }
}
