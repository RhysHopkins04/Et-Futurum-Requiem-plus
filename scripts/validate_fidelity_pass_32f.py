#!/usr/bin/env python3
"""Static regression gates for Pass 32f powered-Shelf hotbar synchronization."""
import json
import pathlib
import sys

root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else '.').resolve()
errors = []

def read(rel):
    try:
        return (root / rel).read_text()
    except Exception as exc:
        errors.append(f"unable to read {rel}: {exc}")
        return ''

def req(text, token, label):
    if token not in text:
        errors.append(f"{label}: missing {token!r}")

parity = read('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
contract_text = read('docs/BACKPORTER_STATE_CONTRACT.json')
doc = read('docs/FIDELITY_PASS_32_BLOCK_ENTITY_IMPORT.md')

# The TE needs a no-update swap primitive so all three slots of each Shelf can be
# exchanged before publishing the block-entity update.
for token in (
    'public ItemStack swapItemNoUpdate(int slot, ItemStack stack)',
    'ItemStack previous = items[slot];',
    'items[slot] = stack;',
    'return previous;',
):
    req(parity, token, 'Shelf no-update swap primitive')

group_start = parity.find('private void swapPoweredShelfGroup')
group_end = parity.find('@Override\n        public boolean onBlockActivated', group_start)
group = parity[group_start:group_end] if group_start >= 0 and group_end >= 0 else ''
if not group:
    errors.append('unable to isolate swapPoweredShelfGroup')
else:
    # Exact modern slot range: one -> 6..8, two -> 3..8, three -> 0..8.
    for token in (
        'int hotbar = 9 - shelfCount * 3;',
        'player.inventory.setInventorySlotContents(hotbar, null);',
        'ItemStack stored = shelf.swapItemNoUpdate(slot, carried);',
        'player.inventory.setInventorySlotContents(hotbar, stored);',
        'shelf.markDirtyAndSync();',
        'syncPlayerInventory(player);',
    ):
        req(group, token, 'powered Shelf hotbar swap')
    if 'player.capabilities.isCreativeMode' in group:
        errors.append('powered Shelf group swap must not special-case Creative mode')

# The direct single-slot path also mutates InventoryPlayer outside a Container click,
# so it must publish a full inventory resync.  Its empty-slot Creative exception is
# intentional modern behaviour and must not leak into the powered group path.
activation_start = parity.find('public boolean onBlockActivated')
activation_end = parity.find('if (entry == POTTED_TORCHFLOWER)', activation_start)
activation = parity[activation_start:activation_end] if activation_start >= 0 and activation_end >= 0 else ''
for token in (
    'ItemStack stored = shelf.swapItemNoUpdate(slot, held);',
    'player.capabilities.isCreativeMode && stored == null && held != null',
    'shelf.markDirtyAndSync();',
    'syncPlayerInventory(player);',
):
    req(activation, token, 'unpowered Shelf single-slot synchronization')

try:
    contract = json.loads(contract_text)
except Exception as exc:
    errors.append('Backporter contract JSON invalid: ' + str(exc))
    contract = {}

if contract.get('contract_revision') not in ('32f', '33', 34, '34', 35, '35'):
    errors.append("contract revision must remain 32f-compatible or advance through Pass 33/34/35")
if contract.get('implemented_in_version') != '3.5.5':
    errors.append("contract implemented_in_version must remain '3.5.5'")
if contract.get('implemented_in_git_ref') != 'refs/tags/3.5.5':
    errors.append("contract implemented_in_git_ref must remain 'refs/tags/3.5.5'")

entries = {e.get('key'): e for e in contract.get('blocks', []) if isinstance(e, dict)}
shelf = entries.get('shelf_family', {})
swap = shelf.get('powered_hotbar_swap', {})
expected = {
    'single_powered_shelf': 'hotbar slots 6..8 (rightmost 3)',
    'two_connected_powered_shelves': 'hotbar slots 3..8 (rightmost 6)',
    'three_connected_powered_shelves': 'hotbar slots 0..8 (all 9)',
}
for key, value in expected.items():
    if swap.get(key) != value:
        errors.append(f'Shelf contract powered_hotbar_swap.{key} must be {value!r}')
if 'full player inventory synchronization' not in swap.get('target_sync', ''):
    errors.append('Shelf contract must declare full player inventory synchronization')
if 'same 3/6/9 exchange as survival' not in swap.get('creative_mode', '').lower():
    errors.append('Shelf contract must declare powered Creative swap parity with Survival')

for token in (
    '## Pass 32f Shelf hotbar synchronization',
    'rightmost-slot mapping',
    'client-side ghost/duplication state',
    'Survival and Creative',
    'contract revision is `32f`',
):
    req(doc, token, 'Pass 32f documentation')

if errors:
    print('Fidelity Pass 32f validation FAILED')
    for error in errors:
        print(' - ' + error)
    sys.exit(1)

print('Fidelity Pass 32f validation PASSED')
print(' - powered Shelf swaps mutate all affected server hotbar/Shelf slots and fully resync InventoryPlayer')
print(' - one/two/three powered Shelf groups retain modern rightmost 3/6/9 slot mapping')
print(' - disconnected powered Shelves remain independent one-Shelf groups targeting hotbar slots 6..8')
print(' - Pass 32f Shelf contract remains valid under contract revision ' + str(contract.get('contract_revision')))
