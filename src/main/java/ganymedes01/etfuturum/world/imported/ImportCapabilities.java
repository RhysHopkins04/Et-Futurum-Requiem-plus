package ganymedes01.etfuturum.world.imported;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.api.imported.ImportedBiomeVolume;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.configuration.configs.ConfigModCompat;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/** Explicit runtime evidence, not certification that third-party providers implement every height hook. */
public final class ImportCapabilities {
    private ImportCapabilities() {}
    public static JsonObject describe(WorldServer world) {
        ImportedBiomes.verifyRegistry();
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1); root.addProperty("efrVersion", Tags.VERSION);
        root.addProperty("importedEarthMode", ConfigMapCompatibility.importedEarthMode);
        root.addProperty("legacyMapCompatibilityMode", ConfigMapCompatibility.mapCompatibilityMode);
        root.addProperty("extendedWorldHeight", ConfigWorld.extendedWorldHeight);
        root.addProperty("moddedDeepslateOres", ConfigModCompat.moddedDeepslateOres);
        root.addProperty("automaticEfrWorldGeneration", !ConfigMapCompatibility.isEnabled());
        root.addProperty("biomeDataKey", ImportedBiomeVolume.NBT_KEY);
        root.addProperty("biomeDataVersion", ImportedBiomeVolume.VERSION);
        root.addProperty("biomeQueries", "server-only; 2D fallback; no implicit client synchronization");
        root.addProperty("dimension", world.provider.dimensionId);
        root.addProperty("providerClass", world.provider.getClass().getName());
        root.addProperty("chunkManagerClass", world.provider.worldChunkMgr.getClass().getName());
        root.addProperty("height", world.getHeight()); root.addProperty("actualHeight", world.getActualHeight());
        root.addProperty("yOffset", ConfigMapCompatibility.importedEarthMode ? 64 : 0);
        root.addProperty("importedPopulationProtection",1);
        root.add("harvestNativeResources",HarvestImportResources.describe());
        JsonObject stateEncodings = new JsonObject();
        stateEncodings.addProperty("campfireWater", 1);
        root.add("blockStateEncodings", stateEncodings);
        JsonObject ids = new JsonObject();
        for (Map.Entry<String, BiomeGenBase> entry : ImportedBiomes.registered().entrySet()) ids.addProperty(entry.getKey(), entry.getValue().biomeID);
        root.add("surfaceBiomeIds", ids);
        JsonArray biomes = new JsonArray();
        for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) if (biome != null) {
            JsonObject entry = new JsonObject(); entry.addProperty("id", biome.biomeID);
            entry.addProperty("name", biome.biomeName); entry.addProperty("class", biome.getClass().getName()); biomes.add(entry);
        }
        root.add("registeredBiomes", biomes);
        JsonObject blocks = new JsonObject();
        for (Object key : net.minecraft.block.Block.blockRegistry.getKeys()) {
            net.minecraft.block.Block block = (net.minecraft.block.Block)net.minecraft.block.Block.blockRegistry.getObject(key);
            blocks.addProperty(key.toString(), net.minecraft.block.Block.getIdFromBlock(block));
        }
        root.add("blockIds", blocks);
        JsonObject items = new JsonObject();
        for (Object key : net.minecraft.item.Item.itemRegistry.getKeys()) {
            net.minecraft.item.Item item = (net.minecraft.item.Item)net.minecraft.item.Item.itemRegistry.getObject(key);
            items.addProperty(key.toString(), net.minecraft.item.Item.getIdFromItem(item));
        }
        root.add("itemIds", items);
        JsonObject mods = new JsonObject();
        for (ModContainer mod : Loader.instance().getActiveModList()) mods.addProperty(mod.getModId(), mod.getVersion());
        root.add("mods", mods);
        root.addProperty("importedStructureActionsVersion", 1);
        root.addProperty("importedStructureActionsKey", ImportedStructureActions.KEY);
        root.add("ntmNativeResources", NtmImportResources.describe(world));
        root.add("ntmNativeStructures", NtmImportStructures.describe(world));
        root.add("ae2NativeResources", Ae2ImportResources.describe(world));
        return root;
    }
    public static File export(WorldServer world) throws IOException {
        File result = new File(world.getSaveHandler().getWorldDirectory(), "efr-import-capabilities.json");
        File temporary = File.createTempFile("efr-import-", ".json", result.getParentFile());
        try {
            Files.write(temporary.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(describe(world)).getBytes(StandardCharsets.UTF_8));
            Files.move(temporary.toPath(), result.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary.toPath()); }
        return result;
    }
}
