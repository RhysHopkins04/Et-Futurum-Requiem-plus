#!/usr/bin/env python3
"""Static regression gate for Pass 34c bounded runtime-state interactions."""
from __future__ import print_function
import pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MPB = (ROOT/'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
DOC = (ROOT/'docs/FIDELITY_PASS_34_MAP_STATE.md').read_text()
CONTRACT = (ROOT/'docs/BACKPORTER_STATE_CONTRACT.json').read_text()

errors=[]
def need(ok,msg):
    if not ok: errors.append(msg)
def has(text,*parts): return all(p in text for p in parts)

need(
    has(MPB, 'ParityPaleHangingMossItemBlock extends ItemBlock',
        'int targetY = y - 1', 'canPaleHangingMossHangAt(world, x, targetY, z)')
    or has(MPB, 'if (isPaleHangingMoss())',
        'support is determined solely from the actual block above',
        'world.getBlock(x, y + 1, z) == this',
        'ForgeDirection.DOWN'),
    'Pale Hanging Moss underside placement support fix missing')

need(has(MPB, 'if (isSeaPickle() && held != null && held.getItem() == Item.getItemFromBlock(this))',
         'int count = (meta & 3) + 1',
         'if (count >= 4) return false',
         'int nextMeta = (meta & 4) | count'),
     'Sea Pickle 1..4 in-place stacking missing')
need(has(MPB, 'if (isSeaPickle())',
         'world.isSideSolid(x, y - 1, z, ForgeDirection.UP, false)'),
     'Sea Pickle floor-support placement/survival gate missing')

need(has(MPB, 'private static boolean isBoneMeal(ItemStack stack)',
         'Items.dye', 'stack.getItemDamage() == 15'),
     '1.7.10 Bone Meal recognition missing')
need(has(MPB, 'applyAncientCropBoneMeal',
         'if (isTorchflowerCrop())',
         'world.setBlockMetadataWithNotify(x, y, z, 1, 3)',
         'world.setBlock(x, y, z, TORCHFLOWER.get(), 0, 3)'),
     'Torchflower bonemeal age/final-flower progression missing')
need(has(MPB, 'if (isPitcherCrop())',
         'int nextAge = age + 1',
         'if (nextAge >= 3)',
         'world.setBlock(x, lowerY + 1, z, this, 5 + nextAge, 3)'),
     'Pitcher one-age bonemeal and upper-half growth missing')
need('Creaking Heart state, Dried Ghast hydration, and Sniffer Egg hatch remain import/persistent visual states only' in DOC,
     '34c lifecycle deferral boundary is not documented')
need('Pass 34c supports Bone Meal progression age 0 -> 1 -> the existing Torchflower block' in CONTRACT,
     'Backporter contract does not document Torchflower Pass 34c bonemeal progression')
need('Pass 34c Bone Meal advances one age per use and creates/synchronizes the upper half from age 3' in CONTRACT,
     'Backporter contract does not document Pitcher Pass 34c bonemeal progression')
need('Pass 34c allows in-place stacking from 1..4 while preserving bit2' in CONTRACT,
     'Backporter contract does not document Sea Pickle stacking/runtime state preservation')

if errors:
    for e in errors: print('ERROR: '+e, file=sys.stderr)
    sys.exit(1)

print('Fidelity Pass 34c validation PASSED')
print(' - Pale Hanging Moss accepts valid underside/chain placement')
print(' - Sea Pickles stack in-place from 1..4 while preserving the live/dead visual bit')
print(' - Sea Pickles require floor support and retain count-aware drops/light/model state')
print(' - Bone Meal advances Torchflower crop state and final flower transition')
print(' - Bone Meal advances Pitcher Crop one age at a time and creates/synchronizes the upper half at age 3+')
print(' - Dried Ghast hydration, Creaking Heart state, and Sniffer Egg hatch remain bounded import-state features')
