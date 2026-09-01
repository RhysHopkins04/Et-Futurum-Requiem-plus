package ganymedes01.etfuturum.client;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
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
            "textures/items/glow_ink_sac.png",
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

    private static final Map<String, String> MODERN_MAP_PARITY_ALIASES = ModernMapParityBlocks.getAssetAliases();
    private static final Map<String, String> DYNAMIC_MODEL_ALIASES = new ConcurrentHashMap<String, String>();

    static {
        // EFR historically bundled the pre-release crying-obsidian artwork. Keep the existing
        // registry block, but serve Mojang's current 1.21.11 texture/animation through AssetDirector.
        DYNAMIC_MODEL_ALIASES.put("textures/blocks/crying_obsidian.png", "textures/block/crying_obsidian.png");
        DYNAMIC_MODEL_ALIASES.put("textures/blocks/crying_obsidian.png.mcmeta", "textures/block/crying_obsidian.png.mcmeta");
    }

    private ModernAssetResourcePack() {
        super(Collections.emptyMap());
    }

    @Override
    public Set<String> getResourceDomains() {
        return Collections.singleton("minecraft");
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (!"minecraft".equals(location.getResourceDomain()) || !isAliasedPath(location.getResourcePath())) {
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
        if (!"minecraft".equals(location.getResourceDomain()) || !isAliasedPath(location.getResourcePath())) {
            throw new FileNotFoundException(location.toString());
        }
        ResourceLocation modern = toModernLocation(location);
        IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(modern);
        InputStream input = resource.getInputStream();
        // Every dynamically bridged modern model texture is decoded and rewritten as explicit ARGB
        // before the 1.7 TextureMap sees it. This preserves zero-alpha pixels used by block decals
        // such as Leaf Litter and Wildflowers. Rectangular entity sheets additionally remain square-
        // normalized because the legacy atlas rejects non-square, non-animated sprites.
        if (isDynamicModelPng(location.getResourcePath())) {
            return normalizeModelTexture(input, modern.getResourcePath().startsWith("textures/entity/"), modern.getResourcePath());
        }
        return input;
    }


    private static boolean isDynamicModelPng(String path) {
        return path != null && path.startsWith("textures/blocks/modern_model/") && path.endsWith(".png");
    }

    private static InputStream normalizeModelTexture(InputStream input, boolean forceSquare, String modernPath) throws IOException {
        BufferedImage source = ImageIO.read(input);
        if (source == null) throw new IOException("Unable to decode modern model texture");

        int width = forceSquare ? Math.max(source.getWidth(), source.getHeight()) : source.getWidth();
        int height = forceSquare ? width : source.getHeight();
        BufferedImage normalized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        if (forceSquare) {
            // Preserve the established entity-sheet compatibility behaviour. These sheets need to
            // be square for the 1.7 atlas and do not use the flowerbed transparency-key encoding.
            Graphics2D graphics = normalized.createGraphics();
            try {
                graphics.drawImage(source, 0, 0, width, height, null);
            } finally {
                graphics.dispose();
            }
        } else {
            /*
             * Copy decoded pixels explicitly instead of asking Graphics2D to convert the image's
             * colour model. Leaf Litter is an indexed/grayscale PNG whose transparent entry is
             * black; that colour-key representation can arrive at old atlas code as opaque black
             * on newer JVM/ImageIO combinations even though the modern texture is transparent.
             * Wildflowers use the same transparent-black convention in their bed/stem textures.
             *
             * These three vanilla textures contain no legitimate opaque #000000 texels, so repair
             * that exact colour key to ARGB 0 while preserving every authored non-black pixel and
             * its decoded alpha. This makes the result independent of the PNG source colour model.
             */
            int[] pixels = new int[source.getWidth() * source.getHeight()];
            source.getRGB(0, 0, source.getWidth(), source.getHeight(), pixels, 0, source.getWidth());
            if (usesGroundDecalBlackTransparencyKey(modernPath)) {
                for (int i = 0; i < pixels.length; i++) {
                    if ((pixels[i] & 0x00FFFFFF) == 0) pixels[i] = 0x00000000;
                }
            }
            normalized.setRGB(0, 0, source.getWidth(), source.getHeight(), pixels, 0, source.getWidth());
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(normalized, "png", bytes);
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    private static boolean usesGroundDecalBlackTransparencyKey(String modernPath) {
        return "textures/block/leaf_litter.png".equals(modernPath)
                || "textures/block/wildflowers.png".equals(modernPath)
                || "textures/block/wildflowers_stem.png".equals(modernPath);
    }

    private static boolean isAliasedPath(String path) {
        return ALIASED_PATHS.contains(path) || MODERN_MAP_PARITY_ALIASES.containsKey(path) || DYNAMIC_MODEL_ALIASES.containsKey(path);
    }

    private static ResourceLocation toModernLocation(ResourceLocation legacy) {
        // Cave Vines are mature EFR blocks that still request the legacy plural atlas path.
        // AssetDirector exposes the 1.21.11 client jar verbatim, where these live under block/.
        if (legacy.getResourcePath().startsWith("textures/blocks/cave_vines")) {
            return new ResourceLocation(Tags.MC_ASSET_VER,
                    legacy.getResourcePath().replace("textures/blocks/", "textures/block/"));
        }
        // Glow lichen has no standalone modern item texture; the modern item model reuses the
        // block texture. Legacy 1.7 item registration still asks for textures/items/glow_lichen,
        // so bridge that one legacy request to the official modern block asset.
        if ("textures/items/glow_lichen.png".equals(legacy.getResourcePath())) {
            return new ResourceLocation(Tags.MC_ASSET_VER, "textures/blocks/glow_lichen.png");
        }
        if ("textures/items/glow_ink_sac.png".equals(legacy.getResourcePath())) {
            return new ResourceLocation(Tags.MC_ASSET_VER, "textures/item/glow_ink_sac.png");
        }
        String dynamicTarget = DYNAMIC_MODEL_ALIASES.get(legacy.getResourcePath());
        if (dynamicTarget != null) {
            return new ResourceLocation(Tags.MC_ASSET_VER, dynamicTarget);
        }
        String parityTarget = MODERN_MAP_PARITY_ALIASES.get(legacy.getResourcePath());
        if (parityTarget != null) {
            return new ResourceLocation(Tags.MC_ASSET_VER, parityTarget);
        }
        return new ResourceLocation(Tags.MC_ASSET_VER, legacy.getResourcePath());
    }

    /**
     * Registers a synthetic legacy-atlas path that resolves to an exact resource from the
     * AssetDirector-provided modern client jar. Used by the runtime JSON model bridge after it
     * discovers the real texture dependencies of a modern model.
     */
    public static void registerDynamicAlias(String legacyPath, String modernPath) {
        if (legacyPath != null && modernPath != null) {
            DYNAMIC_MODEL_ALIASES.put(legacyPath, modernPath);
        }
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
        HashSet<String> paths = new HashSet<>(ALIASED_PATHS);
        paths.addAll(MODERN_MAP_PARITY_ALIASES.keySet());
        paths.addAll(DYNAMIC_MODEL_ALIASES.keySet());
        return Collections.unmodifiableSet(paths);
    }
}
