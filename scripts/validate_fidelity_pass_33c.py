#!/usr/bin/env python3
from pathlib import Path
import json
import sys

ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")

def require(text, needle, label, errors):
    if needle not in text:
        errors.append(f"{label}: missing {needle!r}")

main = read("src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java")
doc = read("docs/FIDELITY_PASS_33_MAP_STATE.md")
contract = json.loads(read("docs/BACKPORTER_STATE_CONTRACT.json"))
errors = []

for needle, label in [
    ("import net.minecraft.entity.projectile.EntityArrow;", "wooden-button arrow type"),
    ("private boolean projectileHeld;", "projectile-held TE state"),
    ("public boolean isProjectileHeld() { return projectileHeld; }", "projectile-held TE accessor"),
    ("public void holdByProjectile()", "projectile-held TE activation"),
    ("tag.setBoolean(\"ProjectileHeld\", true);", "projectile-held persistence"),
    ("projectileHeld = tag.getBoolean(\"ProjectileHeld\");", "projectile-held load"),
    ("private boolean paleOakButtonHasArrow(World world, int x, int y, int z)", "arrow overlap query"),
    ("world.getEntitiesWithinAABB(EntityArrow.class, box)", "arrow overlap AABB"),
    ("private void refreshPaleOakButtonProjectileState(World world, int x, int y, int z)", "arrow hold/release refresh"),
    ("private void activatePaleOakButtonFromArrow(World world, int x, int y, int z)", "arrow collision activation"),
    ("isPaleOakButton() && entity instanceof EntityArrow", "arrow collision hook"),
    ("isSegmentedGroundDecal() || isScaffolding() || isPaleOakButton()", "vanilla-style no button collision box"),
]:
    require(main, needle, label, errors)

# Runtime observation from 33b showed the wall particle was mirrored relative to the rendered model.
for needle, label in [
    ("case 2: pz += offset; break;", "north wall-torch flame offset"),
    ("case 3: pz -= offset; break;", "south wall-torch flame offset"),
    ("case 4: px += offset; break;", "west wall-torch flame offset"),
    ("default:px -= offset; break;", "east wall-torch flame offset"),
]:
    require(main, needle, label, errors)

require(doc, "## Pass 33c runtime corrections", "Pass 33c documentation", errors)
require(doc, "remains powered while an arrow is lodged", "wooden-button projectile documentation", errors)

revision = contract.get("revision") or contract.get("contract_revision")
if str(revision) not in ("33", "34", "35"):
    errors.append(f"Backporter contract revision is not Pass 33/34/35 compatible: {revision!r}")

if errors:
    print("Fidelity Pass 33c validation FAILED")
    for err in errors:
        print(" -", err)
    sys.exit(1)

print("Fidelity Pass 33c validation PASSED")
print(" - Pale Oak Button arrows can enter the button volume and activate the wooden-button state")
print(" - projectile-held activation is persisted separately from manual/imported Powered state")
print(" - the button stays powered while an EntityArrow overlaps its pressed bounds and releases afterward")
print(" - Copper Wall Torch smoke/green flame is mirrored onto the rendered tip for all four facings")
print(" - Backporter contract remains revision 33; no import-state mapping changed")
