#!/usr/bin/env python3
"""Static gate for Pass 35c Bell reciprocal wall-support transitions."""
from __future__ import print_function
import pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
def read(rel): return (ROOT/rel).read_text()

MPB = read('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
BRIDGE = read('src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java')
DOC = read('docs/FIDELITY_PASS_35_MAP_STATE.md')

errors=[]
def need(ok,msg):
    if not ok: errors.append(msg)
def has(text,*parts): return all(p in text for p in parts)

# The complete 4 x 4 visible state matrix must stay intact.
need(has(BRIDGE, '"bell".equals(registryName)', '{"north","east","south","west"}',
         '{"floor","ceiling","single_wall","double_wall"}', 'a*4+f'),
     'Bell 4x4 attachment/facing model matrix regressed')

# Pass 35c specifically guards BOTH directions of the reciprocal wall-support state machine.
need(has(MPB, 'if (attachment == 2)', 'boolean primary = bellSupportAt',
         'boolean opposite = bellSupportAt', 'if (primary && opposite)',
         'setBlockMetadataWithNotify(x, y, z, 12 + facing, 3)'),
     'single_wall -> double_wall promotion on opposite support addition is missing')
need(has(MPB, 'else if (attachment == 3)', 'if (primary || opposite)',
         'int remainingFacing = primary ? facing : ((facing + 2) & 3)',
         'setBlockMetadataWithNotify(x, y, z, 8 + remainingFacing, 3)'),
     'double_wall -> single_wall degradation on support loss is missing')
need(has(MPB, 'if (!bellSupported(world, x, y, z, meta))',
         'dropBlockAsItem(world, x, y, z, 0, 0)', 'world.setBlockToAir(x, y, z)'),
     'unsupported Bell drop/removal handling regressed')

# Metadata/state semantics stay bounded: no new encoding and no forced facing flip on promotion.
need('12 + ((facing + 2) & 3)' not in MPB,
     'single_wall -> double_wall promotion must preserve the existing facing')
need(has(DOC, 'single-wall bell upgrades to double-wall when the opposite support appears',
         'double-wall', 'degrades to the corresponding single-wall state',
         'reciprocal support transitions'),
     'Pass 35 Bell documentation does not describe reciprocal wall-support transitions')

if errors:
    print('Fidelity Pass 35c validation FAILED')
    for e in errors: print(' - '+e)
    sys.exit(1)
print('Fidelity Pass 35c validation PASSED')
print(' - Bell single_wall promotes to double_wall when opposite support appears')
print(' - Bell double_wall degrades to the correct single_wall state when one support is removed')
print(' - unsupported Bells still drop and the 4x4 model/metadata matrix remains unchanged')
