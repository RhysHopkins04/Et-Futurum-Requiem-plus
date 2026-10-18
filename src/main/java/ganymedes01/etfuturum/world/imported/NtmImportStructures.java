package ganymedes01.etfuturum.world.imported;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cpw.mods.fml.common.Loader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;

/** Read-only native structure contracts. Never constructs a generator or invokes worldgen. */
public final class NtmImportStructures {
    private NtmImportStructures() {}

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        return owner.getClass().getField(name).get(owner);
    }

    private static Object privateStatic(Class<?> owner, String name) throws ReflectiveOperationException {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    private static void property(JsonObject out, Class<?> owner, String name) throws ReflectiveOperationException {
        Object value = owner.getField(name).get(null);
        if (value instanceof Boolean) out.addProperty(name, (Boolean)value);
        else if (value instanceof Number) out.addProperty(name, (Number)value);
        else throw new IllegalStateException("Unexpected structure config type: " + name);
    }

    @SuppressWarnings("unchecked")
    private static JsonArray biomes(Object spawn) throws ReflectiveOperationException {
        Predicate<BiomeGenBase> predicate = (Predicate<BiomeGenBase>)field(spawn, "canSpawn");
        JsonArray out = new JsonArray();
        // Legacy mods can retain aliases to one biome in multiple array slots.
        // Export identities once; conflicting objects with one ID are ambiguous.
        Map<Integer, Boolean> results = new java.util.TreeMap<>();
        for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
            if (biome == null) continue;
            boolean eligible = predicate == null || predicate.test(biome);
            Boolean previous = results.put(biome.biomeID, eligible);
            if (previous != null && previous.booleanValue() != eligible)
                throw new IllegalStateException("Conflicting native structure biome predicate for ID " + biome.biomeID);
        }
        for (Map.Entry<Integer, Boolean> entry : results.entrySet())
            if (entry.getValue()) out.add(new com.google.gson.JsonPrimitive(entry.getKey()));
        return out;
    }

    private static JsonObject piece(Object piece) throws ReflectiveOperationException {
        JsonObject out = new JsonObject();
        out.addProperty("name", (String)field(piece, "name"));
        for (String name : new String[]{"heightOffset", "instanceLimit"})
            out.addProperty(name, (Number)field(piece, name));
        for (String name : new String[]{"conformToTerrain", "alignToTerrain", "required"})
            out.addProperty(name, (Boolean)field(piece, name));
        out.addProperty("hasPlatform", field(piece, "platform") != null);
        out.addProperty("hasBlockSelectors", field(piece, "blockTable") != null);
        Object structure = field(piece, "structure");
        out.addProperty("template", (String)structure.getClass().getMethod("getName").invoke(structure));
        JsonArray size = new JsonArray();
        for (String axis : new String[]{"X", "Y", "Z"})
            size.add(new com.google.gson.JsonPrimitive((Number)structure.getClass().getMethod("getSize" + axis).invoke(structure)));
        out.add("size", size);
        Field loaded = structure.getClass().getDeclaredField("isLoaded"); loaded.setAccessible(true);
        out.addProperty("loaded", loaded.getBoolean(structure));
        return out;
    }

    private static JsonObject spawn(Object spawn, boolean custom) throws ReflectiveOperationException {
        JsonObject row = new JsonObject();
        String name = (String)field(spawn, "name");
        if (name != null) row.addProperty("name", name);
        row.addProperty("weight", (Number)field(spawn, "spawnWeight"));
        row.addProperty("customCoordinates", custom);
        row.add("eligibleBiomeIds", biomes(spawn));
        for (String key : new String[]{"minHeight", "maxHeight", "sizeLimit", "rangeLimit"})
            row.addProperty(key, (Number)field(spawn, key));
        Object root = field(spawn, "structure");
        boolean procedural = field(spawn, "start") != null;
        row.addProperty("kind", name == null ? "null-weight" : procedural ? "procedural" : "jigsaw");
        if (root != null) row.add("rootPiece", piece(root));
        Object pools = field(spawn, "pools");
        row.addProperty("hasPools", pools != null);
        row.addProperty("placementVerified", false);
        // A native weight is a relative choice, not an independent 1/N spawn rate.
        row.addProperty("weightSemantics", "relative-biome-choice");
        return row;
    }

    private static boolean registeredCodec(String name,String className) throws ReflectiveOperationException {
        Class<?> expected=Class.forName(className);
        for(Field field:net.minecraft.world.gen.structure.MapGenStructureIO.class.getDeclaredFields()){
            if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())||!Map.class.isAssignableFrom(field.getType()))continue;
            field.setAccessible(true);
            if(expected.equals(((Map<?,?>)field.get(null)).get(name)))return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static JsonObject describe(WorldServer world) {
        JsonObject out = new JsonObject();
        out.addProperty("schemaVersion", 1);
        out.addProperty("available", false);
        out.addProperty("placementEnabled", false);
        out.addProperty("nativeInventoryActionsVersion",1);
        if (!Loader.isModLoaded("hbm")) return out;
        try {
            Class<?> nbt = Class.forName("com.hbm.world.gen.nbt.NBTStructure");
            JsonArray entries = new JsonArray();
            int named = 0;
            for (String mapName : new String[]{"spawnMap", "customSpawnMap"}) {
                Map<Integer, List<?>> map = (Map<Integer, List<?>>)privateStatic(nbt, mapName);
                List<?> nativeList = map.get(0);
                if (nativeList == null) continue;
                List<Object> sorted = new ArrayList<>(nativeList);
                sorted.sort(Comparator.comparing(value -> {
                    try { Object name = field(value, "name"); return name == null ? "" : name.toString(); }
                    catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
                }));
                for (Object entry : sorted) {
                    JsonObject row = spawn(entry, mapName.equals("customSpawnMap"));
                    row.addProperty("nativeOrder", nativeList.indexOf(entry));
                    entries.add(row);
                    if (row.has("name")) named++;
                }
            }
            if (named == 0) throw new IllegalStateException("NTM Earth structure registry is empty");
            out.addProperty("dimension", 0);
            out.addProperty("namedCount", named);
            out.add("entries", entries);
            out.addProperty("startCodecRegistered",registeredCodec("NBTStructures","com.hbm.world.gen.nbt.NBTStructure$Start"));
            out.addProperty("componentCodecRegistered",registeredCodec("NBTComponents","com.hbm.world.gen.nbt.NBTStructure$Component"));
            Class<?> config = Class.forName("com.hbm.config.StructureConfig");
            JsonObject settings = new JsonObject();
            for (String key : new String[]{"enableStructures", "structureMinChunks", "structureMaxChunks",
                    "lootAmountFactor", "enableRuins", "enableOceanStructures", "debugStructures"})
                property(settings, config, key);
            out.add("settings", settings);
            JsonObject fluids=new JsonObject();
            Class<?> fluidRegistry=Class.forName("com.hbm.inventory.fluid.Fluids");
            Class<?> fluidType=Class.forName("com.hbm.inventory.fluid.FluidType");
            for(Field field:fluidRegistry.getFields())if(java.lang.reflect.Modifier.isStatic(field.getModifiers())&&field.getType()==fluidType){
                Object fluid=field.get(null);
                if(fluid!=null)fluids.addProperty((String)fluidType.getMethod("getName").invoke(fluid),
                    (Number)fluidType.getMethod("getID").invoke(fluid));
            }
            out.add("structureFluidIds",fluids);
            out.addProperty("worldMapFeaturesEnabled", world.getWorldInfo().isMapFeaturesEnabled());
            out.addProperty("heightSemantics", "native-unshifted; ocean clamps may be seabed anchors, not surface heights");
            JsonObject legacy = new JsonObject();
            Class<?> general = Class.forName("com.hbm.config.GeneralConfig");
            for (String key : new String[]{"enableDungeons", "enableVaults", "enableMines", "enable528BosniaSimulator"}) property(legacy, general, key);
            Class<?> worldConfig = Class.forName("com.hbm.config.WorldConfig");
            for (String key : new String[]{"antennaStructure", "atomStructure", "dungeonStructure", "barrelStructure",
                    "arcticStructure", "jungleStructure", "pyramidStructure", "vaultfreq", "broadcaster",
                    "minefreq", "geyserChlorine", "capsuleStructure", "dudStructure", "meteoriteSpawn",
                    "spaceshipStructure", "gasbubbleSpawn", "explosivebubbleSpawn"}) property(legacy, worldConfig, key);
            Class<?> mobs = Class.forName("com.hbm.config.MobConfig");
            for (String key : new String[]{"enableHives", "hiveSpawn"}) property(legacy, mobs, key);
            out.add("legacySettings", legacy);
            JsonArray featureRules=new JsonArray();JsonObject legacyBiomes=new JsonObject();
            for(String name:new String[]{"antenna","desert_atom","waste_tank","ancient_tomb","soyuz_capsule","chlorine_geyser"})legacyBiomes.add(name,new JsonArray());
            java.util.Set<Integer> seenBiomeIds=new java.util.HashSet<>();
            for(BiomeGenBase b:BiomeGenBase.getBiomeGenArray())if(b!=null&&seenBiomeIds.add(b.biomeID)){
                JsonObject row=new JsonObject();row.addProperty("id",b.biomeID);row.addProperty("temperature",b.temperature);
                row.addProperty("rainfall",b.rainfall);row.addProperty("heightVariation",b.heightVariation);
                row.addProperty("mesa",b instanceof net.minecraft.world.biome.BiomeGenMesa);featureRules.add(row);
                if(b.temperature>=0.4F&&b.rainfall<=0.6F)legacyBiomes.getAsJsonArray("antenna").add(new com.google.gson.JsonPrimitive(b.biomeID));
                if(b.temperature>=1.5F&&!b.canSpawnLightningBolt()){
                    legacyBiomes.getAsJsonArray("desert_atom").add(new com.google.gson.JsonPrimitive(b.biomeID));
                    legacyBiomes.getAsJsonArray("waste_tank").add(new com.google.gson.JsonPrimitive(b.biomeID));
                }
                if(b.temperature>=2.0F&&!b.canSpawnLightningBolt())legacyBiomes.getAsJsonArray("ancient_tomb").add(new com.google.gson.JsonPrimitive(b.biomeID));
                if(b==BiomeGenBase.beach)legacyBiomes.getAsJsonArray("soyuz_capsule").add(new com.google.gson.JsonPrimitive(b.biomeID));
                if(b==BiomeGenBase.plains)legacyBiomes.getAsJsonArray("chlorine_geyser").add(new com.google.gson.JsonPrimitive(b.biomeID));
            }
            out.add("featureBiomeRules",featureRules);out.add("legacyBiomeIds",legacyBiomes);
            JsonArray cold=new JsonArray(),jungle=new JsonArray();java.util.Map<Integer,String> predicates=new java.util.TreeMap<>();
            for(BiomeGenBase biome:BiomeGenBase.getBiomeGenArray())if(biome!=null){
                boolean c=biome.getFloatTemperature(0,32,0)<0.2F;
                boolean j=biome==BiomeGenBase.jungle||biome==BiomeGenBase.jungleEdge||biome==BiomeGenBase.jungleHills;
                String value=c+":"+j,previous=predicates.put(biome.biomeID,value);
                if(previous!=null&&!previous.equals(value))throw new IllegalStateException("Conflicting legacy structure biome predicate for ID "+biome.biomeID);
                if(previous==null){
                    if(c)cold.add(new com.google.gson.JsonPrimitive(biome.biomeID));
                    if(j)jungle.add(new com.google.gson.JsonPrimitive(biome.biomeID));
                }
            }
            out.add("legacyColdBiomeIds",cold);out.add("legacyJungleBiomeIds",jungle);
            java.util.Set<String> poolNames=new java.util.TreeSet<>();
            Object[] loose=(Object[])Class.forName("com.hbm.util.LootGenerator").getMethod("getLootNames").invoke(null);
            for(Object name:loose)poolNames.add((String)name);
            Map<?,?> pools=(Map<?,?>)Class.forName("com.hbm.itempool.ItemPool").getField("pools").get(null);
            for(Object name:pools.keySet())poolNames.add((String)name);
            JsonArray lootPools=new JsonArray();
            for(String name:poolNames)lootPools.add(new com.google.gson.JsonPrimitive(name));
            out.add("deferredLootPools",lootPools);
            out.addProperty("available", true);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            out.addProperty("error", ex.toString());
        }
        return out;
    }
}
