#!/usr/bin/env python3
from pathlib import Path
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
CONTRACT_PATH = ROOT / "docs/BACKPORTER_STATE_CONTRACT.json"
MPB_PATH = ROOT / "src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java"
BUILD_PATH = ROOT / "build.gradle"

CONTRACT = json.loads(CONTRACT_PATH.read_text())
MPB = MPB_PATH.read_text()
BUILD = BUILD_PATH.read_text()

errors = []

def need(cond, msg):
    if not cond:
        errors.append(msg)

need(str(CONTRACT.get("contract_revision")) == "36",
     "Backporter contract revision must be Pass 36")
need(CONTRACT.get("implemented_in_version") == "3.5.8",
     "Pass 36 contract must record planned development version 3.5.8")
need(CONTRACT.get("implemented_in_git_ref") is None,
     "unreleased Pass 36 contract must not self-reference a commit or claim a release tag")
need(CONTRACT.get("release_status") == "unreleased",
     "Pass 36 contract must explicitly record unreleased status")
need('String stagingVersion = "3.5.8"' in BUILD,
     "build.gradle stagingVersion must be 3.5.8 for the Pass 36 development state")
need(CONTRACT.get("implemented_in_version") != "3.5.5",
     "stale 3.5.5 implementation provenance remains in the Pass 36 contract")
need(CONTRACT.get("implemented_in_git_ref") != "refs/tags/3.5.5",
     "stale refs/tags/3.5.5 provenance remains in the Pass 36 contract")

blocks = {entry.get("key"): entry for entry in CONTRACT.get("blocks", [])}
chest = blocks.get("copper_chest_family", {})
behaviour = chest.get("exactness", {}).get("behaviour", "")
need("every Copper Chest oxidation/wax identity as one compatible family" in behaviour,
     "Copper Chest contract does not document cross-stage/wax family compatibility")
need("normal/trapped chests remain incompatible" in behaviour,
     "Copper Chest contract does not retain normal/trapped incompatibility")
need("EFRPairDirection" in behaviour,
     "Copper Chest contract does not document reciprocal EFRPairDirection preservation")

stale = (
    "The block mixin requires exact\n"
    "        // block identity, so copper oxidation/wax variants cannot cross-pair with each other or vanilla."
)
need(stale not in MPB,
     "ParityCopperChestTileEntity comment still claims exact-block-identity-only pairing")
need("every Copper Chest" in MPB and "oxidation/wax identity as one compatible family" in MPB,
     "ParityCopperChestTileEntity comment does not document Pass 36 Copper Chest family compatibility")
need("normal/trapped chests remain incompatible" in MPB,
     "ParityCopperChestTileEntity comment does not document normal/trapped incompatibility")
need("EFRPairDirection" in MPB,
     "ParityCopperChestTileEntity comment does not document reciprocal EFRPairDirection persistence")

if errors:
    print("Fidelity Pass 36d validation FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("Fidelity Pass 36d validation PASSED")
print(" - Backporter contract provenance is revision 36, planned version 3.5.8 and unreleased")
print(" - unreleased provenance uses no self-referential git SHA/tag and stale 3.5.5 provenance is removed")
print(" - build.gradle stagingVersion is 3.5.8")
print(" - Copper Chest cross-stage/wax family compatibility is documented")
print(" - normal/trapped incompatibility and reciprocal EFRPairDirection remain documented")
