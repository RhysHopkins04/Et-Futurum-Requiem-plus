#!/usr/bin/env python3
"""Static regression guard for Fidelity Pass 25 baseline/runtime-warning cleanup."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
failures = []


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


def require(text, token, label):
    if token not in text:
        failures.append(f"{label} missing: {token}")


def forbid(text, token, label):
    if token in text:
        failures.append(f"{label} returned: {token}")


dynamic = read("src/main/java/ganymedes01/etfuturum/client/DynamicSoundsResourcePack.java")
main = read("src/main/java/ganymedes01/etfuturum/EtFuturum.java")
recipes = read("src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java")
at = read("src/main/resources/META-INF/etfuturum_at.cfg")
world = read("src/main/java/ganymedes01/etfuturum/configuration/configs/ConfigWorld.java")

# --- Sound baseline ---------------------------------------------------------
# AssetDirector must fetch the real sign-wax target, never the hanging alias as if
# it were an OGG path. The dynamic pack supplies the alias itself.
require(main, 'config.addSoundEvent(ver, "block.sign.waxed_interact_fail", "block")',
        "concrete sign wax target registration")
forbid(main, 'config.addSoundEvent(ver, "block.hanging_sign.waxed_interact_fail", "block")',
       "unsafe direct registration of modern type=event alias")

# Minecraft 1.7 already supports sounds.json type=event. Its SoundHandler resolves a
# BARE event-entry name in the containing event's namespace. The Pass19 versioned
# prefix caused SoundHandler to apply minecraft_1.21.11 twice, leaving an empty alias.
for token in (
    'addSoundEventsToCategory("block.hanging_sign.waxed_interact_fail"',
    '"block.sign.waxed_interact_fail"',
    'soundObj.add("type", new JsonPrimitive("event"))',
):
    require(dynamic, token, "1.7-compatible hanging-sign wax alias")
forbid(dynamic, 'Tags.MC_ASSET_VER + ":block.sign.waxed_interact_fail"',
       "double-namespaced hanging-sign event target")
forbid(dynamic, 'block/sign/waxed_interact_fail1',
       "unnecessary hard-coded wax OGG flattening")

# All interaction sounds introduced during the recent parity passes must remain
# explicitly registered or generated through the audited family loop.
for event in (
    "item.dye.use", "item.glow_ink_sac.use", "item.ink_sac.use",
    "block.sign.waxed_interact_fail", "item.honeycomb.wax_on",
    "item.lead.break", "item.lead.tied", "item.lead.untied",
    "block.lily_pad.place", "block.candle.place", "block.candle.extinguish",
    "item.flintandsteel.use", "item.firecharge.use",
):
    require(main, f'config.addSoundEvent(ver, "{event}"', f"modern sound registration {event}")
for family in (
    "scaffolding", "hanging_sign", "nether_wood_hanging_sign",
    "bamboo_wood_hanging_sign", "cherry_wood_hanging_sign",
):
    require(main, f'"{family}"', f"modern sound-family registration {family}")

# --- RecipeSorter baseline --------------------------------------------------
# These are the two custom recipe classes Forge repeatedly reported as unknown.
for cls in ("RecipeFixedFireworks", "RecipeDyedShulkerBox"):
    token = f'RecipeSorter.register(Tags.MOD_ID + ".{cls}", {cls}.class, Category.SHAPELESS, "after:minecraft:shapeless");'
    require(recipes, token, f"RecipeSorter registration for {cls}")

# Registration must occur before map-compat's early return and before recipes are
# inserted into CraftingManager/GameRegistry.
init_pos = recipes.find("public static void init()")
map_return_pos = recipes.find("if (ConfigMapCompatibility.isEnabled())", init_pos)
for cls in ("RecipeFixedFireworks", "RecipeDyedShulkerBox"):
    reg_pos = recipes.find(f'RecipeSorter.register(Tags.MOD_ID + ".{cls}"', init_pos)
    if reg_pos < 0 or (map_return_pos >= 0 and reg_pos > map_return_pos):
        failures.append(f"{cls} RecipeSorter registration occurs too late")

# --- Access-transformer baseline -------------------------------------------
require(at, "public net.minecraft.client.Minecraft field_110446_Y #fileAssets",
        "valid Minecraft.fileAssets AT mapping")
forbid(at, "field_110607_c", "stale nonexistent Minecraft.fileAssets AT mapping")
if len(re.findall(r'^public net\.minecraft\.client\.Minecraft .*#fileAssets$', at, re.M)) != 1:
    failures.append("Minecraft.fileAssets should have exactly one AT mapping")

# --- Resource/dependency/build hygiene -------------------------------------
if (ROOT / "src/main/resources/assets/README.md").exists():
    failures.append("uppercase assets/README.md would be treated as an invalid resource namespace")

for p in (ROOT / "src/main/java").rglob("*.java"):
    text = p.read_text(encoding="utf-8", errors="ignore")
    if "roadhog360.hogutils" in text:
        failures.append(f"removed HogUtils runtime dependency returned in {p.relative_to(ROOT)}")
    if "me.mrnavastar.r" in text:
        failures.append(f"removed shaded helper returned in {p.relative_to(ROOT)}")
for rel in ("dependencies.gradle", "build.gradle", "settings.gradle"):
    p = ROOT / rel
    if p.exists():
        text = p.read_text(encoding="utf-8", errors="ignore")
        if "hogutils" in text.lower() or "me.mrnavastar.r" in text:
            failures.append(f"removed helper dependency returned in {rel}")

# Keep the existing world-safety contract untouched while cleaning runtime noise.
for name in (
    "extendedWorldHeight", "modernOverworldGeneration", "modernOreGeneration",
    "modernLargeOreVeins", "lushCavesWorldgen", "dripstoneCavesWorldgen",
):
    if not re.search(rf'getBoolean\("{re.escape(name)}",\s*catGeneration,\s*false,', world):
        failures.append(f"world-safety default is not false: {name}")

if failures:
    print("Fidelity Pass 25 baseline validation FAILED")
    for failure in failures:
        print(" -", failure)
    sys.exit(1)

print("Fidelity Pass 25 baseline validation PASSED")
print(" - hanging-sign wax failure uses the native 1.21.11 event alias with 1.7-correct same-domain resolution")
print(" - AssetDirector downloads only the concrete target while the 1.7 dynamic pack owns the alias")
print(" - custom fireworks and dyed-shulker recipes are registered with Forge RecipeSorter")
print(" - stale duplicate Minecraft.fileAssets access-transformer mapping is removed")
print(" - resource namespace, Java-8 dependency and removed-helper hygiene remain guarded")
print(" - experimental Plus worldgen defaults remain off")
