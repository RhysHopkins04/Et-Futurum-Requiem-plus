#!/usr/bin/env python3
"""Static regression guard for MapParity 1.21.11 Fidelity Pass 20.

Pass 20 finishes the content-vs-generation split for already-implemented ecology
blocks, corrects Mangrove boat integration/localisation, and makes AssetDirector
model-backed blocks publish a real model texture as their legacy atlas fallback.
"""
from pathlib import Path
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
failures = []

def read(rel):
    p = ROOT / rel
    if not p.is_file():
        failures.append(f"missing file: {rel}")
        return ""
    return p.read_text(encoding="utf-8")

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
roots = read("src/main/java/ganymedes01/etfuturum/blocks/BlockNetherRoots.java")
fungus = read("src/main/java/ganymedes01/etfuturum/blocks/BlockNetherFungus.java")
nylium = read("src/main/java/ganymedes01/etfuturum/blocks/BlockNylium.java")
wart = read("src/main/java/ganymedes01/etfuturum/blocks/BlockNetherwart.java")
manager = read("src/main/java/ganymedes01/etfuturum/world/nether/biome/utils/NetherBiomeManager.java")
bridge = read("src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java")
parity = read("src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java")
lang_us = read("src/main/resources/assets/etfuturum/lang/en_US.lang")
lang_gb = read("src/main/resources/assets/etfuturum/lang/en_GB.lang")
manifest_path = ROOT / "scripts/modern_map_parity_blocks.json"
manifest = json.loads(manifest_path.read_text(encoding="utf-8")) if manifest_path.is_file() else {"blocks": []}
parity_names = [entry.get("name") for entry in manifest.get("blocks", [])]

# Every already-implemented normal block identity must be content-gated, not experiment-gated.
content_flags = {
    "enableCrimsonVegetation": "Crimson Nether ecology",
    "enableWarpedVegetation": "Warped Nether ecology",
    "enableMangroveEcology": "Mangrove roots ecology",
    "enableSculkBlocks": "Sculk base blocks",
    "enableLightningRodBlock": "Lightning Rod content",
    "enableEndGatewayBlock": "End Gateway content",
}
for flag, label in content_flags.items():
    require(config, f"public static boolean {flag};", label + " declaration")
    if not re.search(rf'getBoolean\("{re.escape(flag)}",\s*cat\w+,\s*true,', config):
        failures.append(f"{label} must default true independently of experimental generation: {flag}")

# The only generation/unfinished-mechanics toggles remain default-off.
for flag in ("enableCrimsonBlocks", "enableWarpedBlocks", "enableMangroveBlocks", "enableSculk", "enableLightningRod", "endDimensionProvider"):
    if not re.search(rf'getBoolean\("{re.escape(flag)}",\s*catExperiments,\s*false,', experiments):
        failures.append(f"experimental switch is not default-off: {flag}")
for flag in ("extendedWorldHeight", "modernOverworldGeneration", "modernOreGeneration", "modernLargeOreVeins", "lushCavesWorldgen", "dripstoneCavesWorldgen"):
    if not re.search(rf'getBoolean\("{re.escape(flag)}",\s*catGeneration,\s*false,', world):
        failures.append(f"world-safety default is not false: {flag}")

# Generation may require content, never the reverse.
for token in ("ConfigExperiments.enableCrimsonBlocks", "ConfigExperiments.enableWarpedBlocks"):
    require(manager, token, "Nether biome generation experiment gate")
for forbidden in ("enableCrimsonVegetation", "enableWarpedVegetation", "enableMangroveEcology", "enableCrimsonWoodFamily", "enableWarpedWoodFamily", "enableMangroveWoodFamily"):
    if forbidden in manager:
        failures.append(f"normal content flag leaked into biome-generation opt-in: {forbidden}")
require(config, "if (ConfigExperiments.enableCrimsonBlocks) {", "Crimson generation -> content dependency")
require(config, "enableCrimsonVegetation = true;", "Crimson generation -> vegetation dependency")
require(config, "if (ConfigExperiments.enableWarpedBlocks) enableWarpedVegetation = true;", "Warped generation -> vegetation dependency")
require(config, "if (ConfigExperiments.enableMangroveBlocks) {", "Mangrove generation -> content dependency")
require(config, "enableMangroveEcology = true;", "Mangrove generation -> roots dependency")

# No actual ModBlocks registration may disappear merely because an experiment is off.
if "ConfigExperiments." in blocks:
    failures.append("ModBlocks still contains a direct ConfigExperiments gate; block content must be independent of experimental worldgen")

expected_registrations = {
    "MANGROVE_ROOTS": "ConfigBlocksItems.enableMangroveEcology",
    "MUDDY_MANGROVE_ROOTS": "ConfigBlocksItems.enableMangroveEcology",
    "SHROOMLIGHT": "ConfigBlocksItems.enableCrimsonVegetation || ConfigBlocksItems.enableWarpedVegetation",
    "NETHER_ROOTS": "ConfigBlocksItems.enableCrimsonVegetation || ConfigBlocksItems.enableWarpedVegetation",
    "NETHER_FUNGUS": "ConfigBlocksItems.enableCrimsonVegetation || ConfigBlocksItems.enableWarpedVegetation",
    "NETHER_SPROUTS": "ConfigBlocksItems.enableWarpedVegetation",
    "NETHER_WART": "ConfigBlocksItems.enableNetherwartBlock || ConfigBlocksItems.enableWarpedVegetation",
    "NYLIUM": "ConfigBlocksItems.enableCrimsonVegetation || ConfigBlocksItems.enableWarpedVegetation",
    "WEEPING_VINES": "ConfigBlocksItems.enableCrimsonVegetation",
    "TWISTING_VINES": "ConfigBlocksItems.enableWarpedVegetation",
    "SCULK": "ConfigBlocksItems.enableSculkBlocks",
    "SCULK_CATALYST": "ConfigBlocksItems.enableSculkBlocks",
    "LIGHTNING_ROD": "ConfigBlocksItems.enableLightningRodBlock",
    "END_GATEWAY": "ConfigBlocksItems.enableEndGatewayBlock",
}
for enum_name, gate in expected_registrations.items():
    m = re.search(rf'^\s*{enum_name}\(([^\n]+)', blocks, re.M)
    if not m:
        failures.append(f"implemented modern block registration missing: {enum_name}")
    elif gate not in m.group(1):
        failures.append(f"{enum_name} is not controlled by normal content gate {gate}")

# Metadata-backed ecology blocks must expose both modern subtypes in Creative/NEI.
for text, requirements, label in (
    (roots, ("ConfigBlocksItems.enableCrimsonVegetation", "ConfigBlocksItems.enableWarpedVegetation"), "Nether roots"),
    (fungus, ("ConfigBlocksItems.enableCrimsonVegetation", "ConfigBlocksItems.enableWarpedVegetation"), "Nether fungi"),
    (nylium, ("ConfigBlocksItems.enableCrimsonVegetation", "ConfigBlocksItems.enableWarpedVegetation"), "Nylium"),
    (wart, ("ConfigBlocksItems.enableWarpedVegetation",), "Warped Wart Block"),
):
    forbid(text, "ConfigExperiments", label + " creative/content gate")
    for token in requirements:
        require(text, token, label + " subtype availability")

# Exact modern IDs handled by legacy metadata-backed EFR blocks must not be duplicated in the parity shell registry.
metadata_owned = {
    "mangrove_roots", "muddy_mangrove_roots", "shroomlight", "crimson_roots", "warped_roots",
    "crimson_fungus", "warped_fungus", "nether_sprouts", "crimson_nylium", "warped_nylium",
    "weeping_vines", "twisting_vines", "warped_wart_block", "sculk", "sculk_catalyst", "lightning_rod",
    "end_gateway",
}
for name in sorted(metadata_owned):
    if name in parity_names:
        failures.append(f"duplicate parity shell added for existing EFR block identity/subtype: {name}")
if manifest.get("target") != "Minecraft Java 1.21.11 visual block compatibility shells":
    failures.append("modern parity manifest target is no longer 1.21.11")
if manifest.get("count") != len(parity_names) or len(parity_names) != len(set(parity_names)):
    failures.append("modern parity manifest count/uniqueness invariant failed")

# Mangrove family and ecology pieces requested in this pass.
for token in (
    "MANGROVE_LOG", "MANGROVE_ROOTS", "MUDDY_MANGROVE_ROOTS", "MANGROVE_SIGN",
    "ConfigBlocksItems.enableMangroveWoodFamily", "ConfigBlocksItems.enableMangroveEcology",
):
    require(blocks, token, "Mangrove block-family coverage")
for parity_name in ("mangrove_leaves", "mangrove_propagule", "mangrove_hanging_sign", "mangrove_wall_hanging_sign", "mangrove_shelf"):
    if parity_name not in parity_names:
        failures.append(f"Mangrove parity identity missing: {parity_name}")

# Mangrove boats were registered already, but must participate in the common arrays and have English names.
for token in (
    "MANGROVE_OAK_BOAT(ConfigBlocksItems.enableNewBoats && ConfigBlocksItems.enableMangroveWoodFamily",
    "MANGROVE_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ConfigBlocksItems.enableMangroveWoodFamily",
    "DARK_OAK_BOAT, MANGROVE_OAK_BOAT, CHERRY_BOAT",
    "DARK_OAK_CHEST_BOAT, MANGROVE_CHEST_BOAT, CHERRY_CHEST_BOAT",
):
    require(items, token, "Mangrove boat integration")
for lang, locale in ((lang_us, "en_US"), (lang_gb, "en_GB")):
    require(lang, "item.etfuturum.mangrove_boat.name=Mangrove Boat", locale + " Mangrove Boat translation")
    require(lang, "item.etfuturum.mangrove_chest_boat.name=Mangrove Boat with Chest", locale + " Mangrove Chest Boat translation")

# AssetDirector-backed blocks (notably Pale Oak fence gate) publish a real model texture into the 1.7 atlas fallback.
for token in (
    "public static IIcon getFallbackIcon(PreparedModels models)",
    "if (model.flatIcon != null) return model.flatIcon;",
    "if (quad != null && quad.icon != null) return quad.icon;",
):
    require(bridge, token, "modern model fallback icon bridge")
if parity.count("ModernJsonModelBridge.getFallbackIcon(prepared)") < 2:
    failures.append("both parity model blocks and parity copper chests must replace bootstrap stone with a resolved model fallback icon")
require(parity, "if (fallback != null) blockIcon = fallback;", "resolved modern atlas fallback")
if "PALE_OAK_FENCE_GATE" not in parity:
    failures.append("Pale Oak fence gate parity identity missing")

# Existing legacy textures required by newly default-visible ecology blocks must be present.
textures = (
    "mangrove_roots_top.png", "mangrove_roots_side.png", "muddy_mangrove_roots_top.png", "muddy_mangrove_roots_side.png",
    "shroomlight.png", "crimson_roots.png", "warped_roots.png", "crimson_fungus.png", "warped_fungus.png",
    "nether_sprouts.png", "crimson_nylium.png", "crimson_nylium_side.png", "warped_nylium.png", "warped_nylium_side.png",
    "weeping_vines.png", "weeping_vines_plant.png", "twisting_vines.png", "twisting_vines_plant.png", "warped_wart_block.png",
    "sculk.png", "sculk_catalyst_top.png", "sculk_catalyst_side.png", "lightning_rod.png",
)
tex_root = ROOT / "src/main/resources/assets/minecraft/textures/blocks"
for texture in textures:
    if not (tex_root / texture).is_file():
        failures.append(f"newly default-visible existing block texture missing: {texture}")

# No new Mojang dump was introduced by this pass: the parity layer must continue using AssetDirector.
for p in (ROOT / "src/main/resources").rglob("*"):
    if p.is_file() and "1.21.11" in p.name.lower():
        failures.append(f"version-pinned Mojang asset was bundled directly: {p.relative_to(ROOT)}")

# Removed helper dependencies remain forbidden.
for p in (ROOT / "src/main/java").rglob("*.java"):
    text = p.read_text(encoding="utf-8", errors="ignore")
    if "roadhog360.hogutils" in text or "me.mrnavastar.r" in text:
        failures.append(f"removed helper dependency returned in {p.relative_to(ROOT)}")

if failures:
    print("Fidelity Pass 20 static validation FAILED")
    for failure in failures:
        print(" -", failure)
    sys.exit(1)

print("Fidelity Pass 20 static validation PASSED")
print(" - remaining implemented block content is independent of experimental generation switches")
print(" - Mangrove roots + Nether ecology + Sculk + Lightning Rod + End Gateway content default on")
print(" - Mangrove boats participate in shared arrays and have en_US/en_GB names")
print(" - Pale Oak/model-backed blocks publish an AssetDirector-resolved atlas fallback icon")
print(" - 1.21.11 parity manifest remains unique and worldgen safety defaults remain off")
