#!/usr/bin/env python3
"""Static regression gate for Pass 30's deliberately bounded multiface foundation."""

from pathlib import Path
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[1]
BLOCKS = ROOT / "src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java"
BRIDGE = ROOT / "src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java"
DOC = ROOT / "docs/MODERN_MAP_PARITY_CAPABILITY_MATRIX.md"


def require(text, needle, label, errors):
    if needle not in text:
        errors.append("missing {}: {!r}".format(label, needle))


def main():
    errors = []
    blocks = BLOCKS.read_text(encoding="utf-8")
    bridge = BRIDGE.read_text(encoding="utf-8")
    doc = DOC.read_text(encoding="utf-8")

    for needle, label in (
        ("this == SCULK_VEIN || this == RESIN_CLUMP", "exact two-block scope"),
        ("MULTIFACE_ALL_FACES = 0x3F", "six-bit mask"),
        ('MULTIFACE_FACE_MASK_TAG = "FaceMask"', "stable NBT key"),
        ("class ParityMultifaceTileEntity", "shared tile entity"),
        ("class ParityMultifaceItemBlock", "face-merging item"),
        ("supportedMultifaceMask", "support pruning"),
        ("Integer.bitCount(getMultifaceFaceMask", "face-count drops"),
        ("EnchantmentHelper.getSilkTouchModifier", "Sculk Silk Touch rule"),
        ("setMultifaceFaceBounds", "per-face ray bounds"),
    ):
        require(blocks, needle, label, errors)

    for needle, label in (
        ("new Model[64]", "64-slot model cache"),
        ("entry.usesMultifaceState()", "bounded dynamic model path"),
        ("prepared.multifaceModels[mask]", "all model combinations"),
        ("ModernMapParityBlocks.getMultifaceFaceMask", "world render state"),
    ):
        require(bridge, needle, label, errors)

    require(doc, "bit 0 `down`, bit 1 `up`, bit 2 `north`, bit 3 `south`", "import bit order", errors)
    require(doc, "Waterlogging", "explicit deferred scope", errors)

    audit = subprocess.run(
        [sys.executable, str(ROOT / "scripts/audit_modern_map_parity_capabilities.py"), "--check"],
        cwd=str(ROOT), stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    if audit.returncode:
        errors.append("capability audit failed:\n{}".format(audit.stdout.strip()))

    if errors:
        print("Pass 30 validation FAILED")
        for error in errors:
            print("- " + error)
        return 1
    print("Pass 30 validation passed: shared six-face state remains scoped to Sculk Vein and Resin Clump.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
