#!/usr/bin/env python3
"""Static regression gate for the Pass 30b vertical render-orientation correction."""

from pathlib import Path
import subprocess
import sys


ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(__file__).resolve().parents[1]
BRIDGE = ROOT / "src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java"
DOC = ROOT / "docs/MODERN_MAP_PARITY_CAPABILITY_MATRIX.md"


def main():
    errors = []
    bridge = BRIDGE.read_text(encoding="utf-8")
    doc = DOC.read_text(encoding="utf-8")
    marker = "int maskFace = face == 0 ? 1 : face == 1 ? 0 : face;"
    if bridge.count(marker) != 1:
        errors.append("the bounded DOWN/UP model adapter must exist exactly once")
    if "state.put(faces[face], Boolean.toString((mask & (1 << maskFace)) != 0));" not in bridge:
        errors.append("multiface model selection does not use the adapted vertical face")
    if "invert `FaceMask`" not in doc:
        errors.append("the unchanged persistence/import contract is not documented")

    for validator in ("validate_fidelity_pass_29.py", "validate_fidelity_pass_30.py"):
        result = subprocess.run([sys.executable, str(ROOT / "scripts" / validator), str(ROOT)],
                                cwd=str(ROOT), stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, text=True)
        if result.returncode:
            errors.append("{} failed:\n{}".format(validator, result.stdout.strip()))

    if errors:
        print("Pass 30b validation FAILED")
        for error in errors:
            print("- " + error)
        return 1
    print("Pass 30b validation passed: vertical model adaptation is bounded and state semantics are unchanged.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
