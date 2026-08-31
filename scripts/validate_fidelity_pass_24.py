#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
bridge = (root / 'src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java').read_text()
renderer = (root / 'src/main/java/ganymedes01/etfuturum/client/renderer/block/BlockModernJsonModelRenderer.java').read_text()
blocks = (root / 'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()

fail = []

# Root cause guard: Modern FaceBakery UP order is NW,SW,SE,NE. The bridge's legacy makeFace(UP)
# geometry is SE,NE,NW,SW, exactly a two-vertex/180-degree offset. Coral fan textures are
# directional and depend on their authored 0/90/270 face rotations, so the upward quads need
# the bounded UV correction while their geometry remains unchanged.
facebakery_up = ('NW', 'SW', 'SE', 'NE')
legacy_up = ('SE', 'NE', 'NW', 'SW')
if legacy_up != facebakery_up[2:] + facebakery_up[:2]:
    fail.append('validator assumption failed: legacy UP order is not a 180-degree FaceBakery offset')

required_bridge = (
    'correctCoralFanFaceBakeryUv(entry, model);',
    'private static void correctCoralFanFaceBakeryUv(ModernMapParityBlocks entry, Model model)',
    'if (!name.endsWith("_coral_fan") && !name.endsWith("_coral_wall_fan")) return;',
    'if (!hasPositiveYNormal(quad)) continue;',
    'quad.uv[0] = quad.uv[2];',
    'quad.uv[1] = quad.uv[3];',
    'quad.uv[2] = uv0;',
    'quad.uv[3] = uv1;',
    'private static boolean hasPositiveYNormal(Quad quad)',
    'if ("x".equals(r.axis)) { scaleY = scale; scaleZ = scale; }',
    'else if ("z".equals(r.axis)) { scaleX = scale; scaleY = scale; }',
    'else { scaleX = scale; scaleZ = scale; }',
    'if (r.rescale) { x*=scaleX; y*=scaleY; z*=scaleZ; }',
)
for token in required_bridge:
    if token not in bridge:
        fail.append('missing FaceBakery/coral bridge guard: ' + token)

# Do not reintroduce Pass 22's geometry clamping. Correct UVs, not the authored model coordinates.
for forbidden in (
    'fitCoralFanHorizontalFootprint',
    'double scaleX = width > 1.0D',
    'double scaleZ = depth > 1.0D',
):
    if forbidden in bridge:
        fail.append('coral fan geometry clamp returned: ' + forbidden)

# coral_fan.json and coral_wall_fan.json both use shade:false. The old custom renderer ignored
# that element property and applied directional diffuse lighting, so keep these exact families flat.
required_renderer = (
    'boolean coralFanNoShade = registryName.endsWith("_coral_fan")',
    '|| registryName.endsWith("_coral_wall_fan");',
    'float shade = "wildflowers".equals(registryName) || coralFanNoShade',
    '? 1.0F : diffuseLight(face.nx, face.ny, face.nz);',
)
for token in required_renderer:
    if token not in renderer:
        fail.append('missing coral shade:false renderer guard: ' + token)

# Preserve Pass 22/23 placement and selection behaviour.
required_blocks = (
    'setBlockBounds(2.0F / 16.0F, 0.0F, 2.0F / 16.0F, 14.0F / 16.0F, 4.0F / 16.0F, 14.0F / 16.0F)',
    'name.endsWith("_coral_wall_fan")) GameRegistry.registerBlock',
    'GameRegistry.registerBlock(entry.block, (Class<? extends ItemBlock>) null, name)',
    'world.setBlock(x, y, z, wallEntry.get(), side, 3)',
)
for token in required_blocks:
    if token not in blocks:
        fail.append('coral placement/hitbox regression: ' + token.splitlines()[0])

# Atlas/texture declarations remain untouched.
for token in ('textures/block/tube_coral_fan.png', 'textures/block/dead_horn_coral_fan.png'):
    if token not in blocks:
        fail.append('coral atlas declaration missing: ' + token)

if fail:
    print('Fidelity Pass 24 static validation FAILED')
    for item in fail:
        print(' -', item)
    sys.exit(1)

print('Fidelity Pass 24 static validation PASSED')
print(' - coral fan geometry remains Mojang-authored; no horizontal clamp/scaling is present')
print(' - upward coral fan UVs compensate the legacy UP-face vertex-order offset')
print(' - wall-fan rescale now matches FaceBakery: only axes perpendicular to rotation are scaled')
print(' - live/dead floor and wall fans honour the modern shade:false appearance')
print(' - Pass 22/23 item exposure, wall placement, hitboxes and atlas declarations remain intact')
