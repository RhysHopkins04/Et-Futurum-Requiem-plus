#!/usr/bin/env python3
"""Static regression gates for Fidelity Pass 29b."""

from __future__ import print_function

import importlib.util
import pathlib
import sys


sys.dont_write_bytecode = True


ROOT = pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parents[1]
REGISTRY = ROOT / "src/main/java/ganymedes01/etfuturum/api/StrippedLogRegistry.java"
AUDIT = ROOT / "scripts/audit_modern_map_parity_capabilities.py"
DOC = ROOT / "docs/MODERN_MAP_PARITY_CAPABILITY_MATRIX.md"

errors = []


def require(text, needle, label):
    if needle not in text:
        errors.append(label)


for path in (REGISTRY, AUDIT, DOC):
    if not path.is_file():
        errors.append("missing required file: {}".format(path.relative_to(ROOT)))

if not errors:
    registry_text = REGISTRY.read_text(encoding="utf-8")
    audit_text = AUDIT.read_text(encoding="utf-8")
    doc_text = DOC.read_text(encoding="utf-8")

    for meta in range(3):
        mapping = (
            "addLog(ModernMapParityBlocks.PALE_OAK_LOG.get(), {0}, "
            "ModernMapParityBlocks.STRIPPED_PALE_OAK_LOG.get(), {0});"
        ).format(meta)
        require(registry_text, mapping,
                "Pale Oak Log axis metadata {} lacks a matching stripping conversion".format(meta))

    require(registry_text,
            "addLog(ModernMapParityBlocks.PALE_OAK_WOOD.get(), 0, "
            "ModernMapParityBlocks.STRIPPED_PALE_OAK_WOOD.get(), 0);",
            "Pale Oak Wood no longer converts to Stripped Pale Oak Wood")
    require(registry_text,
            "ConfigBlocksItems.enableModernMapParityBlocks && ConfigBlocksItems.enableStrippedLogs",
            "Pale Oak stripping must remain behind the existing feature gates")
    require(registry_text,
            "if (ConfigBlocksItems.enableBarkLogs && ModernMapParityBlocks.PALE_OAK_WOOD.get() != null",
            "Pale Oak Wood stripping must remain behind bark-log support")

    require(audit_text, "axis-aware placement and axe stripping; canonical item damage 0",
            "capability matrix does not record verified axe stripping")
    require(doc_text, "The conversion is metadata-preserving: `0 -> 0`, `1 -> 1`",
            "Pass 29b axis-preserving stripping contract is undocumented")

    spec = importlib.util.spec_from_file_location("pass29b_capability_audit", str(AUDIT))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    manifest, rows = module.build_matrix(ROOT)
    errors.extend(module.validate_matrix(manifest, rows))
    axis_rows = {row["block"]: row for row in rows if row["style"] == "LOG"}
    for name in ("pale_oak_log", "stripped_pale_oak_log"):
        row = axis_rows.get(name)
        if row is None or "axe stripping" not in row["function"]:
            errors.append("{} capability row lacks the Pass 29b stripping contract".format(name))

if errors:
    print("Fidelity Pass 29b validation FAILED", file=sys.stderr)
    for error in errors:
        print(" - " + error, file=sys.stderr)
    sys.exit(1)

print("Fidelity Pass 29b validation PASSED")
print(" - Pale Oak Log strips in Y/X/Z metadata states 0/1/2")
print(" - every valid axis is preserved during conversion")
print(" - Pale Oak Wood retains its bark-to-stripped-wood conversion")
print(" - feature gates and the 243-row capability audit remain intact")
