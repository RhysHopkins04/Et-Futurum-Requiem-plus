#!/usr/bin/env python3
"""Cumulative static regression checks for Et Futurum Requiem Plus content/map-compat patches."""

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
failures = []


def require(path, text=None):
    target = ROOT / path
    if not target.is_file():
        failures.append(f"missing required file: {path}")
        return ""
    data = target.read_text(encoding="utf-8", errors="replace") if target.suffix != ".png" else ""
    if text is not None and text not in data:
        failures.append(f"{path}: missing expected text: {text}")
    return data


def require_png(path):
    target = ROOT / path
    if not target.is_file():
        failures.append(f"missing required texture: {path}")
        return
    data = target.read_bytes()
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        failures.append(f"invalid PNG signature: {path}")


config = require(
    "src/main/java/ganymedes01/etfuturum/configuration/configs/ConfigMapCompatibility.java",
    'getBoolean("mapCompatibilityMode", CATEGORY, false',
)
for expected in (
    "ConfigModCompat.moddedRawOres = false;",
    "ConfigModCompat.moddedDeepslateOres = false;",
    "ConfigFunctions.registerRawItemAsOre = false;",
    "ConfigMixins.enableElytra = false;",
    "ConfigEntities.enableShulker = false;",
    "ConfigBlocksItems.enableShulkerBoxes = false;",
    "ConfigExperiments.netherDimensionProvider = false;",
    "ConfigExperiments.endDimensionProvider = false;",
    "ConfigExperiments.enableEndCities = false;",
    "ConfigWorld.tileReplacementMode = -1;",
):
    if expected not in config:
        failures.append(f"map compatibility overlay missing: {expected}")

base = require("src/main/java/ganymedes01/etfuturum/configuration/ConfigBase.java")
if 'new ConfigMapCompatibility(createConfigFile("mapcompat"))' not in base:
    failures.append("mapcompat config file is not registered")
if base.count("ConfigMapCompatibility.applyCompatibilityOverrides();") < 3:
    failures.append("mapcompat runtime overlay is not reapplied at all required config phases")

etf = require("src/main/java/ganymedes01/etfuturum/EtFuturum.java")
if "if (!ConfigMapCompatibility.isEnabled()) {\n\t\t\tGameRegistry.registerWorldGenerator" not in etf:
    failures.append("Et Futurum world-generator registration is not mapcompat-gated")
for expected in ("DeepslateOreRegistry.init();", "RawOreRegistry.init();", "SmithingTableRecipes.init();"):
    if expected not in etf:
        failures.append(f"expected upstream integration call disappeared: {expected}")

mixins = require("src/main/java/ganymedes01/etfuturum/mixinplugin/EtFuturumEarlyMixins.java")
if 'if (!ConfigMapCompatibility.isEnabled()) {\n\t\t\tmixins.add("deepslateores.MixinChunk");' not in mixins:
    failures.append("deepslate ore-generation mixin is not mapcompat-gated")

proxy = require("src/main/java/ganymedes01/etfuturum/core/proxy/CommonProxy.java")
if "MinecraftForge.TERRAIN_GEN_BUS.register(WorldEventHandler.INSTANCE);" not in proxy or "ConfigMapCompatibility.isEnabled()" not in proxy:
    failures.append("terrain-generation event registration guard is missing")

recipes = require("src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java")
if "if (ConfigMapCompatibility.isEnabled()) {\n\t\t\treturn;\n\t\t}\n\n\t\tregisterRecipes();" not in recipes:
    failures.append("automatic recipe/loot registration is not mapcompat-gated")

server = require("src/main/java/ganymedes01/etfuturum/core/handlers/ServerEventHandler.java")
if server.count("public void naturalSpawnEvent") != 2 or server.count("if (ConfigMapCompatibility.isEnabled())") < 2:
    failures.append("natural-spawn event guards are incomplete")

blocks = require("src/main/java/ganymedes01/etfuturum/ModBlocks.java")
for registry in (
    "ROOTED_DIRT(ConfigBlocksItems.enableLushCaveBlocks, new BlockRootedDirt())",
    "HANGING_ROOTS(ConfigBlocksItems.enableLushCaveBlocks, new BlockHangingRoots())",
    "SMALL_DRIPLEAF(ConfigBlocksItems.enableLushCaveBlocks, new BlockSmallDripleaf())",
    "BIG_DRIPLEAF_STEM(ConfigBlocksItems.enableLushCaveBlocks, new BlockBigDripleafStem(), null)",
    "BIG_DRIPLEAF(ConfigBlocksItems.enableLushCaveBlocks, new BlockBigDripleaf())",
    "MOSS_BLOCK(ConfigBlocksItems.enableLushCaveBlocks, new BlockMoss())",
    "MOSS_CARPET(ConfigBlocksItems.enableLushCaveBlocks, new BlockMossCarpet())",
    "AZALEA(ConfigBlocksItems.enableLushCaveBlocks, new BlockAzalea())",
    "AZALEA_LEAVES(ConfigBlocksItems.enableLushCaveBlocks, new BlockAzaleaLeaves())",
    "SPORE_BLOSSOM(ConfigBlocksItems.enableLushCaveBlocks, new BlockSporeBlossom())",
):
    if registry not in blocks:
        failures.append(f"missing modern registry entry: {registry}")

require("src/main/java/ganymedes01/etfuturum/blocks/BlockRootedDirt.java", 'setNames("rooted_dirt")')
require("src/main/java/ganymedes01/etfuturum/blocks/BlockHangingRoots.java", 'Utils.getUnlocalisedName("hanging_roots")')
small = require("src/main/java/ganymedes01/etfuturum/blocks/BlockSmallDripleaf.java", 'Utils.getUnlocalisedName("small_dripleaf")')
big = require("src/main/java/ganymedes01/etfuturum/blocks/BlockBigDripleaf.java", 'Utils.getUnlocalisedName("big_dripleaf")')
stem = require("src/main/java/ganymedes01/etfuturum/blocks/BlockBigDripleafStem.java", 'Utils.getUnlocalisedName("big_dripleaf_stem")')
for expected in ("UPPER_BIT = 4", "implements IGrowable", "BIG_DRIPLEAF_STEM.get()", "BIG_DRIPLEAF.get()"):
    if expected not in small:
        failures.append(f"small dripleaf implementation missing: {expected}")
for expected in ("TILT_NONE = 0", "TILT_UNSTABLE = 1", "TILT_PARTIAL = 2", "TILT_FULL = 3",
                 "world.scheduleBlockUpdate(x, y, z, this, 10)", "world.scheduleBlockUpdate(x, y, z, this, 100)",
                 "world.isBlockIndirectlyGettingPowered", "instanceof IProjectile", "growOne"):
    if expected not in big:
        failures.append(f"big dripleaf implementation missing: {expected}")
if "implements IGrowable" not in stem or "Item.getItemFromBlock(ModBlocks.BIG_DRIPLEAF.get())" not in stem:
    failures.append("big dripleaf stem is missing non-item/growth/drop support behaviour")

render_ids = require("src/main/java/ganymedes01/etfuturum/lib/RenderIDs.java")
client_proxy = require("src/main/java/ganymedes01/etfuturum/core/proxy/ClientProxy.java")
for expected in ("SMALL_DRIPLEAF", "BIG_DRIPLEAF"):
    if expected not in render_ids:
        failures.append(f"missing dripleaf render ID: {expected}")
for expected in ("BlockSmallDripleafRenderer(RenderIDs.SMALL_DRIPLEAF)", "BlockBigDripleafRenderer(RenderIDs.BIG_DRIPLEAF)"):
    if expected not in client_proxy:
        failures.append(f"missing dripleaf renderer registration: {expected}")
small_renderer = require("src/main/java/ganymedes01/etfuturum/client/renderer/block/BlockSmallDripleafRenderer.java", "class BlockSmallDripleafRenderer")
big_renderer = require("src/main/java/ganymedes01/etfuturum/client/renderer/block/BlockBigDripleafRenderer.java", "class BlockBigDripleafRenderer")
for expected in ("2.99D * P", "renderLeafPlate", "renderSmallStem", "uvPixels"):
    if expected not in small_renderer:
        failures.append(f"small dripleaf fidelity renderer missing: {expected}")
for expected in ("angle = -22.5D", "angle = -45.0D", "renderStem", "uvRect(leaf.getTipIcon(), 0, 4, 16, 0)",
                 "uvRect(leaf.getSideIcon(), 0, 4, 16, 0)", "Vanilla's UNSTABLE state deliberately uses the upright model"):
    if expected not in big_renderer:
        failures.append(f"big dripleaf fidelity renderer missing: {expected}")
if "if (tilt == TILT_FULL)" not in big:
    failures.append("big dripleaf full-only non-solid collision gate is missing")
projectile_pos = big.find("if (entity instanceof IProjectile)")
power_pos = big.find("if (world.isBlockIndirectlyGettingPowered(x, y, z))", projectile_pos)
if projectile_pos < 0 or power_pos < 0 or projectile_pos > power_pos:
    failures.append("big dripleaf projectile tilt must bypass the normal redstone hold gate")

# P003 -- lush-cave content completion. Moss/azalea are no longer experimental and
# are owned by the default-enabled lush-cave content family. Natural lush-cave generation
# remains intentionally deferred to the dedicated world-generation patch.
lush_config = require("src/main/java/ganymedes01/etfuturum/configuration/configs/ConfigBlocksItems.java")
if 'enableLushCaveBlocks = getBoolean("enableLushCaveBlocks", catBlockNatural, true' not in lush_config:
    failures.append("lush-cave content family is not enabled by default")

experiments = require("src/main/java/ganymedes01/etfuturum/configuration/configs/ConfigExperiments.java")
if "public static boolean enableMossAzalea" in experiments:
    failures.append("legacy experimental enableMossAzalea field still exists")
if 'getCategory(catExperiments).remove("enableMossAzalea")' not in experiments:
    failures.append("legacy enableMossAzalea config migration/removal is missing")
if "ConfigExperiments.enableMossAzalea" in blocks:
    failures.append("moss/azalea registry entries are still gated by the experimental toggle")

azalea = require("src/main/java/ganymedes01/etfuturum/blocks/BlockAzalea.java", "implements ISubBlocksBlock, IGrowable")
for expected in ("rand.nextFloat() < 0.45F", "new WorldGenAzaleaTree(true)", "ModBlocks.MOSS_BLOCK", "ModBlocks.ROOTED_DIRT"):
    if expected not in azalea:
        failures.append(f"azalea growth/support implementation missing: {expected}")

azalea_tree = require("src/main/java/ganymedes01/etfuturum/world/generate/decorate/WorldGenAzaleaTree.java", "class WorldGenAzaleaTree")
for expected in ("4 + rand.nextInt(3)", "Blocks.log, 0", "ModBlocks.ROOTED_DIRT", "ModBlocks.AZALEA_LEAVES", "rand.nextInt(4) == 0 ? 1 : 0"):
    if expected not in azalea_tree:
        failures.append(f"azalea tree implementation missing: {expected}")

moss = require("src/main/java/ganymedes01/etfuturum/blocks/BlockMoss.java", "implements IGrowable")
for expected in ("dx = -3", "dz = -3", "Math.abs(dx) == 3 && Math.abs(dz) == 3", "originY + 5", "originY - 5",
                 "rand.nextFloat() < 0.60F", "rand.nextInt(96)", "roll < 50", "roll < 60", "roll < 85", "roll < 92"):
    if expected not in moss:
        failures.append(f"moss spreading/vegetation implementation missing: {expected}")

moss_carpet = require("src/main/java/ganymedes01/etfuturum/blocks/BlockMossCarpet.java")
if "(double) y + 0.0625D" not in moss_carpet:
    failures.append("moss carpet is missing its 1/16-block collision height")

spore = require("src/main/java/ganymedes01/etfuturum/blocks/BlockSporeBlossom.java", "class BlockSporeBlossom")
for expected in ("RenderIDs.SPORE_BLOSSOM", "ForgeDirection.DOWN", "getCollisionBoundingBoxFromPool",
                 "randomDisplayTick", "spawnSporeBlossomParticle", 'reg.registerIcon("spore_blossom")',
                 'reg.registerIcon("spore_blossom_base")'):
    if expected not in spore:
        failures.append(f"spore blossom implementation missing: {expected}")

spore_fx = require("src/main/java/ganymedes01/etfuturum/client/particle/SporeBlossomFX.java", "class SporeBlossomFX")
for expected in ("ambientHorizontalMotion", "ambientVerticalMotion", 'minecraft:textures/particle/drip_fall.png'):
    if expected not in spore_fx:
        failures.append(f"spore blossom particle implementation missing: {expected}")

spore_renderer = require("src/main/java/ganymedes01/etfuturum/client/renderer/block/BlockSporeBlossomRenderer.java", "class BlockSporeBlossomRenderer")
for expected in ("15.9 * P", "15.7 * P", "24 * P", "-8 * P", "-22.5D", "22.5D", "emitDoubleSided"):
    if expected not in spore_renderer:
        failures.append(f"spore blossom fidelity renderer missing: {expected}")
if "SPORE_BLOSSOM" not in render_ids:
    failures.append("missing spore blossom render ID")
if "BlockSporeBlossomRenderer(RenderIDs.SPORE_BLOSSOM)" not in client_proxy:
    failures.append("missing spore blossom renderer registration")

particles = require("src/main/java/ganymedes01/etfuturum/client/particle/CustomParticles.java")
if "spawnSporeBlossomParticle" not in particles:
    failures.append("spore blossom particle factory is not registered")

for texture in (
    "rooted_dirt.png", "hanging_roots.png",
    "small_dripleaf_top.png", "small_dripleaf_side.png", "small_dripleaf_stem_top.png", "small_dripleaf_stem_bottom.png",
    "big_dripleaf_top.png", "big_dripleaf_side.png", "big_dripleaf_tip.png", "big_dripleaf_stem.png",
):
    require_png(f"src/main/resources/assets/minecraft/textures/blocks/{texture}")

for texture in ("spore_blossom.png", "spore_blossom_base.png"):
    require_png(f"src/main/resources/assets/minecraft/textures/blocks/{texture}")
require_png("src/main/resources/assets/minecraft/textures/particle/drip_fall.png")

lang = require("src/main/resources/assets/etfuturum/lang/en_US.lang")
for expected in (
    "tile.etfuturum.small_dripleaf.name=Small Dripleaf",
    "tile.etfuturum.big_dripleaf.name=Big Dripleaf",
    "tile.etfuturum.big_dripleaf_stem.name=Big Dripleaf Stem",
    "tile.etfuturum.spore_blossom.name=Spore Blossom",
):
    if expected not in lang:
        failures.append(f"missing dripleaf language entry: {expected}")

compost = require("src/main/java/ganymedes01/etfuturum/api/CompostingRegistry.java")
for expected in ("ModBlocks.SMALL_DRIPLEAF.newItemStack()", "ModBlocks.BIG_DRIPLEAF.newItemStack()"):
    if expected not in compost:
        failures.append(f"missing dripleaf composting entry: {expected}")
if "ModBlocks.SPORE_BLOSSOM.newItemStack()" not in compost:
    failures.append("missing spore blossom composting entry")

if 'ModBlocks.MOSS_CARPET.newItemStack(3)' not in recipes or 'ModBlocks.MOSS_BLOCK.newItemStack()' not in recipes:
    failures.append("modern 2 moss blocks -> 3 moss carpet recipe is missing")

# P004 -- natural Lush Cave world generation. Minecraft 1.7.10 has no vertical/3D
# biome storage, so the backport must use deterministic underground regions while leaving the
# surface biome untouched. The entire generator is disabled by Map Compatibility Mode.
world_config = require("src/main/java/ganymedes01/etfuturum/configuration/configs/ConfigWorld.java")
for expected in (
    'lushCavesWorldgen = getBoolean("lushCavesWorldgen", catGeneration, true',
    'lushCaveRarity = getInt("lushCaveRarity", catGeneration, 64',
    'lushCaveRegionRadiusChunks = getInt("lushCaveRegionRadiusChunks", catGeneration, 2',
    'lushCaveMinY = getInt("lushCaveMinY", catGeneration, 10',
    'lushCaveMaxY = getInt("lushCaveMaxY", catGeneration, 60',
):
    if expected not in world_config:
        failures.append(f"lush-cave worldgen config/default missing: {expected}")

if "ConfigWorld.lushCavesWorldgen = false;" not in config:
    failures.append("Map Compatibility Mode does not explicitly suppress Plus Lush Cave worldgen")

main_worldgen = require("src/main/java/ganymedes01/etfuturum/world/EtFuturumWorldGenerator.java")
for expected in (
    "protected WorldGenLushCaves lushCaveGen;",
    "lushCaveGen = new WorldGenLushCaves();",
    "lushCaveGen.generateChunk(world, chunkX, chunkZ);",
):
    if expected not in main_worldgen:
        failures.append(f"main world generator is missing Lush Cave integration: {expected}")
if "caveVineGen.generate(world" in main_worldgen or "protected WorldGenerator caveVineGen" in main_worldgen:
    failures.append("legacy global Cave Vine generation still exists outside Lush Cave regions")

lush_worldgen = require(
    "src/main/java/ganymedes01/etfuturum/world/generate/feature/WorldGenLushCaves.java",
    "class WorldGenLushCaves",
)
for expected in (
    "ConfigMapCompatibility.isEnabled()",
    "world.provider.dimensionId != 0",
    "findRegionAnchor",
    "mixSeed(world.getSeed() ^ DECORATION_SALT",
    "generateSurfaceMarkerAndRoots",
    "new WorldGenAzaleaTree(false)",
    "generateRootSystem",
    "startY - 100",
    "for (int i = 0; i < 20; i++)",
    "decorateMossFloors",
    "decorateMossCeilings",
    "decorateCaveVines",
    "decorateSporeBlossoms",
    "decorateClassicVines",
    "decorateClayAndDripleaf",
    "Blocks.clay",
    "Blocks.water",
    "BlockSmallDripleaf.makeMeta",
    "BlockBigDripleaf.makeMeta",
    "ModBlocks.MOSS_CARPET",
    "ModBlocks.AZALEA",
    "ModBlocks.HANGING_ROOTS",
    "isInsideChunkInner",
):
    if expected not in lush_worldgen:
        failures.append(f"Lush Cave generator implementation missing: {expected}")

# Generation must decorate existing 1.7 cave systems rather than pretending an underground
# region is a normal surface BiomeGenBase. This prevents corrupt/incorrect 2D biome maps.
if "BiomeGenBase" in lush_worldgen or "BiomeDictionary" in lush_worldgen:
    failures.append("Lush Cave generator must not register/treat Lush Caves as a 1.7 surface biome")
if "getChunkFromChunkCoords" in lush_worldgen or "provideChunk(" in lush_worldgen:
    failures.append("Lush Cave population path must not explicitly load/provide neighbouring chunks")


# P005 -- positive-only extended world-height foundation. This is intentionally opt-in while the
# engine/protocol work is runtime-tested. Physical Y stays non-negative: modern -64..319 maps to
# Plus 0..383 via a fixed +64 offset. Map Compatibility Mode must disable the early mixin group
# before Minecraft core classes are transformed.
height_helper = require(
    "src/main/java/ganymedes01/etfuturum/core/utils/WorldHeightCompat.java",
    "class WorldHeightCompat",
)
for expected in (
    "EXTENDED_HEIGHT = 384",
    "EXTENDED_SECTION_COUNT = EXTENDED_HEIGHT / 16",
    "MODERN_Y_OFFSET = 64",
    "FULL_SECTION_MASK = (1 << EXTENDED_SECTION_COUNT) - 1",
    "modernToPhysicalY",
    "physicalToModernY",
):
    if expected not in height_helper:
        failures.append(f"extended-height coordinate contract missing: {expected}")

if 'extendedWorldHeight = getBoolean("extendedWorldHeight", catGeneration, false' not in world_config:
    failures.append("extendedWorldHeight foundation must remain explicit opt-in during P005 validation")
if "ConfigWorld.extendedWorldHeight = false;" not in config:
    failures.append("Map Compatibility Mode does not force extended world height off")

height_mixin_gate = 'if (ConfigWorld.extendedWorldHeight && !ConfigMapCompatibility.isEnabled()) {'
if height_mixin_gate not in mixins:
    failures.append("extended-height early mixin group is not config/mapcompat gated")
for expected in (
    'mixins.add("extendedheight.MixinWorldProvider")',
    'mixins.add("extendedheight.MixinWorld")',
    'mixins.add("extendedheight.MixinChunk")',
    'mixins.add("extendedheight.MixinChunkCache")',
    'mixins.add("extendedheight.MixinAnvilChunkLoader")',
    'mixins.add("extendedheight.MixinS21PacketChunkData")',
    'mixins.add("extendedheight.MixinS26PacketMapChunkBulk")',
    'mixins.add("extendedheight.MixinC07PacketPlayerDigging")',
    'mixins.add("extendedheight.MixinC08PacketPlayerBlockPlacement")',
    'mixins.add("extendedheight.MixinS23PacketBlockChange")',
    'mixins.add("extendedheight.MixinItemBlock")',
    'mixins.add("extendedheight.MixinEntityPlayerMP")',
    'mixins.add("extendedheight.MixinPlayerInstance")',
    'mixins.add("extendedheight.MixinIntegratedServer")',
    'mixins.add("extendedheight.MixinDedicatedServer")',
    'mixins.add("extendedheight.client.MixinRenderGlobal")',
    'mixins.add("extendedheight.client.MixinWorldClient")',
    'mixins.add("extendedheight.client.MixinNetHandlerPlayClient")',
):
    if expected not in mixins:
        failures.append(f"extended-height early mixin selection missing: {expected}")

chunk_height_mixin = require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/MixinChunk.java",
    "WorldHeightCompat.EXTENDED_SECTION_COUNT",
)
for expected in (
    "new ExtendedBlockStorage[WorldHeightCompat.EXTENDED_SECTION_COUNT]",
    "new List[WorldHeightCompat.EXTENDED_SECTION_COUNT]",
    'method = "getAreLevelsEmpty"',
    'method = "relightBlock"',
    "public void enqueueRelightChecks()",
):
    if expected not in chunk_height_mixin:
        failures.append(f"extended-height chunk foundation missing: {expected}")

world_height_mixin = require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/MixinWorld.java",
    "WorldHeightCompat.EXTENDED_HEIGHT",
)
for expected in ("getBlock", "setBlock", "getTileEntity", "getSavedLightValue", "getBlockLightOpacity"):
    if expected not in world_height_mixin:
        failures.append(f"extended-height World bounds missing target: {expected}")

world_provider_height_mixin = require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/MixinWorldProvider.java",
    "this.dimensionId == 0",
)
for expected in (
    '@Inject(method = "getHeight", at = @At("HEAD"), cancellable = true, remap = false)',
    '@Inject(method = "getActualHeight", at = @At("HEAD"), cancellable = true, remap = false)',
):
    if expected not in world_provider_height_mixin:
        failures.append(f"Forge-added WorldProvider height hook must use literal non-remapped name: {expected}")
for forge_helper in ("canBlockFreezeBody", "canSnowAtBody"):
    if forge_helper not in world_height_mixin:
        failures.append(f"Forge-added World weather helper missing from extended-height hook: {forge_helper}")
if 'method = {"canBlockFreezeBody", "canSnowAtBody"}' not in world_height_mixin:
    failures.append("Forge-added freeze/snow body height hooks must be isolated from remapped vanilla World methods")
weather_hook_start = world_height_mixin.find('method = {"canBlockFreezeBody", "canSnowAtBody"}')
weather_hook_end = world_height_mixin.find("private int etfu$extendForgeWeatherHeightBound", weather_hook_start)
if weather_hook_start < 0 or weather_hook_end < 0 or "remap = false" not in world_height_mixin[weather_hook_start:weather_hook_end]:
    failures.append("Forge-added freeze/snow body height hooks must use literal names with remap=false")
if 'method = "getBlockLightOpacity"' not in world_height_mixin or 'remap = false' not in world_height_mixin:
    failures.append("Forge-added World.getBlockLightOpacity height hook must be isolated with remap=false")
require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/MixinAnvilChunkLoader.java",
    "WorldHeightCompat.EXTENDED_SECTION_COUNT",
)

s21 = require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/MixinS21PacketChunkData.java",
    "data.writeInt(this.field_149283_c)",
)
for expected in (
    "data.readInt();",
    "WorldHeightCompat.EXTENDED_SECTION_COUNT",
    "WorldHeightCompat.MAX_CHUNK_DATA_BYTES",
    "data.writeInt(this.field_149280_d)",
    "@Shadow(remap = false) private Semaphore deflateGate;",
    "@Shadow(remap = false)",
):
    if expected not in s21:
        failures.append(f"extended S21 chunk packet support missing: {expected}")

s26 = require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/MixinS26PacketMapChunkBulk.java",
    "WorldHeightCompat.FULL_SECTION_MASK",
)
for expected in (
    "this.field_149265_c[index] = data.readInt()",
    "this.field_149262_d[index] = data.readInt()",
    "WorldHeightCompat.EXTENDED_SECTION_COUNT",
    "data.writeInt(this.field_149265_c[i])",
    "@Shadow(remap = false) private Semaphore deflateGate;",
    "@Shadow(remap = false)",
):
    if expected not in s26:
        failures.append(f"extended S26 bulk chunk packet support missing: {expected}")

for packet_path, marker in (
    ("src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/MixinC07PacketPlayerDigging.java", "readUnsignedShort"),
    ("src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/MixinC08PacketPlayerBlockPlacement.java", "readUnsignedShort"),
    ("src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/MixinS23PacketBlockChange.java", "readUnsignedShort"),
):
    require(packet_path, marker)

upper_updates = require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/MixinPlayerInstance.java",
    "y < WorldHeightCompat.LEGACY_HEIGHT",
)
if "new S23PacketBlockChange" not in upper_updates:
    failures.append("upper-world runtime block updates do not bypass vanilla S22 8-bit Y packing")

item_block_height = require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/MixinItemBlock.java",
    "WorldHeightCompat.EXTENDED_HEIGHT",
)
for expected in (
    '@ModifyConstant(method = "onItemUse"',
    "WorldHeightCompat.LEGACY_MAX_Y",
    "world.provider.dimensionId == 0",
):
    if expected not in item_block_height:
        failures.append(f"extended-height ItemBlock ceiling bridge missing: {expected}")

entity_player_height = require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/MixinEntityPlayerMP.java",
    'method = "onUpdate"',
)
for expected in (
    "WorldHeightCompat.LEGACY_HEIGHT",
    "WorldHeightCompat.EXTENDED_HEIGHT",
    "self.worldObj.provider.dimensionId == 0",
):
    if expected not in entity_player_height:
        failures.append(f"extended-height initial tile-entity sync missing: {expected}")

render_height = require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/client/MixinRenderGlobal.java",
    "WorldHeightCompat.EXTENDED_SECTION_COUNT",
)
if 'method = "loadRenderers"' not in render_height or 'method = "<init>"' not in render_height:
    failures.append("extended-height client renderer does not resize both allocation and render grid")
require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/extendedheight/client/MixinNetHandlerPlayClient.java",
    'method = {"handleChunkData", "handleMapChunkBulk"}',
)

# P006 -- modern Overworld vertical-coordinate architecture. The actual terrain-density replacement
# is intentionally deferred, but every subsequent generator now has a single canonical logical/physical
# contract and a master config gate. Map Compatibility Mode must hard-disable it.
for expected in (
    "MODERN_MIN_Y = -64",
    "MODERN_MAX_Y = 319",
    "MODERN_SEA_LEVEL = 63",
    "MODERN_CLOUD_HEIGHT = 192",
    "PHYSICAL_ZERO_Y = MODERN_Y_OFFSET",
    "PHYSICAL_SEA_LEVEL = MODERN_SEA_LEVEL + MODERN_Y_OFFSET",
    "PHYSICAL_CLOUD_HEIGHT = MODERN_CLOUD_HEIGHT + MODERN_Y_OFFSET",
    "PHYSICAL_AVERAGE_GROUND_LEVEL = MODERN_AVERAGE_GROUND_LEVEL + MODERN_Y_OFFSET",
    "isModernYInRange",
    "isPhysicalYInRange",
):
    if expected not in height_helper:
        failures.append(f"modern Overworld coordinate contract missing: {expected}")

if 'modernOverworldGeneration = getBoolean("modernOverworldGeneration", catGeneration, false' not in world_config:
    failures.append("modernOverworldGeneration master switch/default missing")
if "if (modernOverworldGeneration)" not in world_config or "extendedWorldHeight = true;" not in world_config:
    failures.append("modern Overworld generation does not require/enable the 384-block engine foundation")
if "ConfigWorld.modernOverworldGeneration = false;" not in config:
    failures.append("Map Compatibility Mode does not force modern Overworld generation off")

modern_gate = 'if (ConfigWorld.extendedWorldHeight && ConfigWorld.modernOverworldGeneration && !ConfigMapCompatibility.isEnabled()) {'
if modern_gate not in mixins or 'mixins.add("modernoverworld.MixinWorldProvider")' not in mixins:
    failures.append("modern Overworld vertical-reference mixin is not correctly config/mapcompat gated")

# IntegratedServer is client-only and DedicatedServer is server-only. Keep the P005 height mixins
# side-correct so enabling the new architecture does not produce known invalid-side target warnings.
if 'if (side == MixinEnvironment.Side.CLIENT) {\n\t\t\t\tmixins.add("extendedheight.MixinIntegratedServer")' not in mixins:
    failures.append("extended-height IntegratedServer mixin is not client-side gated")
if '} else {\n\t\t\t\tmixins.add("extendedheight.MixinDedicatedServer")' not in mixins:
    failures.append("extended-height DedicatedServer mixin is not server-side gated")
if "@Shadow @Final public WorldProvider provider;" not in world_height_mixin:
    failures.append("extended-height World provider shadow is missing @Final")

modern_provider = require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/modernoverworld/MixinWorldProvider.java",
    "WorldHeightCompat.PHYSICAL_SEA_LEVEL",
)
for expected in (
    'method = "getCloudHeight"',
    "WorldHeightCompat.PHYSICAL_CLOUD_HEIGHT",
    'method = "getAverageGroundLevel"',
    "WorldHeightCompat.PHYSICAL_AVERAGE_GROUND_LEVEL",
    'method = "getHorizon"',
    "WorldHeightCompat.PHYSICAL_SEA_LEVEL",
    "ConfigWorld.modernOverworldGeneration",
):
    if expected not in modern_provider:
        failures.append(f"modern Overworld reference-height mixin missing: {expected}")
if modern_provider.count("remap = false") < 2:
    failures.append("Forge-added cloud/horizon hooks must use literal non-remapped names")

# P007 -- actual modern Overworld base-terrain foundation. Keep the vanilla ChunkProviderGenerate
# pipeline/structure/light flow, but feed it a 384-high terrain buffer shaped by broad modern-style
# continentalness/erosion/ridge fields. P008a replaces the old 256-high cave carver; the legacy
# ravine path stays no-op until a translated modern underground stage owns it.
if 'mixins.add("modernoverworld.MixinChunkProviderGenerate")' not in mixins:
    failures.append("P007 modern ChunkProviderGenerate mixin is not selected by the modern Overworld gate")

modern_chunk_provider = require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/modernoverworld/MixinChunkProviderGenerate.java",
    "class MixinChunkProviderGenerate",
)
for expected in (
    'method = "provideChunk"',
    "256 * WorldHeightCompat.EXTENDED_HEIGHT",
    '@At(value = "NEW", target = "net/minecraft/world/chunk/Chunk")',
    "(localX * 16 + localZ) * height",
    "sourceIndex = columnBase + y",
    "new ExtendedBlockStorage(sectionIndex << 4, hasSky)",
    'method = "func_147424_a"',
    "generateBaseTerrain",
    'method = "replaceBlocksForBiome"',
    "ChunkProviderEvent.ReplaceBiomeBlocks",
    "applyTranslatedBiomeSurface",
    "ModernOverworldCaveGenerator",
    "new ModernOverworldCaveGenerator(seed)",
    "ModernOverworldRavineGenerator",
    "worldObj.provider.dimensionId == 0",
    "ConfigWorld.modernOverworldGeneration",
    "ConfigMapCompatibility.isEnabled()",
):
    if expected not in modern_chunk_provider:
        failures.append(f"P007 modern chunk-provider bridge missing: {expected}")

modern_terrain = require(
    "src/main/java/ganymedes01/etfuturum/world/generate/terrain/ModernOverworldTerrainGenerator.java",
    "class ModernOverworldTerrainGenerator",
)
for expected in (
    "WorldHeightCompat.EXTENDED_HEIGHT",
    "WorldHeightCompat.PHYSICAL_ZERO_Y",
    "continentalness",
    "erosion",
    "weirdness",
    "peaksAndValleys",
    "continentalBaseHeight",
    "ModBlocks.DEEPSLATE",
    "logicalY <= 0",
    "logicalY >= 8",
    "isBedrock",
    "SURFACE_WINDOW_MIN_PHYSICAL_Y",
    "biome.genTerrainBlocks",
    "preservedZeroBand",
    "MAX_TERRAIN_Y = 250",
):
    if expected not in modern_terrain:
        failures.append(f"P007 modern terrain foundation missing: {expected}")

early_worldgen = require(
    "src/main/java/ganymedes01/etfuturum/world/EtFuturumEarlyWorldGenerator.java",
    "ConfigWorld.modernOverworldGeneration",
)
if "world.provider.dimensionId == 0 && ConfigWorld.modernOverworldGeneration" not in early_worldgen:
    failures.append("legacy EFR deepslate/tuff pass is not suppressed for the P007 modern Overworld")

for expected in (
    "translated Y63 sea level",
    "modern-style continentalness/erosion/ridge mountain shaping",
    "P008b-c modern cheese/spaghetti/noodle noise caves, translated ravines, regional cave-density variation, and deterministic local water/lava aquifers",
):
    if expected not in world_config:
        failures.append(f"P007 modernOverworldGeneration config description missing: {expected}")

# P007b -- runtime terrain/feature staging correction. Elevation must not be hard-clamped by
# discrete legacy biome IDs. Legacy lake generators and P004 cavity-based Lush decoration are
# staged out until P008/P009 provide translated caves, aquifers and underground feature heights.
for forbidden in (
    "BiomeDictionary.Type.OCEAN",
    "BiomeDictionary.Type.RIVER",
    "BiomeDictionary.Type.SWAMP",
    "BiomeDictionary.Type.BEACH",
    "biome == BiomeGenBase.deepOcean",
):
    if forbidden in modern_terrain:
        failures.append(f"P007b legacy biome height shaping still present: {forbidden}")
if "sampleSurfaceLogicalY(worldX, worldZ, amplified)" not in modern_terrain:
    failures.append("P007b base terrain is still coupled to legacy biome IDs")
for expected in (
    'method = "populate"',
    "TerrainGen.populate(provider, world, rand, chunkX, chunkZ, hasVillageGenerated, type)",
    "PopulateChunkEvent.Populate.EventType.LAKE",
    "PopulateChunkEvent.Populate.EventType.LAVA",
    "return false;",
):
    if expected not in modern_chunk_provider:
        failures.append(f"P007b legacy lake staging gate missing: {expected}")

lush_worldgen_p007b = require(
    "src/main/java/ganymedes01/etfuturum/world/generate/feature/WorldGenLushCaves.java",
    "ConfigWorld.modernOverworldGeneration",
)
if "|| ConfigWorld.modernOverworldGeneration" not in lush_worldgen_p007b:
    failures.append("P007b does not stage P004 Lush cavity decoration out of the modern path")



# P008b-c -- compare the P008 noise foundation against a real 1.21 reference world: keep roughly
# comparable total open volume, but add broad regional rarity so cave-poor and cave-rich zones do
# not collapse into one world-spanning component. Broaden cheese chambers, reduce connector density,
# restore a translated 384-safe ravine/canyon family, and make the lowest carved band a geometry-
# owned lava shelf while retaining P008b-a's contained water basins and surface safety.
modern_caves = require(
    "src/main/java/ganymedes01/etfuturum/world/generate/terrain/ModernOverworldCaveGenerator.java",
    "class ModernOverworldCaveGenerator",
)
for expected in (
    "extends MapGenBase",
    "HEIGHT = WorldHeightCompat.EXTENDED_HEIGHT",
    "GRID_STEP = 4",
    "Y_SAMPLES = HEIGHT / GRID_STEP + 1",
    "MIN_CARVE_PHYSICAL_Y = 1",
    "DEEP_FLOOR_FADE_END_PHYSICAL_Y = 1",
    "DEEP_FLOOR_FADE_START_PHYSICAL_Y = 18",
    "deepFloorScale",
    "TUNNEL_SURFACE_ROOF = 9",
    "WorldHeightCompat.physicalToModernY",
    "terrain.sampleSurfacePhysicalY",
    "(localX * 16 + localZ) * HEIGHT",
    "fillNoiseFields",
    "cheeseNoise",
    "spaghettiNoiseA",
    "spaghettiNoiseB",
    "noodleNoiseA",
    "noodleNoiseB",
    "pillarNoiseA",
    "pillarNoiseB",
    "entranceNoise",
    "regionNoise",
    "regionField",
    "124.0D, 92.0D, 124.0D",
    "336.0D, 208.0D, 336.0D",
    "regionClamped",
    "noodleToggle > 0.30D",
    "isCavernPillar",
    "isSurfaceEntrance",
    "dilateTraversablePassages",
    "MASK_ENTRANCE",
    "new ModernOverworldAquifer(seed, terrain)",
    "aquifer.sampleColumn",
    "aquifer.resolve",
    "hasLooseSurfaceCover",
    "!surfaceWaterColumn",
    "Blocks.water",
    "Blocks.lava",
    "FractalNoise3D",
    "NoiseGeneratorImproved",
    "startX * scaleX",
):
    if expected not in modern_caves:
        failures.append(f"P008b-c modern cave/aquifer bridge missing: {expected}")

modern_aquifer = require(
    "src/main/java/ganymedes01/etfuturum/world/generate/terrain/ModernOverworldAquifer.java",
    "class ModernOverworldAquifer",
)
for expected in (
    "CELL_SIZE = 96",
    "SITE_JITTER = 0.28D",
    "WET_SITE_THRESHOLD = 0.55D",
    "DEEP_LAVA_SHELF_MAX_Y = -55",
    "LAVA_SITE_CHANCE = 0.100D",
    "LAVA_POD_LEVEL_MIN = -36",
    "LAVA_POD_LEVEL_MAX = -24",
    "LAVA_POD_BOTTOM_MIN = -53",
    "logicalY <= DEEP_LAVA_SHELF_MAX_Y",
    "PRESSURE_BARRIER_WIDTH",
    "PRESSURE_LEVEL_DIFFERENCE",
    "BASIN_SHELL_WIDTH",
    "sampleColumn",
    "resolve(Column column",
    "needsPressureBarrier",
    "bottomLogicalY",
    "site.radius",
    "Decision.WATER",
    "Decision.LAVA",
    "Decision.PRESERVE",
):
    if expected not in modern_aquifer:
        failures.append(f"P008b-c contained aquifer/deep-lava implementation missing: {expected}")

modern_ravines = require(
    "src/main/java/ganymedes01/etfuturum/world/generate/terrain/ModernOverworldRavineGenerator.java",
    "class ModernOverworldRavineGenerator",
)
for expected in (
    "extends MapGenBase",
    "HEIGHT = WorldHeightCompat.EXTENDED_HEIGHT",
    "BLOCK_COUNT = 256 * HEIGHT",
    "START_CHANCE = 85",
    "terrain.sampleSurfacePhysicalY",
    "WorldHeightCompat.physicalToModernY",
    "columnIndex * HEIGHT",
    "aquifer.sampleColumn",
    "aquifer.resolve",
    "WATER_SURFACE_GUARD",
    "LOOSE_SURFACE_GUARD",
    "deepFloorScale",
):
    if expected not in modern_ravines:
        failures.append(f"P008b-c translated ravine/canyon implementation missing: {expected}")

for forbidden in (
    "x * 16 + z << 8",
    "<< 8 |",
    "* 256 +",
):
    if forbidden in modern_caves or forbidden in modern_aquifer or forbidden in modern_ravines:
        failures.append(f"P008b-c must not reintroduce legacy 256-high carver indexing: {forbidden}")

if "applyMagmaFloors" in modern_caves or "ModBlocks.MAGMA" in modern_aquifer:
    failures.append("P008b-c must stage aquifer magma-floor decoration out until the later cave decorator pass")
if "surfaceAquifer" in modern_aquifer or "resolve(Column column, int physicalY, boolean entrance)" in modern_aquifer:
    failures.append("P008b-c must not force underwater cave mouths to become static aquifer source columns")
if "width *= deepFloorScale(physicalY)" not in modern_caves or "threshold += (1.0D - deepFloorScale)" not in modern_caves:
    failures.append("P008b-c must taper all noise-cave families before the seeded bedrock floor")

if "this.caveGenerator = new ModernOverworldCaveGenerator(seed);" not in modern_chunk_provider:
    failures.append("P008b-c modern cave generator is not installed into ChunkProviderGenerate")
if "this.ravineGenerator = new ModernOverworldRavineGenerator(seed);" not in modern_chunk_provider:
    failures.append("P008b-c translated ravine generator is not installed into ChunkProviderGenerate")
if "NoopModernCarver" in modern_chunk_provider:
    failures.append("P008b-c still suppresses the ravine slot instead of installing the translated canyon carver")
if "type == PopulateChunkEvent.Populate.EventType.LAKE" not in modern_chunk_provider or \
        "type == PopulateChunkEvent.Populate.EventType.LAVA" not in modern_chunk_provider:
    failures.append("P008b-c must keep legacy water/lava lake population suppressed while aquifers own fluids")

# P007c -- terrain-aligned modern biome source and structure/surface integration. The legacy
# GenLayer biome map cannot remain authoritative once P007 terrain shape is independent: doing so
# labels water as Desert/Plains, land as Deep Ocean/Beach, and lets structures spawn against the
# wrong ecology. Dimension 0 gets a dedicated climate/terrain resolver; other dimensions remain
# untouched. Also correct the temporary biome-surface buffer's X/Z convention and translate the
# one legacy scattered feature (DesertPyramid) that hard-anchors itself to Y64.
if 'mixins.add("modernoverworld.MixinMapGenScatteredFeatureStart")' not in mixins:
    failures.append("P007c desert-pyramid grounding mixin is not selected by the modern Overworld gate")
for expected in (
    'method = "registerWorldChunkManager"',
    "new ModernOverworldWorldChunkManager(this.worldObj)",
    "this.dimensionId == 0",
):
    if expected not in modern_provider:
        failures.append(f"P007c modern Overworld biome-manager bridge missing: {expected}")

modern_biome_manager = require(
    "src/main/java/ganymedes01/etfuturum/world/generate/terrain/ModernOverworldWorldChunkManager.java",
    "class ModernOverworldWorldChunkManager",
)
for expected in (
    "extends WorldChunkManager",
    "terrain.sampleSurfaceLogicalY",
    "terrain.sampleContinentalness",
    "terrain.sampleErosion",
    "terrain.sampleWeirdness",
    "BiomeGenBase.deepOcean",
    "BiomeGenBase.ocean",
    "BiomeGenBase.beach",
    "BiomeGenBase.coldBeach",
    "BiomeGenBase.stoneBeach",
    "BiomeGenBase.icePlains",
    "BiomeGenBase.desert",
    "BiomeGenBase.savanna",
    "BiomeGenBase.jungle",
    "BiomeGenBase.swampland",
    "areBiomesViable",
    "findBiomePosition",
):
    if expected not in modern_biome_manager:
        failures.append(f"P007c terrain-aligned biome source missing: {expected}")
for forbidden in ("BiomeGenBase.desertHills", "BiomeGenBase.forestHills", "BiomeGenBase.taigaHills"):
    if forbidden in modern_biome_manager:
        failures.append(f"P007c shape-only legacy hill biome is still emitted: {forbidden}")

# P007d -- retain the broad climate-field frequencies and Forge population remap correction. P007f
# deliberately supersedes P007d's per-column nearest-climate resolver with a regional parameter
# topology source, so the retired selectMacroClimateBiome/climateDistance helpers must not return.
for expected in (
    "TEMPERATURE_SCALE = 1.0D / 4096.0D",
    "HUMIDITY_SCALE = 1.0D / 3584.0D",
    "VARIANT_SCALE = 1.0D / 2048.0D",
    "new FractalNoise(seed ^ SALT_TEMPERATURE, 3, 0.42D)",
    "new FractalNoise(seed ^ SALT_HUMIDITY, 3, 0.42D)",
    "new FractalNoise(seed ^ SALT_VARIANT, 2, 0.48D)",
):
    if expected not in modern_biome_manager:
        failures.append(f"P007d broad climate field missing: {expected}")
if "remap = false))" not in modern_chunk_provider:
    failures.append("P007d Forge TerrainGen.populate redirect is missing remap=false")

# P007e -- preserve the validated continuous terrain/coast geometry and shared ridge signal. P007f
# changes biome topology only; it must never restore the old hard terrain-detail coast switch.
for expected in (
    "inlandDetailAmplitude = 4.0D + roughness * 7.0D + mountain * 9.0D",
    "landDetailBlend = smoothstep(-0.16D, 0.10D, continent)",
    "lerp(3.0D, inlandDetailAmplitude, landDetailBlend)",
    "sampleMountainStrength",
    "return mountainStrength(continent, erosionValue, weirdnessValue)",
):
    if expected not in modern_terrain:
        failures.append(f"P007e continuous coast/mountain terrain signal missing: {expected}")
if "final boolean oceanTerrain = continent < -0.04D" in modern_terrain:
    failures.append("P007e still contains the hard -0.04 coast detail-amplitude switch")

# P007f -- regional modern biome-source / climate-topology rework. Land ecology must be owned by a
# deterministic jittered macro-climate lattice and a multi-parameter target table; ocean/shore
# identity is a separate continental-topology decision. This is intentionally not old GenLayer and
# not a return to direct per-column threshold selection.
for expected in (
    "NORMAL_CLIMATE_CELL_SIZE = 64",
    "LARGE_CLIMATE_CELL_SIZE = 256",
    "CLIMATE_CELL_JITTER = 0.22D",
    "ClimateTarget[] LAND_TARGETS",
    "selectClimateSite",
    "Math.floorDiv(x, climateCellSize)",
    "ParameterSpan",
    "selectParameterizedLandBiome",
    "WEIGHT_TEMPERATURE",
    "WEIGHT_HUMIDITY",
    "WEIGHT_CONTINENT",
    "terrain.sampleContinentalness(sampleX, sampleZ)",
    "terrain.sampleErosion(sampleX, sampleZ)",
    "terrain.sampleWeirdness(sampleX, sampleZ)",
    "terrain.sampleMountainStrength(sampleX, sampleZ)",
    "OCEAN_CORE_CONTINENT_MAX = -0.060D",
    "COAST_TOPOLOGY_CONTINENT_MAX = 0.14D",
    "SHORE_LAND_CONTINENT_MAX = 0.020D",
    "DEEP_OCEAN_CONTINENT_MAX = -0.34D",
    "STONY_SHORE_RUGGEDNESS_MIN = 0.30D",
    "STONY_SHORE_TEMPERATURE_MAX = 0.12D",
    "isCoastalTopology",
    "hasOceanCorridor",
    "offsetX / 2",
    "surface < sea && coastalTopology",
    "surface >= sea",
    "localContinent <= SHORE_LAND_CONTINENT_MAX",
    "selectShoreBiome(inlandBiome, climate)",
    "isFrozenClimate",
    "climate.temperature <= -0.30D",
    "isAridBiome(inlandBiome)",
    "BiomeGenBase.roofedForest",
    "BiomeGenBase.coldTaiga",
    "BiomeGenBase.icePlains",
):
    if expected not in modern_biome_manager:
        failures.append(f"P007f regional biome/climate topology missing: {expected}")

for forbidden in (
    "selectMacroClimateBiome",
    "climateDistance",
    "SHORE_MAX_ABOVE_SEA",
    "MOUNTAIN_MIN_ABOVE_SEA",
    "MOUNTAIN_STRENGTH_MIN",
    "surface <= sea + SHORE_MAX_ABOVE_SEA",
    "surface < sea && continent < OCEAN_CONTINENT_MAX",
    "localSlope",
    "COAST_MAX_ABOVE_SEA",
):
    if forbidden in modern_biome_manager:
        failures.append(f"P007f retired per-column biome threshold rule returned: {forbidden}")

# P007g -- retain P007f's coherent macro regions but soften only their shared boundary. The two
# nearest regional sites are blended in a bounded band, and hot/arid-to-temperate boundaries receive
# a Savanna ecotone rather than a one-block sand/grass seam. Transitional submerged coast columns
# additionally need a short open-water ray toward lower continentalness before receiving Ocean.
for expected in (
    "CLIMATE_TRANSITION_WIDTH_FRACTION = 0.30D",
    "ECOTONE_SECONDARY_INFLUENCE_MIN = 0.16D",
    "ClimateSiteSelection",
    "Math.sqrt(selection.secondary.distanceSquared)",
    "secondaryInfluence = 0.5D * (1.0D - smoothstep01(edgeDistance))",
    "distanceGap >= transitionWidth",
    "ClimatePoint.lerp(primary, secondary, secondaryInfluence)",
    "selectTransitionLandBiome",
    "isAridBiome(primaryBiome) != isAridBiome(secondaryBiome)",
    "return BiomeGenBase.savanna;",
    "OPEN_WATER_NEAR_RADIUS = 12",
    "OPEN_WATER_FAR_RADIUS = 40",
    "OPEN_WATER_CONTINENT_DROP_MIN = 0.010D",
    "isOpenCoastalWater",
    "hasOpenWaterRay",
    "surface < sea && coastalTopology && isOpenCoastalWater",
    "farContinent <= localContinent - OPEN_WATER_CONTINENT_DROP_MIN",
):
    if expected not in modern_biome_manager:
        failures.append(f"P007g biome-boundary/coastal-pool polish missing: {expected}")

# The cold fallback spans the whole frozen temperature range regardless of humidity. This prevents
# a dry point inside a snowy macro climate from falling through to green Plains. Shore selection
# tests frozen climate before ruggedness and arid climate before Stone Beach, keeping snow semantics
# consistent on cold coasts and preventing Stone Beach from manufacturing snow beside Desert/Mesa.
if not all(token in modern_biome_manager for token in (
        "target(BiomeGenBase.icePlains,",
        "span(-1.0D, -0.30D), ANY, ANY, ANY, ANY, ANY",
        "if (isFrozenClimate(climate, inlandBiome))",
        "if (isAridBiome(inlandBiome))",
        "return BiomeGenBase.beach;")):
    failures.append("P007f cold fallback / climate-consistent shore invariants are incomplete")

for expected in (
    "physicalColumn = localX * 16 + localZ",
    "surfaceColumn = localZ * 16 + localX",
    "biomeIndex = localX + localZ * 16",
    "sampleSurfacePhysicalY",
):
    if expected not in modern_terrain:
        failures.append(f"P007c translated surface/terrain sampler bridge missing: {expected}")

modern_scattered_start = require(
    "src/main/java/ganymedes01/etfuturum/mixins/early/modernoverworld/MixinMapGenScatteredFeatureStart.java",
    "class MixinMapGenScatteredFeatureStart",
)
for expected in (
    "ComponentScatteredFeaturePieces.DesertPyramid",
    "terrain.sampleSurfacePhysicalY",
    "targetBaseY",
    "box.offset(0, deltaY, 0)",
    "start.getBoundingBox().offset(0, deltaY, 0)",
    "world.provider.dimensionId != 0",
    "ConfigMapCompatibility.isEnabled()",
):
    if expected not in modern_scattered_start:
        failures.append(f"P007c translated desert-pyramid grounding missing: {expected}")

# Licence boundary: Campfire Backport is reference-only. Existing optional compatibility strings
# mentioning the external mod are legitimate, but its package/source must not be vendored here.
for path in (ROOT / "src").rglob("*.java"):
    text = path.read_text(encoding="utf-8", errors="replace")
    if "import connor135246.campfirebackport" in text or "package connor135246.campfirebackport" in text:
        failures.append(f"Campfire Backport GPL package code detected in fork source: {path.relative_to(ROOT)}")

license_text = require("LICENSE")
if "GNU Lesser General Public License" not in license_text:
    failures.append("upstream LGPL licence file no longer appears intact")

if failures:
    print("Et Futurum Requiem Plus map-compat static validation FAILED")
    for failure in failures:
        print(f" - {failure}")
    sys.exit(1)

print("Et Futurum Requiem Plus map-compat static validation PASSED")
print(" - mapCompatibilityMode default remains false")
print(" - RTG/world-generation hard gates present")
print(" - progression/raw-ore/spawn gates present")
print(" - rooted dirt, hanging roots, dripleaf, moss/azalea, and spore blossom content present")
print(" - azalea tree growth and moss bonemeal vegetation checks present")
print(" - deterministic Lush Cave regions, marker roots, vegetation, clay pools, and Dripleaf worldgen checks present")
print(" - opt-in 384-block positive-Y height foundation, +64 modern offset, storage/protocol/render/placement gates present")
print(" - P006 modern Overworld logical -64..319 coordinate contract and translated sea/horizon/cloud references present")
print(" - P007 modern Overworld 384-high base terrain, Y127 physical sea, translated biome surfaces, and legacy-carver safety gate present")
print(" - P007a 384-stride Chunk construction bypass prevents vanilla bitwise-OR column aliasing")
print(" - P007b continuous biome-independent terrain shaping and legacy lake/Lush staging gates present")
print(" - P007c terrain-aligned modern biomes, X/Z-correct surface bridge, and translated desert-pyramid grounding present")
print(" - P007d broad climate frequencies and Forge lake-hook remap cleanup remain present")
print(" - P007e continuous coast terrain and ridge-aligned terrain signal remain preserved")
print(" - P007f jittered macro-climate cells, parameter-space land biomes, and continental coast topology present")
print(" - P007g blended macro-boundary ecotones and open-water coastal-pool filtering present")
print(" - P008b-c region-varied larger noise caves, translated 384-safe ravines, deep Y-55 lava shelf/pods, contained water aquifers, and safe land mouths present")
print(" - no Campfire Backport GPL package source vendored")
