package efrtest;

import com.google.gson.*;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.client.ModernAssetResourcePack;
import net.minecraft.util.ResourceLocation;
import java.io.*;
import java.lang.reflect.Method;
import java.util.Map;

/** Exercises the actual resource bridge's routing without a Minecraft window or GL context. */
public final class WorkstationAssetContracts {
    public static void run() throws Exception {
        String catalog="/assets/etfuturum/asset_aliases/workstations_1_21_11.json";
        JsonObject aliases;
        try(InputStream input=WorkstationAssetContracts.class.getResourceAsStream(catalog)) {
            if(input==null)throw new AssertionError("Missing workstation alias catalog");
            aliases=new JsonParser().parse(new InputStreamReader(input,"UTF-8")).getAsJsonObject().getAsJsonObject("aliases");
        }
        Method target=ModernAssetResourcePack.class.getDeclaredMethod("toModernLocation",ResourceLocation.class);target.setAccessible(true);
        for(Map.Entry<String,JsonElement> row:aliases.entrySet()) {
            if(!ModernAssetResourcePack.getAliasedPaths().contains(row.getKey()))throw new AssertionError("Unregistered alias "+row.getKey());
            ResourceLocation actual=(ResourceLocation)target.invoke(null,new ResourceLocation("minecraft",row.getKey()));
            if(!Tags.MC_ASSET_VER.equals(actual.getResourceDomain()) || !row.getValue().getAsString().equals(actual.getResourcePath()))throw new AssertionError("Wrong AssetDirector target "+row.getKey());
            if(WorkstationAssetContracts.class.getResource("/assets/minecraft/"+row.getKey())!=null)throw new AssertionError("Copied modern art in runtime resources "+row.getKey());
        }
        if(aliases.entrySet().size()!=32)throw new AssertionError("Unexpected alias count");
        System.out.println("WORKSTATION_ASSETS PASSED: 32 actual bridge routes use AssetDirector; no migrated PNGs in resources");
    }
}
