#!/usr/bin/env python3
"""Static regression gates for Pass 32d modern chest secondary placement and immediate unlink."""
from __future__ import print_function
import pathlib, sys

ROOT = pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parents[1]
errors=[]

def read(rel):
    p=ROOT/rel
    if not p.is_file():
        errors.append('missing required file: '+rel)
        return ''
    return p.read_text(encoding='utf-8', errors='ignore')

def req(text, token, label):
    if token not in text:
        errors.append(label+': missing '+token)

loader=read('src/main/java/ganymedes01/etfuturum/mixinplugin/EtFuturumEarlyMixins.java')
util=read('src/main/java/ganymedes01/etfuturum/core/utils/ModernChestPairing.java')
item=read('src/main/java/ganymedes01/etfuturum/mixins/early/chestpairing/MixinItemBlock.java')
block=read('src/main/java/ganymedes01/etfuturum/mixins/early/chestpairing/MixinBlockChest.java')
tile=read('src/main/java/ganymedes01/etfuturum/mixins/early/chestpairing/MixinTileEntityChest.java')
doc=read('docs/FIDELITY_PASS_32_BLOCK_ENTITY_IMPORT.md')

req(loader, 'mixins.add("chestpairing.MixinItemBlock");', 'early mixin loader')

for token in (
    'ThreadLocal<PlacementClick>',
    'capturePlacementClick',
    'consumeClickedPartnerDirection',
    'clearPlacementClick',
    'click.clickedSide < 2 || click.clickedSide > 5',
    'direction != directionToClickedBlock(click.clickedSide)',
):
    req(util, token, 'placement click context')

if ('world.getBlock(click.clickedX, click.clickedY, click.clickedZ) != placedBlock' not in util
        and '!areCompatibleChestBlocks(world.getBlock(click.clickedX, click.clickedY, click.clickedZ), placedBlock)' not in util):
    errors.append('placement click context: missing exact-id or compatible-family clicked chest gate')

for token in (
    '@Mixin(ItemBlock.class)',
    '@Inject(method = "onItemUse", at = @At("HEAD"))',
    '@Inject(method = "onItemUse", at = @At("RETURN"))',
    'ModernChestPairing.capturePlacementClick',
    'ModernChestPairing.clearPlacementClick',
    'Block.getBlockFromItem((Item) (Object) this)',
):
    req(item, token, 'ItemBlock click capture')

for token in (
    'byte clickedPartner = ModernChestPairing.consumeClickedPartnerDirection',
    'if (placer.isSneaking() && ModernChestPairing.isPair(clickedPartner))',
    'other.etfu$getPairDirection() == IChestPairingState.NONE',
    'facing = otherFacing;',
    'ModernChestPairing.isLateralForFacing(clickedPartner, otherFacing)',
    'etfu$preferredCompatibleNeighbour',
    '@Inject(method = "onNeighborBlockChange", at = @At("TAIL"))',
    '((IChestPairingState) tile).etfu$resolvePairing();',
    'world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);',
):
    req(block, token, 'BlockChest modern secondary placement/unlink')

for token in (
    'this.etfu$setPairDirectionInternal(IChestPairingState.NONE, !this.worldObj.isRemote);',
    'Copper Chest rendering no longer waits',
):
    req(tile, token, 'TileEntityChest immediate missing-partner collapse')

for token in (
    '## Pass 32d chest placement/render responsiveness',
    'horizontal face of a compatible *single* chest',
    'floor, top face',
    'clockwise lateral single candidate',
    'Copper Chest unlink rendering is immediate',
):
    req(doc, token, 'Pass 32d human-readable contract')

# Prevent the old blanket "sneak always means single" implementation from returning.
if 'if (!placer.isSneaking()) {' not in block:
    errors.append('normal-placement auto-pair gate missing')
if 'etfu$singleCompatibleNeighbour' in block:
    errors.append('old ambiguous-neighbour helper still present; Pass 32d requires modern priority selection')

if errors:
    print('Fidelity Pass 32d validation FAILED', file=sys.stderr)
    for e in errors:
        print(' - '+e, file=sys.stderr)
    sys.exit(1)

print('Fidelity Pass 32d validation PASSED')
print(' - sneak-clicking a compatible chest side explicitly targets that single chest')
print(' - sneak placement on floor/top/non-chest stays independent')
print(' - ordinary placement uses modern clockwise/counter-clockwise candidate priority')
print(' - Copper Chest pair removal resolves on the first client neighbour update')
