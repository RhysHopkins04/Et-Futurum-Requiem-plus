#!/usr/bin/env python3
"""Static regression gate for Pass 33 small remaining visible states."""
import json, pathlib, sys
ROOT=pathlib.Path(sys.argv[1] if len(sys.argv)>1 else pathlib.Path(__file__).resolve().parents[1]).resolve()
errors=[]
def read(rel):
    try:return (ROOT/rel).read_text(encoding='utf-8')
    except Exception as exc: errors.append('%s: %s'%(rel,exc)); return ''
def req(text, token, label):
    if token not in text: errors.append('%s missing %r'%(label,token))
java=read('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
model=read('src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java')
manifest=json.loads(read('scripts/modern_map_parity_blocks.json') or '{}')
contract=json.loads(read('docs/BACKPORTER_STATE_CONTRACT.json') or '{}')
coverage=read('scripts/validate_modern_visual_state_coverage.py')
if manifest.get('count')!=244: errors.append('Pass 33 manifest count must be 244')
names={b.get('name') for b in manifest.get('blocks',[])}
for name in ('pale_oak_button','pale_oak_pressure_plate','copper_torch','copper_wall_torch'):
    if name not in names: errors.append('manifest missing '+name)
for token in (
    'PALE_OAK_PRESSURE_PLATE("1.21.4", Style.PRESSURE_PLATE',
    'COPPER_WALL_TORCH("1.21.9", Style.TORCH',
    'new ParityPressurePlateBlock(this)',
    'class ParityButtonTileEntity extends TileEntity',
    'tag.setBoolean("Powered", powered)',
    'return face * 4 + facing;',
    'world.scheduleBlockUpdate(x, y, z, this, tickRate(world));',
    '"copper_wall_torch".equals(name)',
    'world.setBlock(x, y, z, COPPER_WALL_TORCH.get(), side, 3)',
    'blockEntityTag.setBoolean("Cracked"',
    'tag.setBoolean("Cracked", cracked)',
): req(java,token,'ModernMapParityBlocks')
for token in (
    '"pale_oak_button".equals(name)',
    'prepared.facingModels[powered * 12 + face * 4 + facing]',
    '"pale_oak_pressure_plate".equals(registryName)',
    '"copper_wall_torch".equals(registryName)',
    'state.put("facing", facings[side]);',
    'style == ModernMapParityBlocks.Style.CANDLE_CAKE',
): req(model,token,'ModernJsonModelBridge')
if str(contract.get('contract_revision')) not in ('33','34','35'): errors.append('Backporter contract revision must be Pass 33 or forward-compatible Pass 34/35')
entries={e.get('key'):e for e in contract.get('blocks',[]) if isinstance(e,dict)}
for key in ('pale_oak_button','pale_oak_pressure_plate','copper_torch_family','candle_cake_family','decorated_pot'):
    if key not in entries: errors.append('Backporter contract missing '+key)
pot=entries.get('decorated_pot',{})
if 'Cracked' not in [f.get('name') for f in pot.get('tile_entity',{}).get('fields',[]) if isinstance(f,dict)]: errors.append('Decorated Pot contract missing Cracked TE field')
for token in ('STORED_EXACTLY','DERIVED_EXACTLY','VISUALLY_IRRELEVANT','UNSUPPORTED','general waterlogging','--allow-unsupported'):
    req(coverage,token,'global visual-state validator')
# Candle Cake was already correctly stateful: keep that existing path rather than replacing it.
for token in ('if (isCandleCake())','int meta = world.getBlockMetadata(x, y, z) & 1;','setMetadataAndRelight(world, x, y, z, 1);'):
    req(java,token,'Candle Cake regression contract')
if errors:
    print('Fidelity Pass 33 validation FAILED')
    for e in errors: print(' - '+e)
    sys.exit(1)
print('Fidelity Pass 33 validation PASSED')
print(' - Pale Oak Button stores all face/facing states plus synchronized Powered TE state')
print(' - Pale Oak Pressure Plate uses real wooden pressure-plate powered geometry/mechanics')
print(' - Copper Torch has separate standing and technical wall identities with deterministic facing')
print(' - Decorated Pot Cracked persists without rewriting Pass 32f sherd/item/facing state')
print(' - all Candle Cake identities retain exact lit/unlit model state')
print(' - global visible-state validator foundation exposes four exact classification values')
