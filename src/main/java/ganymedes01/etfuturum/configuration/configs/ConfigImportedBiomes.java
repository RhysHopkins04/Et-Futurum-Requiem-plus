package ganymedes01.etfuturum.configuration.configs;

import ganymedes01.etfuturum.configuration.ConfigBase;
import ganymedes01.etfuturum.world.imported.ImportedBiome;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registration only: these settings never select a terrain generator. */
public final class ConfigImportedBiomes extends ConfigBase {
    public static boolean enableModernImportBiomes;
    public static final Map<String, Integer> IDS = new LinkedHashMap<>();

    public ConfigImportedBiomes(File file) {
        super(file);
        configCats.add(getCategory("registration"));
        configCats.add(getCategory("biome ids"));
    }

    @Override protected void syncConfigOptions() {
        enableModernImportBiomes = getBoolean("enableModernImportBiomes", "registration", false,
                "Register modern surface biome identities without generating terrain. Also enabled by importedEarthMode. " +
                "Server/local-world IDs must match the conversion template. Updated multiplayer clients use the server mapping automatically. Full restart required; never change IDs of an existing imported world.");
        for (ImportedBiome entry : ImportedBiome.values()) {
            IDS.put(entry.sourceName(), getInt(entry.path, "biome ids", 210 + entry.ordinal(), -1, 254,
                    "Legacy ID for " + entry.sourceName() + ". -1 disables this identity. " +
                    "255 is the legacy unset sentinel. Occupied or duplicate IDs abort startup rather than replacing another mod's biome. Full restart required."));
        }
    }
}
