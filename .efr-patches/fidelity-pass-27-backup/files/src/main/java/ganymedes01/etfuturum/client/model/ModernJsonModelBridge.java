package ganymedes01.etfuturum.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.client.ModernAssetResourcePack;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Client-only bridge that interprets the real vanilla 1.21.11 blockstate/model/item JSON supplied
 * by AssetDirector and converts it into a tiny renderer-neutral quad model that 1.7.10 can draw.
 * No Mojang model or texture file is redistributed by EFR.
 */
@SideOnly(Side.CLIENT)
public final class ModernJsonModelBridge {
    private static final JsonParser PARSER = new JsonParser();
    private static final Map<String, PreparedModels> CACHE = new HashMap<String, PreparedModels>();

    private ModernJsonModelBridge() {}

    public static synchronized PreparedModels prepare(ModernMapParityBlocks entry, IIconRegister register) {
        String name = entry.getRegistryName();
        PreparedModels prepared = CACHE.get(name);

        if (prepared == null) {
            PreparedModels candidate = new PreparedModels();
            boolean blockSourceReady = false;
            boolean itemSourceReady = false;

            try {
                candidate.block = loadBlockStateModel(entry);
                candidate.block = applySpecialBlockVisual(entry, candidate.block);
                prepareDynamicBlockModels(entry, candidate);
                blockSourceReady = true;
            } catch (Throwable ignored) {
                candidate.block = applySpecialBlockVisual(entry, Model.empty());
            }
            try {
                candidate.item = loadItemModel(entry);
                candidate.item = applySpecialItemVisual(entry, candidate.item);
                itemSourceReady = true;
            } catch (Throwable ignored) {
                candidate.item = Model.empty();
            }

            if (candidate.item.isEmpty() && !candidate.block.isEmpty()) candidate.item = candidate.block.copyForItem();

            /*
             * Do not permanently cache a preparation attempted before AssetDirector's modern
             * resource namespace is available. The first 1.7 texture stitch can happen before
             * the modern client-jar resource pack is injected; the later full resource reload
             * must therefore be allowed to resolve the real 1.21.11 JSON graph again.
             */
            if (blockSourceReady || itemSourceReady) CACHE.put(name, candidate);
            prepared = candidate;
        }

        /*
         * IIcon instances contain coordinates for one particular TextureMap stitch. Minecraft
         * rebuilds the atlas when AssetDirector/ModernAssetResourcePack is added, so cached icons
         * from the bootstrap stitch would point into unrelated sprites in the final atlas. Keep
         * the parsed model graph cached, but re-register/rebind every quad and generated-item icon
         * on every registerBlockIcons pass.
         */
        registerTextures(prepared.block, register);
        registerTextures(prepared.item, register);
        for (Model model : prepared.connectionModels) if (model != null) registerTextures(model, register);
        for (Model model : prepared.facingModels) if (model != null) registerTextures(model, register);
        for (Model model : prepared.chiseledBookshelfBase) if (model != null) registerTextures(model, register);
        for (Model model : prepared.chiseledBookshelfSlots) if (model != null) registerTextures(model, register);
        return prepared;
    }

    /**
     * Returns an atlas-safe icon already registered from the resolved modern model graph.
     * This is only a 1.7 fallback/particle icon; the JSON bridge remains authoritative
     * for world and item rendering.  Prefer a real model texture over a guessed vanilla
     * placeholder so custom-rendered parity blocks never publish stone/missingno in the atlas.
     */
    public static IIcon getFallbackIcon(PreparedModels models) {
        if (models == null) return null;
        IIcon icon = getFallbackIcon(models.block);
        if (icon != null) return icon;
        return getFallbackIcon(models.item);
    }

    private static IIcon getFallbackIcon(Model model) {
        if (model == null) return null;
        if (model.flatIcon != null) return model.flatIcon;
        for (Quad quad : model.quads) {
            if (quad != null && quad.icon != null) return quad.icon;
        }
        return null;
    }

    public static synchronized PreparedModels get(ModernMapParityBlocks entry) {
        PreparedModels models = CACHE.get(entry.getRegistryName());
        return models == null ? PreparedModels.EMPTY : models;
    }

    public static synchronized Model getWorldModel(ModernMapParityBlocks entry, IBlockAccess world, int x, int y, int z) {
        PreparedModels models = CACHE.get(entry.getRegistryName());
        if (models == null) return Model.empty();
        if ("grindstone".equals(entry.getRegistryName())) {
            int state = world.getBlockMetadata(x, y, z) & 15;
            if (state < 0 || state >= 12) state = 0;
            Model oriented = models.facingModels[state];
            if (oriented != null) return oriented;
        }
        if (entry.getRegistryName().endsWith("lightning_rod")) {
            int facing = world.getBlockMetadata(x, y, z) & 7;
            if (facing < 0 || facing >= 6) facing = 1;
            Model oriented = models.facingModels[facing];
            if (oriented != null) return oriented;
        }
        ModernMapParityBlocks.Style style = entry.getStyle();
        String name = entry.getRegistryName();
        if (name.endsWith("_froglight") || name.endsWith("copper_chain")) {
            int axis = world.getBlockMetadata(x, y, z) & 3;
            if (axis > 2) axis = 0;
            Model oriented = models.facingModels[axis];
            if (oriented != null) return oriented;
        }
        if (name.endsWith("copper_lantern")) {
            int hanging = world.getBlockMetadata(x, y, z) & 1;
            Model lantern = models.facingModels[hanging];
            if (lantern != null) return lantern;
        }
        if ("chiseled_bookshelf".equals(name)) {
            int facing = world.getBlockMetadata(x, y, z) & 3;
            int occupied = 0;
            net.minecraft.tileentity.TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof ModernMapParityBlocks.ParityChiseledBookshelfTileEntity) {
                occupied = ((ModernMapParityBlocks.ParityChiseledBookshelfTileEntity) tile).getOccupancyMask();
            }
            return chiseledBookshelfModel(models, facing, occupied);
        }
        if (style == ModernMapParityBlocks.Style.SHELF) {
            int facing = world.getBlockMetadata(x, y, z) & 3;
            int state = shelfState(entry, world, x, y, z);
            Model oriented = models.facingModels[state * 4 + facing];
            if (oriented != null) return oriented;
        }
        if (style == ModernMapParityBlocks.Style.CANDLE) {
            int state = world.getBlockMetadata(x, y, z) & 7;
            Model candle = models.facingModels[state];
            if (candle != null) return candle;
        }
        if (style == ModernMapParityBlocks.Style.CANDLE_CAKE) {
            int state = world.getBlockMetadata(x, y, z) & 1;
            Model candleCake = models.facingModels[state];
            if (candleCake != null) return candleCake;
        }
        if ("turtle_egg".equals(name)) {
            int state = world.getBlockMetadata(x, y, z) & 15;
            if (state >= 12) state = state & 3;
            Model eggs = models.facingModels[state];
            if (eggs != null) return eggs;
        }
        if ("campfire".equals(name) || "soul_campfire".equals(name)) {
            int state = world.getBlockMetadata(x, y, z) & 7;
            Model campfire = models.facingModels[state];
            if (campfire != null) return campfire;
        }
        if ("scaffolding".equals(name)) {
            int state = (world.getBlockMetadata(x, y, z) & 8) != 0 ? 1 : 0;
            Model scaffold = models.facingModels[state];
            if (scaffold != null) return scaffold;
        }
        if ("leaf_litter".equals(name) || "wildflowers".equals(name)) {
            int state = world.getBlockMetadata(x, y, z) & 15;
            Model segmented = models.facingModels[state];
            if (segmented != null) return segmented;
        }
        if (style == ModernMapParityBlocks.Style.SIGN) {
            int state = world.getBlockMetadata(x, y, z) & 15;
            Model oriented = models.facingModels[state];
            if (oriented != null) return oriented;
        }
        if (style == ModernMapParityBlocks.Style.HANGING_SIGN) {
            int state = world.getBlockMetadata(x, y, z) & 15;
            Model oriented = models.facingModels[state];
            if (oriented != null) return oriented;
        }
        if (style == ModernMapParityBlocks.Style.FENCE_GATE) {
            int state = world.getBlockMetadata(x, y, z) & 7;
            Model oriented = models.facingModels[state];
            if (oriented != null) return oriented;
        }
        if (style == ModernMapParityBlocks.Style.WALL_PLANT && name.endsWith("_coral_wall_fan")) {
            int facing = world.getBlockMetadata(x, y, z) & 7;
            if (facing < 2 || facing > 5) facing = 2;
            Model oriented = models.facingModels[facing];
            if (oriented != null) return oriented;
        }
        if (style == ModernMapParityBlocks.Style.PANE || style == ModernMapParityBlocks.Style.FENCE
                || style == ModernMapParityBlocks.Style.WALL) {
            int mask = connectionMask(entry, world, x, y, z);
            Model connected = models.connectionModels[mask];
            if (connected != null) return connected;
        }
        return models.block;
    }

    private static int connectionMask(ModernMapParityBlocks entry, IBlockAccess world, int x, int y, int z) {
        int mask = 0;
        if (connects(entry, world, x, y, z - 1)) mask |= 1;
        if (connects(entry, world, x + 1, y, z)) mask |= 2;
        if (connects(entry, world, x, y, z + 1)) mask |= 4;
        if (connects(entry, world, x - 1, y, z)) mask |= 8;
        return mask;
    }

    private static boolean connects(ModernMapParityBlocks entry, IBlockAccess world, int x, int y, int z) {
        Block other = world.getBlock(x, y, z);
        if (other == null) return false;
        ModernMapParityBlocks otherEntry = ModernMapParityBlocks.fromBlock(other);
        if (otherEntry != null) {
            if (otherEntry.getStyle() == entry.getStyle()) return true;
            if (entry.getStyle() == ModernMapParityBlocks.Style.FENCE
                    && otherEntry.getStyle() == ModernMapParityBlocks.Style.FENCE_GATE) return true;
        }
        return other.isOpaqueCube();
    }

    /** 0=unpowered, 1=powered/unconnected, 2=left, 3=center, 4=right. */
    private static int shelfState(ModernMapParityBlocks entry, IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z) & 7;
        if ((meta & 4) == 0) return 0;
        int facing = meta & 3;
        int leftX = x, leftZ = z, rightX = x, rightZ = z;
        switch (facing) {
            case 1: leftZ++; rightZ--; break;
            case 2: leftX--; rightX++; break;
            case 3: leftZ--; rightZ++; break;
            default: leftX++; rightX--; break;
        }
        boolean left = connectedPoweredShelf(entry, world, leftX, y, leftZ, facing);
        boolean right = connectedPoweredShelf(entry, world, rightX, y, rightZ, facing);
        if (left && right) return 3;
        if (right) return 2;
        if (left) return 4;
        return 1;
    }

    private static boolean connectedPoweredShelf(ModernMapParityBlocks entry, IBlockAccess world,
            int x, int y, int z, int facing) {
        if (world.getBlock(x, y, z) != entry.get()) return false;
        int meta = world.getBlockMetadata(x, y, z) & 7;
        return (meta & 3) == facing && (meta & 4) != 0;
    }

    private static Model chiseledBookshelfModel(PreparedModels models, int facing, int occupiedMask) {
        Model out = new Model();
        Model base = models.chiseledBookshelfBase[facing & 3];
        if (base != null) {
            out.quads.addAll(base.quads);
            out.display.putAll(base.display);
            out.particleTexture = base.particleTexture;
        }
        for (int slot = 0; slot < 6; slot++) {
            int occupied = (occupiedMask >> slot) & 1;
            Model part = models.chiseledBookshelfSlots[(facing & 3) * 12 + slot * 2 + occupied];
            if (part != null) out.quads.addAll(part.quads);
        }
        return out;
    }

    private static void registerTextures(Model model, IIconRegister register) {
        for (Quad quad : model.quads) {
            quad.icon = registerTexture(quad.texture, register);
        }
        if (model.flatTexture != null) model.flatIcon = registerTexture(model.flatTexture, register);
    }

    private static IIcon registerTexture(String texture, IIconRegister register) {
        String normalized = normalizeTexture(texture);
        String safe = normalized.replace(':', '_').replace('/', '_').replace('.', '_') + "_" + Integer.toHexString(normalized.hashCode());
        String synthetic = "modern_model/" + safe;
        String target = texturePath(normalized);
        ModernAssetResourcePack.registerDynamicAlias("textures/blocks/" + synthetic + ".png", target);
        ModernAssetResourcePack.registerDynamicAlias("textures/blocks/" + synthetic + ".png.mcmeta", target + ".mcmeta");
        return register.registerIcon("minecraft:" + synthetic);
    }

    private static String texturePath(String texture) {
        String path = texture;
        int colon = path.indexOf(':');
        if (colon >= 0) path = path.substring(colon + 1);
        if (path.startsWith("textures/")) return path.endsWith(".png") ? path : path + ".png";
        return "textures/" + path + ".png";
    }

    private static String normalizeTexture(String texture) {
        if (texture == null || texture.isEmpty()) return "minecraft:block/stone";
        String value = texture;
        int colon = value.indexOf(':');
        String namespace = colon >= 0 ? value.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? value.substring(colon + 1) : value;
        // Some current Mojang models use bare texture names (for example heavy_core's "all") and
        // older/compat model paths may omit the modern block/ folder. Bare texture paths are block
        // textures unless an explicit item/entity folder was supplied.
        if (path.indexOf('/') < 0) path = "block/" + path;
        return namespace + ":" + path;
    }

    private static Model loadBlockStateModel(ModernMapParityBlocks entry) throws IOException {
        return loadBlockStateModel(entry, defaultsFor(entry));
    }

    private static Model loadBlockStateModel(ModernMapParityBlocks entry, Map<String, String> defaults) throws IOException {
        JsonObject root = readJson("blockstates/" + entry.getRegistryName() + ".json");
        ArrayList<ModelRef> refs = new ArrayList<ModelRef>();

        if (root.has("variants")) {
            JsonObject variants = root.getAsJsonObject("variants");
            Map.Entry<String, JsonElement> best = null;
            int bestScore = Integer.MIN_VALUE;
            for (Map.Entry<String, JsonElement> candidate : variants.entrySet()) {
                int score = scoreVariant(candidate.getKey(), defaults);
                if (score > bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
            if (best != null) addApply(refs, best.getValue());
        }

        if (root.has("multipart")) {
            JsonArray multipart = root.getAsJsonArray("multipart");
            for (JsonElement element : multipart) {
                JsonObject part = element.getAsJsonObject();
                if (!part.has("when") || matchesWhen(part.get("when"), defaults)) addApply(refs, part.get("apply"));
            }
            if (refs.isEmpty() && multipart.size() > 0) addApply(refs, multipart.get(0).getAsJsonObject().get("apply"));
        }

        if (refs.isEmpty()) refs.add(new ModelRef("minecraft:block/" + entry.getRegistryName(), 0, 0, false));
        Model model = combine(refs);
        correctSegmentedGroundDecalTopUv(entry, model);
        correctCoralFanFaceBakeryUv(entry, model);
        return model;
    }

    /**
     * The legacy bridge's generic explicit-UV convention predates Minecraft's modern FaceBakery
     * orientation. For horizontal UP faces it maps both X/U and Z/V backwards, which is a 180-degree
     * texture rotation. That is normally hidden on symmetric cube textures, but it separates the
     * Wildflowers flowerbed pixels from the stem geometry and rotates Leaf Litter's authored decal.
     *
     * Keep the compatibility correction deliberately bounded to the two segmented ground-decal
     * families so already-approved parity models are not reinterpreted globally.
     */
    private static void correctSegmentedGroundDecalTopUv(ModernMapParityBlocks entry, Model model) {
        if (entry == null || model == null) return;
        String name = entry.getRegistryName();
        if (!"leaf_litter".equals(name) && !"wildflowers".equals(name)) return;

        for (Quad quad : model.quads) {
            if (!isUpFacingHorizontalQuad(quad)) continue;
            double[] uv0 = quad.uv[0];
            double[] uv1 = quad.uv[1];
            quad.uv[0] = quad.uv[2];
            quad.uv[1] = quad.uv[3];
            quad.uv[2] = uv0;
            quad.uv[3] = uv1;
        }
    }

    private static boolean isUpFacingHorizontalQuad(Quad quad) {
        if (quad == null || quad.vertices == null || quad.vertices.length != 4) return false;
        Vertex a = quad.vertices[0];
        Vertex b = quad.vertices[1];
        Vertex c = quad.vertices[2];
        double abx = b.x - a.x, aby = b.y - a.y, abz = b.z - a.z;
        double acx = c.x - a.x, acy = c.y - a.y, acz = c.z - a.z;
        double ny = abz * acx - abx * acz;
        return Math.abs(aby) < 1.0E-8D && Math.abs(acy) < 1.0E-8D && ny > 1.0E-10D;
    }

    /**
     * Minecraft's FaceBakery orders UP-face vertices NW, SW, SE, NE. The legacy bridge's
     * makeFace(UP) order is SE, NE, NW, SW, which is the same winding with a two-vertex offset.
     * On symmetric textures this is invisible, but coral-fan textures are directional: their
     * authored face rotations expect the stem/base pixels to point toward the fan centre.
     *
     * Correct only the upward-authored coral-fan faces by rotating their UV assignment 180 degrees.
     * Geometry is left exactly as Mojang authored it. DOWN faces already use the FaceBakery order.
     */
    private static void correctCoralFanFaceBakeryUv(ModernMapParityBlocks entry, Model model) {
        if (entry == null || model == null) return;
        String name = entry.getRegistryName();
        if (!name.endsWith("_coral_fan") && !name.endsWith("_coral_wall_fan")) return;

        for (Quad quad : model.quads) {
            if (!hasPositiveYNormal(quad)) continue;
            double[] uv0 = quad.uv[0];
            double[] uv1 = quad.uv[1];
            quad.uv[0] = quad.uv[2];
            quad.uv[1] = quad.uv[3];
            quad.uv[2] = uv0;
            quad.uv[3] = uv1;
        }
    }

    private static boolean hasPositiveYNormal(Quad quad) {
        if (quad == null || quad.vertices == null || quad.vertices.length != 4) return false;
        Vertex a = quad.vertices[0];
        Vertex b = quad.vertices[1];
        Vertex c = quad.vertices[2];
        double abx = b.x - a.x, aby = b.y - a.y, abz = b.z - a.z;
        double acx = c.x - a.x, acy = c.y - a.y, acz = c.z - a.z;
        double ny = abz * acx - abx * acz;
        return ny > 1.0E-10D;
    }

    private static Model loadItemModel(ModernMapParityBlocks entry) throws IOException {
        String name = canonicalItemName(entry.getRegistryName());
        ItemDefinition definition = null;
        try {
            JsonObject itemRoot = readJson("items/" + name + ".json");
            if (itemRoot.has("model")) definition = resolveItemDefinition(itemRoot.get("model"));
        } catch (IOException ignored) {
            // Older-style fallback below.
        }

        if (definition != null) {
            Model base = loadModel(definition.modelPath, 0, 0, false, new HashMap<String, RawModel>());
            base.specialType = definition.specialType;
            base.specialTexture = definition.specialTexture;
            return base;
        }
        return loadModel("minecraft:item/" + name, 0, 0, false, new HashMap<String, RawModel>());
    }


    private static String canonicalItemName(String name) {
        // These registry identities are placement/state-only blocks in modern Minecraft. When they
        // are exposed as legacy ItemBlocks for map-editing convenience, use the actual obtainable
        // modern item's model instead of rendering the block-only state as a fake item.
        if (name.endsWith("_wall_hanging_sign")) return name.substring(0, name.length() - "_wall_hanging_sign".length()) + "_hanging_sign";
        if (name.endsWith("_wall_sign")) return name.substring(0, name.length() - "_wall_sign".length()) + "_sign";
        if (name.endsWith("_coral_wall_fan")) return name.substring(0, name.length() - "_wall_fan".length()) + "_fan";
        if ("kelp_plant".equals(name)) return "kelp";
        if ("tall_seagrass".equals(name)) return "seagrass";
        if ("torchflower_crop".equals(name)) return "torchflower";
        if ("pitcher_crop".equals(name)) return "pitcher_plant";
        return name;
    }

    private static ItemDefinition resolveItemDefinition(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();
        String type = string(obj, "type", "minecraft:model");
        if ("minecraft:model".equals(type)) return new ItemDefinition(string(obj, "model", null), null, null);
        if ("minecraft:special".equals(type)) {
            String base = string(obj, "base", null);
            String special = null;
            String texture = null;
            if (obj.has("model") && obj.get("model").isJsonObject()) {
                JsonObject spec = obj.getAsJsonObject("model");
                special = string(spec, "type", null);
                texture = string(spec, "texture", null);
            }
            return new ItemDefinition(base, special, texture);
        }
        if ("minecraft:composite".equals(type) && obj.has("models")) {
            JsonArray models = obj.getAsJsonArray("models");
            return models.size() == 0 ? null : resolveItemDefinition(models.get(0));
        }
        if ("minecraft:condition".equals(type)) {
            if (obj.has("on_false")) return resolveItemDefinition(obj.get("on_false"));
            if (obj.has("on_true")) return resolveItemDefinition(obj.get("on_true"));
        }
        if ("minecraft:select".equals(type) || "minecraft:range_dispatch".equals(type)) {
            if (obj.has("fallback")) return resolveItemDefinition(obj.get("fallback"));
            if (obj.has("cases")) {
                JsonArray cases = obj.getAsJsonArray("cases");
                if (cases.size() > 0) {
                    JsonObject c = cases.get(0).getAsJsonObject();
                    if (c.has("model")) return resolveItemDefinition(c.get("model"));
                }
            }
            if (obj.has("entries")) {
                JsonArray entries = obj.getAsJsonArray("entries");
                if (entries.size() > 0) {
                    JsonObject c = entries.get(0).getAsJsonObject();
                    if (c.has("model")) return resolveItemDefinition(c.get("model"));
                }
            }
        }
        return null;
    }

    private static Model combine(List<ModelRef> refs) throws IOException {
        Model out = Model.empty();
        Map<String, RawModel> memo = new HashMap<String, RawModel>();
        for (ModelRef ref : refs) {
            Model model = loadModel(ref.path, ref.x, ref.y, ref.uvlock, memo);
            out.quads.addAll(model.quads);
            if (out.flatTexture == null) out.flatTexture = model.flatTexture;
            out.display.putAll(model.display);
        }
        return out;
    }

    private static Model loadModel(String path, int stateX, int stateY, boolean uvlock, Map<String, RawModel> memo) throws IOException {
        if (path == null) return Model.empty();
        RawModel raw = resolveRawModel(normalizeModel(path), memo);
        Model model = new Model();
        model.display.putAll(raw.display);
        model.particleTexture = resolveTexture(raw.textures, raw.textures.get("particle"));

        if (raw.generated) {
            model.flatTexture = resolveTexture(raw.textures, raw.textures.get("layer0"));
            return model;
        }

        if (raw.elements != null) {
            for (JsonElement elementValue : raw.elements) {
                JsonObject element = elementValue.getAsJsonObject();
                double[] from = vec(element, "from", new double[]{0, 0, 0});
                double[] to = vec(element, "to", new double[]{16, 16, 16});
                ElementRotation rotation = parseElementRotation(element);
                if (!element.has("faces")) continue;
                JsonObject faces = element.getAsJsonObject("faces");
                for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                    Direction direction = Direction.byName(faceEntry.getKey());
                    if (direction == null) continue;
                    JsonObject face = faceEntry.getValue().getAsJsonObject();
                    String texture = resolveTexture(raw.textures, string(face, "texture", null));
                    if (texture == null) continue;
                    double[] uv = face.has("uv") ? vec4(face.getAsJsonArray("uv")) : defaultUv(direction, from, to);
                    int faceRotation = integer(face, "rotation", 0);
                    int tintIndex = integer(face, "tintindex", -1);
                    Quad quad = makeFace(direction, from, to, uv, faceRotation, texture, tintIndex);
                    if (rotation != null) rotateElement(quad, rotation);
                    rotateState(quad, stateX, stateY);
                    model.quads.add(quad);
                }
            }
        }
        return model;
    }

    private static RawModel resolveRawModel(String path, Map<String, RawModel> memo) throws IOException {
        RawModel cached = memo.get(path);
        if (cached != null) return cached.copy();

        JsonObject json = readJson(modelPath(path));
        RawModel result = new RawModel();
        if (json.has("parent")) {
            String parentPath = normalizeModel(json.get("parent").getAsString());
            if ("minecraft:builtin/generated".equals(parentPath)) {
                result.generated = true;
            } else if (!parentPath.startsWith("minecraft:builtin/")) {
                result = resolveRawModel(parentPath, memo);
            }
        }

        if (json.has("textures")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("textures").entrySet()) result.textures.put(e.getKey(), e.getValue().getAsString());
        }
        if (json.has("elements")) result.elements = json.getAsJsonArray("elements");
        if (json.has("display")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("display").entrySet()) result.display.put(e.getKey(), parseTransform(e.getValue().getAsJsonObject()));
        }
        memo.put(path, result.copy());
        return result;
    }

    private static String modelPath(String model) {
        String path = model;
        int colon = path.indexOf(':');
        if (colon >= 0) path = path.substring(colon + 1);
        return "models/" + path + ".json";
    }

    private static String normalizeModel(String model) {
        if (model.indexOf(':') < 0) return "minecraft:" + model;
        return model;
    }

    private static JsonObject readJson(String path) throws IOException {
        ResourceLocation location = new ResourceLocation(Tags.MC_ASSET_VER, path);
        IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
        Reader reader = new InputStreamReader(resource.getInputStream(), "UTF-8");
        try {
            return PARSER.parse(reader).getAsJsonObject();
        } finally {
            reader.close();
        }
    }

    private static void addApply(List<ModelRef> refs, JsonElement apply) {
        if (apply == null) return;
        if (apply.isJsonArray()) {
            JsonArray values = apply.getAsJsonArray();
            if (values.size() > 0) addApply(refs, values.get(0));
            return;
        }
        JsonObject obj = apply.getAsJsonObject();
        refs.add(new ModelRef(string(obj, "model", "minecraft:block/stone"), integer(obj, "x", 0), integer(obj, "y", 0), bool(obj, "uvlock", false)));
    }

    private static int scoreVariant(String key, Map<String, String> defaults) {
        if (key == null || key.isEmpty()) return 10000;
        int score = 0;
        String[] terms = key.split(",");
        for (String term : terms) {
            int eq = term.indexOf('=');
            if (eq < 0) continue;
            String k = term.substring(0, eq);
            String v = term.substring(eq + 1);
            String wanted = defaults.get(k);
            if (wanted == null) score += 1;
            else if (wanted.equals(v)) score += 20;
            else score -= 100;
        }
        return score - terms.length;
    }

    private static boolean matchesWhen(JsonElement when, Map<String, String> defaults) {
        if (when == null || !when.isJsonObject()) return true;
        JsonObject obj = when.getAsJsonObject();
        if (obj.has("OR")) {
            JsonArray or = obj.getAsJsonArray("OR");
            for (JsonElement child : or) if (matchesWhen(child, defaults)) return true;
            return false;
        }
        if (obj.has("AND")) {
            JsonArray and = obj.getAsJsonArray("AND");
            for (JsonElement child : and) if (!matchesWhen(child, defaults)) return false;
            return true;
        }
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            String actual = defaults.get(e.getKey());
            if (actual == null) return false;
            String expected = e.getValue().getAsString();
            boolean okay = false;
            for (String choice : expected.split("\\|")) if (actual.equals(choice)) okay = true;
            if (!okay) return false;
        }
        return true;
    }

    private static Map<String, String> defaultsFor(ModernMapParityBlocks entry) {
        LinkedHashMap<String, String> p = new LinkedHashMap<String, String>();
        p.put("waterlogged", "sea_pickle".equals(entry.getRegistryName()) ? "true" : "false");
        p.put("lit", "false");
        p.put("powered", "false");
        p.put("open", "false");
        p.put("in_wall", "false");
        p.put("facing", entry.getRegistryName().endsWith("lightning_rod") ? "up" : "north");
        p.put("axis", "y");
        p.put("half", "bottom");
        p.put("hinge", "left");
        p.put("shape", "straight");
        p.put("type", "bottom");
        p.put("rotation", "0");
        p.put("candles", "1");
        p.put("pickles", "1");
        p.put("eggs", "1");
        p.put("hatch", "0");
        p.put("layers", "1");
        p.put("flower_amount", "1");
        p.put("segment_amount", "1");
        p.put("face", "wall");
        p.put("attachment", "floor");
        p.put("powered", "false");
        p.put("side_chain", "unconnected");
        for (int slot = 0; slot < 6; slot++) p.put("slot_" + slot + "_occupied", "false");
        p.put("north", entry.getStyle() == ModernMapParityBlocks.Style.WALL ? "none" : "false");
        p.put("east", entry.getStyle() == ModernMapParityBlocks.Style.WALL ? "none" : "false");
        p.put("south", entry.getStyle() == ModernMapParityBlocks.Style.WALL ? "none" : "false");
        p.put("west", entry.getStyle() == ModernMapParityBlocks.Style.WALL ? "none" : "false");
        p.put("up", "true");
        p.put("bottom", "false");
        return p;
    }

    private static void prepareDynamicBlockModels(ModernMapParityBlocks entry, PreparedModels prepared) throws IOException {
        ModernMapParityBlocks.Style style = entry.getStyle();
        String registryName = entry.getRegistryName();
        if (registryName.endsWith("_froglight") || registryName.endsWith("copper_chain")) {
            String[] axes = {"y", "x", "z"};
            for (int axis = 0; axis < axes.length; axis++) {
                Map<String, String> state = defaultsFor(entry);
                state.put("axis", axes[axis]);
                prepared.facingModels[axis] = loadBlockStateModel(entry, state);
            }
        }
        if (registryName.endsWith("copper_lantern")) {
            for (int hanging = 0; hanging <= 1; hanging++) {
                Map<String, String> state = defaultsFor(entry);
                state.put("hanging", Boolean.toString(hanging != 0));
                prepared.facingModels[hanging] = loadBlockStateModel(entry, state);
            }
        }
        if (style == ModernMapParityBlocks.Style.PANE || style == ModernMapParityBlocks.Style.FENCE
                || style == ModernMapParityBlocks.Style.WALL) {
            for (int mask = 0; mask < 16; mask++) {
                Map<String, String> state = defaultsFor(entry);
                boolean n = (mask & 1) != 0;
                boolean e = (mask & 2) != 0;
                boolean s = (mask & 4) != 0;
                boolean w = (mask & 8) != 0;
                if (style == ModernMapParityBlocks.Style.WALL) {
                    state.put("north", n ? "low" : "none");
                    state.put("east", e ? "low" : "none");
                    state.put("south", s ? "low" : "none");
                    state.put("west", w ? "low" : "none");
                    state.put("up", (mask == 5 || mask == 10) ? "false" : "true");
                } else {
                    state.put("north", Boolean.toString(n));
                    state.put("east", Boolean.toString(e));
                    state.put("south", Boolean.toString(s));
                    state.put("west", Boolean.toString(w));
                }
                prepared.connectionModels[mask] = applySpecialBlockVisual(entry, loadBlockStateModel(entry, state));
            }
        }
        if (style == ModernMapParityBlocks.Style.WALL_PLANT && entry.getRegistryName().endsWith("_coral_wall_fan")) {
            // Technical wall-fan blocks are four-way states in modern Minecraft. 1.7 metadata
            // uses side ids 2=N, 3=S, 4=W, 5=E, so resolve those exact Mojang blockstate variants.
            String[] facings = {null, null, "north", "south", "west", "east"};
            for (int side = 2; side <= 5; side++) {
                Map<String, String> state = defaultsFor(entry);
                state.put("facing", facings[side]);
                prepared.facingModels[side] = applySpecialBlockVisual(entry, loadBlockStateModel(entry, state));
            }
        }
        if (style == ModernMapParityBlocks.Style.FENCE_GATE) {
            // 1.7 fence-gate metadata: 0=south, 1=west, 2=north, 3=east, bit 2=open.
            // Resolve every visual state from Mojang's real 1.21.11 blockstate JSON rather than
            // rotating one guessed model, so uvlock/model changes remain AssetDirector-authored.
            String[] facings = {"south", "west", "north", "east"};
            for (int direction = 0; direction < 4; direction++) {
                for (int open = 0; open < 2; open++) {
                    Map<String, String> state = defaultsFor(entry);
                    state.put("facing", facings[direction]);
                    state.put("open", open == 0 ? "false" : "true");
                    state.put("powered", "false");
                    state.put("in_wall", "false");
                    prepared.facingModels[direction | (open << 2)] = loadBlockStateModel(entry, state);
                }
            }
        }
        if ("grindstone".equals(entry.getRegistryName())) {
            String[] faces = {"floor", "wall", "ceiling"};
            String[] facings = {"north", "east", "south", "west"};
            for (int face = 0; face < faces.length; face++) {
                for (int facing = 0; facing < facings.length; facing++) {
                    Map<String, String> state = defaultsFor(entry);
                    state.put("face", faces[face]);
                    state.put("facing", facings[facing]);
                    prepared.facingModels[face * 4 + facing] = loadBlockStateModel(entry, state);
                }
            }
        }
        if (style == ModernMapParityBlocks.Style.SHELF) {
            String[] facings = {"north", "east", "south", "west"};
            String[] chains = {"unconnected", "unconnected", "left", "center", "right"};
            for (int shelfState = 0; shelfState < chains.length; shelfState++) {
                for (int facing = 0; facing < facings.length; facing++) {
                    Map<String, String> state = defaultsFor(entry);
                    state.put("facing", facings[facing]);
                    state.put("powered", Boolean.toString(shelfState != 0));
                    state.put("side_chain", chains[shelfState]);
                    prepared.facingModels[shelfState * 4 + facing] = loadBlockStateModel(entry, state);
                }
            }
        }
        if ("chiseled_bookshelf".equals(registryName)) {
            String[] slotNames = {"top_left", "top_mid", "top_right", "bottom_left", "bottom_mid", "bottom_right"};
            for (int facing = 0; facing < 4; facing++) {
                int rotation = facing * 90;
                prepared.chiseledBookshelfBase[facing] = loadModel("minecraft:block/chiseled_bookshelf",
                        0, rotation, true, new HashMap<String, RawModel>());
                for (int slot = 0; slot < slotNames.length; slot++) {
                    for (int occupied = 0; occupied <= 1; occupied++) {
                        String stateName = occupied == 0 ? "empty" : "occupied";
                        String modelName = "minecraft:block/chiseled_bookshelf_" + stateName + "_slot_" + slotNames[slot];
                        prepared.chiseledBookshelfSlots[facing * 12 + slot * 2 + occupied] = loadModel(modelName,
                                0, rotation, false, new HashMap<String, RawModel>());
                    }
                }
            }
        }
        if (style == ModernMapParityBlocks.Style.CANDLE) {
            for (int count = 1; count <= 4; count++) {
                for (int lit = 0; lit <= 1; lit++) {
                    Map<String, String> state = defaultsFor(entry);
                    state.put("candles", Integer.toString(count));
                    state.put("lit", Boolean.toString(lit != 0));
                    prepared.facingModels[(lit << 2) | (count - 1)] = loadBlockStateModel(entry, state);
                }
            }
        }
        if (style == ModernMapParityBlocks.Style.CANDLE_CAKE) {
            for (int lit = 0; lit <= 1; lit++) {
                Map<String, String> state = defaultsFor(entry);
                state.put("lit", Boolean.toString(lit != 0));
                prepared.facingModels[lit] = loadBlockStateModel(entry, state);
            }
        }
        if ("turtle_egg".equals(registryName)) {
            for (int hatch = 0; hatch <= 2; hatch++) {
                for (int eggs = 1; eggs <= 4; eggs++) {
                    Map<String, String> state = defaultsFor(entry);
                    state.put("eggs", Integer.toString(eggs));
                    state.put("hatch", Integer.toString(hatch));
                    prepared.facingModels[(hatch << 2) | (eggs - 1)] = loadBlockStateModel(entry, state);
                }
            }
        }
        if ("campfire".equals(registryName) || "soul_campfire".equals(registryName)) {
            String[] facings = {"north", "east", "south", "west"};
            for (int lit = 0; lit <= 1; lit++) {
                for (int facing = 0; facing < facings.length; facing++) {
                    Map<String, String> state = defaultsFor(entry);
                    state.put("facing", facings[facing]);
                    state.put("lit", Boolean.toString(lit != 0));
                    Model campfire = loadBlockStateModel(entry, state);
                    if (lit != 0) compactCampfireFlames(campfire);
                    prepared.facingModels[(lit << 2) | facing] = campfire;
                }
            }
        }
        if ("scaffolding".equals(registryName)) {
            for (int bottom = 0; bottom <= 1; bottom++) {
                Map<String, String> state = defaultsFor(entry);
                state.put("bottom", Boolean.toString(bottom != 0));
                prepared.facingModels[bottom] = loadBlockStateModel(entry, state);
            }
        }
        if ("leaf_litter".equals(registryName) || "wildflowers".equals(registryName)) {
            String[] facings = {"north", "east", "south", "west"};
            String amountProperty = "wildflowers".equals(registryName) ? "flower_amount" : "segment_amount";
            for (int amount = 1; amount <= 4; amount++) {
                for (int facing = 0; facing < facings.length; facing++) {
                    Map<String, String> state = defaultsFor(entry);
                    state.put("facing", facings[facing]);
                    state.put(amountProperty, Integer.toString(amount));
                    prepared.facingModels[(amount - 1) * 4 + facing] = loadBlockStateModel(entry, state);
                }
            }
        }
        if (style == ModernMapParityBlocks.Style.SIGN) {
            boolean wallIdentity = entry.getRegistryName().endsWith("_wall_sign");
            prepared.facingModels[0] = signModel(entry, wallIdentity);
            for (int side = 2; side <= 5; side++) {
                Model wall = signModel(entry, true);
                rotatePlacementModel(wall, side);
                prepared.facingModels[side] = wall;
            }
        }
        if (style == ModernMapParityBlocks.Style.HANGING_SIGN) {
            boolean wallIdentity = entry.getRegistryName().endsWith("_wall_hanging_sign");
            if (wallIdentity) {
                prepared.facingModels[0] = hangingSignModel(entry, 2);
                for (int side = 2; side <= 5; side++) {
                    Model wall = hangingSignModel(entry, 2);
                    rotatePlacementModel(wall, side);
                    prepared.facingModels[side] = wall;
                }
            } else {
                // Non-wall hanging signs need both attachment state and horizontal orientation.
                // Metadata 0..7 = attachment bit 0 plus four cardinal rotations in bits 1..2.
                for (int facing = 0; facing < 4; facing++) {
                    for (int attachment = 0; attachment <= 1; attachment++) {
                        Model ceiling = hangingSignModel(entry, attachment);
                        rotateModelY(ceiling, facing * 90);
                        prepared.facingModels[(facing << 1) | attachment] = ceiling;
                    }
                }
                // 8..11 are the optional wall-placement forms of the obtainable hanging-sign block.
                for (int side = 2; side <= 5; side++) {
                    Model wall = hangingSignModel(entry, 2);
                    rotatePlacementModel(wall, side);
                    prepared.facingModels[8 + (side - 2)] = wall;
                }
            }
        }
        if (entry.getRegistryName().endsWith("lightning_rod")) {
            String[] facings = {"down", "up", "north", "south", "west", "east"};
            for (int i = 0; i < facings.length; i++) {
                Map<String, String> state = defaultsFor(entry);
                state.put("facing", facings[i]);
                prepared.facingModels[i] = loadBlockStateModel(entry, state);
            }
        }
    }

    private static Model applySpecialItemVisual(ModernMapParityBlocks entry, Model model) {
        if (model == null) return Model.empty();
        String type = model.specialType;
        if (type == null) return model;
        Model visual = null;
        if ("minecraft:chest".equals(type)) {
            String texture = model.specialTexture == null ? "minecraft:copper" : model.specialTexture;
            if (texture.startsWith("minecraft:")) texture = texture.substring("minecraft:".length());
            visual = chestModel("minecraft:entity/chest/" + texture);
        } else if ("minecraft:conduit".equals(type)) {
            visual = conduitModel();
        } else if ("minecraft:decorated_pot".equals(type)) {
            visual = decoratedPotModel();
        } else if ("minecraft:copper_golem_statue".equals(type)) {
            visual = copperGolemStatueModel(entry, model.specialTexture);
        } else if ("minecraft:hanging_sign".equals(type)) {
            visual = hangingSignModel(entry, 0);
        }
        if (visual == null) return model;
        visual.display.putAll(model.display);
        visual.specialType = model.specialType;
        visual.specialTexture = model.specialTexture;
        visual.particleTexture = model.particleTexture;
        return visual;
    }

    private static Model applySpecialBlockVisual(ModernMapParityBlocks entry, Model model) {
        String name = entry.getRegistryName();

        if (name.endsWith("_coral_fan") || name.endsWith("_coral_wall_fan")) {
            // Vanilla deliberately authors coral fans as overlapping rotated zero-thickness planes
            // whose tips overhang the owning block cell. Adjacent fans therefore intermesh visually.
            // Do not clamp, centre, scale or vertically offset this geometry: the JSON model is the
            // authoritative shape. The much smaller selection boxes remain block-side behaviour only.
            return model;
        }

        if (name.endsWith("copper_chest")) {
            String texture;
            if (name.contains("oxidized")) texture = "minecraft:entity/chest/copper_oxidized";
            else if (name.contains("weathered")) texture = "minecraft:entity/chest/copper_weathered";
            else if (name.contains("exposed")) texture = "minecraft:entity/chest/copper_exposed";
            else texture = "minecraft:entity/chest/copper";
            return chestModel(texture);
        }

        if ("decorated_pot".equals(name)) return decoratedPotModel();
        if (name.endsWith("_wall_sign") && !name.endsWith("_wall_hanging_sign")) return signModel(entry, true);
        if (name.endsWith("_sign") && !name.endsWith("_hanging_sign")) return signModel(entry, false);

        if ("conduit".equals(name)) return conduitModel();

        if ("bell".equals(name)) {
            // BellBlockModel 1.21.11: exact 32x32 ModelPart cuboids/UV origins. Keep the JSON support
            // geometry and add only the entity-rendered bell body/base.
            addModelBox(model, 5, 6, 5, 11, 13, 11, 0, 0, 32, 32, "minecraft:entity/bell/bell_body");
            addModelBox(model, 4, 4, 4, 12, 6, 12, 0, 13, 32, 32, "minecraft:entity/bell/bell_body");
            return model;
        }

        if (name.endsWith("_hanging_sign")) return hangingSignModel(entry, name.endsWith("_wall_hanging_sign") ? 2 : 0);

        if (name.contains("copper_golem_statue")) return copperGolemStatueModel(entry, null);

        // A small number of modern block models are intentionally entity-driven and therefore
        // contain particle-only JSON. Never render those as missingno/stone: preserve their exact
        // model-declared Mojang particle texture as a correctly textured static fallback.
        if (model.quads.isEmpty() && model.particleTexture != null) {
            addBox(model, 0, 0, 0, 16, 16, 16, model.particleTexture);
        }
        return model;
    }

    private static Model conduitModel() {
        Model out = new Model();
        // Mojang ConduitRenderer base part: texOffs(0,0), 6x6x6 on a 32x16 entity texture.
        addModelBox(out, 5, 5, 5, 11, 11, 11, 0, 0, 32, 16, "minecraft:entity/conduit/base");
        return out;
    }

    private static Model decoratedPotModel() {
        Model out = new Model();
        String side = "minecraft:entity/decorated_pot/decorated_pot_side";
        String base = "minecraft:entity/decorated_pot/decorated_pot_base";

        // Keep the exact 14-wide footprint, but model the visible silhouette as a 14-high body
        // plus the inset neck/lip. This avoids the legacy renderer reading the four 0-thickness
        // modern side sheets as one giant full-height panel while retaining Mojang's own textures.
        addPlane(out, Direction.NORTH, 1, 0, 1, 15, 14, 1, 1, 2, 15, 16, side);
        addPlane(out, Direction.SOUTH, 1, 0, 15, 15, 14, 15, 1, 2, 15, 16, side);
        addPlane(out, Direction.WEST, 1, 0, 1, 1, 14, 15, 1, 2, 15, 16, side);
        addPlane(out, Direction.EAST, 15, 0, 1, 15, 14, 15, 1, 2, 15, 16, side);
        // decorated_pot_base is a 32x32 entity sprite. Scale its pixel UVs into IIcon's 0..16
        // range; feeding 27 directly to getInterpolatedV() sampled the neighbouring atlas sprite
        // (visibly water in runtime testing).
        addPlane(out, Direction.DOWN, 1, 0.01, 1, 15, 0.01, 15, 0, 6.5, 7, 13.5, base);
        addPlane(out, Direction.UP, 1, 14.0, 1, 15, 14.0, 15, 0, 6.5, 7, 13.5, base);
        // Modern base renderer neck: 8x3x8, followed by a 6x1x6 raised lip.
        addModelBoxUv(out, 4, 14, 4, 12, 16.7, 12, 0, 0, 32, 32, base, 8, 3, 8);
        addModelBoxUv(out, 5, 15.7, 5, 11, 16.0, 11, 0, 5, 32, 32, base, 6, 1, 6);
        return out;
    }

    private static Model signModel(ModernMapParityBlocks entry, boolean wall) {
        String name = entry.getRegistryName();
        String suffix = wall && name.endsWith("_wall_sign") ? "_wall_sign" : "_sign";
        String wood = name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : "pale_oak";
        String texture = "minecraft:entity/signs/" + wood;
        Model out = new Model();
        // SignBlockEntityRenderer uses a 24x12x2 board scaled by 2/3 => exactly 16x8x4/3.
        addModelBoxUv(out, 0, 6, 7.333, 16, 14, 8.667, 0, 0, 64, 32, texture, 24, 12, 2);
        if (!wall) {
            addModelBoxUv(out, 7.333, 0, 7.333, 8.667, 9.333, 8.667, 0, 14, 64, 32, texture, 2, 14, 2);
        }
        return out;
    }

    /** attachment: 0=ceiling chains, 1=ceiling-middle/V chain, 2=wall. */
    private static Model hangingSignModel(ModernMapParityBlocks entry, int attachment) {
        String name = entry.getRegistryName();
        String suffix = name.endsWith("_wall_hanging_sign") ? "_wall_hanging_sign" : "_hanging_sign";
        String wood = name.substring(0, name.length() - suffix.length());
        String texture = "minecraft:entity/signs/hanging/" + wood;
        Model out = new Model();

        /*
         * HangingSignBlockEntityRenderer anchors the model at block-space Y=10/16, then renders
         * the ModelPart with Y/Z inverted.  Bake that transform here so the board occupies the
         * lower 10 pixels and every attachment occupies the six pixels above it, exactly like
         * 1.21.11.  The entity sheet is 64x32; chain UVs therefore MUST be scaled to the legacy
         * IIcon's 0..16 interpolation range instead of being treated as a 16x16 block texture.
         */
        addModelBox(out, 1, 0, 7, 15, 10, 9, 0, 12, 64, 32, texture);

        if (attachment == 2) {
            // WALL = support plank plus the same four crossed chain strips used by CEILING.
            addModelBox(out, 0, 14, 6, 16, 16, 10, 0, 0, 64, 32, texture);
            addHangingSignChain(out, 3.0D, -45.0D, 0, 6, 3, 12, texture);
            addHangingSignChain(out, 3.0D,  45.0D, 6, 6, 9, 12, texture);
            addHangingSignChain(out, 13.0D, -45.0D, 0, 6, 3, 12, texture);
            addHangingSignChain(out, 13.0D,  45.0D, 6, 6, 9, 12, texture);
        } else if (attachment == 1) {
            // CEILING_MIDDLE = one vertical 12x6 V-chain sheet (UV 14,6 -> 26,12).
            addDoublePlaneUv(out, Direction.NORTH, 2, 10, 8, 14, 16, 8,
                    14, 6, 26, 12, 64, 32, texture, -1);
        } else {
            // CEILING = four 3x6 strips, each rotated +/-45 degrees around its chain pivot.
            addHangingSignChain(out, 3.0D, -45.0D, 0, 6, 3, 12, texture);
            addHangingSignChain(out, 3.0D,  45.0D, 6, 6, 9, 12, texture);
            addHangingSignChain(out, 13.0D, -45.0D, 0, 6, 3, 12, texture);
            addHangingSignChain(out, 13.0D,  45.0D, 6, 6, 9, 12, texture);
        }
        return out;
    }

    private static void addHangingSignChain(Model model, double centerX, double angle,
            double u0, double v0, double u1, double v1, String texture) {
        String normalized = normalizeTexture(texture);
        double[] from = {centerX - 1.5D, 10.0D, 8.0D};
        double[] to = {centerX + 1.5D, 16.0D, 8.0D};
        double[] uv = {u0 * 16.0D / 64.0D, v0 * 16.0D / 32.0D,
                u1 * 16.0D / 64.0D, v1 * 16.0D / 32.0D};
        Quad front = makeFace(Direction.NORTH, from, to, uv, 0, normalized, -1);
        Quad back = makeFace(Direction.SOUTH, from, to, uv, 0, normalized, -1);
        ElementRotation rotation = new ElementRotation(new double[]{centerX, 0.0D, 8.0D}, "y", angle, false);
        rotateElement(front, rotation);
        rotateElement(back, rotation);
        model.quads.add(front);
        model.quads.add(back);
    }

    private static void addDoublePlaneUv(Model model, Direction dir,
            double x0,double y0,double z0,double x1,double y1,double z1,
            double u0,double v0,double u1,double v1,double texW,double texH,
            String texture,int tintIndex) {
        addDoublePlane(model, dir, x0, y0, z0, x1, y1, z1,
                u0 * 16.0D / texW, v0 * 16.0D / texH,
                u1 * 16.0D / texW, v1 * 16.0D / texH, texture, tintIndex);
    }

    private static void addDoublePlane(Model model, Direction dir,
            double x0,double y0,double z0,double x1,double y1,double z1,
            double u0,double v0,double u1,double v1,String texture,int tintIndex) {
        Direction opposite;
        switch (dir) {
            case NORTH: opposite = Direction.SOUTH; break;
            case SOUTH: opposite = Direction.NORTH; break;
            case EAST: opposite = Direction.WEST; break;
            case WEST: opposite = Direction.EAST; break;
            case UP: opposite = Direction.DOWN; break;
            default: opposite = Direction.UP; break;
        }
        model.quads.add(makeFace(dir, new double[]{x0,y0,z0}, new double[]{x1,y1,z1}, new double[]{u0,v0,u1,v1}, 0,
                normalizeTexture(texture), tintIndex));
        model.quads.add(makeFace(opposite, new double[]{x0,y0,z0}, new double[]{x1,y1,z1}, new double[]{u0,v0,u1,v1}, 0,
                normalizeTexture(texture), tintIndex));
    }

    private static void rotateModelY(Model model, int degrees) {
        int y = ((degrees % 360) + 360) % 360;
        if (y == 0) return;
        for (Quad quad : model.quads) rotateState(quad, 0, y);
    }

    private static void rotatePlacementModel(Model model, int side) {
        int y;
        switch (side) {
            case 2: y = 180; break; // north clicked face -> sign faces south
            case 3: y = 0; break;
            case 4: y = 90; break;
            case 5: y = 270; break;
            default: y = 0; break;
        }
        if (y == 0) return;
        for (Quad quad : model.quads) rotateState(quad, 0, y);
    }

    /**
     * The modern campfire flame sheet reaches one model pixel above the block. Through the legacy
     * 1.7 immediate renderer that overhang reads noticeably taller than the modern in-game flame.
     * Keep Mojang's authored base point, but compact only the fire-textured vertices by 10%.
     */
    private static void compactCampfireFlames(Model model) {
        if (model == null) return;
        final double base = 1.0D / 16.0D;
        for (Quad quad : model.quads) {
            if (quad.texture == null || quad.texture.indexOf("campfire_fire") < 0) continue;
            for (Vertex vertex : quad.vertices) {
                if (vertex.y > base) vertex.y = base + (vertex.y - base) * 0.90D;
            }
        }
    }

    private static String copperGolemTexture(ModernMapParityBlocks entry, String itemTexture) {
        if (itemTexture != null && !itemTexture.isEmpty()) {
            String t = itemTexture;
            if (t.startsWith("minecraft:textures/")) t = "minecraft:" + t.substring("minecraft:textures/".length());
            if (t.endsWith(".png")) t = t.substring(0, t.length() - 4);
            return t;
        }
        String name = entry.getRegistryName();
        if (name.contains("oxidized")) return "minecraft:entity/copper_golem/oxidized_copper_golem";
        if (name.contains("weathered")) return "minecraft:entity/copper_golem/weathered_copper_golem";
        if (name.contains("exposed")) return "minecraft:entity/copper_golem/exposed_copper_golem";
        return "minecraft:entity/copper_golem/copper_golem";
    }

    private static Model copperGolemStatueModel(ModernMapParityBlocks entry, String itemTexture) {
        String texture = copperGolemTexture(entry, itemTexture);
        Model out = new Model();
        /*
         * Exact standing CopperGolemEntityModel cuboid proportions/UV origins from 1.21.11,
         * scaled from the entity model's 24-pixel standing height into one block.  The modern
         * statue renderer uses this entity model rather than the particle-only block JSON.
         */
        addGolemBox(out, -4, 13, -3,  4, 19,  3,  0, 15, texture); // body
        addGolemBox(out, -4,  8, -5,  4, 13,  5,  0,  0, texture); // head
        addGolemBox(out, -1, 11, -6,  1, 14, -4, 56,  0, texture); // nose
        addGolemBox(out, -1,  4, -1,  1,  8,  1, 37,  8, texture); // rod
        addGolemBox(out, -2,  0, -2,  2,  4,  2, 37,  0, texture); // top block
        addGolemBox(out, -7, 12, -2, -4, 22,  2, 36, 16, texture); // right arm
        addGolemBox(out,  4, 12, -2,  7, 22,  2, 50, 16, texture); // left arm
        addGolemBox(out, -4, 19, -2,  0, 24,  2,  0, 27, texture); // right leg
        addGolemBox(out,  0, 19, -2,  4, 24,  2, 16, 27, texture); // left leg
        // CopperGolemStatueModel applies a PI roll to the entity model. Our Y coordinates already
        // bake that upright conversion; mirror X as well so the entity-sheet UV orientation and
        // left/right face assignment match the modern statue renderer.
        mirrorModelX(out);
        return out;
    }

    private static void mirrorModelX(Model model) {
        for (Quad quad : model.quads) {
            for (Vertex vertex : quad.vertices) vertex.x = 1.0D - vertex.x;
            // Mirroring reverses winding; reverse both geometry and UV order together so faces
            // remain outward-facing without changing which texel belongs to each vertex.
            for (int a = 0, b = quad.vertices.length - 1; a < b; a++, b--) {
                Vertex v = quad.vertices[a]; quad.vertices[a] = quad.vertices[b]; quad.vertices[b] = v;
                double[] uv = quad.uv[a]; quad.uv[a] = quad.uv[b]; quad.uv[b] = uv;
            }
        }
    }

    private static void addGolemBox(Model model, double x0, double yDown0, double z0,
                                    double x1, double yDown1, double z1,
                                    int texU, int texV, String texture) {
        final double scale = 16.0D / 24.0D;
        double bx0 = 8.0D + x0 * scale;
        double bx1 = 8.0D + x1 * scale;
        double by0 = (24.0D - yDown1) * scale;
        double by1 = (24.0D - yDown0) * scale;
        double bz0 = 8.0D + z0 * scale;
        double bz1 = 8.0D + z1 * scale;
        // The statue geometry is scaled from a 24px entity into one block, but its 64x64
        // entity texture is NOT scaled. Keep the original entity cuboid dimensions for UV
        // layout or every face samples a compressed/wrong part of the copper-golem sheet.
        addModelBoxUv(model, bx0, by0, bz0, bx1, by1, bz1, texU, texV, 64, 64, texture,
                x1 - x0, yDown1 - yDown0, z1 - z0);
    }

    private static Model chestModel(String texture) {
        Model out = new Model();
        // Closed vanilla chest silhouette. UVs are mapped from the same 64x64 layout used by the
        // modern copper chest entity textures.
        addModelBox(out, 1, 0, 1, 15, 10, 15, 0, 19, 64, 64, texture);
        addModelBox(out, 1, 10, 1, 15, 15, 15, 0, 0, 64, 64, texture);
        addModelBox(out, 7, 8, 0, 9, 12, 1, 0, 0, 64, 64, texture);
        return out;
    }

    private static void addBox(Model model, double x0,double y0,double z0,double x1,double y1,double z1,String texture) {
        double[] from={x0,y0,z0}, to={x1,y1,z1}, uv={0,0,16,16};
        for (Direction d : Direction.values()) model.quads.add(makeFace(d,from,to,uv,0,normalizeTexture(texture)));
    }

    private static void addModelBox(Model model, double x0,double y0,double z0,double x1,double y1,double z1,
                                    int texU,int texV,int texW,int texH,String texture) {
        addModelBoxUv(model, x0, y0, z0, x1, y1, z1, texU, texV, texW, texH, texture,
                x1 - x0, y1 - y0, z1 - z0);
    }

    private static void addModelBoxUv(Model model, double x0,double y0,double z0,double x1,double y1,double z1,
                                      int texU,int texV,int texW,int texH,String texture,
                                      double uvW,double uvH,double uvD) {
        double w=uvW, h=uvH, d=uvD;
        String t=normalizeTexture(texture);
        // Reproduce ModelPart.Cuboid/Quad's exact entity-sheet layout while allowing geometry
        // to be scaled independently (the Copper Golem statue is a 24px entity scaled to 1 block).
        addModelFace(model, Direction.EAST,  new double[][]{{x1,y0,z1},{x1,y0,z0},{x1,y1,z0},{x1,y1,z1}}, texU+d+w, texV+d, texU+d+w+d, texV+d+h, texW,texH,t);
        addModelFace(model, Direction.WEST,  new double[][]{{x0,y0,z0},{x0,y0,z1},{x0,y1,z1},{x0,y1,z0}}, texU, texV+d, texU+d, texV+d+h, texW,texH,t);
        addModelFace(model, Direction.DOWN,  new double[][]{{x1,y0,z1},{x0,y0,z1},{x0,y0,z0},{x1,y0,z0}}, texU+d, texV, texU+d+w, texV+d, texW,texH,t);
        addModelFace(model, Direction.UP,    new double[][]{{x1,y1,z0},{x0,y1,z0},{x0,y1,z1},{x1,y1,z1}}, texU+d+w, texV+d, texU+d+w+w, texV, texW,texH,t);
        addModelFace(model, Direction.NORTH, new double[][]{{x1,y0,z0},{x0,y0,z0},{x0,y1,z0},{x1,y1,z0}}, texU+d, texV+d, texU+d+w, texV+d+h, texW,texH,t);
        addModelFace(model, Direction.SOUTH, new double[][]{{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}}, texU+d+w+d, texV+d, texU+d+w+d+w, texV+d+h, texW,texH,t);
    }

    private static void addModelFace(Model model, Direction dir, double[][] xyz,
                                     double u0,double v0,double u1,double v1,double texW,double texH,String texture) {
        Vertex[] vertices = new Vertex[4];
        for (int i=0;i<4;i++) vertices[i]=new Vertex(xyz[i][0]/16.0D,xyz[i][1]/16.0D,xyz[i][2]/16.0D);
        double[][] uv = new double[][]{
                {u1*16.0D/texW, v0*16.0D/texH},
                {u0*16.0D/texW, v0*16.0D/texH},
                {u0*16.0D/texW, v1*16.0D/texH},
                {u1*16.0D/texW, v1*16.0D/texH}};
        model.quads.add(new Quad(vertices,uv,texture,dir.shade,-1));
    }

    private static void addPlane(Model model, Direction dir,double x0,double y0,double z0,double x1,double y1,double z1,
                                 double u0,double v0,double u1,double v1,String texture) {
        model.quads.add(makeFace(dir,new double[]{x0,y0,z0},new double[]{x1,y1,z1},new double[]{u0,v0,u1,v1},0,normalizeTexture(texture)));
    }

    private static Transform parseTransform(JsonObject json) {
        return new Transform(vec(json, "rotation", new double[]{0, 0, 0}), vec(json, "translation", new double[]{0, 0, 0}), vec(json, "scale", new double[]{1, 1, 1}));
    }

    private static ElementRotation parseElementRotation(JsonObject element) {
        if (!element.has("rotation")) return null;
        JsonObject r = element.getAsJsonObject("rotation");
        return new ElementRotation(vec(r, "origin", new double[]{8, 8, 8}), string(r, "axis", "y"), number(r, "angle", 0.0D), bool(r, "rescale", false));
    }

    private static Quad makeFace(Direction d, double[] from, double[] to, double[] uv, int rotation, String texture) {
        return makeFace(d, from, to, uv, rotation, texture, -1);
    }

    private static Quad makeFace(Direction d, double[] from, double[] to, double[] uv, int rotation, String texture, int tintIndex) {
        double x0 = from[0] / 16.0D, y0 = from[1] / 16.0D, z0 = from[2] / 16.0D;
        double x1 = to[0] / 16.0D, y1 = to[1] / 16.0D, z1 = to[2] / 16.0D;
        Vertex[] v;
        switch (d) {
            case DOWN:  v = new Vertex[]{new Vertex(x0,y0,z1),new Vertex(x0,y0,z0),new Vertex(x1,y0,z0),new Vertex(x1,y0,z1)}; break;
            case UP:    v = new Vertex[]{new Vertex(x1,y1,z1),new Vertex(x1,y1,z0),new Vertex(x0,y1,z0),new Vertex(x0,y1,z1)}; break;
            case NORTH: v = new Vertex[]{new Vertex(x1,y1,z0),new Vertex(x1,y0,z0),new Vertex(x0,y0,z0),new Vertex(x0,y1,z0)}; break;
            case SOUTH: v = new Vertex[]{new Vertex(x0,y1,z1),new Vertex(x0,y0,z1),new Vertex(x1,y0,z1),new Vertex(x1,y1,z1)}; break;
            case WEST:  v = new Vertex[]{new Vertex(x0,y1,z0),new Vertex(x0,y0,z0),new Vertex(x0,y0,z1),new Vertex(x0,y1,z1)}; break;
            case EAST:  v = new Vertex[]{new Vertex(x1,y1,z1),new Vertex(x1,y0,z1),new Vertex(x1,y0,z0),new Vertex(x1,y1,z0)}; break;
            default: throw new IllegalStateException();
        }
        double[][] t = new double[][]{{uv[0],uv[1]},{uv[0],uv[3]},{uv[2],uv[3]},{uv[2],uv[1]}};
        int turns = ((rotation % 360) + 360) % 360 / 90;
        for (int i = 0; i < turns; i++) {
            double[] tmp = t[0]; t[0]=t[1]; t[1]=t[2]; t[2]=t[3]; t[3]=tmp;
        }
        return new Quad(v, t, texture, d.shade, tintIndex);
    }

    private static double[] defaultUv(Direction d, double[] from, double[] to) {
        switch (d) {
            case DOWN: case UP: return new double[]{from[0], from[2], to[0], to[2]};
            case NORTH: case SOUTH: return new double[]{from[0], 16.0D-to[1], to[0], 16.0D-from[1]};
            case WEST: case EAST: return new double[]{from[2], 16.0D-to[1], to[2], 16.0D-from[1]};
            default: return new double[]{0,0,16,16};
        }
    }

    private static void rotateElement(Quad q, ElementRotation r) {
        double angle = Math.toRadians(r.angle);
        double ox = r.origin[0]/16.0D, oy=r.origin[1]/16.0D, oz=r.origin[2]/16.0D;
        double scale = r.rescale && Math.abs(r.angle) > 0.001D ? 1.0D / Math.cos(Math.toRadians(Math.abs(r.angle))) : 1.0D;
        double scaleX = 1.0D, scaleY = 1.0D, scaleZ = 1.0D;
        if (r.rescale) {
            // FaceBakery rescales only the two axes perpendicular to the rotation axis.
            // Scaling the rotation axis as well distorts thin models such as coral wall fans.
            if ("x".equals(r.axis)) { scaleY = scale; scaleZ = scale; }
            else if ("z".equals(r.axis)) { scaleX = scale; scaleY = scale; }
            else { scaleX = scale; scaleZ = scale; }
        }
        for (Vertex v : q.vertices) {
            double x=v.x-ox, y=v.y-oy, z=v.z-oz;
            if ("x".equals(r.axis)) { double ny=y*Math.cos(angle)-z*Math.sin(angle); double nz=y*Math.sin(angle)+z*Math.cos(angle); y=ny; z=nz; }
            else if ("z".equals(r.axis)) { double nx=x*Math.cos(angle)-y*Math.sin(angle); double ny=x*Math.sin(angle)+y*Math.cos(angle); x=nx; y=ny; }
            else { double nx=x*Math.cos(angle)+z*Math.sin(angle); double nz=-x*Math.sin(angle)+z*Math.cos(angle); x=nx; z=nz; }
            if (r.rescale) { x*=scaleX; y*=scaleY; z*=scaleZ; }
            v.x=x+ox; v.y=y+oy; v.z=z+oz;
        }
    }

    private static void rotateState(Quad q, int xDegrees, int yDegrees) {
        if (xDegrees != 0) for (Vertex v : q.vertices) rotateAroundCenter(v, 'x', Math.toRadians(xDegrees));
        if (yDegrees != 0) for (Vertex v : q.vertices) rotateAroundCenter(v, 'y', Math.toRadians(yDegrees));
    }

    private static void rotateAroundCenter(Vertex v, char axis, double angle) {
        double x=v.x-0.5D, y=v.y-0.5D, z=v.z-0.5D;
        if (axis=='x') { double ny=y*Math.cos(angle)-z*Math.sin(angle); double nz=y*Math.sin(angle)+z*Math.cos(angle); y=ny; z=nz; }
        else { double nx=x*Math.cos(angle)-z*Math.sin(angle); double nz=x*Math.sin(angle)+z*Math.cos(angle); x=nx; z=nz; }
        v.x=x+0.5D; v.y=y+0.5D; v.z=z+0.5D;
    }

    private static String resolveTexture(Map<String, String> textures, String value) {
        if (value == null) return null;
        String current = value;
        int guard = 0;
        while (guard++ < 32) {
            String key = current.startsWith("#") ? current.substring(1) : current;
            String next = textures.get(key);
            if (next == null) break;
            current = next;
        }
        if (current.startsWith("#")) return null;
        return normalizeTexture(current);
    }

    private static double[] vec(JsonObject obj, String key, double[] fallback) {
        if (!obj.has(key)) return fallback.clone();
        JsonArray a=obj.getAsJsonArray(key); double[] out=new double[Math.min(3,a.size())];
        for(int i=0;i<out.length;i++) out[i]=a.get(i).getAsDouble();
        return out.length==3?out:fallback.clone();
    }
    private static double[] vec4(JsonArray a) { return new double[]{a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble(),a.get(3).getAsDouble()}; }
    private static String string(JsonObject obj,String k,String d){return obj.has(k)?obj.get(k).getAsString():d;}
    private static int integer(JsonObject obj,String k,int d){return obj.has(k)?obj.get(k).getAsInt():d;}
    private static double number(JsonObject obj,String k,double d){return obj.has(k)?obj.get(k).getAsDouble():d;}
    private static boolean bool(JsonObject obj,String k,boolean d){return obj.has(k)?obj.get(k).getAsBoolean():d;}

    public static final class PreparedModels {
        static final PreparedModels EMPTY = new PreparedModels();
        public Model block = Model.empty();
        public Model item = Model.empty();
        public final Model[] connectionModels = new Model[16];
        public final Model[] facingModels = new Model[32];
        public final Model[] chiseledBookshelfBase = new Model[4];
        public final Model[] chiseledBookshelfSlots = new Model[48];
    }

    public static final class Model {
        public final List<Quad> quads = new ArrayList<Quad>();
        public final Map<String, Transform> display = new HashMap<String, Transform>();
        public String flatTexture;
        public String particleTexture;
        public IIcon flatIcon;
        public String specialType;
        public String specialTexture;
        static Model empty(){return new Model();}
        public boolean isEmpty(){return quads.isEmpty() && flatTexture==null && specialType==null;}
        Model copyForItem(){ Model c=new Model(); c.quads.addAll(quads); c.display.putAll(display); c.flatTexture=flatTexture; c.particleTexture=particleTexture; c.flatIcon=flatIcon; return c; }
    }

    public static final class Quad {
        public final Vertex[] vertices;
        public final double[][] uv;
        public final String texture;
        public final float shade;
        public final int tintIndex;
        public IIcon icon;
        Quad(Vertex[] vertices,double[][] uv,String texture,float shade,int tintIndex){this.vertices=vertices;this.uv=uv;this.texture=texture;this.shade=shade;this.tintIndex=tintIndex;}
    }

    public static final class Vertex { public double x,y,z; Vertex(double x,double y,double z){this.x=x;this.y=y;this.z=z;} }
    public static final class Transform {
        public final double[] rotation,translation,scale;
        Transform(double[] rotation,double[] translation,double[] scale){this.rotation=rotation;this.translation=translation;this.scale=scale;}
    }

    private static final class RawModel {
        final Map<String,String> textures=new LinkedHashMap<String,String>();
        JsonArray elements;
        final Map<String,Transform> display=new HashMap<String,Transform>();
        boolean generated;
        RawModel copy(){RawModel c=new RawModel();c.textures.putAll(textures);c.elements=elements;c.display.putAll(display);c.generated=generated;return c;}
    }
    private static final class ModelRef { final String path;final int x,y;final boolean uvlock;ModelRef(String p,int x,int y,boolean u){path=p;this.x=x;this.y=y;uvlock=u;} }
    private static final class ItemDefinition { final String modelPath,specialType,specialTexture;ItemDefinition(String p,String s,String t){modelPath=p;specialType=s;specialTexture=t;} }
    private static final class ElementRotation { final double[] origin;final String axis;final double angle;final boolean rescale;ElementRotation(double[] o,String a,double an,boolean r){origin=o;axis=a;angle=an;rescale=r;} }
    private enum Direction {
        DOWN("down",0.5F),UP("up",1.0F),NORTH("north",0.8F),SOUTH("south",0.8F),WEST("west",0.6F),EAST("east",0.6F);
        final String name;final float shade;Direction(String n,float s){name=n;shade=s;} static Direction byName(String n){for(Direction d:values())if(d.name.equals(n.toLowerCase(Locale.ROOT)))return d;return null;}
    }
}
