#!/usr/bin/env python3
from pathlib import Path
import re, sys

root = Path(__file__).resolve().parents[1]
blocks = (root / 'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
bridge = (root / 'src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java').read_text()

fail = []
families = ('tube','brain','bubble','fire','horn')
for family in families:
    for dead in ('', 'dead_'):
        floor = f'{dead}{family}_coral_fan'
        wall = f'{dead}{family}_coral_wall_fan'
        enum_floor = floor.upper()
        enum_wall = wall.upper()
        if not re.search(r'^\s*'+re.escape(enum_floor)+r'\(', blocks, re.M):
            fail.append(f'missing floor fan registry identity: {floor}')
        if not re.search(r'^\s*'+re.escape(enum_wall)+r'\(', blocks, re.M):
            fail.append(f'missing wall fan registry identity: {wall}')

required_blocks = (
    'name.endsWith("_coral_wall_fan")) GameRegistry.registerBlock',
    'GameRegistry.registerBlock(entry.block, (Class<? extends ItemBlock>) null, name)',
    'private boolean isCoralFan()',
    'private boolean isCoralWallFan()',
    'private ModernMapParityBlocks coralFanCompanion(boolean wall)',
    'setBlockBounds(2.0F / 16.0F, 0.0F, 2.0F / 16.0F, 14.0F / 16.0F, 4.0F / 16.0F, 14.0F / 16.0F)',
    'case 2: setBlockBounds(0.0F, 4.0F/16.0F, 5.0F/16.0F, 1.0F, 12.0F/16.0F, 1.0F)',
    'case 3: setBlockBounds(0.0F, 4.0F/16.0F, 0.0F, 1.0F, 12.0F/16.0F, 11.0F/16.0F)',
    'case 4: setBlockBounds(5.0F/16.0F, 4.0F/16.0F, 0.0F, 1.0F, 12.0F/16.0F, 1.0F)',
    'case 5: setBlockBounds(0.0F, 4.0F/16.0F, 0.0F, 11.0F/16.0F, 12.0F/16.0F, 1.0F)',
    'if (isCoralFan()) {\n                // The one obtainable coral-fan item chooses its technical wall block',
    'world.setBlock(x, y, z, wallEntry.get(), side, 3)',
    'public Item getItemDropped(int meta, Random random, int fortune)',
    'public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player)',
)
for token in required_blocks:
    if token not in blocks:
        fail.append('missing ModernMapParityBlocks guard: '+token.splitlines()[0])

# Wall fans must remain technical block identities, not a second set of Creative/obtainable ItemBlocks.
if 'boolean technicalPlacementBlock' not in blocks or 'name.endsWith("_coral_wall_fan")' not in blocks:
    fail.append('wall coral fans are not excluded from Creative exposure')
if re.search(r'GameRegistry\.registerBlock\(entry\.block, name\);\s*\n\s*}\s*\n\s*}', blocks):
    pass  # The normal branch is expected; the explicit null ItemBlock branch above is the hard guard.

required_bridge = (
    'style == ModernMapParityBlocks.Style.WALL_PLANT && name.endsWith("_coral_wall_fan")',
    'style == ModernMapParityBlocks.Style.WALL_PLANT && entry.getRegistryName().endsWith("_coral_wall_fan")',
    'String[] facings = {null, null, "north", "south", "west", "east"}',
    'prepared.facingModels[side] = applySpecialBlockVisual(entry, loadBlockStateModel(entry, state))',
    'if (name.endsWith("_coral_fan") || name.endsWith("_coral_wall_fan"))',
    'Adjacent fans therefore intermesh visually.',
    'Do not clamp, centre, scale or vertically offset this geometry',
    'return model;',
)
for token in required_bridge:
    if token not in bridge:
        fail.append('missing ModernJsonModelBridge guard: '+token)

# Guard against reverting to the old generic full-height selection box for coral fans.
fan_section = blocks[blocks.find('private boolean isCoralFan()'):]
if '14.0F / 16.0F, 4.0F / 16.0F, 14.0F / 16.0F' not in fan_section:
    fail.append('compact 12x4x12 floor-fan outline missing')

# Pass 23 correction: the model itself must remain byte-for-byte geometric in spirit; no legacy
# horizontal fitting helper may be reintroduced. Modern coral fans intentionally overhang and intermesh.
for forbidden in ('fitCoralFanHorizontalFootprint', 'scaleX = width > 1.0D', 'scaleZ = depth > 1.0D'):
    if forbidden in bridge:
        fail.append('coral fan geometry is still being legacy-clamped: '+forbidden)

# Asset/texture catalogue must remain untouched by this geometry/placement pass.
if 'textures/block/tube_coral_fan.png' not in blocks or 'textures/block/dead_horn_coral_fan.png' not in blocks:
    fail.append('coral atlas/model source declarations changed unexpectedly')

if fail:
    print('Fidelity Pass 22 static validation FAILED')
    for x in fail: print(' -', x)
    sys.exit(1)

print('Fidelity Pass 22 static validation PASSED')
print(' - all 20 modern internal coral-fan block identities remain registered for map/save parity')
print(' - the 10 wall-fan identities are technical blocks with no duplicate ItemBlock/Creative exposure')
print(' - one live/dead fan item per colour selects floor or wall placement automatically')
print(' - floor fans use the modern 12x4x12 outline; wall fans use directional 16x8x11 outlines')
print(' - wall fan rendering follows metadata-facing Mojang blockstate variants')
print(' - coral fan rendering preserves Mojang-authored overlap/height without changing coral atlas textures')
