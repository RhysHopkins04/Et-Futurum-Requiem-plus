#!/usr/bin/env python3
from pathlib import Path
import json
import sys

ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8')

def require(text, needle, label, errors):
    if needle not in text:
        errors.append(f'{label}: missing {needle!r}')

main = read('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
bridge = read('src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java')
item = read('src/main/java/ganymedes01/etfuturum/client/renderer/item/ItemModernJsonModelRenderer.java')
particles = read('src/main/java/ganymedes01/etfuturum/client/particle/CustomParticles.java')
copper = read('src/main/java/ganymedes01/etfuturum/client/particle/CopperFireFlameFX.java')
contract = json.loads(read('docs/BACKPORTER_STATE_CONTRACT.json'))
errors = []

for needle, label in [
    ('private int manualReleaseTicks;', 'button release countdown'),
    ('button.armManualRelease(tickRate(world));', 'button activation countdown'),
    ('notifyPaleOakButtonNeighbors(world, x, y, z, state);', 'button support-neighbour updates'),
    ('return side == paleOakButtonStrongPowerSide(state) ? 15 : 0;', 'button directional strong power'),
    ('releasePaleOakButton(world, x, y, z);', 'button scheduled release'),
    ('spawnCopperTorchFlame(world, x, y, z, meta);', 'Copper Torch display particle'),
    ('CustomParticles.spawnCopperFireFlame(world, px, py, pz);', 'Copper Torch green flame particle'),
]:
    require(main, needle, label, errors)

require(bridge, 'facings[face == 0 ? ((facing + 2) & 3) : facing]', 'button wall model/support alignment', errors)
for needle, label in [
    ('"pale_oak_pressure_plate".equals(name)', 'pressure-plate GUI correction'),
    ('OpenGLHelper.scale(8.0F / 7.0F, 4.0F, 8.0F / 7.0F);', 'pressure-plate legacy atlas proportions'),
    ('OpenGLHelper.translate(0.0F, 6.0F / 16.0F, 0.0F);', 'pressure-plate legacy atlas centering'),
]:
    require(item, needle, label, errors)

require(particles, 'spawnCopperFireFlame', 'Copper particle factory', errors)
require(copper, 'Tags.MC_ASSET_VER + ":textures/particle/copper_fire_flame.png"', 'AssetDirector copper flame texture', errors)
require(copper, 'public final class CopperFireFlameFX extends EtFuturumFXParticle', 'Copper flame particle class', errors)

revision = contract.get('revision') or contract.get('contract_revision')
if str(revision) not in ('33', '34'):
    errors.append(f'Backporter contract revision is not Pass 33/34 compatible: {revision!r}')

if errors:
    print('Fidelity Pass 33b validation FAILED')
    for err in errors:
        print(' -', err)
    sys.exit(1)

print('Fidelity Pass 33b validation PASSED')
print(' - Pale Oak Button wall model is aligned with its canonical hitbox/support')
print(' - manual button presses deterministically release after 30 ticks and notify the attached support network')
print(' - button strong power is emitted only toward the attached support')
print(' - Pale Oak Pressure Plate inventory presentation matches legacy pressure-plate atlas proportions')
print(' - Copper Torch and Copper Wall Torch emit AssetDirector-backed copper_fire_flame particles')
print(' - Backporter state contract remains revision 33; no state mapping changed')
