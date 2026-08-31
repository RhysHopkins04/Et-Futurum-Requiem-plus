#!/usr/bin/env python3
"""Static regression guard for MapParity 1.21.11 Fidelity Pass 19."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")

failures = []

def require(text, token, label):
    if token not in text:
        failures.append(f"{label} missing: {token}")

def forbid(text, token, label):
    if token in text:
        failures.append(f"{label} returned: {token}")

config = read("src/main/java/ganymedes01/etfuturum/configuration/configs/ConfigBlocksItems.java")
experiments = read("src/main/java/ganymedes01/etfuturum/configuration/configs/ConfigExperiments.java")
world = read("src/main/java/ganymedes01/etfuturum/configuration/configs/ConfigWorld.java")
blocks = read("src/main/java/ganymedes01/etfuturum/ModBlocks.java")
items = read("src/main/java/ganymedes01/etfuturum/ModItems.java")
recipes = read("src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java")
tagging = read("src/main/java/ganymedes01/etfuturum/recipes/ModTagging.java")
parity = read("src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java")
stripping = read("src/main/java/ganymedes01/etfuturum/api/StrippedLogRegistry.java")
server = read("src/main/java/ganymedes01/etfuturum/core/handlers/ServerEventHandler.java")
planks = read("src/main/java/ganymedes01/etfuturum/blocks/BlockModernWoodPlanks.java")
leaves = read("src/main/java/ganymedes01/etfuturum/blocks/BlockModernLeaves.java")
sapling = read("src/main/java/ganymedes01/etfuturum/blocks/BlockModernSapling.java")
main = read("src/main/java/ganymedes01/etfuturum/EtFuturum.java")
dynamic = read("src/main/java/ganymedes01/etfuturum/client/DynamicSoundsResourcePack.java")
wood_sign = read("src/main/java/ganymedes01/etfuturum/blocks/BlockWoodSign.java")
bubble = read("src/main/java/ganymedes01/etfuturum/blocks/BlockBubbleColumn.java")
nether_manager = read("src/main/java/ganymedes01/etfuturum/world/nether/biome/utils/NetherBiomeManager.java")
lang = read("src/main/resources/assets/etfuturum/lang/en_US.lang")

# Default-on content flags are separate from old default-off generation/vegetation experiments.
for name in ("enableCrimsonWoodFamily", "enableWarpedWoodFamily", "enableMangroveWoodFamily"):
    require(config, f'getBoolean("{name}", catBlockNatural, true,', "default-on wood content flag")
    require(config, f"public static boolean {name};", "wood content declaration")
for name in ("enableCrimsonBlocks", "enableWarpedBlocks", "enableMangroveBlocks"):
    require(experiments, f'getBoolean("{name}", catExperiments, false,', "default-off experiment flag")
for name in ("extendedWorldHeight", "modernOverworldGeneration", "modernOreGeneration", "modernLargeOreVeins", "lushCavesWorldgen", "dripstoneCavesWorldgen"):
    if not re.search(rf'getBoolean\("{re.escape(name)}",\s*catGeneration,\s*false,', world):
        failures.append(f"world-safety default is not false: {name}")
for name in ("enableCrimsonWoodFamily", "enableWarpedWoodFamily", "enableMangroveWoodFamily"):
    if name in nether_manager:
        failures.append(f"content flag leaked into Nether biome generation gate: {name}")
require(nether_manager, "ConfigExperiments.enableCrimsonBlocks", "Crimson generation experiment gate")
require(nether_manager, "ConfigExperiments.enableWarpedBlocks", "Warped generation experiment gate")

# Existing registry identities must be reused, not duplicated.
wood_owners = {
    "CRIMSON_SIGN": "ConfigBlocksItems.enableCrimsonWoodFamily",
    "WARPED_SIGN": "ConfigBlocksItems.enableWarpedWoodFamily",
    "MANGROVE_SIGN": "ConfigBlocksItems.enableMangroveWoodFamily",
    "CRIMSON_STEM": "ConfigBlocksItems.enableCrimsonWoodFamily",
    "WARPED_STEM": "ConfigBlocksItems.enableWarpedWoodFamily",
    "MANGROVE_LOG": "ConfigBlocksItems.enableMangroveWoodFamily",
}
for enum_name, gate in wood_owners.items():
    m = re.search(rf'^\s*{enum_name}\(([^\n]+)', blocks, re.M)
    if not m:
        failures.append(f"existing wood registry identity missing: {enum_name}")
    elif gate not in m.group(1):
        failures.append(f"{enum_name} is not gated by {gate}")
for duplicate in ("CRIMSON_SIGN", "WARPED_SIGN", "MANGROVE_SIGN", "CRIMSON_STEM", "WARPED_STEM", "MANGROVE_LOG"):
    if re.search(rf'^\s*{duplicate}\(', parity, re.M):
        failures.append(f"duplicate modern registry identity added to ModernMapParityBlocks: {duplicate}")

# All 12 families: Oak uses the vanilla sign bridge; five legacy signs + five newer EFR signs + Pale Oak parity sign.
require(wood_sign, "installVanillaSignTileEntityCompatibility()", "Oak modern sign bridge")
for sign in ("SIGN_SPRUCE", "SIGN_BIRCH", "SIGN_JUNGLE", "SIGN_ACACIA", "SIGN_DARK_OAK",
             "CRIMSON_SIGN", "WARPED_SIGN", "MANGROVE_SIGN", "CHERRY_SIGN", "BAMBOO_SIGN"):
    if not re.search(rf'^\s*{sign}\(', blocks, re.M):
        failures.append(f"normal sign family missing: {sign}")
require(parity, 'PALE_OAK_SIGN("1.21.4", Style.SIGN', "Pale Oak normal sign")
for hanging in ("OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA", "DARK_OAK", "MANGROVE", "CHERRY", "PALE_OAK", "BAMBOO", "CRIMSON", "WARPED"):
    require(parity, f'{hanging}_HANGING_SIGN(', f"{hanging} hanging sign")

# Family subtypes, recipes, stripping and Nether special fire/fuel behaviour.
for token in (
    "ConfigBlocksItems.enableCrimsonWoodFamily", "ConfigBlocksItems.enableWarpedWoodFamily", "ConfigBlocksItems.enableMangroveWoodFamily"
):
    require(planks, token, "modern plank creative availability")
require(planks, "case 0, 1 -> ModSounds.soundNetherWood", "Nether plank sound")
require(planks, "return aWorld.getBlockMetadata(aX, aY, aZ) % getTypes().length > 1;", "Nether plank nonflammability")
require(server, "noBurnItems.add(ModBlocks.CRIMSON_STEM", "Crimson non-fuel rule")
require(server, "noBurnItems.add(ModBlocks.WARPED_STEM", "Warped non-fuel rule")
for token in ("CRIMSON_HANGING_SIGN", "WARPED_HANGING_SIGN", "CRIMSON_SHELF", "WARPED_SHELF"):
    require(server, token, "Nether parity non-fuel rule")
for token in (
    "addLog(ModBlocks.CRIMSON_STEM.get(), 0, ModBlocks.CRIMSON_STEM.get(), 2)",
    "addLog(ModBlocks.WARPED_STEM.get(), 0, ModBlocks.WARPED_STEM.get(), 2)",
    "addLog(ModBlocks.MANGROVE_LOG.get(), 0, ModBlocks.MANGROVE_LOG.get(), 2)",
    "addLog(ModBlocks.BAMBOO_BLOCK.get(), 0, ModBlocks.BAMBOO_BLOCK.get(), 1)",
    "ModernMapParityBlocks.PALE_OAK_LOG.get()",
):
    require(stripping, token, "axe stripping coverage")
for token in (
    "ModBlocks.CRIMSON_SIGN.newItemStack(3)", "ModBlocks.WARPED_SIGN.newItemStack(3)", "ModBlocks.MANGROVE_SIGN.newItemStack(3)",
    "registerModernWoodParityRecipes()", "PALE_OAK_STAIRS", "PALE_OAK_TRAPDOOR", "PALE_OAK_SIGN",
    "addHangingSignRecipe(ModernMapParityBlocks.CRIMSON_HANGING_SIGN", "addHangingSignRecipe(ModernMapParityBlocks.MANGROVE_HANGING_SIGN",
):
    require(recipes, token, "wood crafting parity")
require(items, "ConfigBlocksItems.enableMangroveWoodFamily", "Mangrove boat availability")
require(tagging, "ConfigBlocksItems.enableMangroveWoodFamily", "Mangrove OreDictionary availability")
require(leaves, "ConfigBlocksItems.enableMangroveWoodFamily", "Mangrove leaves availability")
require(sapling, "ConfigBlocksItems.enableMangroveWoodFamily", "Mangrove propagule availability")

# Audio: direct target is downloaded by AssetDirector; the dynamic pack recreates the modern
# type=event alias with a BARE same-domain target name, as required by Minecraft 1.7 SoundHandler.
require(main, 'config.addSoundEvent(ver, "block.sign.waxed_interact_fail", "block")', "sign wax target sound")
forbid(main, 'config.addSoundEvent(ver, "block.hanging_sign.waxed_interact_fail", "block")', "guessed hanging-sign wax OGG registration")
for token in (
    'ImmutableSet.of("minecraft", Tags.MC_ASSET_VER)',
    'addSoundEventsToCategory("block.hanging_sign.waxed_interact_fail"',
    '"block.sign.waxed_interact_fail"',
    'soundObj.add("type", new JsonPrimitive("event"))',
):
    require(dynamic, token, "sounds.json event alias")
forbid(dynamic, 'Tags.MC_ASSET_VER + ":block.sign.waxed_interact_fail"', "double-namespaced 1.7 sound-event alias")
for event in (
    "item.dye.use", "item.glow_ink_sac.use", "item.ink_sac.use", "block.sign.waxed_interact_fail",
    "item.lead.break", "item.lead.tied", "item.lead.untied", "block.lily_pad.place"
):
    require(main, f'config.addSoundEvent(ver, "{event}"', "direct modern sound registration")
for family in ("hanging_sign", "nether_wood_hanging_sign", "bamboo_wood_hanging_sign", "cherry_wood_hanging_sign", "scaffolding"):
    require(main, f'"{family}"', "modern sound family registration")

# Compatibility-safe retirement: block identity remains, but there is no ItemBlock/creative exposure/recipe name gag.
require(blocks, "NETHERITE_STAIRS(ConfigBlocksItems.enableNetherite, new BlockNetheriteStairs(), null)", "Netherite Stairs compatibility registration")
nether_stairs = read("src/main/java/ganymedes01/etfuturum/blocks/BlockNetheriteStairs.java")
require(nether_stairs, "setCreativeTab(null)", "Netherite Stairs creative retirement")
forbid(lang, "Swaggiest stairs ever", "obsolete Netherite Stairs display name")
if "NETHERITE_STAIRS" in recipes:
    failures.append("obsolete Netherite Stairs has a crafting recipe/reference")

# Dye compatibility: retain legacy minecraft:dye while EFR's modern aliases remain OreDictionary-integrated and signs accept both.
require(wood_sign, "held.getItem() == Items.dye", "legacy dye sign compatibility")
require(wood_sign, "held.getItem() == ModItems.DYE.get()", "modern dye alias sign compatibility")
for token in ('registerOre("dyeWhite"', 'registerOre("dyeBlue"', 'registerOre("dyeBrown"', 'registerOre("dyeBlack"'):
    require(tagging, token, "modern dye OreDictionary alias")
forbid(blocks + items, 'GameRegistry.findItem("minecraft", "dye")', "destructive vanilla dye replacement")

# Atlas and asset safety.
require(bubble, 'setBlockTextureName("minecraft:water_still")', "Bubble Column fallback atlas icon")
for tex in (
    "crimson_planks.png", "warped_planks.png", "mangrove_planks.png",
    "crimson_stem.png", "warped_stem.png", "mangrove_log.png",
    "crimson_trapdoor.png", "warped_trapdoor.png", "mangrove_trapdoor.png",
):
    if not (ROOT / "src/main/resources/assets/minecraft/textures/blocks" / tex).is_file():
        failures.append(f"enabled wood family legacy-atlas texture missing: {tex}")

# Removed dependencies/helpers must stay gone.
for p in (ROOT / "src/main/java").rglob("*.java"):
    text = p.read_text(encoding="utf-8", errors="ignore")
    if "roadhog360.hogutils" in text or "me.mrnavastar.r" in text:
        failures.append(f"removed helper dependency returned in {p.relative_to(ROOT)}")
for rel in ("dependencies.gradle", "build.gradle"):
    p = ROOT / rel
    if p.is_file():
        text = p.read_text(encoding="utf-8", errors="ignore")
        if "hogutils" in text.lower() or "me.mrnavastar.r" in text:
            failures.append(f"removed helper dependency returned in {rel}")

if failures:
    print("Fidelity Pass 19 static validation FAILED")
    for failure in failures:
        print(" -", failure)
    sys.exit(1)

print("Fidelity Pass 19 static validation PASSED")
print(" - 12 wood families are available through existing registry identities")
print(" - Crimson/Warped/Mangrove content is separated from default-off experimental generation")
print(" - wood crafting/stripping and Nether non-fire/non-fuel rules remain covered")
print(" - hanging-sign wax sound uses the 1.21.11 type=event alias with 1.7-correct same-domain resolution")
print(" - fake Netherite Stairs is compatibility-retired from normal gameplay")
print(" - legacy + modern dye integration and Bubble Column atlas fallback remain intact")
print(" - worldgen safety defaults and removed helper dependencies remain guarded")
