package ganymedes01.etfuturum.configuration.configs;

import ganymedes01.etfuturum.configuration.ConfigBase;

import java.io.File;

/**
 * Et Futurum Requiem Plus content-provider profile.
 *
 * The saved upstream configuration is intentionally left intact. When this profile is enabled we
 * apply a runtime overlay which prevents Et Futurum from taking ownership of world generation or
 * progression-oriented integrations. Disabling the profile and restarting restores the user's
 * ordinary Et Futurum settings from their existing config files.
 */
public class ConfigMapCompatibility extends ConfigBase {

    public static boolean mapCompatibilityMode;
    public static boolean importedEarthMode;

    private static final String CATEGORY = "map compatibility mode";

    public ConfigMapCompatibility(File file) {
        super(file);
        setCategoryComment(CATEGORY,
                "Content-provider profile for imported/generated maps and RTG-based packs. " +
                "When enabled, Et Futurum keeps its registered content available but suppresses " +
                "world generation, dimension-provider replacement, natural-spawn injection and " +
                "automatic progression integrations. A full game restart is required after changing this option.");
        configCats.add(getCategory(CATEGORY));
    }

    @Override
    protected void syncConfigOptions() {
        mapCompatibilityMode = getBoolean("mapCompatibilityMode", CATEGORY, false,
                "Master Et Futurum Requiem Plus content-only / map-compatibility profile. " +
                "RTG or another world generator remains authoritative while Et Futurum supplies blocks and items. " +
                "This does not delete or rewrite your other Et Futurum configuration values; it overrides selected values at runtime. " +
                "REQUIRES A FULL RESTART.");
        importedEarthMode = getBoolean("importedEarthMode", CATEGORY, false,
                "Imported modern Earth profile: suppress EFR automatic terrain generation and provider replacement, " +
                "but retain the 384-block engine and configured ore/content registrations. " +
                "Enable on BOTH client and server. Mutually exclusive with mapCompatibilityMode. REQUIRES A FULL RESTART.");
    }

    public static boolean isEnabled() {
        return mapCompatibilityMode || importedEarthMode;
    }

    public static boolean allowsExtendedHeight() {
        return !mapCompatibilityMode;
    }

    /**
     * Apply the content-only overlay after all individual config files have been read. This must be
     * safe to call repeatedly because config values also have late construction/init phases.
     */
    public static void applyCompatibilityOverrides() {
        if (mapCompatibilityMode && importedEarthMode) {
            throw new IllegalStateException("Choose importedEarthMode OR mapCompatibilityMode, not both; their height contracts differ.");
        }
        if (!isEnabled()) {
            return;
        }

        if (mapCompatibilityMode) {
            ConfigModCompat.moddedRawOres = false;
            ConfigModCompat.moddedDeepslateOres = false;
            ConfigFunctions.registerRawItemAsOre = false;

            ConfigMixins.enableElytra = false;
            ConfigEntities.enableShulker = false;
            ConfigBlocksItems.enableShulkerBoxes = false;
            ConfigEntities.enableDragonRespawn = false;
        }

        ConfigExperiments.netherDimensionProvider = false;
        ConfigExperiments.endDimensionProvider = false;
        ConfigExperiments.enableEndCities = false;

        // Prevent chunk-load replacement/retrofit behaviour from mutating imported terrain.
        ConfigWorld.tileReplacementMode = -1;

        // Extended world height is an early engine/core protocol change, not a content feature.
        // Legacy Compatibility Mode leaves the 1.7.10/RTG architecture untouched; Earth explicitly needs 384.
        ConfigWorld.extendedWorldHeight = importedEarthMode;
        ConfigWorld.modernOverworldGeneration = false;
        ConfigWorld.modernOreGeneration = false;
        ConfigWorld.modernLargeOreVeins = false;
        ConfigModCompat.modernOverworldTranslateModdedOreHeights = false;
        ConfigModCompat.modernOverworldReshapeModdedOreVeins = false;

        // Explicit belt-and-braces gate for the Plus Lush Cave backport. The three IWorldGenerator
        // registrations are already skipped in this profile, but keeping the feature flag false at
        // runtime also prevents accidental/direct invocation by integration code.
        ConfigWorld.lushCavesWorldgen = false;
        ConfigWorld.dripstoneCavesWorldgen = false;
    }
}
