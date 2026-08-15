package ganymedes01.etfuturum.world.generate.terrain;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.configuration.configs.ConfigModCompat;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * P009d modern ore-distribution bridge for the Plus modern Overworld.
 *
 * <p>Vanilla 1.7.10 places almost every ore in a flat legacy Y band. The 384-high Plus world makes
 * those distributions both vertically wrong and disproportionately concentrated at the bottom of the
 * new underground. This class owns only the modern-Overworld replacement distributions. Legacy,
 * Nether, End, flat/map-compat and non-modern paths remain unchanged.</p>
 *
 * <p>Regular blobs deliberately preserve Forge's existing ore event flow: the BiomeDecorator mixin
 * replaces only the helper call after TerrainGen has already approved each vanilla ore type. Copper,
 * emerald and rare large veins are supplied from the Et Futurum world-generator phase because 1.7.10
 * has no vanilla copper placement and emerald uses biome-specific legacy code.</p>
 */
public final class ModernOverworldOreGenerator {

    private static final long VEIN_COPPER_A = 0x434F505045525631L; // "COPPERV1"
    private static final long VEIN_COPPER_B = 0x434F505045525632L;
    private static final long VEIN_IRON_A = 0x49524F4E56454931L;   // "IRONVEI1"
    private static final long VEIN_IRON_B = 0x49524F4E56454932L;
    private static final long VEIN_GATE = 0x5645494E47415445L;     // "VEINGATE"

    private ModernOverworldOreGenerator() {}

    public static boolean isModernOreWorld(World world) {
        return world != null && world.provider != null && world.provider.dimensionId == 0
                && ConfigWorld.extendedWorldHeight && ConfigWorld.modernOverworldGeneration
                && ConfigWorld.modernOreGeneration && !ConfigMapCompatibility.isEnabled();
    }

    public static void generateCoal(World world, Random rand, int chunkX, int chunkZ) {
        // Current modern placement: 30 exposed-capable upper attempts from 136..top,
        // plus 20 half-buried triangular attempts from 0..192 (peak 96).
        generateUniform(world, rand, chunkX, chunkZ, Blocks.coal_ore, 0, 17,
                30, 136, WorldHeightCompat.MODERN_MAX_Y, 0.0D, Blocks.stone);
        generateTriangle(world, rand, chunkX, chunkZ, Blocks.coal_ore, 0, 17,
                20, 0, 192, 0.5D, Blocks.stone);
    }

    public static void generateIron(World world, Random rand, int chunkX, int chunkZ) {
        // Three modern iron placements: high mountains, the Y16 triangular band, and small deep blobs.
        // The absolute upper endpoint intentionally exceeds the build ceiling; samples above logical
        // 319 simply fail placement, matching the modern height-provider contract.
        generateTriangle(world, rand, chunkX, chunkZ, Blocks.iron_ore, 0, 9,
                90, 80, 384, 0.0D, Blocks.stone);
        generateTriangle(world, rand, chunkX, chunkZ, Blocks.iron_ore, 0, 9,
                10, -24, 56, 0.0D, Blocks.stone);
        generateUniform(world, rand, chunkX, chunkZ, Blocks.iron_ore, 0, 4,
                10, WorldHeightCompat.MODERN_MIN_Y, 72, 0.0D, Blocks.stone);
    }

    public static void generateGold(World world, Random rand, int chunkX, int chunkZ) {
        // Main modern gold is half-buried and triangular from -64..32 (peak -16).
        generateTriangle(world, rand, chunkX, chunkZ, Blocks.gold_ore, 0, 9,
                4, WorldHeightCompat.MODERN_MIN_Y, 32, 0.5D, Blocks.stone);

        // The extra bottom placement has a per-chunk count uniformly chosen from 0 or 1.
        int deepAttempts = rand.nextInt(2);
        generateUniform(world, rand, chunkX, chunkZ, Blocks.gold_ore, 0, 9,
                deepAttempts, WorldHeightCompat.MODERN_MIN_Y, -48, 0.5D, Blocks.stone);
    }

    public static void generateRedstone(World world, Random rand, int chunkX, int chunkZ) {
        generateUniform(world, rand, chunkX, chunkZ, Blocks.redstone_ore, 0, 8,
                4, WorldHeightCompat.MODERN_MIN_Y, 15, 0.0D, Blocks.stone);

        // Modern lower redstone uses a triangular provider whose theoretical lower endpoint is
        // 32 blocks below the world bottom. Out-of-world samples are discarded, which creates the
        // intended density increase toward logical Y-64 without inventing extra in-range attempts.
        generateTriangle(world, rand, chunkX, chunkZ, Blocks.redstone_ore, 0, 8,
                8, -96, -32, 0.0D, Blocks.stone);
    }

    public static void generateDiamond(World world, Random rand, int chunkX, int chunkZ) {
        // Current 1.21-era diamond placements. The main/large/buried triangle is centred on the
        // world bottom by resolving above-bottom anchors to logical -144..16 (peak -64).
        generateTriangle(world, rand, chunkX, chunkZ, Blocks.diamond_ore, 0, 4,
                7, -144, 16, 0.5D, Blocks.stone);

        // Added in the later 1.20.x ore pass and retained by 1.21: two medium half-buried attempts.
        generateUniform(world, rand, chunkX, chunkZ, Blocks.diamond_ore, 0, 8,
                2, WorldHeightCompat.MODERN_MIN_Y, -4, 0.5D, Blocks.stone);

        if (rand.nextInt(9) == 0) {
            generateTriangle(world, rand, chunkX, chunkZ, Blocks.diamond_ore, 0, 12,
                    1, -144, 16, 0.7D, Blocks.stone);
        }

        // Buried diamonds never place when directly exposed to cave air.
        generateTriangle(world, rand, chunkX, chunkZ, Blocks.diamond_ore, 0, 8,
                4, -144, 16, 1.0D, Blocks.stone);
    }

    public static void generateLapis(World world, Random rand, int chunkX, int chunkZ) {
        generateTriangle(world, rand, chunkX, chunkZ, Blocks.lapis_ore, 0, 7,
                2, -32, 32, 0.0D, Blocks.stone);
        generateUniform(world, rand, chunkX, chunkZ, Blocks.lapis_ore, 0, 7,
                4, WorldHeightCompat.MODERN_MIN_Y, 64, 1.0D, Blocks.stone);
    }

    /** Copper, mountain emerald and Badlands bonus gold are not cleanly owned by vanilla 1.7's base decorator. */
    public static void generateSupplementalOres(World world, Random rand, int chunkX, int chunkZ) {
        if (!isModernOreWorld(world)) {
            return;
        }

        if (ModBlocks.COPPER_ORE.isEnabled()) {
            ModernOverworldCaveRegionSource regions = new ModernOverworldCaveRegionSource(world.getSeed(),
                    ConfigWorld.modernLushCaveMinY, ConfigWorld.modernLushCaveMaxY,
                    ConfigWorld.modernDripstoneCaveMinY, ConfigWorld.modernDripstoneCaveMaxY);

            // Modern copper is 16 triangular attempts from logical -16..112 (peak 48).
            // Dripstone Cave positions use the size-20 configured feature INSTEAD of the normal
            // size-10 feature; they are not a second independent copper pass.
            for (int i = 0; i < 16; i++) {
                int logicalY = sampleTriangle(rand, -16, 112);
                int x = (chunkX << 4) + 8 + rand.nextInt(16);
                int z = (chunkZ << 4) + 8 + rand.nextInt(16);
                int physicalY = WorldHeightCompat.modernToPhysicalY(logicalY);
                int size = regions.isDripstone(x, physicalY, z) ? 20 : 10;
                generateBlob(world, rand, x, physicalY, z, ModBlocks.COPPER_ORE.get(), 0, size, Blocks.stone, 0.0D);
            }
        }

        BiomeGenBase biome = world.getBiomeGenForCoords((chunkX << 4) + 8, (chunkZ << 4) + 8);
        if (BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.MOUNTAIN)) {
            // Modern emerald uses 100 size-3 attempts on a very tall -16..480 triangle.
            // Most low/out-of-world samples fail, leaving strong high-altitude bias.
            generateTriangle(world, rand, chunkX, chunkZ, Blocks.emerald_ore, 0, 3,
                    100, -16, 480, 0.0D, Blocks.stone);
        }
        if (ConfigWorld.enableExtraMesaGold && BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.MESA)) {
            generateUniform(world, rand, chunkX, chunkZ, Blocks.gold_ore, 0, 9,
                    50, 32, 256, 0.0D, Blocks.stone);
        }
    }

    /**
     * Rare, chunk-local evaluation of globally continuous 3D fields. Because every chunk samples the
     * same world-coordinate noise there are no chunk-edge seams and no neighbour chunk loads.
     */
    public static void generateLargeVeins(World world, int chunkX, int chunkZ) {
        if (!isModernOreWorld(world) || !ConfigWorld.modernLargeOreVeins) {
            return;
        }
        if (ModBlocks.COPPER_ORE.isEnabled()) {
            generateNoiseVein(world, chunkX, chunkZ, 0, 50,
                    ModBlocks.COPPER_ORE.get(), 0, ModBlocks.RAW_ORE_BLOCK.isEnabled() ? ModBlocks.RAW_ORE_BLOCK.get() : null, 0,
                    Blocks.stone, 1, VEIN_COPPER_A, VEIN_COPPER_B);
        }
        generateNoiseVein(world, chunkX, chunkZ, -60, -8,
                Blocks.iron_ore, 0, ModBlocks.RAW_ORE_BLOCK.isEnabled() ? ModBlocks.RAW_ORE_BLOCK.get() : null, 1,
                ModBlocks.TUFF.isEnabled() ? ModBlocks.TUFF.get() : Blocks.stone, 0, VEIN_IRON_A, VEIN_IRON_B);
    }

    /** Optional clumpier compatibility geometry for standard WorldGenMinable ores from other mods. */
    public static boolean generateExternalModOre(World world, Random rand, int x, int y, int z,
            Block ore, int meta, int size, Block target) {
        if (!isModernOreWorld(world)) {
            return false;
        }
        generateBlob(world, rand, x, y, z, ore, meta, Math.max(1, size), target, 0.0D);
        return true; // vanilla WorldGenMinable reports success even when no target block was replaced
    }

    /**
     * Byte-for-behaviour copy of 1.7.10 WorldGenMinable's ellipsoid chain, except the caller may
     * supply a translated starting Y. This lets external mods retain their requested vein size and
     * legacy shape while correcting the +64 logical-coordinate shift.
     */
    public static boolean generateExternalModOreLegacy(World world, Random rand, int x, int y, int z,
            Block ore, int meta, int size, Block target) {
        size = Math.max(1, size);
        float angle = rand.nextFloat() * (float) Math.PI;
        double x0 = (double) ((float) (x + 8) + MathHelper.sin(angle) * (float) size / 8.0F);
        double x1 = (double) ((float) (x + 8) - MathHelper.sin(angle) * (float) size / 8.0F);
        double z0 = (double) ((float) (z + 8) + MathHelper.cos(angle) * (float) size / 8.0F);
        double z1 = (double) ((float) (z + 8) - MathHelper.cos(angle) * (float) size / 8.0F);
        double y0 = (double) (y + rand.nextInt(3) - 2);
        double y1 = (double) (y + rand.nextInt(3) - 2);
        for (int step = 0; step <= size; ++step) {
            double cx = x0 + (x1 - x0) * (double) step / (double) size;
            double cy = y0 + (y1 - y0) * (double) step / (double) size;
            double cz = z0 + (z1 - z0) * (double) step / (double) size;
            double randomRadius = rand.nextDouble() * (double) size / 16.0D;
            double radiusXZ = (double) (MathHelper.sin((float) step * (float) Math.PI / (float) size) + 1.0F)
                    * randomRadius + 1.0D;
            double radiusY = radiusXZ;
            int minX = MathHelper.floor_double(cx - radiusXZ / 2.0D);
            int minY = Math.max(0, MathHelper.floor_double(cy - radiusY / 2.0D));
            int minZ = MathHelper.floor_double(cz - radiusXZ / 2.0D);
            int maxX = MathHelper.floor_double(cx + radiusXZ / 2.0D);
            int maxY = Math.min(world.getActualHeight() - 1, MathHelper.floor_double(cy + radiusY / 2.0D));
            int maxZ = MathHelper.floor_double(cz + radiusXZ / 2.0D);

            for (int px = minX; px <= maxX; ++px) {
                double dx = ((double) px + 0.5D - cx) / (radiusXZ / 2.0D);
                if (dx * dx >= 1.0D) continue;
                for (int py = minY; py <= maxY; ++py) {
                    double dy = ((double) py + 0.5D - cy) / (radiusY / 2.0D);
                    if (dx * dx + dy * dy >= 1.0D) continue;
                    for (int pz = minZ; pz <= maxZ; ++pz) {
                        double dz = ((double) pz + 0.5D - cz) / (radiusXZ / 2.0D);
                        if (dx * dx + dy * dy + dz * dz >= 1.0D) continue;
                        if (world.getBlock(px, py, pz).isReplaceableOreGen(world, px, py, pz, target)) {
                            world.setBlock(px, py, pz, ore, meta, 2);
                        }
                    }
                }
            }
        }
        return true; // preserve vanilla WorldGenMinable's return contract
    }

    public static boolean shouldTranslateExternalModOre(Block block, int meta) {
        if (block == null || !ConfigModCompat.modernOverworldTranslateModdedOreHeights) {
            return false;
        }
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(block);
        if (id == null || "minecraft".equals(id.modId) || Tags.MOD_ID.equals(id.modId)
                || ConfigModCompat.modernOverworldModdedOreCompatibilityBlacklist.contains(id.modId)) {
            return false;
        }
        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            return false;
        }
        ItemStack stack = new ItemStack(item, 1, meta);
        int[] ids = OreDictionary.getOreIDs(stack);
        for (int oreId : ids) {
            String name = OreDictionary.getOreName(oreId);
            if (name != null && name.startsWith("ore") && !name.startsWith("oreDeepslate")
                    && !ConfigModCompat.modernOverworldModdedOreCompatibilityBlacklist.contains(name)) {
                return true;
            }
        }
        return false;
    }

    public static int translateLegacyModOreY(World world, int y) {
        if (!isModernOreWorld(world) || y < 0 || y > WorldHeightCompat.LEGACY_MAX_Y) {
            return y;
        }
        return Math.min(world.getActualHeight() - 1, y + WorldHeightCompat.MODERN_Y_OFFSET);
    }

    private static void generateNoiseVein(World world, int chunkX, int chunkZ, int minLogicalY, int maxLogicalY,
            Block ore, int oreMeta, Block rawBlock, int rawMeta, Block matrix, int matrixMeta, long saltA, long saltB) {
        final int baseX = chunkX << 4;
        final int baseZ = chunkZ << 4;
        final long seed = world.getSeed();

        for (int logicalY = minLogicalY; logicalY <= maxLogicalY; logicalY++) {
            int physicalY = WorldHeightCompat.modernToPhysicalY(logicalY);
            for (int localX = 0; localX < 16; localX++) {
                int x = baseX + localX;
                for (int localZ = 0; localZ < 16; localZ++) {
                    int z = baseZ + localZ;
                    // Reject most of the volume using the cheapest broad field before evaluating the
                    // two strand fields. This keeps the rare large-vein pass bounded during chunk gen.
                    double gate = valueNoise(seed ^ VEIN_GATE ^ saltA, x, logicalY, z, 1.0D / 170.0D, 1.0D / 90.0D);
                    if (gate < 0.60D) {
                        continue;
                    }
                    // Two narrow, independently warped fields intersect into long snake-like strands.
                    double a = valueNoise(seed ^ saltA, x, logicalY, z, 1.0D / 46.0D, 1.0D / 28.0D);
                    double b = valueNoise(seed ^ saltB, x, logicalY, z, 1.0D / 58.0D, 1.0D / 34.0D);
                    if (Math.abs(a) > 0.115D || Math.abs(b) > 0.18D) {
                        continue;
                    }

                    Block current = world.getBlock(x, physicalY, z);
                    if (!current.isReplaceableOreGen(world, x, physicalY, z, Blocks.stone)) {
                        continue;
                    }

                    double choice = unitHash(seed ^ saltB, x, logicalY, z);
                    if (rawBlock != null && choice > 0.992D && !isAdjacentToAir(world, x, physicalY, z)) {
                        world.setBlock(x, physicalY, z, rawBlock, rawMeta, 2);
                    } else if (choice > 0.62D) {
                        world.setBlock(x, physicalY, z, ore, oreMeta, 2);
                    } else if (choice > 0.16D) {
                        world.setBlock(x, physicalY, z, matrix, matrixMeta, 2);
                    }
                }
            }
        }
    }

    private static void generateTriangle(World world, Random rand, int chunkX, int chunkZ,
            Block ore, int meta, int size, int count, int minY, int maxY,
            double airDiscard, Block target) {
        for (int i = 0; i < count; i++) {
            generateOne(world, rand, chunkX, chunkZ, ore, meta, size,
                    sampleTriangle(rand, minY, maxY), airDiscard, target);
        }
    }

    private static void generateUniform(World world, Random rand, int chunkX, int chunkZ,
            Block ore, int meta, int size, int count, int minY, int maxY,
            double airDiscard, Block target) {
        if (count <= 0 || maxY < minY) {
            return;
        }
        for (int i = 0; i < count; i++) {
            int logicalY = minY + rand.nextInt(maxY - minY + 1);
            generateOne(world, rand, chunkX, chunkZ, ore, meta, size, logicalY, airDiscard, target);
        }
    }

    private static void generateOne(World world, Random rand, int chunkX, int chunkZ,
            Block ore, int meta, int size, int logicalY, double airDiscard, Block target) {
        if (logicalY < WorldHeightCompat.MODERN_MIN_Y || logicalY > WorldHeightCompat.MODERN_MAX_Y) {
            return;
        }

        // P009c deliberately offsets the placement square by +8 so every compact modern vein starts
        // inside the already-prepared 1.7 decoration neighbourhood. This is a backport safety offset,
        // not a change to the modern attempts-per-chunk or height-provider distributions.
        int x = (chunkX << 4) + 8 + rand.nextInt(16);
        int z = (chunkZ << 4) + 8 + rand.nextInt(16);
        int y = WorldHeightCompat.modernToPhysicalY(logicalY);
        generateBlob(world, rand, x, y, z, ore, meta, size, target, airDiscard);
    }

    /**
     * Java-8 port of modern OreFeature's short ellipsoid-chain geometry.
     *
     * <p>The configured {@code size} is the number of candidate ellipsoids, not a target block count.
     * The chain is generated first, ellipsoids fully contained by another are culled, and overlapping
     * candidates share one visited set. This is why legitimate modern size-4 diamond features can be
     * visually small while size-9 iron and size-17 coal still form recognisably larger clusters.</p>
     *
     * <p>P009c's loaded-chunk checks remain mandatory around every World access. Modern OreFeature can
     * assume its generation region is available; 1.7 BiomeDecorator cannot safely synchronously provide
     * a missing neighbour while it is already decorating.</p>
     */
    private static boolean generateBlob(World world, Random rand, int x, int y, int z,
            Block ore, int meta, int size, Block target, double airDiscard) {
        if (y < 0 || y >= world.getActualHeight() || size <= 0) {
            return false;
        }

        final float angle = rand.nextFloat() * (float) Math.PI;
        final float halfSpan = size / 8.0F;
        final double sin = Math.sin(angle);
        final double cos = Math.cos(angle);

        final double startX = x + sin * halfSpan;
        final double endX = x - sin * halfSpan;
        final double startZ = z + cos * halfSpan;
        final double endZ = z - cos * halfSpan;
        final double startY = y + rand.nextInt(3) - 2;
        final double endY = y + rand.nextInt(3) - 2;

        // x, y, z and radius for every configured OreFeature node.
        final double[] ellipsoids = new double[size * 4];
        for (int node = 0; node < size; node++) {
            double t = node / (double) size;
            double randomScale = rand.nextDouble() * size / 16.0D;
            double radius = ((Math.sin(Math.PI * t) + 1.0D) * randomScale + 1.0D) * 0.5D;
            int base = node * 4;
            ellipsoids[base] = startX + (endX - startX) * t;
            ellipsoids[base + 1] = startY + (endY - startY) * t;
            ellipsoids[base + 2] = startZ + (endZ - startZ) * t;
            ellipsoids[base + 3] = radius;
        }

        // Modern OreFeature removes an ellipsoid when another completely contains it. This does not
        // inflate the union; it simply avoids re-scanning geometry that cannot add any new candidates.
        for (int first = 0; first < size - 1; first++) {
            int firstBase = first * 4;
            if (ellipsoids[firstBase + 3] <= 0.0D) {
                continue;
            }
            for (int second = first + 1; second < size; second++) {
                int secondBase = second * 4;
                if (ellipsoids[secondBase + 3] <= 0.0D) {
                    continue;
                }

                double radiusDelta = ellipsoids[firstBase + 3] - ellipsoids[secondBase + 3];
                double dx = ellipsoids[firstBase] - ellipsoids[secondBase];
                double dy = ellipsoids[firstBase + 1] - ellipsoids[secondBase + 1];
                double dz = ellipsoids[firstBase + 2] - ellipsoids[secondBase + 2];
                if (radiusDelta * radiusDelta > dx * dx + dy * dy + dz * dz) {
                    if (radiusDelta > 0.0D) {
                        ellipsoids[secondBase + 3] = -1.0D;
                    } else {
                        ellipsoids[firstBase + 3] = -1.0D;
                    }
                }
            }
        }

        boolean changed = false;
        Set<Long> visited = new HashSet<Long>(Math.max(16, size * 4));

        for (int node = 0; node < size; node++) {
            int base = node * 4;
            double radius = ellipsoids[base + 3];
            if (radius <= 0.0D) {
                continue;
            }

            double cx = ellipsoids[base];
            double cy = ellipsoids[base + 1];
            double cz = ellipsoids[base + 2];
            int minX = MathHelper.floor_double(cx - radius);
            int maxX = MathHelper.floor_double(cx + radius);
            int minY = Math.max(0, MathHelper.floor_double(cy - radius));
            int maxY = Math.min(world.getActualHeight() - 1, MathHelper.floor_double(cy + radius));
            int minZ = MathHelper.floor_double(cz - radius);
            int maxZ = MathHelper.floor_double(cz + radius);

            for (int px = minX; px <= maxX; px++) {
                double xNorm = (px + 0.5D - cx) / radius;
                if (xNorm * xNorm >= 1.0D) {
                    continue;
                }

                for (int py = minY; py <= maxY; py++) {
                    double yNorm = (py + 0.5D - cy) / radius;
                    if (xNorm * xNorm + yNorm * yNorm >= 1.0D) {
                        continue;
                    }

                    for (int pz = minZ; pz <= maxZ; pz++) {
                        double zNorm = (pz + 0.5D - cz) / radius;
                        if (xNorm * xNorm + yNorm * yNorm + zNorm * zNorm >= 1.0D) {
                            continue;
                        }

                        long key = packBlockKey(px, py, pz);
                        if (!visited.add(key) || !isChunkLoaded(world, px, pz)) {
                            continue;
                        }

                        Block current = world.getBlock(px, py, pz);
                        if (!current.isReplaceableOreGen(world, px, py, pz, target)) {
                            continue;
                        }

                        // Match modern OreFeature RNG order: consume the exposure roll first, then test
                        // adjacency only when that roll falls below discard_chance_on_air_exposure.
                        if (airDiscard > 0.0D && rand.nextFloat() < (float) airDiscard
                                && isAdjacentToAir(world, px, py, pz)) {
                            continue;
                        }

                        if (world.setBlock(px, py, pz, ore, meta, 2)) {
                            changed = true;
                        }
                    }
                }
            }
        }

        return changed;
    }

    private static long packBlockKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (z & 0x3FFFFFF) << 12)
                | (long) (y & 0xFFF);
    }

    private static boolean isAdjacentToAir(World world, int x, int y, int z) {
        return isLoadedAir(world, x + 1, y, z) || isLoadedAir(world, x - 1, y, z)
                || isLoadedAir(world, x, y + 1, z) || isLoadedAir(world, x, y - 1, z)
                || isLoadedAir(world, x, y, z + 1) || isLoadedAir(world, x, y, z - 1);
    }

    /**
     * Chunk-provider existence checks are deliberately used before any neighbour World access during
     * decoration. ChunkProviderServer#chunkExists is a lookup only; unlike World#getBlock it does not
     * synchronously provide/populate a missing chunk.
     */
    private static boolean isChunkLoaded(World world, int x, int z) {
        return world.getChunkProvider() != null && world.getChunkProvider().chunkExists(x >> 4, z >> 4);
    }

    private static boolean isLoadedAir(World world, int x, int y, int z) {
        return y >= 0 && y < world.getActualHeight() && isChunkLoaded(world, x, z) && world.isAirBlock(x, y, z);
    }

    /**
     * Symmetric discrete triangular height provider used by modern ore placements.
     * Out-of-world endpoints are intentionally allowed; generateOne rejects those sampled positions.
     */
    private static int sampleTriangle(Random rand, int minY, int maxY) {
        if (maxY <= minY) {
            return minY;
        }
        int span = maxY - minY;
        int lowerHalf = span / 2;
        int upperHalf = span - lowerHalf;
        return minY + rand.nextInt(upperHalf + 1) + rand.nextInt(lowerHalf + 1);
    }

    private static double valueNoise(long seed, double x, double y, double z, double xzScale, double yScale) {
        double sx = x * xzScale;
        double sy = y * yScale;
        double sz = z * xzScale;
        int ix = floor(sx), iy = floor(sy), iz = floor(sz);
        double tx = smooth(sx - ix), ty = smooth(sy - iy), tz = smooth(sz - iz);
        double x00 = lerp(hashSigned(seed, ix, iy, iz), hashSigned(seed, ix + 1, iy, iz), tx);
        double x10 = lerp(hashSigned(seed, ix, iy + 1, iz), hashSigned(seed, ix + 1, iy + 1, iz), tx);
        double x01 = lerp(hashSigned(seed, ix, iy, iz + 1), hashSigned(seed, ix + 1, iy, iz + 1), tx);
        double x11 = lerp(hashSigned(seed, ix, iy + 1, iz + 1), hashSigned(seed, ix + 1, iy + 1, iz + 1), tx);
        return lerp(lerp(x00, x10, ty), lerp(x01, x11, ty), tz);
    }

    private static double hashSigned(long seed, int x, int y, int z) {
        return unitHash(seed, x, y, z) * 2.0D - 1.0D;
    }

    private static double unitHash(long seed, int x, int y, int z) {
        long v = seed ^ (long) x * 341873128712L ^ (long) y * 132897987541L ^ (long) z * 42317861L;
        v ^= v >>> 33; v *= 0xff51afd7ed558ccdL;
        v ^= v >>> 33; v *= 0xc4ceb9fe1a85ec53L;
        v ^= v >>> 33;
        return ((v >>> 11) & ((1L << 53) - 1L)) / (double) (1L << 53);
    }

    private static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    private static double smooth(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
