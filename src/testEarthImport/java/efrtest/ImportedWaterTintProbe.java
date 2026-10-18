package efrtest;

import ganymedes01.etfuturum.client.handlers.ImportedWaterTintHandler;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.world.imported.ImportedBiomes;
import ganymedes01.etfuturum.world.imported.ImportedWaterProfiles;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.MinecraftForge;
import static efrtest.EarthImportContracts.check;

/** Exercise the actual Forge biome getter used by Angelica's smooth-water cache. */
final class ImportedWaterTintProbe {
    static void run() {
        ImportedWaterTintHandler handler=new ImportedWaterTintHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        boolean enabled=ConfigMapCompatibility.modernImportedWater;
        try {
            ConfigMapCompatibility.modernImportedWater=true;
            check(ConfigMapCompatibility.importedEarthMode,"probe needs imported profile");
            check(BiomeGenBase.ocean.getWaterColorMultiplier()==0x3f76e4,"Angelica biome ocean tint remained white");
            check(BiomeGenBase.river.getWaterColorMultiplier()==0x3f76e4,"river biome tint remained white");
            check(BiomeGenBase.swampland.getWaterColorMultiplier()==0x617b64,"swamp biome tint mismatch");
            check(BiomeGenBase.frozenOcean.getWaterColorMultiplier()==0x3938c9,"frozen ocean tint mismatch");
            for(String name:new String[]{"warm_ocean","lukewarm_ocean","cold_ocean","mangrove_swamp"}) {
                BiomeGenBase biome=ImportedBiomes.resolve("minecraft:"+name);
                check(biome!=null && biome.getWaterColorMultiplier()==ImportedWaterProfiles.named("minecraft:"+name).waterColor,"imported biome direct tint "+name);
                check(ImportedWaterProfiles.color(biome)==biome.getWaterColorMultiplier(),"block/direct-biome tint disagreement "+name);
            }
            check(ImportedWaterProfiles.color(BiomeGenBase.plains,0x123456)==0x123456,"custom biome/event tint was lost");
            ConfigMapCompatibility.modernImportedWater=false;
            check(BiomeGenBase.ocean.getWaterColorMultiplier()==0xffffff,"disabled profile must retain legacy water tint");
            check(BiomeGenBase.ocean.waterColorMultiplier==0xffffff,"underlying biome field was mutated");
            System.out.println("IMPORTED_WATER_TINT PASSED: actual Forge biome getters, ordinary/imported biomes, block agreement, custom tint, config off");
        } finally {
            ConfigMapCompatibility.modernImportedWater=enabled;
            MinecraftForge.EVENT_BUS.unregister(handler);
        }
    }
}
