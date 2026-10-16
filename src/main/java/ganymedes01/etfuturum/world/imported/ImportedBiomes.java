package ganymedes01.etfuturum.world.imported;

import ganymedes01.etfuturum.api.tags.BiomeTags;
import ganymedes01.etfuturum.configuration.configs.ConfigImportedBiomes;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ImportedBiomes {
    private static final Map<String, BiomeGenBase> REGISTERED = new LinkedHashMap<>();
    private ImportedBiomes() {}

    public static void register() {
        if (!ConfigImportedBiomes.enableModernImportBiomes && !ConfigMapCompatibility.importedEarthMode) return;
        if (!REGISTERED.isEmpty()) throw new IllegalStateException("Imported biome registration is startup-only");
        validateAllocation(ConfigImportedBiomes.IDS);
        for (ImportedBiome definition : ImportedBiome.values()) {
            int id = ConfigImportedBiomes.IDS.get(definition.sourceName());
            if (id == -1) continue;
            BiomeGenBase biome = new ImportedSurfaceBiome(id, definition);
            BiomeDictionary.registerBiomeType(biome, definition.types());
            BiomeTags.addTags(biome, "etfuturum:imported_surface_biome", definition.sourceName());
            REGISTERED.put(definition.sourceName(), biome);
        }
    }

    /** Validate all IDs before allocating anything in the global biome array. */
    public static void validateAllocation(Map<String,Integer> configured) {
        Set<Integer> used = new HashSet<>();
        for (ImportedBiome definition : ImportedBiome.values()) {
            Integer id = configured.get(definition.sourceName());
            if (id == null) throw new IllegalStateException("Missing configured ID for " + definition.sourceName());
            if (id == -1) continue;
            if (id < 0 || id >= 255 || !used.add(id)) throw new IllegalStateException("Invalid/duplicate imported biome ID " + id);
            BiomeGenBase existing = BiomeGenBase.getBiomeGenArray()[id];
            if (existing != null) throw new IllegalStateException(definition.sourceName() + " ID " + id + " is occupied by " + existing.biomeName);
        }

    }

    /** Detect another mod overwriting a registered identity later in startup. */
    public static void verifyRegistry() {
        for (Map.Entry<String, BiomeGenBase> e : REGISTERED.entrySet()) {
            if (BiomeGenBase.getBiomeGenArray()[e.getValue().biomeID] != e.getValue()) {
                throw new IllegalStateException("Imported biome overwritten by another registration: " + e.getKey());
            }
        }
    }
    public static BiomeGenBase resolve(String sourceName) { return REGISTERED.get(sourceName); }
    public static Map<String, BiomeGenBase> registered() { return Collections.unmodifiableMap(REGISTERED); }
}
