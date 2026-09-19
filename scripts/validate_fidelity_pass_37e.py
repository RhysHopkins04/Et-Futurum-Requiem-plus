#!/usr/bin/env python3
"""Static gate for Pass 37e Coral Wall Fan atomic placement correction."""
from __future__ import print_function
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]

def text(rel):
    return (ROOT / rel).read_text()

MPB = text('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
DOC = text('docs/FIDELITY_PASS_37_VEGETATION_LIFECYCLE.md')
DOC_NORM = ' '.join(DOC.lower().split())
errors=[]

def need(ok,msg):
    if not ok: errors.append(msg)

def method(src, signature, next_signature=None, limit=18000):
    start=src.find(signature)
    if start < 0: return ''
    if next_signature:
        end=src.find(next_signature,start+len(signature))
        if end>start: return src[start:end]
    return src[start:start+limit]

item=method(MPB,'public static final class ParityAquaticItemBlock','/** Keeps Copper Golem Statue')
need('if (parity.isCoralFan() && side >= 2 && side <= 5)' in item,
     'Coral Fan horizontal placement does not select the wall identity before publication')
need('ModernMapParityBlocks wallEntry = parity.coralFanCompanion(true);' in item,
     'Coral Fan placement does not resolve its technical wall companion')
need('placementBlock = wallEntry.get();' in item and 'placementParity = (ParityModelBlock) placementBlock;' in item,
     'Coral Fan wall companion is not used as the actual placed block/runtime parity object')
need('world.setBlock(x, y, z, placementBlock, meta, 3)' in item,
     'aquatic ItemBlock still publishes the floor Coral Fan before wall conversion')
need('placementParity.initializePass37AquaticPlacement' in item,
     'post-placement aquatic initialization does not follow the actually published block identity')
need('placementBlock.onBlockPlacedBy' in item,
     'post-placement callback does not follow the actually published block identity')

# Preserve the exact support contract: horizontal placement is valid from the wall alone, while floor
# placement still uses the sturdy-top path.
survive=method(MPB,'private boolean canSurviveAtPlacement','private int pass37AquaticPlacementMeta')
need('if (isCoralFan() && side>=2 && side<=5)' in survive and 'canSupportPlantFace' in survive,
     'Coral Fan horizontal preflight no longer validates the clicked wall face')
need('if (isCoralPlantForm() || isCoralFan()) return canSupportPlantTop' in survive,
     'Coral Fan floor placement support contract regressed')

coral=method(MPB,'private boolean canCoralSurvive','private boolean canGroundVegetationSurvive')
need('if (isCoralWallFan())' in coral and 'canSupportPlantFace' in coral,
     'placed Coral Wall Fan no longer uses directional wall support')

# Item/drop and static map identity contract must remain intact.
pick=method(MPB,'public ItemStack getPickBlock','@Override\n        public void onBlockClicked')
drop=method(MPB,'public Item getItemDropped','@Override\n        public ItemStack getPickBlock')
need('if (isCoralWallFan())' in pick and 'coralFanCompanion(false)' in pick,
     'wall-fan pick-block no longer resolves to the obtainable floor-fan item')
need('if (isCoralWallFan())' in drop and 'coralFanCompanion(false)' in drop,
     'wall-fan drops no longer resolve to the obtainable floor-fan item')

for phrase in ('Pass 37e Coral Wall Fan placement follow-up',
               'placement-order race',
               'publishes the matching technical `_coral_wall_fan` identity directly',
               'no block beneath a valid wall-mounted fan is required'):
    need(' '.join(phrase.lower().split()) in DOC_NORM,
         'Pass 37e documentation missing: '+phrase)

if errors:
    print('Fidelity Pass 37e validation FAILED')
    for e in errors: print(' -',e)
    sys.exit(1)

print('Fidelity Pass 37e validation PASSED')
print(' - horizontal Coral Fan use atomically publishes the technical wall-fan identity')
print(' - wall-only support no longer depends on a temporary floor-fan state or a block below')
print(' - floor placement, water bit, wall facing, drops and pick-block contracts remain intact')
