#!/usr/bin/env python3
"""Static gate for Pass 37b vegetation runtime corrections and chunk-loader safety."""
from __future__ import print_function
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]

def text(rel):
    return (ROOT / rel).read_text()

MPB = text('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
LISTENER = text('src/main/java/ganymedes01/etfuturum/world/EtFuturumWorldListener.java')
SAPLING = text('src/main/java/ganymedes01/etfuturum/blocks/BlockModernSapling.java')
MANGROVE = text('src/main/java/ganymedes01/etfuturum/world/generate/decorate/WorldGenPass37MangroveTree.java')
DOC = text('docs/FIDELITY_PASS_37_VEGETATION_LIFECYCLE.md')

errors=[]
def need(ok,msg):
    if not ok: errors.append(msg)

def method(src, signature, next_signature=None):
    start=src.find(signature)
    if start < 0: return ''
    if next_signature:
        end=src.find(next_signature,start+len(signature))
        if end>start: return src[start:end]
    # bounded fallback
    return src[start:start+7000]

# Pitcher age 3->4 must never notify the stale upper before it is synchronized.
grow=method(MPB,'private boolean growPitcher','private boolean isCoralBlockBelow')
need(bool(grow),'growPitcher helper missing')
need('world.setBlockMetadataWithNotify(x,ly,z,next,2)' in grow,
     'Pitcher lower state must publish with flag 2 before upper synchronization')
need('world.setBlock(x,ly+1,z,this,5+next,3)' in grow,
     'Pitcher upper age/half synchronization with flag 3 missing')
need('setBlockMetadataWithNotify(x,ly,z,next,3)' not in grow,
     'Pitcher regression: lower still notifies neighbours before upper synchronization')
need('world.getBlockLightValue(x,ly,z) < 8' in grow,
     'Pitcher sufficient-light growth gate missing')
need('return world.getBlock(x,ly,z)==this' in grow and '(world.getBlockMetadata(x,ly+1,z)&15)==5+next' in grow,
     'Pitcher postcondition does not verify synchronized lower/upper pair')

bone=method(MPB,'private boolean applyAncientCropBoneMeal','@Override\n        public int quantityDropped')
need('growPitcher(world, x, lowerY, z, 1)' in bone,
     'Pitcher Bone Meal must share growPitcher update-order path')
need('setBlockMetadataWithNotify(x, lowerY, z, nextAge, 3)' not in bone,
     'Pitcher Bone Meal still contains the old unsafe direct lower update')

# Mature vegetation must not inherit generic parity "place anywhere" behavior.
canplace=method(MPB,'public boolean canPlaceBlockAt(World world','@Override\n        public boolean getBlocksMovement')
need('if (isTorchflower() || isPaleOakSapling() || isEyeblossom() || isWildflowers())' in canplace
     and 'canGroundVegetationSurvive(world,x,y,z)' in canplace,
     'Mature Torchflower vegetation-soil placement gate missing')
need('isTorchflowerCrop() || isPitcherCrop()' in canplace and 'world.getBlockLightValue(x,y,z)>=8' in canplace,
     'Ancient crop farmland/sufficient-light placement gate missing')
need('return canGroundVegetationSurvive(world, x, y, z);' in method(MPB,'private boolean canPitcherPlantStandAt','private boolean isCandle'),
     'Mature Pitcher Plant still accepts arbitrary solid-top support')
neighbor=method(MPB,'public void onNeighborBlockChange(World world','@Override\n        public int tickRate')
need('isTorchflower() || isPaleOakSapling() || isEyeblossom() || isSegmentedGroundDecal()' in neighbor,
     'Mature Torchflower support-loss handling missing')

# Preserve deliberately correct observed Sea Pickle behavior: dry is supported but unlit.
need('if (isSeaPickle()) return (meta & 4) != 0 ? 6 + (meta & 3) * 3 : 0;' in MPB,
     'Sea Pickle live/dry light contract regressed')
need('if (isSeaPickle())\n                return world.isSideSolid' in MPB or
     ('if (isSeaPickle())' in canplace and 'world.isSideSolid' in canplace),
     'Sea Pickle dry support survival gate missing')

# World listener must be observational only for already-loaded chunks.
mark=method(LISTENER,'public void markBlockForUpdate','Block soulsand')
guard=mark.find('if (!world.blockExists(x, y, z)) return;')
read=mark.find('world.getBlock(x, y, z)')
need(guard >= 0 and read > guard,
     'EtFuturumWorldListener must guard blockExists before getBlock during chunk-load callbacks')
need('if (!world.blockExists(x, y - 1, z)) return;' in LISTENER,
     'Basalt listener path can still force-load the below chunk/block')
need('if (!world.blockExists(nx, ny, nz)) continue;' in LISTENER,
     'Basalt neighbour scan can still force-load adjacent chunks')
need('if (!world.blockExists(x, y - 1, z) || !world.blockExists(x, y + 1, z)) return;' in LISTENER,
     'Bubble-column listener path can still dereference unavailable positions')

# Mangrove runtime report: no adjacency ban, and modern 85% tall bias remains explicit.
canstay=method(SAPLING,'public boolean canBlockStay','@Override\n\tpublic void updateTick')
need('x + 1' not in canstay and 'x - 1' not in canstay and 'z + 1' not in canstay and 'z - 1' not in canstay,
     'Mangrove propagule survival unexpectedly gained an adjacency prohibition')
need('random.nextFloat() < 0.85F' in MANGROVE,
     'Mangrove modern 85% tall-feature selection bias regressed')

# Documentation must make runtime-correct vs fixed behavior unambiguous.
for phrase in ('Pass 37b runtime corrections','Dry Sea Pickles deliberately remain placeable',
               'lower age is','chunk-loader TileEntity activation callbacks'):
    need(phrase in DOC, 'Pass 37b documentation missing: '+phrase)

if errors:
    print('Fidelity Pass 37b validation FAILED')
    for e in errors: print(' -',e)
    sys.exit(1)
print('Fidelity Pass 37b validation PASSED')
print(' - Pitcher age/half publication order is neighbour-safe and shared by Bone Meal/random growth')
print(' - mature Torchflower/Pitcher Plant substrate rules no longer fall through generic parity placement')
print(' - dry Sea Pickle unlit behavior remains intentional')
print(' - EtFuturumWorldListener cannot recursively force-load chunks from update callbacks')
print(' - Mangrove propagules have no adjacency ban and retain the 85% tall-tree bias')
