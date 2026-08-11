#!/usr/bin/env python3
"""Static regression checks for the first Et Futurum Requiem Plus map-compatibility patch."""

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

for texture in (
    "rooted_dirt.png", "hanging_roots.png",
    "small_dripleaf_top.png", "small_dripleaf_side.png", "small_dripleaf_stem_top.png", "small_dripleaf_stem_bottom.png",
    "big_dripleaf_top.png", "big_dripleaf_side.png", "big_dripleaf_tip.png", "big_dripleaf_stem.png",
):
    require_png(f"src/main/resources/assets/minecraft/textures/blocks/{texture}")

lang = require("src/main/resources/assets/etfuturum/lang/en_US.lang")
for expected in (
    "tile.etfuturum.small_dripleaf.name=Small Dripleaf",
    "tile.etfuturum.big_dripleaf.name=Big Dripleaf",
    "tile.etfuturum.big_dripleaf_stem.name=Big Dripleaf Stem",
):
    if expected not in lang:
        failures.append(f"missing dripleaf language entry: {expected}")

compost = require("src/main/java/ganymedes01/etfuturum/api/CompostingRegistry.java")
for expected in ("ModBlocks.SMALL_DRIPLEAF.newItemStack()", "ModBlocks.BIG_DRIPLEAF.newItemStack()"):
    if expected not in compost:
        failures.append(f"missing dripleaf composting entry: {expected}")

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
print(" - rooted dirt, hanging roots, and dripleaf registrations/assets present")
print(" - no Campfire Backport GPL package source vendored")
