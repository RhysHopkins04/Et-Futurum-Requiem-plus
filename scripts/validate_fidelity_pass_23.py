#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
blocks = (root / 'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
bridge = (root / 'src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java').read_text()

fail = []

# The modern floor model is four rotated zero-thickness planes with intentional overhang. The bridge
# must load that authored JSON unchanged rather than scaling it into the legacy 0..1 cell.
required_bridge = (
    'if (name.endsWith("_coral_fan") || name.endsWith("_coral_wall_fan"))',
    'Vanilla deliberately authors coral fans as overlapping rotated zero-thickness planes',
    'Adjacent fans therefore intermesh visually.',
    'Do not clamp, centre, scale or vertically offset this geometry',
    'return model;',
)
for token in required_bridge:
    if token not in bridge:
        fail.append('missing exact-geometry guard: ' + token)

for forbidden in (
    'fitCoralFanHorizontalFootprint',
    'double scaleX = width > 1.0D',
    'double scaleZ = depth > 1.0D',
):
    if forbidden in bridge:
        fail.append('legacy coral fan fitting still present: ' + forbidden)

# Preserve the Pass 22 behaviour fixes. The visual model is tall/overhanging by design, but its
# selection box is deliberately smaller and its wall forms remain technical placement blocks.
required_blocks = (
    'setBlockBounds(2.0F / 16.0F, 0.0F, 2.0F / 16.0F, 14.0F / 16.0F, 4.0F / 16.0F, 14.0F / 16.0F)',
    'name.endsWith("_coral_wall_fan")) GameRegistry.registerBlock',
    'GameRegistry.registerBlock(entry.block, (Class<? extends ItemBlock>) null, name)',
    'world.setBlock(x, y, z, wallEntry.get(), side, 3)',
    'public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player)',
)
for token in required_blocks:
    if token not in blocks:
        fail.append('Pass 22 placement/hitbox regression: ' + token.splitlines()[0])

# The atlas references are not part of this correction.
for token in ('textures/block/tube_coral_fan.png', 'textures/block/dead_horn_coral_fan.png'):
    if token not in blocks:
        fail.append('coral atlas declaration missing: ' + token)

if fail:
    print('Fidelity Pass 23 static validation FAILED')
    for item in fail:
        print(' -', item)
    sys.exit(1)

print('Fidelity Pass 23 static validation PASSED')
print(' - live/dead floor and wall coral fans retain Mojang-authored model geometry')
print(' - no horizontal clamping/scaling helper remains')
print(' - compact floor and directional wall selection boxes remain intact')
print(' - wall fan technical IDs/item-placement compatibility remain intact')
print(' - coral atlas declarations remain unchanged')
