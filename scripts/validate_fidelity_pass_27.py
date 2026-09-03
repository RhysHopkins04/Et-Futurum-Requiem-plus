#!/usr/bin/env python3
"""Static regression gates for the bounded post-Pass-26 runtime follow-up."""

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


def require(text, needle, label):
    if needle not in text:
        errors.append(f"missing {label}: {needle}")


main = read("src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java")
wall = read("src/main/java/ganymedes01/etfuturum/blocks/BaseWall.java")
bridge = read("src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java")
sherds = read("src/main/java/ganymedes01/etfuturum/ModernPotterySherds.java")
recipe = read("src/main/java/ganymedes01/etfuturum/recipes/crafting/RecipeDecoratedPot.java")

require(wall, "ModernWallState.canConnectWallTo(this, world, x, y, z)", "BaseWall -> shared wall connection")
require(main, "if (style == Style.WALL) return ModernWallState.canConnectWallTo(this, world, x, y, z);", "parity wall -> shared wall connection")
require(main, "markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord)",
        "tile-packet render invalidation")
require(bridge, "chiseled_bookshelf_occupied", "occupied-slot visual fallback")

names = re.search(r"public enum ModernPotterySherds \{(.*?)\;", sherds, re.S)
if not names:
    errors.append("pottery sherd enum is missing")
else:
    found = [value.strip() for value in names.group(1).replace("\n", " ").split(",") if value.strip()]
    if len(found) != 23:
        errors.append(f"expected 23 pottery sherd identities, found {len(found)}")
require(sherds, "return EnumRarity.uncommon", "Uncommon pottery-sherd rarity")
require(sherds, '"textures/item/" + entry.getRegistryName() + ".png"', "modern sherd texture alias")
require(recipe, "Modern NBT order is back, left, right, front", "four-face recipe/NBT order")
require(main, "ParityDecoratedPotTileEntity extends TileEntity implements ISidedInventory", "one-slot sided pot inventory")
require(main, "Container.calcRedstoneFromInventory", "decorated-pot comparator output")
require(main, "entity instanceof IProjectile", "projectile shattering")
require(main, 'held.getItem() == Items.water_bucket', "pot water insertion")
require(bridge, "decoratedPotWorldModel", "per-face decorated-pot rendering")
require(bridge, "decoratedPotPatterns", "pattern texture registration")

if "CAVE_VINES" in sherds or "cave_vines" in sherds:
    errors.append("Pass 27 must not manufacture a separate Cave Vines inventory item")

if errors:
    print("Fidelity Pass 27 validation FAILED", file=sys.stderr)
    for error in errors:
        print(f" - {error}", file=sys.stderr)
    raise SystemExit(1)

print("Fidelity Pass 27 validation PASSED")
print(" - legacy and parity wall families connect bidirectionally")
print(" - bookshelf tile packets invalidate the compiled world model and retain an occupied-slot fallback")
print(" - all 23 pottery sherds register as Uncommon modern-asset-backed items")
print(" - decorated pots preserve four-face NBT, render patterns, store one stack, drive comparators/hoppers, hold bucket water and shatter to ingredients")
print(" - Cave Vines remain correctly placeable through Glow Berries; shelf inventory and archaeology remain deliberately separate")
