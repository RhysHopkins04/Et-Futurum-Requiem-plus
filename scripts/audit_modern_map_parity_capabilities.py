#!/usr/bin/env python3
"""Generate the current capability matrix for every modern parity identity.

This deliberately distinguishes a stable visual identity from verified state or
mechanic parity.  The matrix is generated from the canonical manifest so a new
identity cannot silently escape later fidelity audits.
"""

from __future__ import print_function

import argparse
import collections
import csv
import io
import json
import pathlib
import sys


FIELDS = (
    "block",
    "introduced",
    "style",
    "parity_create_path",
    "existing_efr_equivalent",
    "implementation_profile",
    "visual",
    "blockstate",
    "shape",
    "function",
    "nbt",
    "map_import",
    "review_status",
    "known_difference",
)

SPECIAL_STATE_NAMES = {
    "chiseled_bookshelf",
    "decorated_pot",
    "potted_torchflower",
    "suspicious_sand",
    "suspicious_gravel",
    "turtle_egg",
    "campfire",
    "soul_campfire",
    "scaffolding",
    "grindstone",
    "leaf_litter",
    "wildflowers",
    "pale_oak_button",
    "pale_oak_pressure_plate",
    "copper_torch",
    "copper_wall_torch",
}

PASS_30_MULTIFACE_NAMES = {"sculk_vein", "resin_clump"}

PASS_32_CONTRACT_NAMES = {
    "chiseled_bookshelf", "decorated_pot", "campfire", "soul_campfire",
    "suspicious_sand", "suspicious_gravel",
}

PASS_33_VISIBLE_NAMES = {
    "pale_oak_button", "pale_oak_pressure_plate", "copper_torch", "copper_wall_torch",
    "decorated_pot",
}

PASS_34_VISIBLE_NAMES = {
    "pale_moss_carpet", "pale_hanging_moss", "creaking_heart", "dried_ghast",
    "torchflower_crop", "pitcher_crop", "pitcher_plant", "sniffer_egg", "mangrove_propagule",
    "sea_pickle", "tall_seagrass",
}

PASS_35_VISIBLE_NAMES = {
    "trial_spawner", "vault", "crafter", "bell", "respawn_anchor", "sculk_sensor",
    "calibrated_sculk_sensor", "sculk_shrieker", "jigsaw", "repeating_command_block",
    "chain_command_block", "structure_block",
}


def has_pass32_contract(name, style):
    return (name in PASS_32_CONTRACT_NAMES or style in {"SHELF", "SIGN", "HANGING_SIGN"}
            or name.endswith("copper_chest"))

VISUAL_SHELL_GAPS = {
    "crafter": "No inventory, disabled-slot, recipe, redstone craft or block-entity implementation.",
    "trial_spawner": "No trial-spawner phase, cooldown, reward or block-entity implementation.",
    "vault": "No vault state machine, player interaction data or block-entity implementation.",
    "conduit": "No structure detection, activation, range, effects or target behaviour.",
    "bell": "No attachment state, ringing interaction, redstone response or animation state.",
    "lectern": "No stored book, page, GUI, comparator or redstone-pulse implementation.",
    "grindstone": "Orientation models exist; repair/disenchant GUI and XP mechanics do not.",
    "respawn_anchor": "No charges, dimension respawn, explosion or comparator implementation.",
    "sculk_sensor": "No vibration, phase, cooldown or redstone implementation.",
    "calibrated_sculk_sensor": "No calibration filtering, vibration phase or redstone implementation.",
    "sculk_shrieker": "No shrieking, cooldown, can_summon or player-trigger implementation.",
    "powder_snow": "No sinking, leather-boots collision, freezing or bucket behaviour.",
    "frogspawn": "No water-surface survival or hatch lifecycle.",
}


def repository_root(path=None):
    if path:
        return pathlib.Path(path).resolve()
    return pathlib.Path(__file__).resolve().parents[1]


def concrete_class(name, style):
    if name.endswith("copper_chest"):
        return "ParityCopperChestBlock"
    if style == "STAIRS":
        return "ParityStairBlock"
    if style == "SLAB":
        return "ParitySlabBlock"
    if name == "pale_oak_door":
        return "ParityDoorBlock"
    if name == "pale_oak_trapdoor":
        return "ParityTrapdoorBlock"
    if name == "pale_oak_pressure_plate":
        return "ParityPressurePlateBlock"
    return "ParityModelBlock"


def existing_efr_equivalent(name):
    known = {
        "lodestone": "ModBlocks.LODESTONE / BlockLodestone",
        "lightning_rod": "ModBlocks.LIGHTNING_ROD / BlockLightningRod",
        "mangrove_propagule": "ModBlocks.SAPLING metadata 0 / BlockModernSapling",
        "mangrove_leaves": "ModBlocks.LEAVES metadata 0 / BlockModernLeaves",
    }
    return known.get(name, "not resolved by the Pass 29 foundation")


def is_copper_weathering_family(name):
    return any(token in name for token in (
        "copper_chest",
        "copper_golem_statue",
        "copper_bars",
        "copper_chain",
        "copper_lantern",
        "lightning_rod",
    ))


def profile_for(name, style):
    if is_copper_weathering_family(name):
        return "PASS_36_COPPER_LIFECYCLE"
    if name in PASS_35_VISIBLE_NAMES or name.endswith("copper_golem_statue"):
        return "PASS_35_VISIBLE_STATE"
    if name in PASS_34_VISIBLE_NAMES:
        return "PASS_34_VISIBLE_STATE"
    if name in PASS_33_VISIBLE_NAMES or style == "CANDLE_CAKE":
        return "PASS_33_VISIBLE_STATE"
    if name in PASS_30_MULTIFACE_NAMES:
        return "PASS_30_MULTIFACE"
    if style == "LOG":
        return "PASS_29_AXIS_LOG"
    if style == "WALL":
        return "PASS_31_WALL_STATE"
    if name.endswith("copper_chest"):
        return "COPPER_CHEST_COMPAT"
    if style in {"STAIRS", "SLAB", "DOOR", "TRAPDOOR"}:
        return "DEDICATED_GEOMETRY"
    if (style in {"SIGN", "HANGING_SIGN", "CANDLE", "CANDLE_CAKE", "SHELF"}
            or style in {"PANE", "FENCE", "FENCE_GATE", "WALL"}
            or name in SPECIAL_STATE_NAMES
            or name.endswith("_froglight")
            or name.endswith("copper_chain")
            or name.endswith("copper_lantern")
            or name.endswith("lightning_rod")
            or name.endswith("_coral_fan")):
        return "SPECIALIZED_PARITY"
    return "VISUAL_SHELL"


def gap_for(name, style, profile):
    if profile == "PASS_36_COPPER_LIFECYCLE":
        if name.endswith("copper_golem_statue"):
            return "Block-local lifecycle, pose cycling and comparator output are implemented; Copper Golem entity reanimation and general waterlogging remain deferred."
        return "Oxidation, waxing, wax-off, scraping and lightning cleaning are implemented through shared IDegradable state-preserving transitions; general waterlogging remains deferred."
    if profile == "PASS_35_VISIBLE_STATE":
        return "Persistent visible state is represented exactly for map import; large gameplay/state-machine mechanics remain explicitly deferred."
    if profile == "PASS_34_VISIBLE_STATE":
        return "Visible/import state is represented exactly; lifecycle/entity/water mechanics remain explicitly deferred."
    if profile == "PASS_33_VISIBLE_STATE":
        if name == "pale_oak_button": return "Visible face/facing/powered state and wooden-button timing are represented exactly; projectile activation remains deferred."
        if name == "pale_oak_pressure_plate": return "Raised/depressed state and normal wooden pressure-plate mechanics are represented."
        if name in {"copper_torch", "copper_wall_torch"}: return "Standing/wall registry split, wall facing, placement and support behaviour are represented; no separate wall item exists."
        if name == "decorated_pot": return "Cracked is persisted exactly but is visually irrelevant in Mojang 1.21.11 assets; existing sherd/item/facing state remains Pass 32f-sensitive."
        if name.endswith("candle_cake"): return "Lit state is exact; normal cake eating/bite conversion remains deferred."
    if profile == "PASS_30_MULTIFACE":
        return ("Six-face placement/render/support/drop/persistence parity is implemented; "
                "waterlogging and block-specific spreading/growth mechanics remain deferred.")
    if profile == "PASS_29_AXIS_LOG":
        return "None for valid non-waterlogged axis and axe-stripping states; metadata 3 renders as Y fallback."
    if name in VISUAL_SHELL_GAPS:
        return VISUAL_SHELL_GAPS[name]
    if profile == "PASS_31_WALL_STATE":
        return ("Neighbour-derived none/low/tall/up parity is implemented; waterlogging and explicit "
                "debug-stick/import overrides are intentionally not stored in 1.7 metadata.")
    if is_copper_weathering_family(name):
        return "Static identity exists; oxidation, waxing and scraping lifecycle remains deferred."
    if name.endswith("candle_cake"):
        return "Lit state exists; normal cake eating/bite conversion remains incomplete."
    if name == "turtle_egg":
        return "Egg count and hatch models exist; hatching and trampling lifecycle remains incomplete."
    if name in {"leaf_litter", "wildflowers"}:
        return "Amount/facing state exists; exact support, bonemeal and spreading behaviour needs review."
    if has_pass32_contract(name, style):
        return ("Persistent target state is defined by docs/BACKPORTER_STATE_CONTRACT.json; "
                "entry-specific unsupported properties remain explicit in that contract.")
    if name.endswith("_froglight"):
        return "Axis state is represented; retain as a regression-sensitive family."
    if profile == "DEDICATED_GEOMETRY":
        return "Uses a real 1.7-compatible block class; exact modern state import remains under review."
    return "Exact 1.21.11 state and mechanics have not yet been classified; do not infer completeness from the model."


def row_for(entry):
    name = entry["name"]
    style = entry["style"]
    profile = profile_for(name, style)
    row = {
        "block": name,
        "introduced": entry["ver"],
        "style": style,
        "parity_create_path": concrete_class(name, style),
        "existing_efr_equivalent": existing_efr_equivalent(name),
        "implementation_profile": profile,
        "visual": "AssetDirector 1.21.11 model/item bridge",
        "blockstate": "default state only",
        "shape": "generic style bounds",
        "function": "visual identity shell",
        "nbt": "none",
        "map_import": "registry identity only",
        "review_status": "REQUIRES_EXACT_1_21_11_AUDIT",
        "known_difference": gap_for(name, style, profile),
    }

    if profile == "PASS_36_COPPER_LIFECYCLE":
        row.update({
            "blockstate": entry.get("meta") or "existing Pass 29-35 metadata preserved across copper identity transitions",
            "shape": "existing AssetDirector model/bounds; state is preserved through lifecycle replacement",
            "function": "shared IDegradable natural oxidation + wax on/off + one-stage scrape + lightning cleaning",
            "nbt": "none unless family already owns a TileEntity; Copper Chest snapshots/restores full TileEntityChest NBT",
            "map_import": "static registry/metadata mapping remains authoritative; Pass 36 adds runtime lifecycle without re-encoding importer state",
            "review_status": "PASS_36_VERIFIED",
            "mechanics_review_status": "PASS_36_VERIFIED",
        })
        if name.endswith("copper_chest"):
            row["review_status"] = "PASS_32_CONTRACT_VERIFIED"
            row["blockstate"] = "vanilla chest facing plus exact weathering/waxed registry identity and persisted EFRPairDirection"
            row["function"] = "IDegradable lifecycle plus copper-family pairing; normal/trapped chests stay incompatible"
            row["nbt"] = "TileEntityChest Items/CustomName plus EFRPairDirection preserved during identity replacement"
        elif name.endswith("copper_golem_statue"):
            row["review_status"] = "PASS_35_VERIFIED"
            row["blockstate"] = "meta=pose*4+facing; lifecycle preserves all 16 states"
            row["function"] = "IDegradable lifecycle; non-axe pose cycling; comparator 1..4; entity reanimation deferred"
            row["nbt"] = "none"
        else:
            row["nbt"] = "none"
    elif profile == "PASS_35_VISIBLE_STATE":
        row.update({
            "blockstate": entry.get("meta") or "Pass 35 exact technical visible state",
            "shape": "exact AssetDirector 1.21.11 state model; Bell/statue entity-driven geometry retained",
            "function": "state/import parity only; gameplay systems deferred",
            "nbt": "Vault/Crafter use tiny synchronized state TEs where metadata is insufficient",
            "map_import": "deterministic Pass 35 mapping in docs/BACKPORTER_STATE_CONTRACT.json",
            "review_status": "PASS_35_VERIFIED",
        })
        if name.endswith("copper_golem_statue"):
            row["blockstate"] = "meta=pose*4+facing; facing N/E/S/W 0..3; pose standing/sitting/running/star 0..3"
            row["nbt"] = "none"
        elif name == "vault":
            row["nbt"] = "ParityVaultStateTileEntity VaultState byte 0..3"
        elif name == "crafter":
            row["nbt"] = "ParityCrafterStateTileEntity Triggered + Crafting booleans"
        else:
            row["nbt"] = "none"
    elif profile == "PASS_34_VISIBLE_STATE":
        row.update({
            "blockstate": entry.get("meta") or "Pass 34 exact visible state",
            "shape": "exact AssetDirector 1.21.11 model state; bounded legacy selection/collision where applicable",
            "function": "persistent/import-visible state only; lifecycle mechanics deferred",
            "nbt": "Pass 34 TE only where metadata is insufficient",
            "map_import": "deterministic Pass 34 mapping in docs/BACKPORTER_STATE_CONTRACT.json",
            "review_status": "PASS_34_VERIFIED",
        })
        if name == "pale_moss_carpet":
            row["nbt"] = "ParityPaleMossCarpetTileEntity Bottom + four side enums"
        elif name == "mangrove_propagule":
            row["nbt"] = "BlockModernSapling.MangrovePropaguleStateTileEntity Hanging + Age"
            row["parity_create_path"] = "existing ModBlocks.SAPLING metadata 0 / BlockModernSapling (no duplicate parity block)"
        else:
            row["nbt"] = "none"
    elif profile == "PASS_33_VISIBLE_STATE":
        if name == "pale_oak_button":
            row.update({
                "blockstate": "metadata 0..11 = face(wall/floor/ceiling) x facing(N/E/S/W); Powered boolean in parity TE",
                "shape": "orientation-aware 6x4x2/1 pressed-unpressed button bounds",
                "function": "support checks, activation, 30-tick wooden-button release and redstone output",
                "nbt": "ParityButtonTileEntity Powered boolean",
                "map_import": "face/facing -> metadata 0..11; powered -> TE Powered",
                "review_status": "PASS_33_VERIFIED",
            })
        elif name == "pale_oak_pressure_plate":
            row.update({
                "blockstate": "powered=false/true in metadata 0/1",
                "shape": "BlockPressurePlate raised/depressed bounds with exact JSON model state",
                "function": "normal wooden pressure-plate entity detection and redstone",
                "nbt": "none",
                "map_import": "powered -> metadata 0/1",
                "review_status": "PASS_33_VERIFIED",
            })
        elif name in {"copper_torch", "copper_wall_torch"}:
            row.update({
                "blockstate": "standing identity has no metadata state; wall identity uses metadata side 2=N,3=S,4=W,5=E",
                "shape": "standing/wall torch bounds and exact Mojang JSON model",
                "function": "single obtainable standing item; wall placement swaps to technical wall identity; support loss drops standing item",
                "nbt": "none",
                "map_import": "minecraft:copper_torch -> etfuturum:copper_torch; minecraft:copper_wall_torch facing -> etfuturum:copper_wall_torch metadata 2..5",
                "review_status": "PASS_33_VERIFIED",
            })
        elif name == "decorated_pot":
            row.update({
                "blockstate": "facing metadata plus Pass 32f stored-water extension; Cracked boolean persisted in TE",
                "shape": "existing exact decorated-pot parity model/bounds",
                "function": "existing one-stack/shatter/sherd implementation preserved",
                "nbt": "ParityDecoratedPotTileEntity sherds/item/Cracked",
                "map_import": "deterministic Pass 33 contract; cracked stored exactly but classified visually irrelevant",
                "review_status": "PASS_33_VERIFIED",
            })
        else:
            row.update({
                "blockstate": "lit=false/true in metadata 0/1",
                "shape": "exact Candle Cake JSON model selected by lit metadata",
                "function": "ignition/extinguishing and dynamic light retained",
                "nbt": "none",
                "map_import": "lit -> metadata 0/1",
                "review_status": "PASS_33_VERIFIED",
            })
    elif profile == "PASS_29_AXIS_LOG":
        row.update({
            "blockstate": "axis=y/x/z in metadata 0/1/2",
            "shape": "full cube",
            "function": "axis-aware placement and axe stripping; canonical item damage 0",
            "map_import": "axis maps deterministically to metadata 0/1/2",
            "review_status": "PASS_29_VERIFIED",
        })
    elif profile == "PASS_30_MULTIFACE":
        row.update({
            "blockstate": "six booleans in synchronized FaceMask bits DOWN/UP/NORTH/SOUTH/WEST/EAST",
            "shape": "union outline and exact ray target from 1/16-thick attached faces; no collision",
            "function": "supported placement, same-block face merging, support pruning and face-count drops",
            "nbt": "ParityMultifaceTileEntity FaceMask integer",
            "map_import": "six boolean properties map deterministically to FaceMask bits 0..5",
            "review_status": "PASS_30_VERIFIED",
        })
    elif profile == "PASS_31_WALL_STATE":
        row.update({
            "blockstate": "north/east/south/west=none|low|tall plus up, derived from live neighbours",
            "shape": "8px post; 6px arms at 14px low or 16px tall; independent 1.5-block collision prisms",
            "function": "modern wall connections and post/side recomputation without consuming subtype metadata",
            "map_import": "source wall connection/up properties are derived state and intentionally discarded",
            "review_status": "PASS_31_VERIFIED",
        })
    elif profile == "COPPER_CHEST_COMPAT":
        row.update({
            "blockstate": "vanilla chest facing/adjacency plus exact registry weathering/waxed identity",
            "shape": "vanilla chest bounds and renderer",
            "function": "vanilla chest inventory/lid/comparator foundation",
            "nbt": "TileEntityChest-compatible Items plus optional CustomName",
            "map_import": "deterministic static Pass 32 contract plus Pass 36 runtime lifecycle",
            "review_status": "PASS_32_CONTRACT_VERIFIED",
        })
    elif profile == "DEDICATED_GEOMETRY":
        row.update({
            "blockstate": "dedicated 1.7-compatible metadata",
            "shape": "dedicated stair/slab/door/trapdoor class",
            "function": "normal 1.7-compatible block mechanics",
            "map_import": "property-to-metadata mapping required",
            "review_status": "REGRESSION_SENSITIVE",
        })
    elif profile == "SPECIALIZED_PARITY":
        row.update({
            "blockstate": "specialized metadata, derived state and/or tile data",
            "shape": "specialized or neighbour-derived bounds",
            "function": "partial specialized implementation",
            "map_import": "partial; verify every source property and NBT field",
            "review_status": "REGRESSION_SENSITIVE",
        })
        if name in {"chiseled_bookshelf", "decorated_pot", "suspicious_sand", "suspicious_gravel"} or style == "SHELF":
            row["nbt"] = "dedicated parity tile entity"
        elif name in {"campfire", "soul_campfire"}:
            row["nbt"] = "four-slot cooking parity tile entity"
        elif style in {"SIGN", "HANGING_SIGN"}:
            row["nbt"] = "two-sided sign tile entity"
        if has_pass32_contract(name, style):
            row["map_import"] = "deterministic Pass 32 target metadata/NBT contract in docs/BACKPORTER_STATE_CONTRACT.json"
            row["review_status"] = "PASS_32_CONTRACT_VERIFIED"
    return row


def build_matrix(root):
    manifest_path = root / "scripts" / "modern_map_parity_blocks.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    return manifest, [row_for(entry) for entry in manifest["blocks"]]


def validate_matrix(manifest, rows):
    errors = []
    declared = manifest.get("count")
    if declared != 244:
        errors.append("manifest count must be 244 after the Pass 33 Copper Wall Torch identity, found {!r}".format(declared))
    if len(rows) != declared:
        errors.append("generated row count {} does not match manifest {}".format(len(rows), declared))
    names = [row["block"] for row in rows]
    duplicates = sorted(name for name, count in collections.Counter(names).items() if count != 1)
    if duplicates:
        errors.append("duplicate/missing unique identities: {}".format(", ".join(duplicates)))
    for index, row in enumerate(rows):
        missing = [field for field in FIELDS if not row.get(field)]
        if missing:
            errors.append("row {} ({}) has empty fields: {}".format(index, row.get("block"), ", ".join(missing)))
    pass29 = {row["block"] for row in rows if row["review_status"] == "PASS_29_VERIFIED"}
    expected = {"pale_oak_log", "stripped_pale_oak_log"}
    if pass29 != expected:
        errors.append("Pass 29 verified set must be exactly {}, found {}".format(sorted(expected), sorted(pass29)))
    return errors


def emit_json(manifest, rows):
    payload = {
        "schema": 1,
        "target": manifest["target"],
        "source_manifest_count": manifest["count"],
        "blocks": rows,
    }
    print(json.dumps(payload, indent=2, sort_keys=False))


def emit_tsv(rows):
    output = io.StringIO()
    writer = csv.DictWriter(output, fieldnames=FIELDS, delimiter="\t", lineterminator="\n")
    writer.writeheader()
    writer.writerows(rows)
    sys.stdout.write(output.getvalue())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", help="repository root (defaults to the script's parent repository)")
    parser.add_argument("--format", choices=("json", "tsv"), default="json")
    parser.add_argument("--check", action="store_true", help="validate full manifest coverage without emitting the matrix")
    args = parser.parse_args()

    manifest, rows = build_matrix(repository_root(args.repo))
    errors = validate_matrix(manifest, rows)
    if errors:
        for error in errors:
            print("ERROR: " + error, file=sys.stderr)
        return 1
    if args.check:
        print("Modern map parity capability audit PASSED: {} identities classified".format(len(rows)))
        return 0
    if args.format == "tsv":
        emit_tsv(rows)
    else:
        emit_json(manifest, rows)
    return 0


if __name__ == "__main__":
    sys.exit(main())
