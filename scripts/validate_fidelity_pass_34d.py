#!/usr/bin/env python3
"""Static regression gate for Pass 34d hanging-moss placement and mature Pitcher Plant parity."""
from __future__ import print_function
import json, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MPB = (ROOT/'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
BRIDGE = (ROOT/'src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java').read_text()
VIS = (ROOT/'scripts/validate_modern_visual_state_coverage.py').read_text()
AUDIT = (ROOT/'scripts/audit_modern_map_parity_capabilities.py').read_text()
MANIFEST = json.loads((ROOT/'scripts/modern_map_parity_blocks.json').read_text())
CONTRACT = json.loads((ROOT/'docs/BACKPORTER_STATE_CONTRACT.json').read_text())
DOC = (ROOT/'docs/FIDELITY_PASS_34_MAP_STATE.md').read_text()

errors=[]
def need(ok,msg):
    if not ok: errors.append(msg)
def has(text,*parts): return all(part in text for part in parts)

# Pale Hanging Moss must no longer rely on generic 1.7 ItemBlock placement.
need(has(MPB, 'ParityPaleHangingMossItemBlock extends ItemBlock',
         'if (side != 0', 'int targetY = y - 1',
         'canPaleHangingMossHangAt(world, x, targetY, z)',
         'placeBlockAt(stack, player, world, x, targetY, z'),
     'dedicated underside Pale Hanging Moss ItemBlock placement is missing')
need(has(MPB, 'entry == PALE_HANGING_MOSS',
         'GameRegistry.registerBlock(entry.block, ParityPaleHangingMossItemBlock.class, name)'),
     'Pale Hanging Moss is not registered with its dedicated ItemBlock')
need(has(MPB, 'canPaleHangingMossHangAt',
         'above == this', '!above.getMaterial().isLiquid()',
         '!above.isReplaceable(world, x, y + 1, z)'),
     'Pale Hanging Moss support predicate does not accept real non-replaceable/foliage support')
need(has(MPB, 'if (isPaleHangingMoss())',
         'world.setBlockMetadataWithNotify(x, y, z, 1, 2)',
         'world.setBlockMetadataWithNotify(x, y + 1, z, 0, 3)'),
     'Pale Hanging Moss tip/body chain transition regressed')

# Mature Pitcher Plant is distinct from Pitcher Crop and has exact half state.
need(has(MPB, 'private boolean isPitcherPlant() { return entry == PITCHER_PLANT; }',
         'if (isPitcherPlant()) return 0',
         'world.setBlock(x, y + 1, z, this, 1, 3)'),
     'mature Pitcher Plant normal two-block placement is missing')
need(has(MPB, 'if (isPitcherPlant())', 'boolean upper = (meta & 1) != 0',
         'int otherY = upper ? y - 1 : y + 1', 'world.setBlockToAir(x, otherY, z)'),
     'mature Pitcher Plant paired break cleanup is missing')
need(has(MPB, 'if (isPitcherPlant()) return 1;',
         'isPitcherCrop() || isPitcherPlant()'),
     'mature Pitcher Plant sane one-item drops/pick damage mapping missing')
need(has(BRIDGE, '"pitcher_plant".equals(registryName)',
         'state.put("half", half == 0 ? "lower" : "upper")',
         'prepared.facingModels[half] = loadBlockStateModel(entry, state)'),
     'mature Pitcher Plant lower/upper model preparation missing')
need(has(BRIDGE, '"pitcher_plant".equals(name)',
         'Model staged = models.facingModels[state]'),
     'mature Pitcher Plant world model selection does not use metadata half')
need('name == "pitcher_plant" and prop == "half"' in VIS,
     'global visual-state validator does not classify pitcher_plant.half')
need('"pitcher_plant"' in AUDIT,
     'capability audit does not include mature Pitcher Plant in Pass 34 visible-state set')

entries = MANIFEST.get('entries', MANIFEST.get('blocks', []))
pitcher_manifest = next((e for e in entries if e.get('name') == 'pitcher_plant'), None)
need(pitcher_manifest is not None and '0=lower' in pitcher_manifest.get('meta','') and '1=upper' in pitcher_manifest.get('meta',''),
     'canonical manifest does not document mature Pitcher Plant metadata halves')

bykey={b.get('key'):b for b in CONTRACT.get('blocks',[])}
pitcher=bykey.get('pitcher_plant')
need(pitcher is not None, 'Backporter contract missing mature pitcher_plant entry')
if pitcher:
    need(pitcher.get('target_ids') == ['etfuturum:pitcher_plant'], 'pitcher_plant target registry identity is wrong')
    need(pitcher.get('source_properties',{}).get('half','').startswith('exact'), 'pitcher_plant half is not exact in contract')
    need(pitcher.get('visual_property_classification',{}).get('half') == 'STORED_EXACTLY', 'pitcher_plant half exactness classification missing')
    need('meta 0=half=lower' in pitcher.get('metadata',{}).get('layout',''), 'pitcher_plant metadata layout missing')
need('Pass 34d runtime corrections' in DOC and 'ParityPaleHangingMossItemBlock' in DOC and 'Pitcher Plant' in DOC,
     'Pass 34d runtime corrections are not documented')

if errors:
    for e in errors: print('ERROR: '+e, file=sys.stderr)
    sys.exit(1)
print('Fidelity Pass 34d validation PASSED')
print(' - Pale Hanging Moss uses an explicit underside-placement ItemBlock and preserves tip/body chain state')
print(' - mature Pitcher Plant now stores/renders exact lower and upper halves and places as a two-block plant')
print(' - Pitcher Plant paired breaking yields one sane item drop without leaving the mate behind')
print(' - Backporter contract, manifest, capability audit and global visual-state classifier cover pitcher_plant.half')
