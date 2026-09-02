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
}

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
    "sculk_vein": "Six independent attachment faces are not represented.",
    "resin_clump": "Six independent attachment faces are not represented.",
    "pale_oak_button": "Visual button shell; wooden-button power and projectile mechanics are absent.",
    "pale_oak_pressure_plate": "Visual layer shell; entity detection and redstone mechanics are absent.",
    "powder_snow": "No sinking, leather-boots collision, freezing or bucket behaviour.",
    "frogspawn": "No water-surface survival or hatch lifecycle.",
    "sniffer_egg": "No crack/hatch lifecycle.",
    "creaking_heart": "No axis, active state, environment checks or resin response.",
    "dried_ghast": "No hydration state or timed transformation lifecycle.",
    "copper_golem_statue": "Pose/orientation state and copper lifecycle are not represented.",
    "copper_torch": "Standing/wall identity, wall facing and support behaviour are not represented.",
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
    if style == "LOG":
        return "PASS_29_AXIS_LOG"
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
    if profile == "PASS_29_AXIS_LOG":
        return "None for valid non-waterlogged axis and axe-stripping states; metadata 3 renders as Y fallback."
    if name in VISUAL_SHELL_GAPS:
        return VISUAL_SHELL_GAPS[name]
    if style == "WALL":
        return "Connections are boolean/low only; per-side tall and exact up state remain deferred."
    if is_copper_weathering_family(name):
        return "Static identity exists; oxidation, waxing and scraping lifecycle remains deferred."
    if name.endswith("candle_cake"):
        return "Lit state exists; normal cake eating/bite conversion remains incomplete."
    if name == "turtle_egg":
        return "Egg count and hatch models exist; hatching and trampling lifecycle remains incomplete."
    if name in {"leaf_litter", "wildflowers"}:
        return "Amount/facing state exists; exact support, bonemeal and spreading behaviour needs review."
    if name in {"campfire", "soul_campfire", "decorated_pot", "chiseled_bookshelf"} or style == "SHELF":
        return "Substantial implementation exists; exact modern semantics and map-import NBT remain under review."
    if style in {"SIGN", "HANGING_SIGN"}:
        return "Substantial two-sided implementation exists; modern text/NBT conversion remains a Backporter contract."
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

    if profile == "PASS_29_AXIS_LOG":
        row.update({
            "blockstate": "axis=y/x/z in metadata 0/1/2",
            "shape": "full cube",
            "function": "axis-aware placement and axe stripping; canonical item damage 0",
            "map_import": "axis maps deterministically to metadata 0/1/2",
            "review_status": "PASS_29_VERIFIED",
        })
    elif profile == "COPPER_CHEST_COMPAT":
        row.update({
            "blockstate": "vanilla chest facing/adjacency plus registry weathering identity",
            "shape": "vanilla chest bounds and renderer",
            "function": "vanilla chest inventory/lid/comparator foundation",
            "nbt": "TileEntityChest-compatible inventory",
            "map_import": "identity and vanilla chest NBT; oxidation transition contract pending",
            "review_status": "REGRESSION_SENSITIVE",
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
    return row


def build_matrix(root):
    manifest_path = root / "scripts" / "modern_map_parity_blocks.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    return manifest, [row_for(entry) for entry in manifest["blocks"]]


def validate_matrix(manifest, rows):
    errors = []
    declared = manifest.get("count")
    if declared != 243:
        errors.append("manifest count must remain 243, found {!r}".format(declared))
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
