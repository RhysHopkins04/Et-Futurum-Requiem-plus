package ganymedes01.etfuturum.configuration.configs;

import com.google.common.collect.Lists;
import ganymedes01.etfuturum.configuration.ConfigBase;
import ganymedes01.etfuturum.lib.Reference;

import java.io.File;
import java.util.List;

public class ConfigExperiments extends ConfigBase {

	private static final String catExperiments = "EXPERIMENTAL FEATURES -- TREAD CAREFULLY";
	private static final List<String> enabledFeatures = Lists.newLinkedList();
	public static boolean enableSculk;
	public static boolean enableCrimsonBlocks;
	public static boolean enableWarpedBlocks;
	public static boolean enableMangroveBlocks;
	public static boolean enableLightningRod;

	public static boolean netherDimensionProvider;
	public static boolean endDimensionProvider;
	public static boolean enableEndCities;

	public ConfigExperiments(File file) {
		super(file);
		setCategoryComment(catExperiments,
                """
                        Unfinished features. Handle with care! To automatically enable all of these at once, use "-Detfuturum.testing=true" in your program arguments.
                        For the safety of people playing any packs that include these features, a chat message will be issued when any of them are enabled.
                        These features are not finished, may cause breakages and are subject to receive major changes at any time.
                        This can also include breaking changes, and even changed IDs.
                        
                        Note that when a config option has no comment at all, not even saying what the default value is, that means the option was removed.
                        In that case check the regular configs as it was likely moved there.
                        """);

		configCats.add(getCategory(catExperiments));
	}

	@Override
	protected void syncConfigOptions() {
		enableCrimsonBlocks = getBoolean("enableCrimsonBlocks", catExperiments, false, "EXPERIMENTAL Crimson Forest BIOME/WORLDGEN switch. Crimson wood and vegetation content are controlled separately in blocksitems.cfg and remain available without enabling generation.");
		enableWarpedBlocks = getBoolean("enableWarpedBlocks", catExperiments, false, "EXPERIMENTAL Warped Forest BIOME/WORLDGEN switch. Warped wood and vegetation content are controlled separately in blocksitems.cfg and remain available without enabling generation.");
		enableMangroveBlocks = getBoolean("enableMangroveBlocks", catExperiments, false, "Reserved EXPERIMENTAL Mangrove biome/worldgen switch. Mangrove wood, leaves, propagules, roots and muddy roots are normal content and are controlled separately in blocksitems.cfg.");
		enableSculk = getBoolean("enableSculk", catExperiments, false, "Reserved EXPERIMENTAL Sculk/Deep Dark mechanics or worldgen switch. Sculk block content is controlled separately by enableSculkBlocks in blocksitems.cfg.");
		// Dripstone graduated from experiments in Et Futurum Requiem Plus P008e.
		// Remove the legacy property; blocksitems.cfg:enableDripstone now owns the content family.
		getCategory(catExperiments).remove("enableDripstone");
		// Moss and azalea graduated from experiments in Et Futurum Requiem Plus P003.
		// Remove the legacy property from existing configs; blocksitems.cfg:enableLushCaveBlocks now owns this family.
		getCategory(catExperiments).remove("enableMossAzalea");
		enableLightningRod = getBoolean("enableLightningRod", catExperiments, false, "Reserved EXPERIMENTAL lightning-rod behaviour switch. The Lightning Rod content identity is controlled separately by enableLightningRodBlock in blocksitems.cfg.");

		netherDimensionProvider = getBoolean("netherDimensionProvider", catExperiments, false, "Enables the Nether dimension provider override needed for supplying custom biomes. This is partially ignored if Netherlicious is installed. Netherlicious has compat to generate Et Futurum Requiem biomes with Netherlicious blocks.\nThis is so you can have vanilla-style biomes in Netherlicious while Requiem is installed. Turning this off or setting each individual biome ID to -1 will prevent my version of Nether biomes from generating. Don't forget to turn off my Nether blocks in blocksitems.cfg since my biomes will generate with Netherlicious blocks if available. [not implemented yet]");
		endDimensionProvider = getBoolean("endDimensionProvider", catExperiments, false, "Enables outer end island generation from 1.9. The End Gateway block content is available separately through blocksitems.cfg:enableEndGatewayBlock; this switch only opts into the unfinished provider/outer-island generation. The new dragon fight is currently not implemented and it does not spawn any gateways.");
		enableEndCities = getBoolean("enableEndCities", catExperiments, false, "Enables End City structure generation on outer End islands. Requires the endDimensionProvider experimental feature to be enabled.");
	}

	@Override
	public boolean getBoolean(String name, String category, boolean defaultValue, String comment) {
		boolean value = super.getBoolean(name, category, defaultValue, comment) || Reference.TESTING;
		if (value) {
			enabledFeatures.add(name);
		}
		return value;
	}

	public static List<String> getEnabledElements() {
		return enabledFeatures;
	}

	public static String buildLoadedExperimentsList(boolean color) {
		StringBuilder elements = new StringBuilder();
		for (int i = 0; i < getEnabledElements().size(); i++) {
			if (color) {
				elements.append("\u00a7e");
			}
			elements.append(getEnabledElements().get(i));
			if (color) {
				elements.append("\u00a7r");
			}
			if (i != getEnabledElements().size() - 1) {
				elements.append(", ");
			}
		}
		return elements.toString();
	}
}
