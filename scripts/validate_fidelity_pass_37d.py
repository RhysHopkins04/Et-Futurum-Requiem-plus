#!/usr/bin/env python3
"""Static gate for Pass 37d Sea Pickle support correction."""
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

def method(src, signature, next_signature=None, limit=16000):
    start=src.find(signature)
    if start < 0: return ''
    if next_signature:
        end=src.find(next_signature,start+len(signature))
        if end>start: return src[start:end]
    return src[start:start+limit]

placement=method(MPB,'public boolean canPlaceBlockAt(World world','@Override\n        public boolean getBlocksMovement')
need('if (isSeaPickle())' in placement and 'return canSupportPlantTop(world, x, y, z);' in placement,
     'Sea Pickle initial placement still bypasses parity-aware sturdy-top support')
need('return world.isSideSolid(x, y - 1, z, ForgeDirection.UP, false);' not in placement,
     'Sea Pickle initial placement still uses raw Forge isSideSolid')

support=method(MPB,'private boolean canSupportPlantFace','private boolean isMagma')
need('support instanceof ParityModelBlock' in support and 'isFullOpaqueModel()' in support,
     'parity full-cube support bridge missing')

neighbor=method(MPB,'public void onNeighborBlockChange(World world','@Override\n        public int tickRate')
need('isSeaPickle()' in neighbor and 'canSupportPlantTop(world, x, y, z)' in neighbor,
     'Sea Pickle neighbour survival does not share parity-aware support bridge')

activate=method(MPB,'public boolean onBlockActivated(World world','@Override\n        public int quantityDropped', limit=32000)
need('if (isSeaPickle())' in activate and 'isCoralBlockBelow(world,x,y,z)' in activate,
     'Sea Pickle Bone Meal is not restricted to live pickle over Coral Block')
need('bonemealSeaPickles(world,x,y,z)' in activate,
     'Sea Pickle Bone Meal spread hook missing')

spread=method(MPB,'private void bonemealSeaPickles','private boolean paleMossSideSupported')
need('world.setBlockMetadataWithNotify(x,y,z,7,3)' in spread,
     'Sea Pickle Bone Meal does not force source pickle to live count 4')
need('world.rand.nextInt(6)==0' in spread and 'world.rand.nextInt(4)' in spread,
     'Sea Pickle bounded modern spread probabilities regressed')

pick=method(MPB,'public ItemStack getPickBlock','@Override\n        public void onBlockClicked')
need('isPitcherCrop()' in pick and 'ModernVegetationItems.getPitcherPod()' in pick,
     'Pitcher Crop pick-block contract regressed')
drops=method(MPB,'public ArrayList<ItemStack> getDrops','@Override\n        public boolean removedByPlayer')
need('if (meta<5)' in drops and 'age>=4' in drops and 'ModernVegetationItems.getPitcherPod()' in drops,
     'Pitcher lower-half-only age-sensitive drop contract regressed')

moss=method(MPB,'private boolean maybeCreatePaleMossTopper','private void advancePaleOak')
need('paleMossSideSupported(world,x,y+1,z,d)' in moss,
     'Pale Moss Carpet topper no longer requires continuing upper wall support')

for phrase in ('Pass 37d Sea Pickle support follow-up',
               'canSupportPlantTop',
               'Bone Meal remains valid only for a live Sea Pickle directly above a Coral Block',
               'Pitcher Crop pick-block resolves to Pitcher Pod',
               'Pale Moss Carpet Bone Meal likewise remains conditional'):
    need(' '.join(phrase.lower().split()) in DOC_NORM,
         'Pass 37d documentation missing: '+phrase)

if errors:
    print('Fidelity Pass 37d validation FAILED')
    for e in errors: print(' -',e)
    sys.exit(1)

print('Fidelity Pass 37d validation PASSED')
print(' - Sea Pickle placement and survival share the parity-aware sturdy-top support bridge')
print(' - live Sea Pickles on Coral Blocks retain the bounded modern Bone Meal interaction')
print(' - Pitcher and Pale Moss Carpet behaviors remain intentionally unchanged')
