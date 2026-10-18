package ganymedes01.etfuturum.world.imported;

import com.google.gson.*;
import cpw.mods.fml.common.Loader;
import net.minecraft.block.Block;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Exact-pack HarvestCraft flags, registry names and live Forge biome predicates. */
public final class HarvestImportResources {
    private static final String[] GARDENS={"berry","desert","grass","gourd","ground","herb","leafy","mushroom","stalk","textile","tropical","water"};
    private static boolean type(BiomeGenBase b,Type type){return BiomeDictionary.isBiomeOfType(b,type);}
    public static boolean eligible(String rule,BiomeGenBase b) {
        if(type(b,Type.DEAD))return false;
        boolean forest=type(b,Type.FOREST),cold=type(b,Type.COLD),wet=type(b,Type.WET),plains=type(b,Type.PLAINS);
        switch(rule) {
            case "berry":return forest||type(b,Type.HILLS)||cold||wet;
            case "desert":return type(b,Type.SANDY);
            case "grass":return plains&&!cold;
            case "gourd":return plains||forest;
            case "ground":return type(b,Type.MOUNTAIN)||type(b,Type.HILLS)||type(b,Type.MESA)||type(b,Type.SAVANNA);
            case "herb":case "leafy":return forest&&!cold||wet;
            case "mushroom":return forest&&!cold&&!type(b,Type.MOUNTAIN)||wet;
            case "stalk":return (plains||forest)&&!cold||wet;
            case "textile":return cold||type(b,Type.HILLS)||wet;
            case "tropical":return wet||type(b,Type.HOT);
            case "water":return (type(b,Type.OCEAN)||type(b,Type.RIVER))&&!cold;
            case "tree/temperate":return forest&&!cold&&!type(b,Type.SPOOKY)&&!type(b,Type.MOUNTAIN);
            case "tree/coastal":return type(b,Type.OCEAN);
            case "tree/tropical":return (wet||type(b,Type.HOT))&&!plains&&!type(b,Type.DRY);
            // This nesting is present in the actual Lb generator, not an inferred biome rule.
            case "tree/coniferous":return eligible("tree/tropical",b)&&type(b,Type.CONIFEROUS);
            default:throw new IllegalArgumentException("Unknown HarvestCraft biome rule "+rule);
        }
    }
    public static JsonObject rules() {
        try(InputStreamReader reader=new InputStreamReader(HarvestImportResources.class.getResourceAsStream("/assets/etfuturum/harvest_import_rules.json"),StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        } catch(Exception failure){throw new IllegalStateException("HarvestCraft import rules missing",failure);}
    }
    private static JsonArray biomes(String rule) {
        JsonArray ids=new JsonArray();
        for(BiomeGenBase b:BiomeGenBase.getBiomeGenArray())if(b!=null && eligible(rule,b))ids.add(new JsonPrimitive(b.biomeID));
        return ids;
    }
    private static JsonObject block(Class<?> registry,String field) throws Exception {
        Block block=(Block)registry.getField(field).get(null);String name=Block.blockRegistry.getNameForObject(block);
        if(block==null || name==null || !name.startsWith("harvestcraft:"))throw new IllegalStateException("Missing HarvestCraft block "+field);
        JsonObject row=new JsonObject();row.addProperty("block",name);row.addProperty("blockId",Block.getIdFromBlock(block));return row;
    }
    public static JsonObject describe() {
        JsonObject out=new JsonObject();out.addProperty("schemaVersion",1);out.addProperty("available",false);
        if(!Loader.isModLoaded("harvestcraft"))return out;
        try {
            Class<?> registry=Class.forName("com.pam.harvestcraft.BlockRegistry");JsonObject rules=rules();
            out.addProperty("rulesJarSha256",rules.get("jarSha256").getAsString());
            JsonObject settings=new JsonObject();
            for(java.lang.reflect.Field field:registry.getFields())if(java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                    (field.getName().endsWith("Generation") || field.getName().endsWith("Rarity"))) {
                if(field.getType()==boolean.class)settings.addProperty(field.getName(),field.getBoolean(null));
                if(field.getType()==int.class)settings.addProperty(field.getName(),field.getInt(null));
            }
            out.add("settings",settings);JsonArray gardens=new JsonArray(),trees=new JsonArray();
            for(String name:GARDENS) {
                JsonObject row=block(registry,"pam"+name+"Garden");row.addProperty("name",name);
                row.addProperty("enabled",registry.getField("enable"+name+"gardenGeneration").getBoolean(null));
                row.add("biomeIds",biomes(name));gardens.add(row);
            }
            for(JsonElement element:rules.getAsJsonArray("trees")) {
                JsonObject nativeRow=element.getAsJsonObject(),row=block(registry,nativeRow.get("field").getAsString());
                for(java.util.Map.Entry<String,JsonElement> entry:nativeRow.entrySet())row.add(entry.getKey(),entry.getValue());
                row.addProperty("enabled",registry.getField(row.get("toggle").getAsString()).getBoolean(null));
                row.add("biomeIds",biomes("tree/"+row.get("group").getAsString()));trees.add(row);
            }
            out.add("gardens",gardens);out.add("trees",trees);
            out.add("beehive",block(registry,"pamBeehive"));out.add("salt",block(registry,"pamSalt"));
            out.addProperty("available",true);
        } catch(Throwable failure) {out.addProperty("error",failure.toString());}
        return out;
    }
}
