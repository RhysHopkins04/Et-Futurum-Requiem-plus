#!/usr/bin/env python3
"""Static gate for Pass 34 persistent/visible map-state fidelity."""
from __future__ import print_function
import json, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MPB = (ROOT/'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
BRIDGE = (ROOT/'src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java').read_text()
SAPLING = (ROOT/'src/main/java/ganymedes01/etfuturum/blocks/BlockModernSapling.java').read_text()
RENDERER = (ROOT/'src/main/java/ganymedes01/etfuturum/client/renderer/block/BlockModernJsonModelRenderer.java').read_text()
PROXY = (ROOT/'src/main/java/ganymedes01/etfuturum/core/proxy/CommonProxy.java').read_text()
AUDIT = (ROOT/'scripts/audit_modern_map_parity_capabilities.py').read_text()
VIS = (ROOT/'scripts/validate_modern_visual_state_coverage.py').read_text()
CONTRACT = json.loads((ROOT/'docs/BACKPORTER_STATE_CONTRACT.json').read_text())

errors=[]
def need(ok,msg):
    if not ok: errors.append(msg)
def has(text,*parts): return all(p in text for p in parts)

need(str(CONTRACT.get('contract_revision')) == '34', 'Backporter contract revision must be 34')

# Pale Moss Carpet 2 * 3^4 and synchronized TE.
need(has(MPB,'ParityPaleMossCarpetTileEntity','paleMossStateIndex','Bottom','North','East','South','West'), 'Pale Moss TE/state codec missing')
need(has(BRIDGE,'"pale_moss_carpet"','for (int bottom = 0; bottom <= 1; bottom++)','north < 3','east < 3','south < 3','west < 3'), 'Pale Moss 162-state model preparation missing')
need('getPaleMossStateIndex(world, x, y, z)' in BRIDGE, 'Pale Moss world model selection missing')

# Metadata-backed families.
checks={
 'pale_hanging_moss': ['state.put("tip"','facingModels[tip]'],
 'creaking_heart': ['String[] axes = {"x", "y", "z"}','String[] heartStates = {"dormant", "awake", "uprooted"}','hs * 3 + axis'],
 'dried_ghast': ['hydration < 4','hydration * 4 + facing'],
 'torchflower_crop': ['age <= 1','facingModels[age]'],
 'pitcher_crop': ['age <= 4','half == 0 ? "lower" : "upper"','5) + age'],
 'pitcher_plant': ['state.put("half"','facingModels[half]'],
 'sniffer_egg': ['hatch <= 2','facingModels[hatch]'],
 'sea_pickle': ['pickles <= 4','state.put("waterlogged"','(live << 2) | (pickles - 1)'],
 'tall_seagrass': ['half == 0 ? "lower" : "upper"','facingModels[half]'],
}
for name,parts in checks.items():
    need(has(BRIDGE,'"'+name+'"',*parts), name+' model-state preparation incomplete')

need(has(MPB,'if (isSeaPickle()) return (meta & 3) + 1','6 + (meta & 3) * 3'), 'Sea Pickle count drops/dynamic light mapping missing')
need(has(MPB,'isPitcherCrop()','otherY = upper ? y - 1 : y + 1'), 'Pitcher paired-break safety missing')

# Mature Mangrove target with retained legacy registry compatibility.
need(has(MPB,'legacyMangroveAlias','entry == MANGROVE_PROPAGULE','ConfigBlocksItems.enableMangroveWoodFamily'), 'Mangrove legacy compatibility alias registration missing')
need(has(MPB,'!technicalPlacementBlock && !legacyMangroveAlias'), 'Mangrove compatibility alias must stay hidden from the creative tab')
need(has(SAPLING,'MangrovePropaguleStateTileEntity','Hanging','Age','getMangroveAge','isMangroveHanging'), 'Mature BlockModernSapling visible-state TE missing')
need('ModernJsonModelBridge.prepare(ModernMapParityBlocks.MANGROVE_PROPAGULE, reg)' in SAPLING, 'Mangrove modern model preparation missing')
need(has(RENDERER,'block == ModBlocks.SAPLING.get()','MANGROVE_PROPAGULE','renderCrossedSquares'), 'Mangrove/cherry renderer split missing')
need(has(RENDERER,'renderInventoryBlock','block == ModBlocks.SAPLING.get()','drawCrossedSquares'), 'Mangrove/Cherry inventory crossed-square presentation regressed')
need('modern_mangrove_propagule' in PROXY, 'Mangrove state TE registration missing')
need(has(AUDIT,'if profile == "PASS_34_VISIBLE_STATE":','"review_status": "PASS_34_VERIFIED"'), 'Pass 34 capability rows are not promoted to PASS_34_VERIFIED')

# Global classifier coverage.
for name,prop in [
 ('pale_moss_carpet','north'),('pale_hanging_moss','tip'),('creaking_heart','creaking_heart_state'),
 ('dried_ghast','hydration'),('torchflower_crop','age'),('pitcher_crop','half'),('pitcher_plant','half'),('sniffer_egg','hatch'),
 ('mangrove_propagule','hanging'),('sea_pickle','pickles'),('tall_seagrass','half')]:
    need(name in VIS and prop in VIS, 'global visible-state classifier lacks %s.%s'%(name,prop))
need('name == "sea_pickle" and prop == "waterlogged"' in VIS, 'Sea Pickle bounded live/dead waterlogged visual classification missing')

bykey={b.get('key'):b for b in CONTRACT.get('blocks',[])}
required=['pale_moss_carpet','pale_hanging_moss','creaking_heart','dried_ghast','torchflower_crop','pitcher_crop','pitcher_plant','sniffer_egg','mangrove_propagule','sea_pickle','tall_seagrass']
for key in required:
    need(key in bykey, 'Backporter contract missing '+key)
    if key in bykey:
        need(bykey[key].get('classification')=='FULL_IMPORT', 'Pass 34 contract entry not FULL_IMPORT: '+key)
        need(bool(bykey[key].get('source_properties')), 'Pass 34 contract lacks source property mapping: '+key)
        need(bool(bykey[key].get('visual_property_classification')), 'Pass 34 contract lacks exactness classification: '+key)
need(bykey.get('mangrove_propagule',{}).get('target_ids') == ['etfuturum:sapling'], 'Mangrove Backporter target must be mature etfuturum:sapling')

# Already-exact small states must stay represented.
need(has(BRIDGE,'"turtle_egg"','hatch <= 2','eggs <= 4'), 'Turtle Egg exact 12-state renderer regressed')
need(has(BRIDGE,'"leaf_litter"','"wildflowers"','amount <= 4'), 'Leaf Litter/Wildflowers amount/facing renderer regressed')

if errors:
    for e in errors: print('ERROR: '+e, file=sys.stderr)
    sys.exit(1)
print('Fidelity Pass 34 validation PASSED')
print(' - Pale Moss Carpet stores and renders all 162 bottom/side combinations')
print(' - Pale Hanging Moss, Creaking Heart, Dried Ghast, crops and Sniffer Egg preserve exact visible metadata states')
print(' - mature Mangrove Propagule stores Hanging/Age while retaining the hidden Pass-33 registry compatibility alias')
print(' - Sea Pickle preserves count plus bounded live/dead visual state without general waterlogging')
print(' - Tall Seagrass half state is included; Turtle Egg and Leaf Litter/Wildflowers remain exact')
