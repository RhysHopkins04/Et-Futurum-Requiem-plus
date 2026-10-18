package efrtest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ganymedes01.etfuturum.world.imported.NtmImportStructures;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.WorldServer;

/** Executes the exporter against the installed native jar; never places a structure. */
public final class NtmStructuresProbe {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void run(WorldServer world) throws Exception {
        JsonObject first = NtmImportStructures.describe(world);
        check(first.get("available").getAsBoolean(), "native structure export failed: " + first);
        check(first.get("startCodecRegistered").getAsBoolean()&&first.get("componentCodecRegistered").getAsBoolean(),"native structure save codecs not registered");
        check(!first.get("placementEnabled").getAsBoolean(), "unverified native structure placement enabled");
        Set<String> expected = new HashSet<>(Arrays.asList(("spire features bunker vertibird crashed_vertibird beached_patrol "
            + "aircraft_carrier oil_rig lighthouse dish water_pump forestchem labolatory forest_post radio factory crane "
            + "broadcaster_tower plane1 plane2 desert_shack_1 desert_shack_2 desert_shack_3 dead_dish_small "
            + "ruinA ruinB ruinC ruinD ruinE ruinF ruinG ruinH ruinI ruinJ tower_base").split(" ")));
        Set<String> actual = new HashSet<>(); int nulls = 0, roots = 0;
        Class<?> registry = Class.forName("com.hbm.world.gen.nbt.NBTStructure");
        for (JsonElement element : first.getAsJsonArray("entries")) {
            JsonObject row = element.getAsJsonObject();
            if (!row.has("name")) { nulls++; continue; }
            String name = row.get("name").getAsString();
            check(actual.add(name), "duplicate native structure: " + name);
            Object nativeRow = registry.getMethod("getStructure", String.class).invoke(null, name);
            check(row.get("weight").getAsInt() == nativeRow.getClass().getField("spawnWeight").getInt(nativeRow), "weight mismatch: " + name);
            if (row.has("rootPiece")) {
                roots++;
                JsonObject piece = row.getAsJsonObject("rootPiece");
                check(piece.get("loaded").getAsBoolean(), "native template failed to load: " + name);
                JsonArray size = piece.getAsJsonArray("size");
                for (JsonElement axis : size) check(axis.getAsInt() > 0, "empty native structure: " + name);
                check(!row.getAsJsonArray("eligibleBiomeIds").isJsonNull(), "missing biome contract");
            }
            if (name.equals("oil_rig")) check(row.get("minHeight").getAsInt()==11 && row.get("maxHeight").getAsInt()==12
                && row.getAsJsonObject("rootPiece").get("heightOffset").getAsInt()==-20, "ocean anchor contract changed");
            if (name.startsWith("ruin")) check(row.getAsJsonObject("rootPiece").get("conformToTerrain").getAsBoolean(), "terrain-conforming ruins lost");
        }
        check(actual.equals(expected) && roots==33 && nulls==2, "Earth catalogue incomplete: " + actual);
        check(first.get("namedCount").getAsInt()==35, "wrong native catalogue count");
        check(first.equals(NtmImportStructures.describe(world)), "native catalogue unstable across repeated read-only export");
        // Sample the actual native private weighted chooser with independent RNGs.
        Object generator = Class.forName("com.hbm.world.gen.nbt.NBTStructure$GenStructure").getConstructor().newInstance();
        Class<?> base = net.minecraft.world.gen.MapGenBase.class;
        java.lang.reflect.Field worldField = base.getDeclaredField("worldObj"); worldField.setAccessible(true); worldField.set(generator, world);
        java.lang.reflect.Field randomField = base.getDeclaredField("rand"); randomField.setAccessible(true);
        java.util.Random rng = (java.util.Random)randomField.get(generator);
        java.lang.reflect.Method find = generator.getClass().getDeclaredMethod("findSpawn", net.minecraft.world.biome.BiomeGenBase.class);
        find.setAccessible(true);
        JsonObject evidence = new JsonObject(); evidence.add("contract", first);
        JsonArray weighted = new JsonArray();
        for (net.minecraft.world.biome.BiomeGenBase biome : net.minecraft.world.biome.BiomeGenBase.getBiomeGenArray()) {
            if (biome == null) continue;
            for (long seed : new long[]{0, -1, 12345, 9223372036854775807L}) {
                rng.setSeed(seed); Object selected = find.invoke(generator, biome);
                JsonObject sample = new JsonObject(); sample.addProperty("biome", biome.biomeID); sample.addProperty("seed", seed);
                sample.addProperty("selected", selected == null ? "" : (String)selected.getClass().getField("name").get(selected));
                sample.addProperty("after", rng.nextInt(1000000007)); weighted.add(sample);
            }
        }
        evidence.add("weightedSamples", weighted);
        JsonArray grids = new JsonArray();
        for (int gx : new int[]{-5000, -1, 0, 1, 5000}) for (int gz : new int[]{-5000, -1, 0, 1, 5000}) {
            int spacing = first.getAsJsonObject("settings").get("structureMaxChunks").getAsInt();
            int gap = spacing - first.getAsJsonObject("settings").get("structureMinChunks").getAsInt();
            java.util.Random nativeGrid = new java.util.Random((long)gx * 341873128712L + (long)gz * 132897987541L + world.getSeed() + 996996996L);
            int cx = gx * spacing + nativeGrid.nextInt(gap), cz = gz * spacing + nativeGrid.nextInt(gap);
            Object selected = generator.getClass().getMethod("getStructureAt", net.minecraft.world.World.class, int.class, int.class).invoke(generator, world, cx, cz);
            JsonObject sample = new JsonObject(); sample.addProperty("worldSeed", world.getSeed()); sample.addProperty("cx", cx); sample.addProperty("cz", cz);
            sample.addProperty("biome", world.getWorldChunkManager().getBiomeGenAt(cx * 16 + 8, cz * 16 + 8).biomeID);
            sample.addProperty("selected", selected == null ? "" : (String)selected.getClass().getField("name").get(selected));
            sample.addProperty("after", rng.nextInt(1000000007)); grids.add(sample);
        }
        evidence.add("gridSamples", grids);
        java.nio.file.Files.write(new java.io.File("ntm-native-structure-selection.json").toPath(),
            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(evidence).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println("NTM_NATIVE_STRUCTURES PASSED: 35 Earth choices, 33 loaded native templates, 2 procedural families, biome predicates, null weights and native ocean/terrain anchors");
    }
}
