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
print(" - no Campfire Backport GPL package source vendored")
