#!/usr/bin/env python3
"""Static regression gates for Pass 32e Shelf parity and final Backporter contract metadata."""
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
bridge = read('src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java')
contract_text = read('docs/BACKPORTER_STATE_CONTRACT.json')
doc = read('docs/FIDELITY_PASS_32_BLOCK_ENTITY_IMPORT.md')

for token in (
    'public int getOccupancyMask()',
    'if (items[slot] != null) mask |= 1 << slot;',
    '((ParityShelfTileEntity) tile).getOccupancyMask()',
    'if (isDecoratedPot()) {',
    'Container.calcRedstoneFromInventory((IInventory) tile)',
):
    req(parity, token, 'Shelf comparator')

# Both gameplay grouping and visual side_chain must identify ANY parity Shelf, not this exact wood block.
for token in (
    'ModernMapParityBlocks neighborEntry = ModernMapParityBlocks.fromBlock(world.getBlock(x, y, z));',
    'neighborEntry.getStyle() != Style.SHELF',
    '(meta & 3) == facing && (meta & 4) != 0',
):
    req(parity, token, 'mixed-wood Shelf gameplay')
if 'world.getBlock(x, y, z) != this' in parity[parity.find('private boolean isConnectedPoweredShelf'):parity.find('private void swapPoweredShelfGroup')]:
    errors.append('Shelf gameplay connection still requires exact same block instance')

for token in (
    'ModernMapParityBlocks neighborEntry = ModernMapParityBlocks.fromBlock(world.getBlock(x, y, z));',
    'neighborEntry.getStyle() != ModernMapParityBlocks.Style.SHELF',
    '(meta & 3) == facing && (meta & 4) != 0',
):
    req(bridge, token, 'mixed-wood Shelf visual side_chain')
if 'world.getBlock(x, y, z) != entry.get()' in bridge[bridge.find('private static boolean connectedPoweredShelf'):bridge.find('private static Model chiseledBookshelfModel')]:
    errors.append('Shelf visual connection still requires exact same wood block')

try:
    contract = json.loads(contract_text)
except Exception as exc:
    errors.append('Backporter contract JSON invalid: ' + str(exc))
    contract = {}

revision = str(contract.get('contract_revision', ''))
if revision not in ('32e', '32f') and not (revision.isdigit() and int(revision) >= 33):
    errors.append("contract contract_revision must be '32e' or a later fidelity pass revision")
for key, expected in (
    ('implemented_in_version', '3.5.5'),
    ('implemented_in_git_ref', 'refs/tags/3.5.5'),
    ('source_minecraft', '1.21.11'),
):
    if contract.get(key) != expected:
        errors.append(f'contract {key} must be {expected!r}')
for stale in ('authoritative_source_commit', 'generated_by_pass'):
    if stale in contract:
        errors.append('contract still contains stale provenance field ' + stale)

entries = {e.get('key'): e for e in contract.get('blocks', []) if isinstance(e, dict)}
shelf = entries.get('shelf_family', {})
sp = shelf.get('source_properties', {})
if 'stored exactly' not in sp.get('facing', '') or 'stored exactly' not in sp.get('powered', ''):
    errors.append('Shelf contract must explicitly mark facing and powered as stored')
if 'DERIVED' not in sp.get('side_chain', ''):
    errors.append('Shelf contract must explicitly mark side_chain as DERIVED')
if 'ANY wood type' not in json.dumps(shelf):
    errors.append('Shelf contract must declare mixed-wood chaining')
comp = shelf.get('comparator', {})
if comp.get('slot_bits') != {'0': 1, '1': 2, '2': 4} or comp.get('output_range') != '0..7':
    errors.append('Shelf comparator contract must map slot bits 1/2/4 to output 0..7')
if comp.get('stack_size_affects_output') is not False:
    errors.append('Shelf comparator must be independent of stack size')
if 'waterlogged' not in ' '.join(shelf.get('unsupported', [])):
    errors.append('Shelf waterlogging limitation missing from contract')

for token in ('## Pass 32e finalization', 'occupied-slot bitmask', 'any wood variant', 'refs/tags/3.5.5'):
    req(doc, token, 'Pass 32e documentation')

if errors:
    print('Fidelity Pass 32e validation FAILED')
    for e in errors:
        print(' - ' + e)
    sys.exit(1)
print('Fidelity Pass 32e validation PASSED')
print(' - Shelf comparator is occupancy bitmask 1/2/4 -> 0..7, stack-size independent')
print(' - powered same-facing Shelves chain and group across mixed wood variants')
print(' - Backporter contract provenance remains on version/tag 3.5.5 with Pass 32e-or-later finalization revision')
