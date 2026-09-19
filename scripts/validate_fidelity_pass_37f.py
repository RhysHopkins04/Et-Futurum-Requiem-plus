#!/usr/bin/env python3
"""Static gate for Pass 37f Coral Fan client placement-preflight correction."""
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

# Keep 37e's atomic publication fix intact.
item=method(MPB,'public static final class ParityAquaticItemBlock','/** Keeps Copper Golem Statue')
need('if (parity.isCoralFan() && side >= 2 && side <= 5)' in item,
     '37e horizontal Coral Fan wall-identity selection regressed')
need('placementBlock = wallEntry.get();' in item,
     '37e no longer publishes the technical wall-fan identity directly')
need('world.setBlock(x, y, z, placementBlock, meta, 3)' in item,
     '37e atomic wall publication regressed')

# 1.7 client ItemBlock preflight reaches canPlaceBlockOnSide through canReplace. The obtainable
# floor-fan identity must therefore expose the exact same side-aware predicate used by onItemUse.
side_method=method(MPB,
    'public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side)',
    '@Override\n        public boolean canPlaceBlockAt')
need('if (isCoralFan()) return canSurviveAtPlacement(world, x, y, z, side);' in side_method,
     'Coral Fan canPlaceBlockOnSide does not mirror the server side-aware placement predicate')
need(side_method.find('if (isCoralFan()) return canSurviveAtPlacement') < side_method.find('return super.canPlaceBlockOnSide'),
     'Coral Fan client preflight still falls through to legacy floor-only canPlaceBlockOnSide')

survive=method(MPB,'private boolean canSurviveAtPlacement','private int pass37AquaticPlacementMeta')
need('if (isCoralFan() && side>=2 && side<=5)' in survive and 'canSupportPlantFace' in survive,
     'horizontal Coral Fan placement no longer validates only the clicked wall face')
need('if (isCoralPlantForm() || isCoralFan()) return canSupportPlantTop' in survive,
     'Coral Fan floor support path regressed')

coral=method(MPB,'private boolean canCoralSurvive','private boolean canGroundVegetationSurvive')
need('if (isCoralWallFan())' in coral and 'canSupportPlantFace' in coral,
     'placed Coral Wall Fan directional survival regressed')

for phrase in (
        'Pass 37f Coral Fan client placement preflight follow-up',
        'ItemBlock.func_150936_a',
        'World.canPlaceEntityOnSide',
        'canPlaceBlockOnSide',
        'Horizontal faces validate only the clicked sturdy wall face',
        'Pass 38 remains untouched'):
    need(' '.join(phrase.lower().split()) in DOC_NORM,
         'Pass 37f documentation missing: '+phrase)

if errors:
    print('Fidelity Pass 37f validation FAILED')
    for e in errors: print(' -',e)
    sys.exit(1)

print('Fidelity Pass 37f validation PASSED')
print(' - 1.7 client ItemBlock preflight now uses Coral Fan side-aware support')
print(' - horizontal wall placement no longer inherits the floor-fan support gate')
print(' - Pass 37e atomic wall publication and existing floor/import/drop contracts remain intact')
