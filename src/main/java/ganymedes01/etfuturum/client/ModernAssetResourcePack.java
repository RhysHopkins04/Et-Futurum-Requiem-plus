package ganymedes01.etfuturum.client;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Curated bridge from legacy {@code minecraft:textures/blocks/...} resource requests to the
 * modern Mojang client jar downloaded at runtime by MCLib AssetDirector.
 *
 * <p>The class deliberately extends {@link DefaultResourcePack}. AssetDirector ignores built-in
 * packs while checking user resource-pack overrides, which prevents a delegation loop while still
 * allowing real user packs to override the modern assets. Only explicitly listed paths are
 * bridged; vanilla 1.7 textures remain untouched.</p>
 */
@SideOnly(Side.CLIENT)
public final class ModernAssetResourcePack extends DefaultResourcePack {

    private static final Set<String> ALIASED_PATHS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "textures/blocks/amethyst_block.png",
            "textures/blocks/amethyst_cluster.png",
            "textures/blocks/budding_amethyst.png",
            "textures/blocks/small_amethyst_bud.png",
            "textures/blocks/medium_amethyst_bud.png",
            "textures/blocks/large_amethyst_bud.png",
            "textures/blocks/calcite.png",
            "textures/blocks/smooth_basalt.png",
            "textures/blocks/tuff.png",
            "textures/blocks/tuff_bricks.png",
            "textures/blocks/deepslate_top.png",
            "textures/blocks/cobbled_deepslate.png",
            "textures/blocks/polished_deepslate.png",
            "textures/blocks/chiseled_deepslate.png",
            "textures/blocks/deepslate_bricks.png",
            "textures/blocks/cracked_deepslate_bricks.png",
            "textures/blocks/deepslate_tiles.png",
            "textures/blocks/cracked_deepslate_tiles.png",
            "textures/blocks/deepslate_coal_ore.png",
            "textures/blocks/deepslate_iron_ore.png",
            "textures/blocks/deepslate_copper_ore.png",
            "textures/blocks/deepslate_gold_ore.png",
            "textures/blocks/deepslate_redstone_ore.png",
            "textures/blocks/deepslate_emerald_ore.png",
            "textures/blocks/deepslate_lapis_ore.png",
            "textures/blocks/deepslate_diamond_ore.png",
            "textures/blocks/copper_ore.png",
            "textures/blocks/raw_iron_block.png",
            "textures/blocks/raw_gold_block.png",
            "textures/blocks/raw_copper_block.png",
            "textures/items/amethyst_shard.png",
            "textures/items/copper_ingot.png",
            "textures/items/raw_iron.png",
            "textures/items/raw_gold.png",
            "textures/items/raw_copper.png",
            "textures/blocks/moss_block.png",
            "textures/blocks/rooted_dirt.png",
            "textures/blocks/hanging_roots.png",
            "textures/blocks/azalea_plant.png",
            "textures/blocks/azalea_side.png",
            "textures/blocks/azalea_top.png",
            "textures/blocks/azalea_leaves.png",
            "textures/blocks/flowering_azalea_side.png",
            "textures/blocks/flowering_azalea_top.png",
            "textures/blocks/flowering_azalea_leaves.png",
            "textures/blocks/cave_vines.png",
            "textures/blocks/cave_vines_lit.png",
            "textures/blocks/cave_vines_plant.png",
            "textures/blocks/cave_vines_plant_lit.png",
            "textures/blocks/small_dripleaf_top.png",
            "textures/blocks/small_dripleaf_side.png",
            "textures/blocks/small_dripleaf_stem_top.png",
            "textures/blocks/small_dripleaf_stem_bottom.png",
            "textures/blocks/big_dripleaf_top.png",
            "textures/blocks/big_dripleaf_side.png",
            "textures/blocks/big_dripleaf_tip.png",
            "textures/blocks/big_dripleaf_stem.png",
            "textures/blocks/spore_blossom.png",
            "textures/blocks/spore_blossom_base.png",
            "textures/blocks/glow_lichen.png",
            "textures/items/glow_berries.png",
            "textures/items/glow_lichen.png",
            "textures/particle/drip_fall.png",
            "textures/blocks/dripstone_block.png",
            "textures/blocks/pointed_dripstone_down_base.png",
            "textures/blocks/pointed_dripstone_down_frustum.png",
            "textures/blocks/pointed_dripstone_down_middle.png",
            "textures/blocks/pointed_dripstone_down_tip.png",
            "textures/blocks/pointed_dripstone_down_tip_merge.png",
            "textures/blocks/pointed_dripstone_up_base.png",
            "textures/blocks/pointed_dripstone_up_frustum.png",
            "textures/blocks/pointed_dripstone_up_middle.png",
            "textures/blocks/pointed_dripstone_up_tip.png",
            "textures/blocks/pointed_dripstone_up_tip_merge.png",
            "textures/items/pointed_dripstone.png"
    )));

    private ModernAssetResourcePack() {
        super(Collections.emptyMap());
    }

    @Override
    public Set<String> getResourceDomains() {
        return Collections.singleton("minecraft");
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (!"minecraft".equals(location.getResourceDomain()) || !ALIASED_PATHS.contains(location.getResourcePath())) {
            return false;
        }
        try {
            Minecraft.getMinecraft().getResourceManager().getResource(toModernLocation(location));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        if (!"minecraft".equals(location.getResourceDomain()) || !ALIASED_PATHS.contains(location.getResourcePath())) {
            throw new FileNotFoundException(location.toString());
        }
        IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(toModernLocation(location));
        return resource.getInputStream();
    }

    private static ResourceLocation toModernLocation(ResourceLocation legacy) {
        // Glow lichen has no standalone modern item texture; the modern item model reuses the
        // block texture. Legacy 1.7 item registration still asks for textures/items/glow_lichen,
        // so bridge that one legacy request to the official modern block asset.
        if ("textures/items/glow_lichen.png".equals(legacy.getResourcePath())) {
            return new ResourceLocation(Tags.MC_ASSET_VER, "textures/blocks/glow_lichen.png");
        }
        return new ResourceLocation(Tags.MC_ASSET_VER, legacy.getResourcePath());
    }

    @Override
    public String getPackName() {
        return "Et Futurum Requiem AssetDirector modern-asset bridge";
    }

    @SuppressWarnings("unchecked")
    public static void inject() {
        ModernAssetResourcePack pack = new ModernAssetResourcePack();
        Minecraft minecraft = Minecraft.getMinecraft();
        List<IResourcePack> defaultPacks = ReflectionHelper.getPrivateValue(
                Minecraft.class, minecraft, "defaultResourcePacks", "field_110449_ao");
        defaultPacks.add(pack);
        IResourceManager manager = minecraft.getResourceManager();
        if (manager instanceof SimpleReloadableResourceManager) {
            ((SimpleReloadableResourceManager) manager).reloadResourcePack(pack);
        }
    }

    /** Exposed for validation/auditing without leaking a mutable collection. */
    public static Set<String> getAliasedPaths() {
        return ALIASED_PATHS;
    }
}
