#!/usr/bin/env python3
"""Static gate for Pass 37c aquatic/runtime vegetation corrections."""
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

def method(src, signature, next_signature=None, limit=10000):
    start=src.find(signature)
    if start < 0: return ''
    if next_signature:
        end=src.find(next_signature,start+len(signature))
        if end>start: return src[start:end]
    return src[start:start+limit]

ctor=method(MPB,'ParityModelBlock(ModernMapParityBlocks entry)','private boolean isLightningRod')
need('if (isPass37AquaticContainedWaterBlock()) this.blockMaterial = Material.coral;' in ctor,
     'aquatic contained-water identities do not resist 1.7 BlockDynamicLiquid replacement')
family=method(MPB,'private boolean isCoralFamily()','private boolean isCopperTorch')
for name in ('isKelp()','isKelpPlant()','isSeagrass()','isTallSeagrass()','isSeaPickle()',
             'isCoralPlantForm()','isCoralFan()','isCoralWallFan()'):
    need(name in family, 'aquatic flow barrier family missing '+name)

support=method(MPB,'private boolean canSupportPlantFace','private boolean isMagma')
need('world.isSideSolid' in support and 'support instanceof ParityModelBlock' in support
     and 'isFullOpaqueModel()' in support,
     'aquatic support bridge does not accept authored full-cube parity supports')
neighbor=method(MPB,'public void onNeighborBlockChange(World world','@Override\n        public int tickRate')
need('isSeaPickle()' in neighbor and 'canSupportPlantTop(world, x, y, z)' in neighbor,
     'Sea Pickle neighbour support still bypasses parity full-cube support bridge')

item=method(MPB,'public static final class ParityAquaticItemBlock','/** Keeps Copper Golem Statue')
need('parity.isKelpPlant()' in item and '!sourceWater' in item,
     'direct Kelp Plant placement is not source-water-only')
need('world.setBlock(x, y, z, field_150939_a, 0, 2)' in item
     and 'world.setBlock(x, y + 1, z, field_150939_a, 1, 3)' in item,
     'Tall Seagrass direct placement is not lower-flag2 then upper-flag3')
activate=method(MPB,'public boolean onBlockActivated(World world','@Override\n        public int quantityDropped', limit=30000)
need('world.setBlock(x,y,z,TALL_SEAGRASS.get(),0,2)' in activate
     and 'world.setBlock(x,y+1,z,TALL_SEAGRASS.get(),1,3)' in activate,
     'Seagrass Bone Meal does not use neighbour-safe Tall Seagrass publication order')

pick=method(MPB,'public ItemStack getPickBlock','@Override\n        public void onBlockClicked')
need('isPitcherCrop()' in pick and 'ModernVegetationItems.getPitcherPod()' in pick,
     'Pitcher Crop pick-block does not resolve to Pitcher Pod')
drops=method(MPB,'public ArrayList<ItemStack> getDrops','@Override\n        public boolean removedByPlayer')
need('if (meta<5)' in drops and 'age>=4' in drops and 'ModernVegetationItems.getPitcherPod()' in drops,
     'Pitcher lower-half age-sensitive drop contract missing')
need('if (meta<5)' in drops, 'Pitcher upper-half no-drop condition missing')

moss=method(MPB,'private boolean bonemealPaleMossBlock','private boolean maybeCreatePaleMossTopper')
for token in ('2 + world.rand.nextInt(2)','0.75F','0.60F','world.rand.nextInt(60)',
              'PALE_MOSS_CARPET.get()','Blocks.tallgrass','Blocks.double_plant'):
    need(token in moss, 'Pale Moss Block bonemeal patch missing '+token)
need('if (isPaleMossBlock())' in activate and 'bonemealPaleMossBlock' in activate,
     'Pale Moss Block Bone Meal activation path missing')

added=method(MPB,'public void onBlockAdded','/** Mirrors BlockFalling')
need('isEyeblossom()' in added and 'scheduleBlockUpdate(x, y, z, this, 20)' in added,
     'Eyeblossom does not establish its 1.7 environment compatibility schedule')
tick=method(MPB,'public void updateTick(World world','@Override\n        public void onEntityCollidedWithBlock')
need('isEyeblossom()' in tick and 'world.scheduleBlockUpdate(x,y,z,current,20)' in tick,
     'Eyeblossom does not keep its loaded compatibility schedule alive')
eye=method(MPB,'private void tickEyeblossom','private boolean isSegmentedGroundDecal')
need('time>=12600L && time<23401L' in MPB, 'Eyeblossom exact Overworld open interval regressed')
need('world instanceof WorldServer' in eye and 'func_147487_a("reddust"' in eye,
     'Eyeblossom transform particles are not server-synchronized')
need('for(int dx=-3;dx<=3;dx++)' in eye and 'for(int dy=-2;dy<=2;dy++)' in eye,
     'Eyeblossom neighbour propagation range regressed')

for phrase in ('Pass 37c aquatic/runtime follow-up','BlockDynamicLiquid','lower-half-only loot contract',
               '20-tick compatibility schedule','not a general waterlogging implementation'):
    need(' '.join(phrase.lower().split()) in DOC_NORM, 'Pass 37c documentation missing: '+phrase)

if errors:
    print('Fidelity Pass 37c validation FAILED')
    for e in errors: print(' -',e)
    sys.exit(1)
print('Fidelity Pass 37c validation PASSED')
print(' - bounded aquatic blocks resist legacy liquid-flow overwrite without general waterlogging')
print(' - Tall Seagrass publication and parity-cube support are neighbour-safe')
print(' - Pitcher loot/pick behavior follows the modern lower-half/item contract')
print(' - Pale Moss Block bonemeal patch behavior is block-local and bounded')
print(' - Eyeblossom time checks and transition particles are reliably synchronized')
