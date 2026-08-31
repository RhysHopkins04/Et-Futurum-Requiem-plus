#!/usr/bin/env python3
"""Validate the AssetDirector-backed Minecraft 1.21.11 visual model compatibility layer."""
from collections import Counter
from pathlib import Path
import json, re, sys

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "scripts/modern_map_parity_blocks.json"
JAVA = ROOT / "src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java"
BRIDGE = ROOT / "src/main/java/ganymedes01/etfuturum/client/ModernAssetResourcePack.java"
MODEL = ROOT / "src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java"
BLOCK_RENDER = ROOT / "src/main/java/ganymedes01/etfuturum/client/renderer/block/BlockModernJsonModelRenderer.java"
ITEM_RENDER = ROOT / "src/main/java/ganymedes01/etfuturum/client/renderer/item/ItemModernJsonModelRenderer.java"
CLIENT_PROXY = ROOT / "src/main/java/ganymedes01/etfuturum/core/proxy/ClientProxy.java"
COPPER_CHEST_TESR = ROOT / "src/main/java/ganymedes01/etfuturum/client/renderer/tileentity/TileEntityParityCopperChestRenderer.java"
PARITY_TESR = ROOT / "src/main/java/ganymedes01/etfuturum/client/renderer/tileentity/TileEntityModernParityRenderer.java"
SIGN_GUI = ROOT / "src/main/java/ganymedes01/etfuturum/client/gui/inventory/GuiEditWoodSign.java"
CLIENT_EVENTS = ROOT / "src/main/java/ganymedes01/etfuturum/core/handlers/client/ClientEventHandler.java"
SERVER_EVENTS = ROOT / "src/main/java/ganymedes01/etfuturum/core/handlers/ServerEventHandler.java"
ARMOR_SOUNDS = ROOT / "src/main/java/ganymedes01/etfuturum/api/ArmorSoundsRegistry.java"
BUBBLE_COLUMN = ROOT / "src/main/java/ganymedes01/etfuturum/blocks/BlockBubbleColumn.java"
WOOD_SIGN = ROOT / "src/main/java/ganymedes01/etfuturum/blocks/BlockWoodSign.java"
WOOD_SIGN_TESR = ROOT / "src/main/java/ganymedes01/etfuturum/client/renderer/tileentity/TileEntityWoodSignRenderer.java"
WOOD_SIGN_TE = ROOT / "src/main/java/ganymedes01/etfuturum/tileentities/TileEntityWoodSign.java"
SIGN_UPDATE = ROOT / "src/main/java/ganymedes01/etfuturum/network/WoodSignUpdateHandler.java"
MOD_ITEMS = ROOT / "src/main/java/ganymedes01/etfuturum/ModItems.java"
RENDER_IDS = ROOT / "src/main/java/ganymedes01/etfuturum/lib/RenderIDs.java"
CONFIG = ROOT / "src/main/java/ganymedes01/etfuturum/configuration/configs/ConfigBlocksItems.java"
MAIN = ROOT / "src/main/java/ganymedes01/etfuturum/EtFuturum.java"
DYNAMIC_SOUNDS = ROOT / "src/main/java/ganymedes01/etfuturum/client/DynamicSoundsResourcePack.java"
LANG = ROOT / "src/main/resources/assets/etfuturum/lang/en_US.lang"
MC_ASSETS = ROOT / "src/main/resources/assets/minecraft"

failures=[]
manifest=json.loads(MANIFEST.read_text(encoding="utf-8")); blocks=manifest.get("blocks",[]); names=[e["name"] for e in blocks]
if manifest.get("target") != "Minecraft Java 1.21.11 visual block compatibility shells": failures.append("manifest target is not pinned to Minecraft Java 1.21.11")
if manifest.get("count") != len(blocks): failures.append("manifest count mismatch")
if len(names)!=242 or len(names)!=len(set(names)): failures.append(f"expected 242 unique visual-gap identities, found {len(names)}")
required={"kelp","seagrass","conduit","tube_coral_block","scaffolding","bell","candle","sculk_sensor","decorated_pot","trial_spawner","vault","crafter","pale_oak_planks","resin_bricks","firefly_bush","dried_ghast","copper_chest","copper_golem_statue","oak_shelf"}
if required-set(names): failures.append("required families missing: "+", ".join(sorted(required-set(names))))
versions=Counter(e["ver"] for e in blocks)
for v in ("1.13","1.17","1.19","1.20","1.21","1.21.4","1.21.5","1.21.6","1.21.9"):
    if not versions[v]: failures.append("no entries for "+v)

java=JAVA.read_text(encoding="utf-8")
for name in names:
    if not re.search(r"^\s*"+re.escape(name.upper())+r"\(",java,re.M): failures.append("enum missing "+name)
for expected in (
    'GameRegistry.findBlock("minecraft", name)', 'GameRegistry.findBlock(Tags.MOD_ID, name)',
    'GameRegistry.registerBlock(entry.block, name);', 'ConfigBlocksItems.enableModernMapParityBlocks',
    'return new ParityModelBlock(this);', 'RenderIDs.MODERN_MAP_PARITY', 'ModernJsonModelBridge.prepare(entry, reg)',
):
    if expected not in java: failures.append("registry/model invariant missing: "+expected)
# Vanilla Block() calls isOpaqueCube() before ParityModelBlock's entry field is assigned.
# Keep that virtual call null-safe, then refresh Block's cached opacity fields after assignment.
for expected in (
    'return entry != null && isFullOpaqueModel();',
    'this.opaque = isOpaqueCube();',
    'this.lightOpacity = this.opaque ? 255 : 0;',
):
    if expected not in java: failures.append("constructor-safe parity opacity invariant missing: "+expected)
for expected in (
    'new ParityCopperChestBlock(this)',
    'extends BlockChest',
    'extends TileEntityChest',
    'GameRegistry.registerTileEntity(ParityCopperChestTileEntity.class',
    'setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z)',
    'entry.style == Style.PANE || entry.style == Style.FENCE || entry.style == Style.WALL',
    'addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask',
    'addCollisionBox(mask, list, x, y, z',
    'collisionHeight = 1.0F;',
    'if (isLightningRod())',
    'case 1: return 1;',
    '"conduit".equals(name)',
    'setBlockBounds(0.3125F, 0.3125F, 0.3125F, 0.6875F, 0.6875F, 0.6875F);',
    '"decorated_pot".equals(name)',
    'setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 1.0F, 0.9375F);',
):
    if expected not in java: failures.append("stateful parity block invariant missing: "+expected)
# The rejected first implementation hard-coded one guessed texture per block. It must never return.
if 'textures/blocks/modern_parity/' in java or 'iconName(texture)' in java:
    failures.append("rejected guessed one-texture parity renderer is still present")

bridge=BRIDGE.read_text(encoding="utf-8")
for expected in ('DYNAMIC_MODEL_ALIASES','registerDynamicAlias','new ResourceLocation(Tags.MC_ASSET_VER, dynamicTarget)'):
    if expected not in bridge: failures.append("dynamic AssetDirector bridge invariant missing: "+expected)

model=MODEL.read_text(encoding="utf-8")
for expected in (
    'blockstates/" + entry.getRegistryName() + ".json', 'items/" + name + ".json', 'models/" + path + ".json',
    'resolveRawModel', 'element.getAsJsonObject("faces")', 'parseElementRotation', 'rotateState',
    'registerDynamicAlias', 'minecraft:entity/chest/', 'minecraft:entity/decorated_pot/', 'minecraft:entity/conduit/base',
    'minecraft:entity/signs/hanging/', 'minecraft:entity/copper_golem/', 'minecraft:builtin/generated',
    'getWorldModel(ModernMapParityBlocks entry, IBlockAccess world, int x, int y, int z)',
    'connectionModels[mask]', 'facingModels[facing]',
    'entry.getRegistryName().endsWith("lightning_rod") ? "up" : "north"',
    'prepared.connectionModels[mask] = applySpecialBlockVisual',
    'prepared.facingModels[i] = loadBlockStateModel(entry, state)',
    'if (raw.generated)', 'model.flatTexture = resolveTexture(raw.textures, raw.textures.get("layer0"))',
):
    if expected not in model: failures.append("modern JSON model bridge invariant missing: "+expected)
# Texture-atlas lifecycle guard. Parsed JSON may be cached, but IIcon UVs must never be.
# The bootstrap atlas is replaced after AssetDirector is injected, so every texture stitch must
# re-register the model's sprites and an early failed JSON lookup must be retried later.
if 'iconsReady' in model:
    failures.append("modern model bridge must not cache per-atlas IIcon readiness")
for expected in (
    'if (prepared == null)',
    'boolean blockSourceReady = false;',
    'boolean itemSourceReady = false;',
    'if (blockSourceReady || itemSourceReady) CACHE.put(name, candidate);',
    'registerTextures(prepared.block, register);',
    'registerTextures(prepared.item, register);',
):
    if expected not in model: failures.append("texture-atlas rebind invariant missing: "+expected)
prepare_start=model.find('public static synchronized PreparedModels prepare')
prepare_end=model.find('public static synchronized PreparedModels get', prepare_start)
prepare_body=model[prepare_start:prepare_end]
null_branch_start=prepare_body.find('if (prepared == null)')
last_register=max(prepare_body.rfind('registerTextures(prepared.block, register);'), prepare_body.rfind('registerTextures(prepared.item, register);'))
branch_cache=prepare_body.find('if (blockSourceReady || itemSourceReady) CACHE.put(name, candidate);')
if min(prepare_start, prepare_end, null_branch_start, last_register, branch_cache) < 0 or last_register < branch_cache:
    failures.append("texture registration is not guaranteed to run after the source-cache branch")

block_renderer=BLOCK_RENDER.read_text(encoding="utf-8")
for expected in (
    'ModernJsonModelBridge.getWorldModel(entry, world, x, y, z)',
    'sampleBrightness(block, world, x, y, z, face)',
    'block.getMixedBrightnessForBlock(world, x + dx, y + dy, z + dz)',
    'FaceInfo.from(q)', 'getInterpolatedU', 'getInterpolatedV',
):
    if expected not in block_renderer: failures.append("placed-block renderer invariant missing: "+expected)
item_renderer=ITEM_RENDER.read_text(encoding="utf-8")
for expected in (
    'model.flatIcon', 'model.display.get(key)', 'generatedFlatItem',
    'if (type == ItemRenderType.INVENTORY && generatedFlatItem) return;',
    'helper == ItemRendererHelper.INVENTORY_BLOCK',
    'return model == null || model.flatIcon == null;',
    'if (flat && type == ItemRenderType.INVENTORY)',
    'renderFlatInventory(model.flatIcon);',
    'new net.minecraft.client.renderer.entity.RenderItem().renderIcon(0, 0, icon, 16, 16);',
    'if (!(type == ItemRenderType.INVENTORY && !flat))',
    'ItemRenderer.renderItemIn2D', 'Normal.from(q)',
):
    if expected not in item_renderer: failures.append("item renderer invariant missing: "+expected)
# Special item types must be converted into renderer-neutral geometry in the bridge, not hard-coded in the IItemRenderer.
for expected in ('minecraft:chest', 'minecraft:conduit', 'minecraft:decorated_pot', 'minecraft:copper_golem_statue', 'minecraft:hanging_sign'):
    if expected not in model: failures.append("special item visual bridge missing: "+expected)
for expected in (
    'canonicalItemName(entry.getRegistryName())',
    'name.endsWith("_wall_hanging_sign")',
    "if (path.indexOf('/') < 0) path = \"block/\" + path;",
    'String key = current.startsWith("#") ? current.substring(1) : current;',
    'double nx=x*Math.cos(angle)-z*Math.sin(angle); double nz=x*Math.sin(angle)+z*Math.cos(angle);',
    'addModelFace(model, Direction.EAST',
    'addModelBox(model, 5, 6, 5, 11, 13, 11, 0, 0, 32, 32, "minecraft:entity/bell/bell_body")',
):
    if expected not in model: failures.append("fidelity-pass bridge invariant missing: "+expected)

asset_bridge=(ROOT/'src/main/java/ganymedes01/etfuturum/client/ModernAssetResourcePack.java').read_text(encoding='utf-8')
for expected in (
    'isDynamicModelPng(location.getResourcePath())',
    'normalizeModelTexture(input, modern.getResourcePath().startsWith("textures/entity/"), modern.getResourcePath())',
    'BufferedImage.TYPE_INT_ARGB',
    'graphics.drawImage(source, 0, 0, width, height, null)',
):
    if expected not in asset_bridge: failures.append("modern dynamic-texture alpha/square normalization missing: "+expected)
proxy=CLIENT_PROXY.read_text(encoding="utf-8")
for expected in ('new ItemModernJsonModelRenderer(entry)', 'new BlockModernJsonModelRenderer()',
                 'ParityCopperChestTileEntity.class, new TileEntityParityCopperChestRenderer()'):
    if expected not in proxy: failures.append("client renderer registration missing: "+expected)
if not COPPER_CHEST_TESR.is_file():
    failures.append("Copper Chest TESR source missing")
else:
    chest_tesr=COPPER_CHEST_TESR.read_text(encoding="utf-8")
    for expected in ('extends TileEntitySpecialRenderer', 'new ModelChest()', 'prevLidAngle',
                     'textures/entity/chest/', 'copper_oxidized', 'copper_weathered', 'copper_exposed'):
        if expected not in chest_tesr: failures.append("Copper Chest TESR invariant missing: "+expected)
if 'MODERN_MAP_PARITY = RenderingRegistry.getNextAvailableRenderId()' not in RENDER_IDS.read_text(encoding="utf-8"):
    failures.append("dedicated modern parity render ID missing")

config=CONFIG.read_text(encoding="utf-8")
if 'enableModernMapParityBlocks = getBoolean("enableModernMapParityBlocks", catBlockMisc, true,' not in config: failures.append("map parity config must default true")
main=MAIN.read_text(encoding="utf-8")
if 'ModBlocks.init();\n\t\tModernMapParityBlocks.init();\n\t\tModItems.init();' not in main: failures.append("parity registry init ordering changed")
lang=LANG.read_text(encoding="utf-8")
for name in names:
    if f"tile.etfuturum.{name}.name=" not in lang: failures.append("en_US localization missing for "+name)

# Loader-first rule: no new parity texture/model dump is allowed in the distributed resources.
for p in MC_ASSETS.rglob("*"):
    if p.is_file() and ("modern_parity" in p.parts or p.suffix.lower() in {".json"} and "1.21.11" in p.name): failures.append("modern parity asset was bundled directly: "+str(p.relative_to(ROOT)))
for forbidden in ("WorldGenerator", "IWorldGenerator", "import net.minecraft.entity.EntityLiving;"):
    if forbidden in java:
        failures.append("visual parity registry coupled to gameplay/worldgen type: " + forbidden)

# Placement-orientation state is visual block-state plumbing; EntityLivingBase is only used by
# Block.onBlockPlacedBy to recover the placing player's yaw for floor/ceiling Grindstones.
for expected in (
    'private boolean isGrindstone()',
    'if (isGrindstone()) {',
    'int face = meta / 4;',
    'int facing = meta & 3;',
    'if ("grindstone".equals(entry.getRegistryName())) {',
    'state.put("face", faces[face]);',
    'String[] faces = {"floor", "wall", "ceiling"};',
    'prepared.facingModels[face * 4 + facing] = loadBlockStateModel(entry, state);',
    'p.put("waterlogged", "sea_pickle".equals(entry.getRegistryName()) ? "true" : "false")',
    'int tintIndex = integer(face, "tintindex", -1);',
    'tintColour(entry, q, world, x, y, z)',
    'applyParityGuiCorrection(entry, model)',
):
    haystack = java + model + block_renderer + item_renderer
    if expected not in haystack:
        failures.append("fidelity-pass-4 invariant missing: " + expected)

for expected in (
    'textures/blocks/crying_obsidian.png',
    'textures/block/crying_obsidian.png',
):
    if expected not in asset_bridge:
        failures.append("modern Crying Obsidian AssetDirector override missing: " + expected)

for expected in (
    'addModelBox(out, 1, 0, 7, 15, 10, 9, 0, 12, 64, 32, texture);',
    'mirrorModelX(out)',
    'addModelBoxUv(model, bx0, by0, bz0, bx1, by1, bz1',
):
    if expected not in model:
        failures.append("special-model fidelity invariant missing: " + expected)

for expected in (
    'name.endsWith("_wall_sign")',
    'hangingSignModel(entry, attachment)',
    'signModel(entry, true)',
    'yaw = 90.0F;',
):
    haystack = java + model + item_renderer
    if expected not in haystack:
        failures.append("fidelity-pass-5 invariant missing: " + expected)

for expected in (
    '"pale_oak_leaves".equals(name)',
    '"wildflowers".equals(name) && quad.tintIndex == 0',
    '"leaf_litter".equals(name)) return dryFoliageColour(world, x, y, z)',
):
    if expected not in block_renderer:
        failures.append("modern plant tint fidelity invariant missing: " + expected)


for expected in (
    'addHangingSignChain(out, 3.0D, -45.0D',
    'addDoublePlaneUv(out, Direction.NORTH, 2, 10, 8, 14, 16, 8',
    'feeding 27 directly to getInterpolatedV()',
    'GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)',
    'GL11.glPopAttrib()',
    'getRenderBlockPass()',
):
    haystack = java + model + item_renderer + block_renderer
    if expected not in haystack:
        failures.append("fidelity-pass-6 invariant missing: " + expected)

# Fidelity Pass 7: Shelf geometry/state and segmented ground decals must come from the authored
# modern model graph. Dynamic block textures are normalized to ARGB so transparent texels survive
# the legacy atlas, and the Shelf GUI offset is read from its model instead of a guessed constant.
for expected in (
    'private boolean isShelf()',
    'private boolean isSegmentedGroundDecal()',
    '1.0F / 16.0F',
    '3.0F / 16.0F',
    'addShelfCollisionBoxes',
    'final float eleven = 11.0F / 16.0F;',
    'final float thirteen = 13.0F / 16.0F;',
    'world.getBlockMetadata(x, y, z) & 3',
    'int amountBits = world.getBlockMetadata(x, y, z) & 12;',
    'if (isSegmentedGroundDecal() || isScaffolding()) return null;',
    'style == ModernMapParityBlocks.Style.SHELF',
    'String amountProperty = "wildflowers".equals(registryName) ? "flower_amount" : "segment_amount";',
    'prepared.facingModels[(amount - 1) * 4 + facing] = loadBlockStateModel(entry, state);',
    'p.put("side_chain", "unconnected");',
    'Model segmented = models.facingModels[state];',
    'Transform authoredGui = null;',
    'authoredGui = model == null ? null : model.display.get("gui");',
    'authoredGui.translation[0]',
    'BufferedImage.TYPE_INT_ARGB',
):
    haystack = java + model + item_renderer + asset_bridge
    if expected not in haystack:
        failures.append("fidelity-pass-7 invariant missing: " + expected)

for forbidden in ('leafLitterModel()', 'wildflowersModel()', 'translateX = -5.5F / 16.0F'):
    if forbidden in model + item_renderer:
        failures.append("fidelity-pass-7 obsolete fallback still present: " + forbidden)

# Fidelity Pass 8: runtime screenshots proved that Pass 7 still rotated the Shelf's authored GUI
# translation and only fixed the Y extent of Segmented blocks. Ground-decal transparency must also
# survive colour-key PNG decoding on the user's Java 25/legacy TextureMap path.
for expected in (
    'setSegmentedGroundDecalBounds(world.getBlockMetadata(x, y, z) & 15);',
    'private void setSegmentedGroundDecalBounds(int meta)',
    'int amount = ((meta >> 2) & 3) + 1;',
    'case 1: setBlockBounds(0.5F, 0.0F, 0.0F, 1.0F, height, 0.5F);',
    'return 0;',
    'usesGroundDecalBlackTransparencyKey(modernPath)',
    '"textures/block/leaf_litter.png".equals(modernPath)',
    '"textures/block/wildflowers.png".equals(modernPath)',
    '"textures/block/wildflowers_stem.png".equals(modernPath)',
    'source.getRGB(0, 0, source.getWidth(), source.getHeight(), pixels, 0, source.getWidth());',
    'if ((pixels[i] & 0x00FFFFFF) == 0) pixels[i] = 0x00000000;',
):
    haystack = java + asset_bridge
    if expected not in haystack:
        failures.append("fidelity-pass-8 segmented/alpha invariant missing: " + expected)

shelf_translation = item_renderer.find('if (authoredGui != null)')
shelf_yaw = item_renderer.find('if (yaw != 0.0F)')
if shelf_translation < 0 or shelf_yaw < 0 or shelf_translation > shelf_yaw:
    failures.append("fidelity-pass-8 Shelf authored GUI translation must be applied before compatibility yaw")
for expected in (
    '(float) (authoredGui.translation[0] / 16.0D)',
    '(float) (authoredGui.translation[1] / 16.0D)',
    '(float) (authoredGui.translation[2] / 16.0D)',
):
    if expected not in item_renderer:
        failures.append("fidelity-pass-8 Shelf direct GUI-axis translation missing: " + expected)
for forbidden in (
    '(float) (-authoredGui.translation[2] / 16.0D)',
    'return "leaf_litter".equals(name) || "wildflowers".equals(name) ? 1 : 0;',
):
    if forbidden in java + item_renderer:
        failures.append("fidelity-pass-8 rejected runtime behaviour still present: " + forbidden)

# Fidelity Pass 9: segmented decorations must expose their real 1-4 gameplay states, Wildflowers'
# horizontal flowerbed UVs must align with the authored stem geometry, and Leaf Litter must use the
# actual modern dry-foliage colormap rather than Pass 8's fixed-tan approximation.
for expected in (
    'correctSegmentedGroundDecalTopUv(entry, model);',
    'private static void correctSegmentedGroundDecalTopUv(ModernMapParityBlocks entry, Model model)',
    'if (!"leaf_litter".equals(name) && !"wildflowers".equals(name)) return;',
    'if (!isUpFacingHorizontalQuad(quad)) continue;',
    'quad.uv[0] = quad.uv[2];',
    'quad.uv[1] = quad.uv[3];',
    'private static boolean isUpFacingHorizontalQuad(Quad quad)',
):
    if expected not in model:
        failures.append("fidelity-pass-9 segmented UV invariant missing: " + expected)

for expected in (
    'public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side,',
    'held.getItem() != Item.getItemFromBlock(this)',
    'int amount = ((meta >> 2) & 3) + 1;',
    'if (amount >= 4) return false;',
    'world.setBlockMetadataWithNotify(x, y, z, (meta & 3) | (amount << 2), 3);',
    'public int quantityDropped(int meta, int fortune, Random random)',
    'return ((meta >> 2) & 3) + 1;',
    'isSegmentedGroundDecal() || isCandle() || isTurtleEgg() || isCampfire() || isScaffolding()',
):
    if expected not in java:
        failures.append("fidelity-pass-9 segmented state invariant missing: " + expected)

for expected in (
    'dryFoliageColour(world, x, y, z)',
    'new ResourceLocation(Tags.MC_ASSET_VER, "textures/colormap/dry_foliage.png")',
    'biome.getFloatTemperature(x, y, z)',
    'biome.getIntRainfall()',
    'downfall *= temperature;',
    'dryFoliagePixels = pixels;',
):
    if expected not in block_renderer:
        failures.append("fidelity-pass-9 dry-foliage tint invariant missing: " + expected)
if 'if ("leaf_litter".equals(name)) return 0xD6AA74;' in block_renderer:
    failures.append("fidelity-pass-9 obsolete fixed Leaf Litter tint still active")

# Fidelity Pass 10: mechanics parity for stackable/lightable blocks, campfires, signs, scaffolding and copper chest sounds.
parity_tesr = PARITY_TESR.read_text(encoding="utf-8") if PARITY_TESR.exists() else ""
sign_gui = SIGN_GUI.read_text(encoding="utf-8")
client_events = CLIENT_EVENTS.read_text(encoding="utf-8")
for expected in (
    'style == Style.CANDLE || style == Style.CANDLE_CAKE',
    'GameRegistry.registerTileEntity(ParitySignTileEntity.class',
    'GameRegistry.registerTileEntity(ParityCampfireTileEntity.class',
    'class ParityCampfireTileEntity extends TileEntity',
    'private static final int COOK_TIME = 600;',
    'private final ItemStack[] cookingItems = new ItemStack[4];',
    'if (isCandle()) return (meta & 4) != 0 ? ((meta & 3) + 1) * 3 : 0;',
    'if (isCampfire()) return (meta & 4) != 0 ? (isSoulCampfire() ? 10 : 15) : 0;',
    'if (isCandle() || isTurtleEgg()) return (meta & 3) + 1;',
    'private int computeScaffoldingMeta(IBlockAccess world, int x, int y, int z)',
    'return !isScaffolding() && super.isLadder(world, x, y, z, entity);',
    'world.scheduleBlockUpdate(x, y, z, this, 1);',
    'openParitySignEditor(world, x, y, z',
):
    if expected not in java:
        failures.append("fidelity-pass-10 mechanics invariant missing: " + expected)
for expected in (
    'state.put("candles", Integer.toString(count));',
    'prepared.facingModels[(lit << 2) | (count - 1)]',
    'state.put("eggs", Integer.toString(eggs));',
    'prepared.facingModels[(hatch << 2) | (eggs - 1)]',
    '"campfire".equals(registryName) || "soul_campfire".equals(registryName)',
    'state.put("bottom", Boolean.toString(bottom != 0));',
):
    if expected not in model:
        failures.append("fidelity-pass-10 model-state invariant missing: " + expected)
for expected in (
    'class Sign extends TileEntitySpecialRenderer',
    'class Campfire extends TileEntitySpecialRenderer',
    'campfire.getCookingItem(slot)',
):
    if expected not in parity_tesr:
        failures.append("fidelity-pass-10 tile renderer invariant missing: " + expected)
# Pass 12 keeps the vanilla 1.7 sign-editing controls/packet flow but routes parity signs through
# EFR's proven editor wrapper so hanging-sign keyboard edits are actually visible and accepted.
sign_handler = (ROOT / "src/main/java/ganymedes01/etfuturum/network/WoodSignOpenHandler.java").read_text(encoding="utf-8")
for expected in (
    'new GuiEditWoodSign((TileEntityWoodSign) tileEntity, message.back)',
    'C12 packet flow while now also understanding parity models',
):
    if expected not in sign_handler:
        failures.append("fidelity-pass-12 parity sign editor routing invariant missing: " + expected)
for expected in (
    'ModernMapParityBlocks.fromBlock(tileSign.getBlockType())',
    'parity.getStyle() == ModernMapParityBlocks.Style.HANGING_SIGN',
    'ChatAllowedCharacters.isAllowedCharacter(typedChar)',
    'new WoodSignUpdateMessage(tileSign.xCoord, tileSign.yCoord, tileSign.zCoord',
):
    if expected not in sign_gui:
        failures.append("fidelity-pass-12 parity sign editor invariant missing: " + expected)
if '0xFF7A5A38' in sign_gui or 'drawRect(left, top, left + 150' in sign_gui:
    failures.append("fidelity-pass-12 obsolete hand-drawn parity sign popup still present")
for expected in (
    'blockID.contains("copper_chest")',
    '"copper_chest_oxidized"',
    '"copper_chest_weathered"',
):
    if expected not in client_events:
        failures.append("fidelity-pass-10 copper chest sound invariant missing: " + expected)
for expected in (
    'block.campfire.crackle', 'block.candle.extinguish', 'item.flintandsteel.use', 'item.firecharge.use',
    'block.copper_chest.open', 'block.copper_chest_weathered.open', 'block.copper_chest_oxidized.open',
):
    if expected not in MAIN.read_text(encoding="utf-8"):
        failures.append("fidelity-pass-10 requested sound asset missing: " + expected)


# Fidelity Pass 11/12: runtime corrections from mechanics testing.
for expected in (
    'boolean bottom = distance > 0 && below != this;',
    'entity.motionY = -0.15D;',
    'EtFuturum.proxy.isScaffoldingJumpHeld(player)',
    'entity.motionY = 0.18D;',
    'spawnCandleFlames(world, x, y, z, (meta & 3) + 1, random);',
):
    if expected not in java:
        failures.append("fidelity-pass-11 runtime mechanics invariant missing: " + expected)
for expected in (
    'setHardness(isScaffolding() ? 0.0F',
    'block.scaffolding.break',
    'block.scaffolding.place',
    'feet >= y + 1.0D - 1.0E-5D',
    'distance > 0 && bottom && feet >= y + 2.0D / 16.0D - 1.0E-5D',
    '2.0F/16.0F',
    'new CandleFlameFX(world, x, y, z)',
    'baseScale = particleScale * 0.50F',
    'direction = horizontalFacing(player);',
    'direction = ForgeDirection.UP;',
):
    if expected not in java:
        failures.append("fidelity-pass-12 runtime correction invariant missing: " + expected)
for expected in (
    'compactCampfireFlames(campfire);',
    'quad.texture.indexOf("campfire_fire") < 0',
    '(vertex.y - base) * 0.90D',
):
    if expected not in model:
        failures.append("fidelity-pass-11 campfire flame invariant missing: " + expected)
for expected in (
    'Minecraft.getMinecraft().currentScreen instanceof GuiEditSign',
    'Minecraft.getMinecraft().currentScreen instanceof GuiEditWoodSign',
    '0.015625F * (hanging ? 0.9F : 0.6666667F)',
    '{0.22D, 0.455D, 0.22D}',
    'GL11.glScalef(0.55F, 0.55F, 0.55F);',
    'rotateQuadrant(',
):
    if expected not in parity_tesr:
        failures.append("fidelity-pass-11 renderer invariant missing: " + expected)

crying = ROOT / 'src/main/resources/assets/minecraft/textures/blocks/crying_obsidian.png'
if crying.exists():
    failures.append("legacy bundled Crying Obsidian must be migrated to the AssetDirector modern texture")

# P013 editor-preview correction: editor text/cursor must render in front of the JSON board.
sign_renderer = parity_tesr
for expected in (
    "if (nativeEditorPreview) GL11.glDisable(GL11.GL_DEPTH_TEST);",
    "if (nativeEditorPreview) GL11.glEnable(GL11.GL_DEPTH_TEST);",
):
    if expected not in sign_renderer:
        failures.append("P013 sign editor depth correction invariant missing: " + expected)


# P014 sign-face correction: modern hanging signs use a narrower 60px text area and
# a larger 0.9 text scale; both sign families need their text safely in front of the board.
for expected in (
    "hanging ? 0.078F : -0.050F",
    "0.015625F * (hanging ? 0.9F : 0.6666667F)",
    "int lineHeight = hanging ? 9 : 10;",
):
    if expected not in sign_renderer:
        failures.append("P014 sign text-plane/scale invariant missing: " + expected)
for expected in (
    "int maxCharacters = hanging ? 13 : 15;",
    "int maxPixelWidth = hanging ? 60 : 90;",
    "fontRendererObj.getStringWidth(candidate) <= maxPixelWidth",
):
    if expected not in sign_gui:
        failures.append("P014 sign editor width invariant missing: " + expected)


# P015 hanging-sign correction: text must sit on the visible front face and non-wall hanging
# signs must retain both attachment type and player-facing orientation instead of staying N/S.
for expected in (
    "hanging ? 0.078F : -0.050F",
    "private static float hangingYaw(int meta)",
    "return ((meta >> 1) & 3) * 90.0F;",
):
    if expected not in sign_renderer:
        failures.append("P015 hanging-sign renderer invariant missing: " + expected)
for expected in (
    "attachment | (facing << 1)",
    "return 8 + (side - 2);",
    "meta >= 8",
):
    if expected not in java:
        failures.append("P015 hanging-sign placement/orientation invariant missing: " + expected)
for expected in (
    "rotateModelY(ceiling, facing * 90);",
    "prepared.facingModels[(facing << 1) | attachment] = ceiling;",
    "prepared.facingModels[8 + (side - 2)] = wall;",
):
    if expected not in model:
        failures.append("P015 hanging-sign model-state invariant missing: " + expected)

# P016 sign-system correction: E/W ceiling text uses the same transform convention as the JSON
# geometry, wall hanging signs project perpendicular to their support with bracket-only collision,
# and all EFR/parity signs persist independent front/back text plus modern modifiers.
wood_sign = WOOD_SIGN.read_text(encoding="utf-8")
wood_sign_tesr = WOOD_SIGN_TESR.read_text(encoding="utf-8")
wood_sign_te = WOOD_SIGN_TE.read_text(encoding="utf-8")
sign_update = SIGN_UPDATE.read_text(encoding="utf-8")
mod_items = MOD_ITEMS.read_text(encoding="utf-8")
for expected in (
    "float worldYaw = hanging && !hangingWallForm ? -stateYaw : stateYaw;",
    "renderTextFace(font, sign, false",
    "renderTextFace(font, sign, true",
    "sign.isEditingBack()",
):
    if expected not in sign_renderer:
        failures.append("P016 two-sided/EW sign renderer invariant missing: " + expected)
for expected in (
    "perpendicularWallHangingFacing(clickedSide)",
    "placer.isSneaking() ? 1",
    "y + 14.0D / 16.0D",
    '"block." + hangingSignSoundFamily() + ".hit"',
    '"block." + hangingSignSoundFamily() + ".fall"',
    "boolean back = signBackSide",
    "tryApplySignModifier",
):
    if expected not in java:
        failures.append("P016 hanging-sign placement/collision/interaction invariant missing: " + expected)
for expected in (
    "private final String[] backText",
    "FrontTextColour", "BackTextColour", "FrontGlowing", "BackGlowing", "Waxed",
    "return !waxed;",
):
    if expected not in wood_sign_te:
        failures.append("P016 two-sided sign persistence invariant missing: " + expected)
for expected in (
    "boolean back = isBackSide",
    "sign.setWaxed(true)",
    "sign.setGlowing(back, true)",
    "sign.setTextColour(back, colour)",
    '"item.glow_ink_sac.use"',
    '"item.dye.use"',
):
    if expected not in wood_sign:
        failures.append("P016 legacy EFR sign modifier invariant missing: " + expected)
for expected in (
    "renderFace(font, sign, false",
    "renderFace(font, sign, true",
    "sign.getTextColour(back)",
    "sign.isGlowing(back)",
):
    if expected not in wood_sign_tesr:
        failures.append("P016 legacy EFR two-sided renderer invariant missing: " + expected)
for expected in (
    "sign.func_145911_b() != player",
    "maxCharacters = parity != null && parity.getStyle() == ModernMapParityBlocks.Style.HANGING_SIGN ? 13 : 15",
    "sign.setText(message.back, clean)",
):
    if expected not in sign_update:
        failures.append("P016 sign update authority/limit invariant missing: " + expected)
if 'GLOW_INK_SAC(ConfigBlocksItems.enableModernMapParityBlocks, new BaseItem("glow_ink_sac"))' not in mod_items:
    failures.append("P016 Glow Ink Sac registration invariant missing")
if 'textures/items/glow_ink_sac.png' not in bridge or 'textures/item/glow_ink_sac.png' not in bridge:
    failures.append("P016 Glow Ink Sac AssetDirector alias invariant missing")

# P018 sound completeness + literal vanilla-oak sign bridge. Direct modern sound events that are
# played outside ModSounds must be explicitly requested from AssetDirector, and the removed
# 1.21.11 leash-knot event names must not return. Literal 1.7 oak signs are upgraded in-place to
# the same two-sided/wax/dye/glow tile implementation without replacing their block IDs.
server_events = SERVER_EVENTS.read_text(encoding="utf-8")
main_source = MAIN.read_text(encoding="utf-8")
for expected in (
    'config.addSoundEvent(ver, "item.dye.use", "player")',
    'config.addSoundEvent(ver, "item.glow_ink_sac.use", "player")',
    'config.addSoundEvent(ver, "item.ink_sac.use", "player")',
    'config.addSoundEvent(ver, "block.sign.waxed_interact_fail", "block")',
    '"scaffolding",',
    '"hanging_sign",',
    '"nether_wood_hanging_sign",',
    '"bamboo_wood_hanging_sign",',
    '"cherry_wood_hanging_sign"',
    'new String[] {"break", "step", "place", "hit", "fall"}',
    'config.addSoundEvent(ver, "item.lead.break", "player")',
    'config.addSoundEvent(ver, "item.lead.tied", "player")',
    'config.addSoundEvent(ver, "item.lead.untied", "player")',
    'config.addSoundEvent(ver, "block.lily_pad.place", "block")',
    'if (sound == ModSounds.soundPainting) continue;',
):
    if expected not in main_source:
        failures.append("P018 direct modern sound registration invariant missing: " + expected)
for removed in ('entity.leash_knot.break', 'entity.leash_knot.place'):
    if removed in main_source or ('"' + removed + '"') in server_events:
        failures.append("P018 removed 1.21.11 sound event returned: " + removed)
dynamic_sounds = DYNAMIC_SOUNDS.read_text(encoding="utf-8")
if 'config.addSoundEvent(ver, "block.hanging_sign.waxed_interact_fail", "block")' in main_source:
    failures.append("P019 hanging-sign wax event alias is still being handed to AssetDirector as a direct OGG event")
for expected in (
    'ImmutableSet.of("minecraft", Tags.MC_ASSET_VER)',
    'addSoundEventsToCategory("block.hanging_sign.waxed_interact_fail"',
    '"block.sign.waxed_interact_fail"',
    'soundObj.add("type", new JsonPrimitive("event"))',
):
    if expected not in dynamic_sounds:
        failures.append("P019 modern sound-event alias invariant missing: " + expected)
if 'Tags.MC_ASSET_VER + ":block.sign.waxed_interact_fail"' in dynamic_sounds:
    failures.append("P025 hanging-sign type=event target must stay bare for 1.7 same-domain resolution")
for expected in (
    'installVanillaSignTileEntityCompatibility()',
    'tileClassField.set(Blocks.standing_sign, TileEntityWoodSign.class)',
    'tileClassField.set(Blocks.wall_sign, TileEntityWoodSign.class)',
    'upgradeVanillaSignTile(World world, int x, int y, int z)',
    'handleVanillaSignActivation(World world, int x, int y, int z, EntityPlayer player)',
    'tryApplyModifierCommon(world, x, y, z, player, held, sign, back)',
):
    if expected not in wood_sign:
        failures.append("P018 vanilla oak sign compatibility invariant missing: " + expected)
for expected in (
    'Blocks.standing_sign',
    'Blocks.wall_sign',
    'new ResourceLocation("textures/entity/sign.png")',
):
    if expected not in wood_sign_tesr:
        failures.append("P018 vanilla oak sign renderer invariant missing: " + expected)
for expected in (
    'boolean vanillaSign = tileSign.getBlockType() == Blocks.standing_sign || tileSign.getBlockType() == Blocks.wall_sign;',
    'tileSign.getBlockType() == Blocks.standing_sign',
):
    if expected not in sign_gui:
        failures.append("P018 vanilla oak sign editor invariant missing: " + expected)
for expected in (
    'BlockWoodSign.upgradeVanillaSignTile',
    'BlockWoodSign.handleVanillaSignActivation',
    'Tags.MC_ASSET_VER + ":item.lead.break"',
    'placementSound = "item.lead.tied"',
    'Tags.MC_ASSET_VER + ":item.lead.untied"',
):
    if expected not in server_events:
        failures.append("P018 server sign/sound bridge invariant missing: " + expected)
if 'installVanillaSignTileEntityCompatibility();' not in main_source:
    failures.append("P018 vanilla sign tile factory is not installed during preInit")

armor_sounds = ARMOR_SOUNDS.read_text(encoding="utf-8")
bubble_column = BUBBLE_COLUMN.read_text(encoding="utf-8")
if 'item.armor.equip_turtle_helmet' in armor_sounds:
    failures.append("P018 obsolete turtle-helmet sound id remains")
if 'Tags.MC_ASSET_VER + ":item.armor.equip_turtle"' not in armor_sounds:
    failures.append("P018 modern turtle armour equip sound invariant missing")
if 'setBlockTextureName("minecraft:water_still")' not in bubble_column:
    failures.append("P018 Bubble Column atlas fallback still relies on a missing block icon")
assets_root = ROOT / "src/main/resources/assets"
if (assets_root / "README.md").exists():
    failures.append("P018 resource-pack namespace warning remains: assets/README.md")
if not (ROOT / "src/main/resources/META-INF/etfuturum-asset-credits.md").is_file():
    failures.append("P018 relocated asset credits are missing")
for stale in ('entity.leash_knot.break', 'entity.leash_knot.place'):
    if stale in bubble_column:
        failures.append("P018 stale runtime sound token remains in Bubble Column: " + stale)

if failures:
    print("Modern 1.21.11 visual block parity validation FAILED")
    for f in failures: print(" -",f)
    sys.exit(1)
print("Modern 1.21.11 visual block parity validation PASSED")
print(f" - {len(names)} stable modern registry identities remain available for map conversion")
print(" - placed blocks resolve real 1.21.11 blockstate/model JSON through AssetDirector")
print(" - inventory/held items resolve real 1.21.11 item definitions/models separately")
print(" - model parents, elements, faces, UVs, rotations, variants and multipart defaults are bridged to the 1.7 renderer")
print(" - generated item sprites stay flat in inventory while 3D item models use modern display transforms")
print(" - placed-model lighting samples exterior/neighbor light from rotated quad geometry")
print(" - Copper Bars/walls/fences/panes resolve connected multipart states and Lightning Rods resolve six facings")
print(" - Copper Chests reuse vanilla chest inventory/lid behavior with stage-correct AssetDirector textures")
print(" - entity-style visuals retain dedicated adapters for pots, conduits, bells, hanging signs and copper golem statues")
print(" - hanging-sign attachment UVs, decorated-pot base UV bounds and item GL-state isolation remain guarded")
print(" - Shelves use facing-aware authored models, exact three-prism collision and the model-authored GUI translation")
print(" - Leaf Litter/Wildflowers resolve 1-4 facing states with stackable amounts and colour-key-safe ARGB cutouts")
print(" - Wildflowers flowerbed UVs stay aligned to stem geometry and Leaf Litter samples the real modern dry-foliage colormap")
print(" - discovered Mojang textures are dynamically stitched and rebound on every atlas rebuild")
print(" - no parity PNG/model JSON files are redistributed")
print(" - Pass 10 mechanics gates cover candle/turtle-egg stacking, campfires, writable signs, scaffolding and copper-chest sounds")
print(" - Pass 11 runtime gates cover campfire alignment and baseline candle/scaffolding/sign mechanics")
print(" - Pass 12 gates cover half-scale candle flames, parity sign typing, scaffold collision context, break speed and modern sounds")
print(" - Pass 13 gates keep scaffold ascent input-driven, improve extension/overhang collision, and keep parity sign editor text visible")
print(" - Pass 14 gates enforce modern 90px/60px sign line widths with a 13-character hanging-sign ceiling")
print(" - Pass 15 gates put hanging-sign text on the visible face and preserve attachment plus four-way ceiling orientation")
print(" - Pass 16 gates add E/W-aligned hanging geometry/text, sideways wall mounting, two-sided editing, wax/dye/glow and hanging-sign sounds")
print(" - Pass 18 gates request direct modern sign/scaffold/lead/lily-pad sounds, fix turtle armour audio, keep Bubble Column on a real water atlas icon, and extend two-sided modifiers to literal vanilla oak signs")
print(" - no parity PNG/model JSON files are bundled; no Campfire Backport source is vendored")
