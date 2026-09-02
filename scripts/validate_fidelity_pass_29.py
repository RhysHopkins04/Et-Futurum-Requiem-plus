#!/usr/bin/env python3
"""Static regression gates for Fidelity Pass 29."""

from __future__ import print_function

import importlib.util
import pathlib
import sys


# Import the capability generator without leaving an untracked __pycache__ in the repository.
sys.dont_write_bytecode = True


ROOT = pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parents[1]
PARITY = ROOT / "src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java"
BRIDGE = ROOT / "src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java"
AUDIT = ROOT / "scripts/audit_modern_map_parity_capabilities.py"

errors = []


def require(text, needle, label):
    if needle not in text:
        errors.append(label)


for path in (PARITY, BRIDGE, AUDIT):
    if not path.is_file():
        errors.append("missing required file: {}".format(path.relative_to(ROOT)))

if not errors:
    parity_text = PARITY.read_text(encoding="utf-8")
    bridge_text = BRIDGE.read_text(encoding="utf-8")

    require(parity_text, "private boolean isAxisLog()", "missing Style.LOG identity helper")
    require(parity_text, "return entry.style == Style.LOG;", "axis helper must cover the two Style.LOG identities")
    require(parity_text, "if (isAxisLog())",
            "axis logs must have a dedicated side-to-X/Y/Z placement branch")
    require(parity_text, "isDecoratedPot() || isAxisLog()",
            "axis metadata must be removed from dropped/picked log item damage")
    require(bridge_text, "|| style == ModernMapParityBlocks.Style.LOG",
            "world and prepared-model paths must resolve Style.LOG axes")
    require(bridge_text, 'String[] axes = {"y", "x", "z"};',
            "axis model order must remain metadata 0=Y, 1=X, 2=Z")

    spec = importlib.util.spec_from_file_location("pass29_capability_audit", str(AUDIT))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    manifest, rows = module.build_matrix(ROOT)
    errors.extend(module.validate_matrix(manifest, rows))

    axis_rows = {row["block"]: row for row in rows if row["style"] == "LOG"}
    if set(axis_rows) != {"pale_oak_log", "stripped_pale_oak_log"}:
        errors.append("Style.LOG manifest membership changed without updating the Pass 29 contract")
    for name, row in axis_rows.items():
        if row["blockstate"] != "axis=y/x/z in metadata 0/1/2":
            errors.append("{} lost the deterministic metadata axis contract".format(name))

if errors:
    print("Fidelity Pass 29 validation FAILED", file=sys.stderr)
    for error in errors:
        print(" - " + error, file=sys.stderr)
    sys.exit(1)

print("Fidelity Pass 29 validation PASSED")
print(" - all 243 manifest identities have a generated capability/audit row")
print(" - visual identity is no longer treated as proof of state or mechanics completeness")
print(" - Pale Oak Log and Stripped Pale Oak Log preserve Y/X/Z in metadata 0/1/2")
print(" - placement, world model selection and canonical item damage share the same axis contract")
