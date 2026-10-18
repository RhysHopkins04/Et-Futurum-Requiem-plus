package ganymedes01.etfuturum.world.imported;

import com.google.gson.*;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Values read from the installed official 1.21.11 biome data/environment defaults. */
public final class ImportedWaterProfiles {
    public static final class Profile {
        public final int waterColor,fogColor;public final float start,end;
        Profile(JsonObject json) {
            waterColor=json.get("water_color").getAsInt();fogColor=json.get("fog_color").getAsInt();
            start=json.get("fog_start").getAsFloat();end=json.get("fog_end").getAsFloat();
            if(waterColor<0||waterColor>0xffffff||fogColor<0||fogColor>0xffffff||!Float.isFinite(start)||!Float.isFinite(end)||end<=start)
                throw new IllegalStateException("Invalid imported water profile");
        }
    }
    private static final Map<String,Profile> PROFILES=load();
    private static Map<String,Profile> load() {
        Map<String,Profile> result=new LinkedHashMap<>();
        try(InputStreamReader reader=new InputStreamReader(Objects.requireNonNull(ImportedWaterProfiles.class.getResourceAsStream("/assets/etfuturum/imported_water_profiles.json")),StandardCharsets.UTF_8)) {
            JsonObject root=new JsonParser().parse(reader).getAsJsonObject();
            for(Map.Entry<String,JsonElement> entry:root.getAsJsonObject("profiles").entrySet())result.put(entry.getKey(),new Profile(entry.getValue().getAsJsonObject()));
        } catch(Exception failure) {throw new IllegalStateException("Cannot load modern biome water profiles",failure);}
        return Collections.unmodifiableMap(result);
    }
    public static boolean enabled(){return ConfigMapCompatibility.importedEarthMode && ConfigMapCompatibility.modernImportedWater;}
    public static Profile named(String name){Profile p=PROFILES.get(name);return p==null?PROFILES.get("minecraft:plains"):p;}
    public static Profile forBiome(BiomeGenBase biome) {
        if(biome instanceof ImportedSurfaceBiome)return named(((ImportedSurfaceBiome)biome).definition.sourceName());
        switch(biome.biomeID) {
            case 0:return named("minecraft:ocean");case 24:return named("minecraft:deep_ocean");
            case 6:case 134:return named("minecraft:swamp");
            case 10:return named("minecraft:frozen_ocean");case 11:return named("minecraft:frozen_river");
            default:return named("minecraft:plains");
        }
    }
    public static int color(BiomeGenBase biome) {
        if(biome instanceof ImportedSurfaceBiome)return forBiome(biome).waterColor;
        // Other mods may deliberately color their water; retain their non-default tint.
        int own=biome.getWaterColorMultiplier();
        if(biome.biomeID!=6 && biome.biomeID!=134 && own!=0xffffff)return own;
        return forBiome(biome).waterColor;
    }
    public static int blendedColor(IBlockAccess world,int x,int z) {
        int r=0,g=0,b=0;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++) {
            int c=color(world.getBiomeGenForCoords(x+dx,z+dz));r+=(c>>16)&255;g+=(c>>8)&255;b+=c&255;
        }
        return ((r/9)<<16)|((g/9)<<8)|(b/9);
    }
}
