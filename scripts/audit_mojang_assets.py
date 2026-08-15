#!/usr/bin/env python3
"""Audit directly bundled minecraft-domain assets.

Plus uses MCLib AssetDirector for modern Mojang assets. This script intentionally does not claim
that every remaining minecraft-domain file is unlawful: some are EFR-created compatibility art,
modified derivatives, or legacy overrides that need human provenance review. It does make the
remaining migration debt explicit and prevents migrated Caves & Cliffs assets from returning.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
MC = ROOT / "src/main/resources/assets/minecraft"
BRIDGE = ROOT / "src/main/java/ganymedes01/etfuturum/client/ModernAssetResourcePack.java"

text = BRIDGE.read_text(encoding="utf-8")
aliased = set(re.findall(r'\"(textures/[^\"]+)\"', text))
regressions = []
for rel in sorted(aliased):
    if (MC / rel).is_file():
        regressions.append(rel)

all_files = [p.relative_to(MC).as_posix() for p in MC.rglob("*") if p.is_file()]
print(f"AssetDirector aliases: {len(aliased)}")
print(f"Remaining directly bundled assets/minecraft files: {len(all_files)}")
if regressions:
    print("ERROR: migrated AssetDirector assets were re-bundled:")
    for rel in regressions:
        print(f" - {rel}")
    sys.exit(1)
print("Caves & Cliffs migrated-asset guard PASSED")
