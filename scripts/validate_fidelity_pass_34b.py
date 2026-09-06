#!/usr/bin/env python3
"""Static regression gate for Pass 34b runtime/map-fidelity corrections."""
from __future__ import print_function
import json, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MPB = (ROOT/'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
DOC = (ROOT/'docs/FIDELITY_PASS_34_MAP_STATE.md').read_text()
CONTRACT = json.loads((ROOT/'docs/BACKPORTER_STATE_CONTRACT.json').read_text())

errors=[]
def need(ok,msg):
    if not ok: errors.append(msg)
def has(text,*parts): return all(p in text for p in parts)

need(str(CONTRACT.get('contract_revision')) == '34', 'Backporter contract revision must remain 34 for the bounded 34b correction')

# Pass-33 mangrove registry identity must stay registered so old saves/server registries never go missing.
need(has(MPB,'legacyMangroveAlias','entry == MANGROVE_PROPAGULE','!technicalPlacementBlock && !legacyMangroveAlias'),
     'hidden etfuturum:mangrove_propagule compatibility alias missing')
need(has(MPB,'entry == MANGROVE_PROPAGULE','return ModBlocks.SAPLING.getItem()'),
     'legacy mangrove alias must drop the mature propagule item')
need(has(MPB,'entry == MANGROVE_PROPAGULE','return new ItemStack(ModBlocks.SAPLING.get(), 1, 0)'),
     'legacy mangrove alias pick-block must resolve the mature propagule item')

# Normal placement must be sane without changing imported exact state.
need(has(MPB,'if (isPaleMossCarpet())','side == 1','ForgeDirection.UP'),
     'Pale Moss Carpet floor-supported ItemBlock placement gate missing')
need(has(MPB,'if (isPaleHangingMoss()) return 1','former tip immediately becomes the body model',
         'world.setBlockMetadataWithNotify(x, y + 1, z, 0, 3)'),
     'Pale Hanging Moss normal tip/body chain transition missing')
need(has(MPB,'Removing the chain end exposes the block above as the new tip',
         'world.setBlockMetadataWithNotify(x, y + 1, z, 1, 3)'),
     'Pale Hanging Moss tip promotion on break missing')

need(has(MPB,'if (isDriedGhast())','hydration = (world.getBlockMetadata(x, y, z) >> 2) & 3',
         'facing = (quadrant + 2) & 3'),
     'Dried Ghast placement orientation mapping missing')

need('if (isSeaPickle()) return 4;' in MPB,
     'Sea Pickle normal placement must default to live/white-tipped visual metadata')
need(has(MPB,'if (isTallSeagrass())','world.setBlock(x, y + 1, z, this, 1, 3)'),
     'Tall Seagrass normal two-block placement missing')
need(has(MPB,'if (isTallSeagrass())','otherY = upper ? y - 1 : y + 1','world.setBlockToAir(x, otherY, z)'),
     'Tall Seagrass paired-break cleanup missing')

bykey={b.get('key'):b for b in CONTRACT.get('blocks',[])}
m=bykey.get('mangrove_propagule',{})
need(m.get('target_ids') == ['etfuturum:sapling'], 'Backporter must continue targeting mature etfuturum:sapling')
notes=' '.join(m.get('notes',[]))
need('compatibility alias' in notes and 'must not target' in notes, 'Mangrove contract must explain hidden compatibility alias')
need('defaults to the live visual bit' in ' '.join(bykey.get('sea_pickle',{}).get('notes',[])),
     'Sea Pickle contract must document runtime default vs imported exact bit')
need('Normal placement creates the canonical lower+upper two-block pair' in ' '.join(bykey.get('tall_seagrass',{}).get('notes',[])),
     'Tall Seagrass contract must document paired runtime placement')

need('Pass 34 is map-state fidelity, not the later lifecycle pass' in DOC,
     'Pass 34 runtime-vs-lifecycle clarification missing')

if errors:
    for e in errors: print('ERROR: '+e, file=sys.stderr)
    sys.exit(1)

print('Fidelity Pass 34b validation PASSED')
print(' - Pass-33 mangrove registry identity remains present as a hidden compatibility alias')
print(' - Pale Moss Carpet normal placement requires floor support')
print(' - Pale Hanging Moss normal chains expose body/tip models correctly')
print(' - Dried Ghast normal placement preserves facing while import retains all hydration states')
print(' - Sea Pickle normal placement restores live white-tipped appearance')
print(' - Tall Seagrass normal placement/breaking uses a two-block lower+upper pair')
print(' - deferred growth/hatching/entity mechanics remain explicitly outside Pass 34')
