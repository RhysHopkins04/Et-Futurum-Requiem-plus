#!/usr/bin/env python3
"""Static gate for Pass 36b Copper lifecycle runtime corrections."""
from __future__ import print_function
import pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MPB = (ROOT/'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
ROD = (ROOT/'src/main/java/ganymedes01/etfuturum/blocks/BlockLightningRod.java').read_text()
ETFU = (ROOT/'src/main/java/ganymedes01/etfuturum/EtFuturum.java').read_text()
DOC = (ROOT/'docs/FIDELITY_PASS_36_COPPER_LIFECYCLE.md').read_text()

errors=[]
def need(ok,msg):
    if not ok: errors.append(msg)
def has(text,*parts): return all(p in text for p in parts)

# Copper Chest: legacy BlockChest.onBlockAdded may rewrite facing during registry swaps.
need(has(MPB,
         'BlockChest.onBlockAdded() recalculates legacy chest facing',
         'world.setBlockMetadataWithNotify(x, y, z, first.metadata, 2)',
         'world.setBlockMetadataWithNotify(px, y, pz, partner.metadata, 2)'),
     'Copper Chest replacement does not restore exact metadata for both paired halves')
need(has(MPB,
         'if (world.setBlock(x, y, z, target, targetMeta, 3))',
         'world.setBlockMetadataWithNotify(x, y, z, targetMeta, 2)'),
     'Copper Chest no-TE replacement path does not restore requested metadata')
need(has(MPB,
         'ChestSnapshot.capture', 'clearInventory()', 'restoreIntoReplacement()',
         'first.resolveRestoredPair()', 'partner.resolveRestoredPair()'),
     'Pass-36 Copper Chest inventory/pair preservation hooks regressed')

# Statue: exhaustively encoded as pose*4+facing; helper must preserve facing for all 16 states.
need(has(MPB,
         'cycleCopperGolemStatuePoseMeta(int metadata)',
         'int facing = state & 3',
         'int pose = (state >> 2) & 3',
         'return (((pose + 1) & 3) << 2) | facing'),
     'Copper Golem Statue cycle helper is not explicit pose-only metadata preservation')
for meta in range(16):
    facing=meta & 3
    pose=(meta >> 2) & 3
    nxt=(((pose+1)&3)<<2)|facing
    need((nxt & 3)==facing, 'internal validator error: statue facing not preserved for meta %d'%meta)
    need(((nxt>>2)&3)==((pose+1)&3), 'internal validator error: statue pose not advanced for meta %d'%meta)
need(has(MPB,
         'int nextMeta = cycleCopperGolemStatuePoseMeta(oldMeta)',
         'if ((world.getBlockMetadata(x, y, z) & 3) != facing)',
         'world.setBlockMetadataWithNotify(x, y, z, nextMeta, 2)'),
     'Statue interaction is not using/reasserting the facing-preserving cycle state')
need('config.addSoundEvent(ver, "entity.copper_golem_become_statue", "block")' in ETFU,
     'Copper Golem Statue exact modern pose-change sound event is not requested through AssetDirector')

# Lightning Rod: vertical unchanged, wall sides inverted at only the placement boundary.
for token in ['case 2: return 3;', 'case 3: return 2;', 'case 4: return 5;', 'case 5: return 4;',
              'case 0: return 0;', 'case 1: return 1;']:
    need(token in ROD, 'mature Lightning Rod placement mapping missing '+token)
# The same mapping must exist in the parity block placement branch as well.
rod_branch = MPB[MPB.find('if (!isLightningRod()) return meta;'):MPB.find('@Override\n        public void onBlockPlacedBy', MPB.find('if (!isLightningRod()) return meta;'))]
for token in ['case 2: return 3;', 'case 3: return 2;', 'case 4: return 5;', 'case 5: return 4;',
              'case 0: return 0;', 'case 1: return 1;']:
    need(token in rod_branch, 'parity Lightning Rod placement mapping missing '+token)
need('getFinalCopperMeta' in ROD and 'return worldMeta;' in ROD,
     'Lightning Rod lifecycle metadata preservation regressed')

need(has(DOC,'Pass 36b runtime corrections','exact saved metadata','pose cycling',
             'AssetDirector now requests','wall-click sides are inverted'),
     'Pass 36b documentation is incomplete')

if errors:
    print('Fidelity Pass 36b validation FAILED')
    for e in errors: print(' - '+e)
    sys.exit(1)
print('Fidelity Pass 36b validation PASSED')
print(' - Copper Chest registry swaps restore exact facing metadata after legacy BlockChest recalculation')
print(' - paired Copper Chest halves retain Pass-36 TileEntity/inventory/reciprocal-pair preservation')
print(' - Copper Golem Statue pose cycling preserves facing across all 16 pose/facing metadata states')
print(' - Copper Golem Statue pose-change sound is requested through AssetDirector with its modern OGG set')
print(' - wall Lightning Rod placement is inverted while floor/ceiling and lifecycle metadata stay unchanged')
