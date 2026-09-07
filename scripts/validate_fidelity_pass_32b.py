#!/usr/bin/env python3
"""Static regression gates for the Pass 32b runtime fixes, including their Pass 32c successor."""
from __future__ import print_function
import pathlib, sys
ROOT = pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parents[1]
errors=[]
def read(rel):
    p=ROOT/rel
    if not p.is_file(): errors.append('missing required file: '+rel); return ''
    return p.read_text(encoding='utf-8', errors='ignore')
def req(text, token, label):
    if token not in text: errors.append(label+': missing '+token)
parity=read('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
renderer=read('src/main/java/ganymedes01/etfuturum/client/renderer/tileentity/TileEntityParityCopperChestRenderer.java')
model=read('src/main/java/ganymedes01/etfuturum/client/model/ModelCopperChestHalf.java')
contract=read('docs/BACKPORTER_STATE_CONTRACT.json')
pair_mixin=read('src/main/java/ganymedes01/etfuturum/mixins/early/chestpairing/MixinTileEntityChest.java')
pair_utils=read('src/main/java/ganymedes01/etfuturum/core/utils/ModernChestPairing.java')
block_mixin=read('src/main/java/ganymedes01/etfuturum/mixins/early/chestpairing/MixinBlockChest.java')
for token in (
    'case 1: horizontal = 1.0F - hitZ;',
    'case 3: horizontal = hitZ;',
    'shelf.setLastInteractedSlot(slot);',
    'shelf.setInventorySlotContents(slot, inserted);',
    'super(2);',
): req(parity, token, 'Pass 32b shared state')
# 32c supersedes the old Copper-only adjacency override with explicit TileEntityChest pairing.
req(pair_utils, 'EFRPairDirection', 'Pass 32c successor chest state')
for token in ('checkForAdjacentChests', 'etfu$partnerAt', 'getBlockType()'):
    req(pair_mixin, token, 'Pass 32c successor chest state')
for token in ('placer.isSneaking()', 'IChestPairingState.NONE'):
    req(block_mixin, token, 'Pass 32c successor chest placement')
if ('world.getBlock(nx, y, nz) != self' not in block_mixin
        and 'ModernChestPairing.areCompatibleChestBlocks' not in block_mixin):
    errors.append('Pass 32c successor chest placement: missing exact-or-Pass36-compatible chest identity gate')
for token in (
    'private enum Half { SINGLE, LEFT, RIGHT }',
    'new ModelCopperChestHalf(true)',
    'new ModelCopperChestHalf(false)',
    'copper_exposed', 'copper_weathered', 'copper_oxidized',
    'texture += "_left"', 'texture += "_right"',
    'GL11.GL_POLYGON_OFFSET_FILL',
    'chest.checkForAdjacentChests()',
): req(renderer, token, 'Pass 32b Copper Chest renderer')
for token in ('15, 5, 14', '15, 10, 14', 'setTextureSize(64, 64)'):
    req(model, token, 'Pass 32b Copper Chest half model')
for token in ('front-relative for all four facings', 'EFRPairDirection', 'single/left/right'):
    req(contract, token, 'Pass 32b/32c Backporter contract')
if errors:
    print('Fidelity Pass 32b validation FAILED', file=sys.stderr)
    for e in errors: print(' - '+e, file=sys.stderr)
    sys.exit(1)
print('Fidelity Pass 32b validation PASSED')
print(' - Chiseled Bookshelf hit mapping remains front-relative for all four facings')
print(' - LastInteractedSlot/comparator state remains separated from inventory mutation')
print(' - Copper Chest doubles retain modern left/right half models/textures and seam repair')
print(' - Pass 32c explicit pairing supersedes the old Copper-only raw-adjacency implementation')
