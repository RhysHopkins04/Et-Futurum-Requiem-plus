#!/usr/bin/env python3
"""Static regression gates for Pass 32c explicit chest pairing and bookshelf hand-safety repair."""
from __future__ import print_function
import json, pathlib, sys
ROOT = pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parents[1]
errors=[]
def read(rel):
    p=ROOT/rel
    if not p.is_file(): errors.append('missing required file: '+rel); return ''
    return p.read_text(encoding='utf-8', errors='ignore')
def req(text, token, label):
    if token not in text: errors.append(label+': missing '+token)
loader=read('src/main/java/ganymedes01/etfuturum/mixinplugin/EtFuturumEarlyMixins.java')
state=read('src/main/java/ganymedes01/etfuturum/core/utils/IChestPairingState.java')
util=read('src/main/java/ganymedes01/etfuturum/core/utils/ModernChestPairing.java')
tile=read('src/main/java/ganymedes01/etfuturum/mixins/early/chestpairing/MixinTileEntityChest.java')
base=read('src/main/java/ganymedes01/etfuturum/mixins/early/chestpairing/MixinTileEntity.java')
block=read('src/main/java/ganymedes01/etfuturum/mixins/early/chestpairing/MixinBlockChest.java')
parity=read('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
doc=read('docs/FIDELITY_PASS_32_BLOCK_ENTITY_IMPORT.md')
contract_text=read('docs/BACKPORTER_STATE_CONTRACT.json')

for token in ('chestpairing.MixinTileEntity', 'chestpairing.MixinTileEntityChest', 'chestpairing.MixinBlockChest'):
    req(loader, token, 'early mixin loader')
for token in ('byte UNKNOWN = -1', 'byte NONE = 0', 'byte WEST = 1', 'byte EAST = 2', 'byte NORTH = 3', 'byte SOUTH = 4'):
    req(state, token, 'pair-state interface')
for token in ('NBT_PAIR_DIRECTION = "EFRPairDirection"', 'PAIR_BLOCK_EVENT = 2', 'isLateralForFacing', 'opposite', 'isManagedChestBlock', 'Blocks.chest', 'Blocks.trapped_chest', 'isCopperChestBlock'):
    req(util, token, 'pair-state utilities')
for token in (
    'tag.hasKey(ModernChestPairing.NBT_PAIR_DIRECTION, 1)',
    'tag.setByte(ModernChestPairing.NBT_PAIR_DIRECTION, direction)',
    '@Inject(method = "checkForAdjacentChests"',
    'this.etfu$migrateLegacyPair()',
    'other.etfu$setPairDirection(expectedOther)',
    'ownBlock == null || this.worldObj.getBlock(x, this.yCoord, z) != ownBlock',
    'ownMeta != otherMeta',
): req(tile, token, 'TileEntityChest explicit pairing')
for token in (
    'getDescriptionPacket',
    '@Inject(method = "onDataPacket", at = @At("HEAD"), cancellable = true, remap = false)',
    'S35PacketUpdateTileEntity',
    'IChestPairingState',
    'isManagedChestBlock',
):
    req(base, token, 'TileEntity pair sync')
for token in (
    '@Inject(method = "canPlaceBlockAt"',
    'isManagedChestBlock((Block) (Object) this)',
    'cir.setReturnValue(true)',
    '@Inject(method = "func_149954_e"',
    'placer.isSneaking()',
    'world.getBlock(nx, y, nz) != self',
    'other.etfu$setPairDirection(ModernChestPairing.opposite(candidate))',
    '@Inject(method = "breakBlock"',
    '@Inject(method = "func_149951_m"',
    'new InventoryLargeChest("container.chestDouble"',
): req(block, token, 'BlockChest modern placement/inventory')
for token in (
    'syncPlayerInventory(player);',
    '((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);',
    'public static boolean isCopperChestBlock(Block block)',
    'public static final class ParityCopperChestTileEntity extends TileEntityChest',
    'Pairing is supplied by the Pass 32c TileEntityChest mixin',
): req(parity, token, 'Parity runtime integration')
if 'public void checkForAdjacentChests()' in parity[parity.find('ParityCopperChestTileEntity'):parity.find('/** Text storage', parity.find('ParityCopperChestTileEntity'))]:
    errors.append('Copper Chest must not retain its old raw-adjacency checkForAdjacentChests override')

try:
    contract=json.loads(contract_text)
except Exception as exc:
    errors.append('Backporter contract JSON invalid: '+str(exc)); contract={}
if contract.get('contract_revision') not in ('32c','32d','32e','32f'): errors.append('contract revision must be 32c or a later Pass 32 revision')
entries={e.get('key'):e for e in contract.get('blocks',[]) if isinstance(e,dict)}
for key in ('vanilla_chest_family','copper_chest_family'):
    e=entries.get(key)
    if not e:
        errors.append('contract missing '+key); continue
    fields={f.get('name') for f in e.get('tile_entity',{}).get('fields',[]) if isinstance(f,dict)}
    if 'EFRPairDirection' not in fields: errors.append(key+' missing EFRPairDirection TE field')
    mapping=e.get('pair_direction_mapping',{})
    if mapping.get('north',{}).get('left')!='east' or mapping.get('east',{}).get('left')!='south':
        errors.append(key+' pair_direction_mapping is incomplete/incorrect')
    if 'type' not in e.get('source_properties',{}): errors.append(key+' must map modern chest type')
vanilla=entries.get('vanilla_chest_family',{})
if vanilla.get('source_ids') != ['minecraft:chest','minecraft:trapped_chest']:
    errors.append('vanilla chest contract must cover normal and trapped chests')

for token in ('## Pass 32c runtime repair', '`EFRPairDirection`', 'sneaking while placing keeps the new chest', 'inventory-container resync', 'unrelated modded'):
    req(doc, token, 'Pass 32c human-readable contract')

if errors:
    print('Fidelity Pass 32c validation FAILED', file=sys.stderr)
    for e in errors: print(' - '+e, file=sys.stderr)
    sys.exit(1)
print('Fidelity Pass 32c validation PASSED')
print(' - vanilla/trapped/Copper Chest pairs use persisted reciprocal EFRPairDirection')
print(' - adjacent singles, sneak placement, different-facing singles and pair unlink are gated')
print(' - client pair state is synced by block event plus TileEntity description packet')
print(' - Copper Chest rendering consumes the shared TileEntityChest adjacency cache')
print(' - Chiseled Bookshelf insert/remove explicitly resynchronizes the player inventory')
