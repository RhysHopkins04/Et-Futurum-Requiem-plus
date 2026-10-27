package ganymedes01.etfuturum.mixinplugin;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.configuration.configs.ConfigModCompat;
import ganymedes01.etfuturum.configuration.configs.ConfigSounds;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.List;
import java.util.Set;

@LateMixin
public class EtFuturumLateMixins implements ILateMixinLoader {
	public static final MixinEnvironment.Side side = MixinEnvironment.getCurrentEnvironment().getSide();

	@Override
	public String getMixinConfig() {
		return "mixins." + Tags.MOD_ID + ".late.json";
	}

	@Override
	public List<String> getMixins(Set<String> loadedMods) {
		List<String> mixins = new ObjectArrayList<>();
        if (side == MixinEnvironment.Side.CLIENT && loadedMods.contains("angelica")
                && getClass().getClassLoader().getResource("com/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuilderMeshingTask.class") != null) {
            mixins.add("containedwater.client.MixinAngelicaFluidBatch");
            mixins.add("containedwater.client.MixinAngelicaWaterSkylight");
        }
		if (loadedMods.contains("harvestcraft") && ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility.importedEarthMode) {
			mixins.add("harvestcraft.MixinFruitTreeHeight");
		}
		if (loadedMods.contains("hbm")) {
			mixins.add("ntm.MixinCoalDustBreakEffects");
            mixins.add("ntm.MixinStructureGround");
			mixins.add("ntm.MixinShredderRecipes");
			mixins.add("ntm.MixinCentrifugeRecipes");
			mixins.add("ntm.MixinCrystallizerRecipes");
		}

		if (ConfigMixins.enableElytra && loadedMods.stream().anyMatch(name -> name.equals("Thaumcraft"))) {
			mixins.add("backlytra.thaumcraft.MixinEventHandlerEntity");
		}

		if (ConfigMixins.enableSpectatorMode) {
			if (loadedMods.contains("IronChest")) {
				mixins.add("spectator.MixinContainerIronChest");
			}
			if (loadedMods.contains("appliedenergistics2")) {
				mixins.add("spectator.MixinPacketInventoryAction");
			}
		}

		if(ConfigBlocksItems.enableDeepslateOres && ConfigModCompat.moddedDeepslateOres && !ConfigModCompat.moddedDeepslateOresBlacklist.contains("appliedenergistics2") && loadedMods.contains("appliedenergistics2")) {
			mixins.add("deepslateores.MixinRenderQuartzOre");
		}

		if(side == MixinEnvironment.Side.CLIENT && ConfigSounds.newBlockSounds) {
			if(loadedMods.contains("TConstruct")) {
				mixins.add("sounds.client.MixinMultiBrick");
				mixins.add("sounds.client.MixinTMetalBlock");
			}
			if(loadedMods.contains("IronChest")) {
				mixins.add("sounds.client.MixinBlockIronChest");
			}
		}

		return mixins;
	}
}
