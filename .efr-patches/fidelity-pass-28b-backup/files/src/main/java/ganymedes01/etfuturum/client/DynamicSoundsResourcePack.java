package ganymedes01.etfuturum.client;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.configuration.configs.ConfigSounds;
import makamys.mclib.json.JsonUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class DynamicSoundsResourcePack implements IResourcePack {

	@Override
	public Set<String> getResourceDomains() {
		// The vanilla namespace carries EFR's cave additions; the versioned AssetDirector namespace
		// also needs a tiny sounds.json overlay for modern sound-event aliases (type=event).
		return ImmutableSet.of("minecraft", Tags.MC_ASSET_VER);
	}

	@Override
	public IMetadataSection getPackMetadata(IMetadataSerializer p_135058_1_, String p_135058_2_) throws IOException {
		return null;
	}

	@Override
	public BufferedImage getPackImage() {
		return null;
	}

	@Override
	public String getPackName() {
		return "Et Futurum Requiem dynamic sounds.json";
	}

	public InputStream getInputStream(ResourceLocation resLoc) {
		if (!resLoc.getResourcePath().equals("sounds.json")) return null;
		JsonObject json = Tags.MC_ASSET_VER.equals(resLoc.getResourceDomain())
				? new JsonCreator().getModernAliasJson()
				: new JsonCreator().getVanillaJson();
		return new ByteArrayInputStream(json.toString().getBytes());
	}

	public boolean resourceExists(ResourceLocation resLoc) {
		return resLoc.getResourcePath().equals("sounds.json")
				&& ("minecraft".equals(resLoc.getResourceDomain()) || Tags.MC_ASSET_VER.equals(resLoc.getResourceDomain()));
	}

	@SuppressWarnings("unchecked")
	public static void inject() {
		if (shouldGenerateJson()) {
			IResourcePack dynamicResourcePack = new DynamicSoundsResourcePack();
			Minecraft.getMinecraft().defaultResourcePacks.add(dynamicResourcePack);
			IResourceManager resMan = Minecraft.getMinecraft().getResourceManager();
			if (resMan instanceof SimpleReloadableResourceManager) {
				((SimpleReloadableResourceManager) resMan).reloadResourcePack(dynamicResourcePack);
			}
		}
	}

	public static boolean shouldGenerateJson() {
		// The modern event alias is always required by hanging-sign interaction parity.
		return true;
	}

	public class JsonCreator {
		private final JsonObject rootObject = new JsonObject();

		private void addSoundsToCategory(String cat, String... sounds) {
			JsonObject soundCat = JsonUtil.getOrCreateObject(rootObject, cat);
			JsonArray soundList = JsonUtil.getOrCreateArray(soundCat, "sounds");
			for (String sound : sounds) {
				soundList.add(new JsonPrimitive(sound));
			}
		}

		private void addSoundEventsToCategory(String cat, String... sounds) {
			addSoundsToCategoryWithSettings(cat, 1.0F, 1.0F, 1, false, true, sounds);
		}

		private void addSoundsToCategoryWithSettings(String cat, float volume, float pitch, String... sounds) {
			addSoundsToCategoryWithSettings(cat, volume, pitch, 1, false, false, sounds);
		}

		private void addSoundsToCategoryWithSettings(String cat, float volume, float pitch, int weight, String... sounds) {
			addSoundsToCategoryWithSettings(cat, volume, pitch, weight, false, false, sounds);
		}

		private void addSoundsToCategoryWithSettings(String cat, float volume, float pitch, int weight, boolean stream, boolean isEvent, String... sounds) {
			JsonObject soundCat = JsonUtil.getOrCreateObject(rootObject, cat);
			JsonArray soundList = JsonUtil.getOrCreateArray(soundCat, "sounds");
			for (String sound : sounds) {
				JsonObject soundObj = new JsonObject();
				soundObj.add("name", new JsonPrimitive(sound));
				if (volume != 1.0F) {
					soundObj.add("volume", new JsonPrimitive(MathHelper.clamp_float(volume, 0, 1)));
				}
				if (pitch != 1.0F) {
					soundObj.add("pitch", new JsonPrimitive(pitch));
				}
				if (weight > 1) {
					soundObj.add("weight", new JsonPrimitive(weight));
				}
				if (stream) {
					soundObj.add("stream", new JsonPrimitive(true));
				}
				if (isEvent) {
					soundObj.add("type", new JsonPrimitive("event"));
				}
				soundList.add(soundObj);
			}
		}

		public JsonObject getVanillaJson() {
			if (ConfigSounds.caveAmbience) {
				addSoundsToCategory("ambient.cave.cave",
						Tags.MC_ASSET_VER + ":ambient/cave/cave14",
						Tags.MC_ASSET_VER + ":ambient/cave/cave15",
						Tags.MC_ASSET_VER + ":ambient/cave/cave16",
						Tags.MC_ASSET_VER + ":ambient/cave/cave17",
						Tags.MC_ASSET_VER + ":ambient/cave/cave18",
						Tags.MC_ASSET_VER + ":ambient/cave/cave19");
			}
			return rootObject;
		}

		public JsonObject getModernAliasJson() {
			// Vanilla 1.21.11: block.hanging_sign.waxed_interact_fail ->
			// { name: block.sign.waxed_interact_fail, type: event }. Minecraft 1.7 already
			// resolves bare type=event names inside the containing sounds.json namespace.
			// Do NOT prefix Tags.MC_ASSET_VER here: SoundHandler applies that domain itself,
			// and a prefixed name becomes a malformed/doubled event ResourceLocation.
			addSoundEventsToCategory("block.hanging_sign.waxed_interact_fail",
					"block.sign.waxed_interact_fail");

			/*
			 * Do not request the Decorated Pot OGGs through AssetDirector. A failed optional
			 * sound download aborts that library's whole per-mod pass before it loads the
			 * modern client JAR, which removes every AssetDirector-backed model and texture.
			 * Define the two versioned events locally using stable vanilla 1.7 sound objects.
			 */
			addSoundsToCategory("block.decorated_pot.insert",
					"minecraft:random/pop");
			addSoundsToCategory("block.decorated_pot.shatter",
					"minecraft:dig/glass1",
					"minecraft:dig/glass2",
					"minecraft:dig/glass3");
			return rootObject;
		}
	}
}
