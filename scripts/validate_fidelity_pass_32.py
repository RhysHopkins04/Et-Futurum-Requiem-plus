#!/usr/bin/env python3
"""Static regression gates for Fidelity Pass 32 block-entity/import fidelity."""

from __future__ import print_function

import importlib.util
import json
import pathlib
import sys

sys.dont_write_bytecode = True
ROOT = pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parents[1]
errors = []


def read(rel):
    path = ROOT / rel
    if not path.is_file():
        errors.append("missing required file: " + rel)
        return ""
    return path.read_text(encoding="utf-8", errors="ignore")


def require(text, token, label):
    if token not in text:
        errors.append(label + ": missing " + token)


def contract_entry(contract, key):
    for entry in contract.get("blocks", []):
        if entry.get("key") == key:
            return entry
    errors.append("Backporter contract missing block entry " + key)
    return {}


parity = read("src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java")
cave_te = read("src/main/java/ganymedes01/etfuturum/tileentities/TileEntityCaveVines.java")
cave_block = read("src/main/java/ganymedes01/etfuturum/blocks/BlockCaveVines.java")
sign_te = read("src/main/java/ganymedes01/etfuturum/tileentities/TileEntityWoodSign.java")
banner_te = read("src/main/java/ganymedes01/etfuturum/tileentities/TileEntityBanner.java")
shulker_te = read("src/main/java/ganymedes01/etfuturum/tileentities/TileEntityShulkerBox.java")
barrel_te = read("src/main/java/ganymedes01/etfuturum/tileentities/TileEntityBarrel.java")
hive_te = read("src/main/java/ganymedes01/etfuturum/tileentities/TileEntityBeeHive.java")
common = read("src/main/java/ganymedes01/etfuturum/core/proxy/CommonProxy.java")
doc = read("docs/FIDELITY_PASS_32_BLOCK_ENTITY_IMPORT.md")
contract_text = read("docs/BACKPORTER_STATE_CONTRACT.json")

# Chiseled Bookshelf: persistent last slot, interaction updates, comparator output and old occupancy fallback.
for token in (
    "private int lastInteractedSlot = -1;",
    "getLastInteractedSlot()",
    "setLastInteractedSlot(int slot)",
    'tag.setInteger("LastInteractedSlot", lastInteractedSlot)',
    'tag.hasKey("LastInteractedSlot")',
    "shelf.setLastInteractedSlot(slot);",
    "isChiseledBookshelf() || super.hasComparatorInputOverride()",
    "return slot >= 0 && slot < 6 ? slot + 1 : 0;",
    "worldObj.func_147453_f(xCoord, yCoord, zCoord, getBlockType())",
    'new ItemStack(Items.book)',
):
    require(parity, token, "Chiseled Bookshelf Pass 32 state")

# Cave Vines: canonical Age, old-save compatibility, network sync, natural maturity gate.
for token in (
    "private int age = 0;",
    "public int getAge()",
    "public void setAge(int value)",
    'compound.setInteger("Age", age)',
    'compound.hasKey("Age")',
    'compound.hasKey("MaxLength")',
    'compound.getBoolean("TipSheared")',
    "27 - this.maxLength",
    "public Packet getDescriptionPacket()",
    "public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet)",
):
    require(cave_te, token, "Cave Vine Age persistence/migration")
for token in (
    "oldVine.getAge()",
    "newVine.setAge(Math.min(25, oldAge + (manualPlace ? 0 : 1)))",
    "teCaveVines.getAge() < 25",
):
    require(cave_block, token, "Cave Vine Age growth")

# Existing TE contracts must remain real and bounded rather than being replaced.
for token in (
    "public int getOccupancyMask()",
    "if (items[slot] != null) mask |= 1 << slot;",
    "if (isShelf()) {",
    "((ParityShelfTileEntity) tile).getOccupancyMask()",
    "if (isDecoratedPot()) {",
    "Container.calcRedstoneFromInventory((IInventory) tile)",
    "neighborEntry.getStyle() != Style.SHELF",
    "(meta & 3) == facing && (meta & 4) != 0",
):
    require(parity, token, "Pass 32e Shelf comparator/mixed-wood gameplay")

for token in (
    "private final ItemStack[] items = new ItemStack[3];",
    'tag.setTag("Items", list);',
    "// Modern order: back, left, right, front.",
    'tag.setTag("sherds", list);',
    'tag.setTag("item", itemTag);',
    "private final ItemStack[] cookingItems = new ItemStack[4];",
    'tag.setTag("CookingItems", list);',
    'tag.setInteger("brush_count", progress);',
    "falling.field_145810_d = tileData;",
    "public static final class ParityCopperChestTileEntity extends TileEntityChest",
):
    require(parity, token, "existing Pass 32 parity TE contract")

for token in (
    'compound.setString("BackText" + (i + 1), backText[i])',
    'compound.setInteger("FrontTextColour", frontTextColour)',
    'compound.setInteger("BackTextColour", backTextColour)',
    'compound.setBoolean("FrontGlowing", frontGlowing)',
    'compound.setBoolean("BackGlowing", backGlowing)',
    'compound.setBoolean("Waxed", waxed)',
):
    require(sign_te, token, "sign front/back data contract")
for token in ('nbt.setInteger("Base", baseColor)', 'nbt.setBoolean("IsStanding", isStanding)', 'nbt.setTag("Patterns", patterns)'):
    require(banner_te, token, "banner contract")
for token in ('nbt.setTag("Items"', 'nbt.setByte("Color", color)', 'nbt.setByte("Facing", facing)', 'nbt.setString("CustomName"'):
    require(shulker_te, token, "shulker contract")
for token in ('compound.setTag("Items"', 'compound.setByte("Type"', 'compound.setString("CustomName"'):
    require(barrel_te, token, "barrel contract")
for token in ('compound.setTag("Bees"', 'compound.setInteger("honeyLevel", honeyLevel)'):
    require(hive_te, token, "beehive contract")

# Machine-readable contract syntax and semantic coverage.
try:
    contract = json.loads(contract_text) if contract_text else {}
except Exception as exc:
    errors.append("Backporter contract is not valid JSON: {}".format(exc))
    contract = {}

if contract.get("schema") != 1:
    errors.append("Backporter contract schema must be 1")
if contract.get("source_minecraft") != "1.21.11":
    errors.append("Backporter contract source_minecraft must be 1.21.11")
if contract.get("contract_revision") not in ("32e", "32f"):
    errors.append("Backporter contract contract_revision must be 32e or a later Pass 32 finalization revision")
if contract.get("implemented_in_version") != "3.5.5":
    errors.append("Backporter contract implemented_in_version must be 3.5.5")
if contract.get("implemented_in_git_ref") != "refs/tags/3.5.5":
    errors.append("Backporter contract implemented_in_git_ref must be refs/tags/3.5.5")
if "authoritative_source_commit" in contract or "generated_by_pass" in contract:
    errors.append("Backporter contract must not retain stale Pass 31/32c provenance fields")

required_entries = {
    "chiseled_bookshelf", "cave_vines_tip", "cave_vines_plant", "shelf_family", "decorated_pot",
    "campfire", "soul_campfire", "suspicious_sand", "suspicious_gravel", "sign_family",
    "hanging_sign_family", "copper_chest_family", "banner_family", "shulker_box_family", "barrel",
    "beehive", "bee_nest", "blast_furnace", "smoker", "glow_lichen", "sculk_vein", "resin_clump",
}
entries = {entry.get("key"): entry for entry in contract.get("blocks", [])}
missing = sorted(required_entries - set(entries))
if missing:
    errors.append("Backporter contract missing required entries: " + ", ".join(missing))

chiseled = contract_entry(contract, "chiseled_bookshelf")
if chiseled.get("classification") != "FULL_IMPORT":
    errors.append("chiseled_bookshelf must be FULL_IMPORT after Pass 32")
ch_fields = {f.get("name") for f in chiseled.get("tile_entity", {}).get("fields", [])}
for name in ("Items", "OccupancyMask", "LastInteractedSlot"):
    if name not in ch_fields:
        errors.append("chiseled_bookshelf contract missing TE field " + name)

cave = contract_entry(contract, "cave_vines_tip")
cave_fields = {f.get("name") for f in cave.get("tile_entity", {}).get("fields", [])}
for name in ("Age", "MaxLength", "TipSheared"):
    if name not in cave_fields:
        errors.append("cave_vines_tip contract missing TE field " + name)

shelf = contract_entry(contract, "shelf_family")
shelf_json = json.dumps(shelf)
if "side_chain" not in " ".join(shelf.get("derived", [])):
    errors.append("shelf contract must declare side_chain derived")
if "ANY wood type" not in shelf_json:
    errors.append("shelf contract must allow mixed-wood powered same-facing side_chain derivation")
if "three TE slots 0..2" not in shelf_json:
    errors.append("shelf contract must document all three inventory slots")
comparator = shelf.get("comparator", {})
if comparator.get("mode") != "occupied_slot_bitmask" or comparator.get("slot_bits") != {"0": 1, "1": 2, "2": 4}:
    errors.append("shelf contract comparator must be occupancy bitmask 1/2/4")
if comparator.get("output_range") != "0..7" or comparator.get("stack_size_affects_output") is not False:
    errors.append("shelf comparator contract must be 0..7 and stack-size independent")
if "waterlogged" not in " ".join(shelf.get("unsupported", [])):
    errors.append("shelf contract must explicitly retain waterlogging as unsupported")

pot = contract_entry(contract, "decorated_pot")
if "back, left, right, front" not in json.dumps(pot):
    errors.append("decorated pot contract must lock sherd order back/left/right/front")

camp = contract_entry(contract, "campfire")
if "four" not in json.dumps(camp).lower() or "CookingItems" not in json.dumps(camp):
    errors.append("campfire contract must document four CookingItems slots")

brush = contract_entry(contract, "suspicious_sand")
if "concrete item" not in json.dumps(brush).lower():
    errors.append("brushable contract must require concrete resolved buried item")

sign = contract_entry(contract, "sign_family")
for name in ("Text1..Text4", "BackText1..BackText4", "FrontTextColour", "BackTextColour", "Waxed"):
    if name not in json.dumps(sign):
        errors.append("sign contract missing " + name)

copper = contract_entry(contract, "copper_chest_family")
if len(copper.get("source_ids", [])) != 8:
    errors.append("copper chest contract must list all eight weathering/waxed identities")
if "Items" not in json.dumps(copper):
    errors.append("copper chest contract must retain inventory")

# All registered EFR TE families plus Crafter/Lectern must be classified in one audit.
audit = {entry.get("implementation"): entry.get("classification") for entry in contract.get("tile_entity_audit", [])}
for impl in (
    "ParityChiseledBookshelfTileEntity", "TileEntityCaveVines", "ParityShelfTileEntity",
    "ParityDecoratedPotTileEntity", "ParityCampfireTileEntity", "ParityBrushableTileEntity",
    "ParitySignTileEntity", "ParityCopperChestTileEntity", "ParityMultifaceTileEntity",
    "TileEntityWoodSign", "TileEntityBanner", "TileEntityShulkerBox", "TileEntityBarrel",
    "TileEntityBeeHive", "TileEntityBlastFurnace", "TileEntitySmoker", "TileEntityGlowLichen",
    "TileEntitySculkCatalyst", "TileEntityNewBrewingStand", "TileEntityNewBeacon", "TileEntityGateway",
    "TileEntityCauldronPotion", "Crafter", "Lectern",
):
    if audit.get(impl) not in {"FULL_IMPORT", "PARTIAL_IMPORT", "VISUAL_ONLY_IMPORT", "UNSUPPORTED"}:
        errors.append("tile_entity_audit missing valid classification for " + impl)

for token in (
    'GameRegistry.registerTileEntity(TileEntityBarrel.class',
    'GameRegistry.registerTileEntity(TileEntitySmoker.class',
    'GameRegistry.registerTileEntity(TileEntityBlastFurnace.class',
    'GameRegistry.registerTileEntity(TileEntityShulkerBox.class',
    'GameRegistry.registerTileEntity(TileEntityBanner.class',
    'GameRegistry.registerTileEntity(TileEntityBeeHive.class',
    'GameRegistry.registerTileEntity(TileEntityGlowLichen.class',
    'GameRegistry.registerTileEntity(TileEntityCaveVines.class',
):
    require(common, token, "registered mature TE audit source")

for token in (
    "docs/BACKPORTER_STATE_CONTRACT.json",
    "back, left, right, front",
    "Crafter",
    "Lectern",
    "Waterlogging remains outside Pass 32",
):
    require(doc, token, "Pass 32 human-readable contract")

# Capability rows that are actually in the 243-entry parity layer should now point at Pass 32.
audit_path = ROOT / "scripts" / "audit_modern_map_parity_capabilities.py"
if audit_path.is_file():
    spec = importlib.util.spec_from_file_location("pass32_capability_audit", str(audit_path))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    manifest, rows = module.build_matrix(ROOT)
    errors.extend(module.validate_matrix(manifest, rows))
    wanted = {"chiseled_bookshelf", "decorated_pot", "campfire", "soul_campfire", "suspicious_sand", "suspicious_gravel"}
    wanted.update(entry["name"] for entry in manifest["blocks"] if entry["style"] in {"SHELF", "SIGN", "HANGING_SIGN"})
    wanted.update(entry["name"] for entry in manifest["blocks"] if entry["name"].endswith("copper_chest"))
    by_name = {row["block"]: row for row in rows}
    wrong = sorted(name for name in wanted if by_name.get(name, {}).get("review_status") != "PASS_32_CONTRACT_VERIFIED")
    if wrong:
        errors.append("Pass 32 capability rows not promoted: " + ", ".join(wrong))

if errors:
    print("Fidelity Pass 32 validation FAILED", file=sys.stderr)
    for error in errors:
        print(" - " + error, file=sys.stderr)
    sys.exit(1)

print("Fidelity Pass 32 validation PASSED")
print(" - Chiseled Bookshelf persists/synchronizes LastInteractedSlot and exposes modern comparator output")
print(" - Cave Vine tips persist canonical Age 0..25 with old MaxLength/TipSheared migration")
print(" - Shelf, Pot, Campfire, Brushable, Sign and Copper Chest contracts match current EFR persistence")
print(" - banners, shulkers, barrels, hives and furnace variants are explicitly classified")
print(" - one machine-readable 1.21.11 -> EFR registry/metadata/tile-NBT contract is present")
print(" - Crafter/Lectern and other large deferred mechanics remain outside Pass 32")
