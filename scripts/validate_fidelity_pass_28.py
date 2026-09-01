#!/usr/bin/env python3
"""Static regression gates for Fidelity Pass 28 plus bounded 28a/28b corrections."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

def require(text, needle, label):
    if needle not in text:
        errors.append(f"missing {label}: {needle}")

main = read("src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java")
bridge = read("src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java")
renderer = read("src/main/java/ganymedes01/etfuturum/client/renderer/tileentity/TileEntityModernParityRenderer.java")
proxy = read("src/main/java/ganymedes01/etfuturum/core/proxy/ClientProxy.java")
converter = read("src/main/java/ganymedes01/etfuturum/core/utils/structurenbt/BlockStateConverter.java")
archaeology = read("src/main/java/ganymedes01/etfuturum/ModernArchaeology.java")
recipes = read("src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java")
sounds = read("src/main/java/ganymedes01/etfuturum/EtFuturum.java")
dynamic_sounds = read("src/main/java/ganymedes01/etfuturum/client/DynamicSoundsResourcePack.java")
wood_fence = read("src/main/java/ganymedes01/etfuturum/blocks/BlockWoodFence.java")
modern_fence = read("src/main/java/ganymedes01/etfuturum/blocks/BlockModernWoodFence.java")
fence_mixin = read("src/main/java/ganymedes01/etfuturum/mixins/early/fencewallconnect/MixinBlockFence.java")
us = read("src/main/resources/assets/etfuturum/lang/en_US.lang")
gb = read("src/main/resources/assets/etfuturum/lang/en_GB.lang")

require(bridge, "other instanceof BlockWall", "legacy-wall visual connection")
require(bridge, "other instanceof BlockFence", "legacy-fence visual connection")
require(main, "new ParityStairBlock(this, base)", "real stair behavior with modern inventory model")
require(main, "ParitySlabBlock(false", "real single/double slab behavior")
require(main, "ParityDoorBlock", "real Pale Oak door behavior")
require(main, "ParityTrapdoorBlock", "real Pale Oak trapdoor behavior")
require(main, "ModernJsonModelBridge.prepare(entry, reg)", "functional-block inventory model preparation")
require(main, "return entry.getRegistryName();", "modern slab language identity")
require(main, "ParityDoorItemBlock", "Pale Oak door inventory icon alias")
require(converter, "parity.getDoubleSlab()", "double-slab map-state conversion")
require(main, "7.0F / 16.0F", "corrected copper-lantern bounds")
require(main, "default: horizontal = 1.0F - hitX", "bookshelf north slot mapping")
require(main, "if (stored != null)", "occupied bookshelf removal before insertion")
require(main, "ParityShelfTileEntity extends TileEntity implements ISidedInventory", "Shelf inventory")
require(main, "swapPoweredShelfGroup", "powered Shelf hotbar swapping")
require(main, "int hotbar = 9 - shelfCount * 3", "rightmost powered-Shelf hotbar range")
require(renderer, "public static final class Shelf", "Shelf displayed-item TESR")
require(renderer, "rotate(0.82D - slot * 0.32D", "Shelf display/click slot agreement")
require(proxy, "ParityShelfTileEntity.class", "Shelf TESR binding")
require(main, "isDecoratedPot() ? 0.0F", "instant decorated-pot hand breaking")
require(main, "world.setBlock(x, y, z, Blocks.water", "water release after pot break")
require(dynamic_sounds, 'addSoundsToCategory("block.decorated_pot.insert"', "local decorated-pot insert sound")
require(dynamic_sounds, 'addSoundsToCategory("block.decorated_pot.shatter"', "local decorated-pot shatter sound")
require(dynamic_sounds, '"minecraft:random/glass1"', "valid local decorated-pot shatter fallback")
if '"minecraft:dig/glass1"' in dynamic_sounds:
    errors.append("decorated-pot shatter fallback still references a nonexistent 1.7 sound path")
if 'config.addSoundEvent(ver, "block.decorated_pot.' in sounds:
    errors.append("decorated-pot sounds must not gate AssetDirector client-JAR loading")
require(archaeology, "setMaxDamage(64)", "Brush durability")
require(main, "ParityBrushableTileEntity", "suspicious-block brushable data")
require(main, "fallSuspiciousBlock", "suspicious-block gravity")
require(main, "falling.field_145810_d = tileData", "buried-item NBT retention while falling")
require(bridge, 'state.put("dusted"', "four dusted visual states")
require(recipes, "ModernArchaeology.getBrush()", "Brush recipe")

for label, source in (("wood fence", wood_fence), ("modern wood fence", modern_fence),
                      ("vanilla/modded fence mixin", fence_mixin)):
    require(source, "ModernMapParityBlocks.Style.FENCE", f"{label} reciprocal Pale Oak connection")

for label, lang in (("en_US", us), ("en_GB", gb)):
    require(lang, "item.etfuturum.angler_pottery_sherd.name=Angler Pottery Sherd", f"{label} Angler name")
    if "+item.etfuturum.angler_pottery_sherd" in lang:
        errors.append(f"{label} still contains a literal leading + in the Angler key")

if errors:
    print("Fidelity Pass 28 validation FAILED", file=sys.stderr)
    for error in errors:
        print(" - " + error, file=sys.stderr)
    raise SystemExit(1)

print("Fidelity Pass 28/28a/28b validation PASSED")
print(" - wall/fence visual connections, bookshelf slots, and copper-lantern bounds are gated")
print(" - Pale Oak/Resin stairs and slabs plus Pale Oak door/trapdoor use functional block classes")
print(" - Shelf storage/display/hotbar swapping and Brush archaeology are registered and persistent")
print(" - decorated-pot hardness, sound registration, and water release are gated")
print(" - reciprocal Pale Oak fence links, Shelf slot order, inventory models, and suspicious gravity are gated")
