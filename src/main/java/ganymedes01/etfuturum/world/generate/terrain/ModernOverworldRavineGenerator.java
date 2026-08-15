package ganymedes01.etfuturum.world.generate.terrain;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.MapGenBase;

import java.util.Random;

/**
 * P008b-c clean-room 384-height canyon/ravine carver for the Plus modern Overworld.
 *
 * <p>The P008 noise field intentionally owns cheese/spaghetti/noodle caves. This second carver adds
 * the sparse, long, vertically stretched cuts that remain part of modern cave variety. Starts are
 * deterministic per source chunk through MapGenBase's normal seed contract, but all block addressing
 * uses the Plus 384 stride and all deep fluid decisions are delegated to ModernOverworldAquifer.</p>
 */
public final class ModernOverworldRavineGenerator extends MapGenBase {

    private static final int HEIGHT = WorldHeightCompat.EXTENDED_HEIGHT;
    private static final int BLOCK_COUNT = 256 * HEIGHT;
    private static final int MIN_CARVE_PHYSICAL_Y = 1;
    private static final int START_CHANCE = 145;
    private static final int WATER_SURFACE_GUARD = 18;
    private static final int LOOSE_SURFACE_GUARD = 16;

    private final ModernOverworldTerrainGenerator terrain;
    private final ModernOverworldAquifer aquifer;

    public ModernOverworldRavineGenerator(long seed) {
        this.terrain = new ModernOverworldTerrainGenerator(seed);
        this.aquifer = new ModernOverworldAquifer(seed, terrain);
    }

    @Override
    protected void func_151538_a(World world, int sourceChunkX, int sourceChunkZ,
            int targetChunkX, int targetChunkZ, Block[] blocks) {
        if (world == null || world.provider == null || world.provider.dimensionId != 0) {
            return;
        }
        if (blocks == null || blocks.length != BLOCK_COUNT) {
            throw new IllegalArgumentException("Modern Overworld ravines require a 16x16x384 block buffer");
        }
        if (this.rand.nextInt(START_CHANCE) != 0) {
            return;
        }

        final boolean amplified = world.getWorldInfo().getTerrainType() == WorldType.AMPLIFIED;
        final double startX = sourceChunkX * 16 + this.rand.nextInt(16) + 0.5D;
        final double startZ = sourceChunkZ * 16 + this.rand.nextInt(16) + 0.5D;
        final int surfaceY = terrain.sampleSurfacePhysicalY((int) Math.floor(startX), (int) Math.floor(startZ), amplified);

        // P009: ravines were too numerous and too often competed with the upper cavern layer.
        // Keep occasional land-facing cuts, but bias most starts deeper into the expanded underground.
        final boolean nearSurface = this.rand.nextInt(7) == 0;
        final int depth = nearSurface ? 5 + this.rand.nextInt(14) : 28 + this.rand.nextInt(118);
        final double startY = clamp(surfaceY - depth, 14.0D, HEIGHT - 24.0D);
        final float heading = this.rand.nextFloat() * (float) Math.PI * 2.0F;
        final float pitch = (this.rand.nextFloat() - 0.5F) * 0.20F;
        final float width = 2.7F + this.rand.nextFloat() * 3.2F;
        final float verticalStretch = 1.9F + this.rand.nextFloat() * 1.7F;
        final int length = 76 + this.rand.nextInt(85);
        final long pathSeed = this.rand.nextLong();

        carvePath(pathSeed, targetChunkX, targetChunkZ, blocks, amplified,
                startX, startY, startZ, heading, pitch, width, verticalStretch, length);
    }

    private void carvePath(long pathSeed, int targetChunkX, int targetChunkZ, Block[] blocks,
            boolean amplified, double startX, double startY, double startZ, float startHeading,
            float startPitch, float baseWidth, float verticalStretch, int length) {
        final Random pathRandom = new Random(pathSeed);
        final float[] verticalIrregularity = new float[HEIGHT];
        float current = 1.0F;
        for (int y = 0; y < HEIGHT; ++y) {
            if (y == 0 || pathRandom.nextInt(3) == 0) {
                current = 0.82F + pathRandom.nextFloat() * pathRandom.nextFloat() * 0.70F;
            }
            verticalIrregularity[y] = current * current;
        }

        double x = startX;
        double y = startY;
        double z = startZ;
        float heading = startHeading;
        float pitch = startPitch;
        float headingVelocity = 0.0F;
        float pitchVelocity = 0.0F;

        final ModernOverworldAquifer.Column[] aquiferColumns = new ModernOverworldAquifer.Column[256];
        final int[] surfaceCache = new int[256];
        final byte[] surfaceFlags = new byte[256];
        for (int i = 0; i < surfaceCache.length; ++i) {
            surfaceCache[i] = Integer.MIN_VALUE;
        }

        for (int step = 0; step < length; ++step) {
            final double progress = (step + 0.5D) / length;
            final double middle = Math.sin(progress * Math.PI);
            double horizontalRadius = 1.65D + middle * baseWidth;
            horizontalRadius *= 0.82D + pathRandom.nextFloat() * 0.36D;
            final double verticalRadius = horizontalRadius * verticalStretch
                    * (0.86D + pathRandom.nextFloat() * 0.28D);

            final double cosPitch = Math.cos(pitch);
            x += Math.cos(heading) * cosPitch;
            y += Math.sin(pitch);
            z += Math.sin(heading) * cosPitch;

            pitch *= 0.72F;
            pitch += pitchVelocity * 0.045F;
            heading += headingVelocity * 0.055F;
            pitchVelocity *= 0.78F;
            headingVelocity *= 0.72F;
            pitchVelocity += (pathRandom.nextFloat() - pathRandom.nextFloat()) * pathRandom.nextFloat() * 0.12F;
            headingVelocity += (pathRandom.nextFloat() - pathRandom.nextFloat()) * pathRandom.nextFloat() * 0.22F;

            if (pathRandom.nextInt(5) == 0) {
                continue;
            }

            final double chunkCenterX = targetChunkX * 16 + 8.0D;
            final double chunkCenterZ = targetChunkZ * 16 + 8.0D;
            if (Math.abs(x - chunkCenterX) > 24.0D + horizontalRadius
                    || Math.abs(z - chunkCenterZ) > 24.0D + horizontalRadius) {
                continue;
            }

            final int minLocalX = Math.max(0, floor(x - horizontalRadius) - targetChunkX * 16 - 1);
            final int maxLocalX = Math.min(15, floor(x + horizontalRadius) - targetChunkX * 16 + 1);
            final int minLocalZ = Math.max(0, floor(z - horizontalRadius) - targetChunkZ * 16 - 1);
            final int maxLocalZ = Math.min(15, floor(z + horizontalRadius) - targetChunkZ * 16 + 1);
            final int minY = Math.max(MIN_CARVE_PHYSICAL_Y, floor(y - verticalRadius) - 1);
            final int maxY = Math.min(HEIGHT - 2, floor(y + verticalRadius) + 1);

            for (int localX = minLocalX; localX <= maxLocalX; ++localX) {
                final int worldX = targetChunkX * 16 + localX;
                final double nx = (worldX + 0.5D - x) / horizontalRadius;
                final double nx2 = nx * nx;
                if (nx2 >= 1.0D) {
                    continue;
                }

                for (int localZ = minLocalZ; localZ <= maxLocalZ; ++localZ) {
                    final int worldZ = targetChunkZ * 16 + localZ;
                    final double nz = (worldZ + 0.5D - z) / horizontalRadius;
                    final double horizontal = nx2 + nz * nz;
                    if (horizontal >= 1.0D) {
                        continue;
                    }

                    final int columnIndex = localX * 16 + localZ;
                    if (surfaceCache[columnIndex] == Integer.MIN_VALUE) {
                        final int surface = terrain.sampleSurfacePhysicalY(worldX, worldZ, amplified);
                        surfaceCache[columnIndex] = surface;
                        final int surfaceLogical = WorldHeightCompat.physicalToModernY(surface);
                        if (surfaceLogical < WorldHeightCompat.MODERN_SEA_LEVEL - 1) {
                            surfaceFlags[columnIndex] |= 1;
                        }
                        if (hasLooseSurfaceCover(blocks, columnIndex * HEIGHT, surface)) {
                            surfaceFlags[columnIndex] |= 2;
                        }
                        aquiferColumns[columnIndex] = aquifer.sampleColumn(worldX, worldZ, amplified);
                    }

                    final int surface = surfaceCache[columnIndex];
                    final boolean waterSurface = (surfaceFlags[columnIndex] & 1) != 0;
                    final boolean looseSurface = (surfaceFlags[columnIndex] & 2) != 0;
                    final int columnBase = columnIndex * HEIGHT;

                    for (int physicalY = maxY; physicalY >= minY; --physicalY) {
                        final double ny = (physicalY + 0.5D - y) / verticalRadius;
                        final double shape = horizontal * verticalIrregularity[physicalY] + ny * ny;
                        if (shape >= deepFloorScale(physicalY)) {
                            continue;
                        }

                        final int surfaceGap = surface - physicalY;
                        if (waterSurface && surfaceGap < WATER_SURFACE_GUARD) {
                            continue;
                        }
                        if (looseSurface && surfaceGap < LOOSE_SURFACE_GUARD) {
                            continue;
                        }

                        final int index = columnBase + physicalY;
                        final Block block = blocks[index];
                        if (!isCarvable(block)) {
                            continue;
                        }

                        final ModernOverworldAquifer.Decision decision = aquifer.resolve(
                                aquiferColumns[columnIndex], physicalY);
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
                    }
                }
            }
        }
    }

    private static boolean isCarvable(Block block) {
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

    private static double deepFloorScale(int physicalY) {
        final double t = clamp((physicalY - 1.0D) / 17.0D, 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    private static int floor(double value) {
        final int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }
}
