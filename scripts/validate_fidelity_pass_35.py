#!/usr/bin/env python3
"""Static gate for Pass 35 complex/technical visible-state parity."""
from __future__ import print_function
import json, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MPB = (ROOT/'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
BRIDGE = (ROOT/'src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java').read_text()
RENDER = (ROOT/'src/main/java/ganymedes01/etfuturum/client/renderer/block/BlockModernJsonModelRenderer.java').read_text()
PLUGIN = (ROOT/'src/main/java/ganymedes01/etfuturum/mixinplugin/EtFuturumEarlyMixins.java').read_text()
CMD_IFACE = (ROOT/'src/main/java/ganymedes01/etfuturum/core/utils/IModernCommandBlockState.java').read_text()
CMD_BLOCK = (ROOT/'src/main/java/ganymedes01/etfuturum/mixins/early/commandblockstate/MixinBlockCommandBlock.java').read_text()
CMD_TE = (ROOT/'src/main/java/ganymedes01/etfuturum/mixins/early/commandblockstate/MixinTileEntityCommandBlock.java').read_text()
AUDIT = (ROOT/'scripts/audit_modern_map_parity_capabilities.py').read_text()
VIS = (ROOT/'scripts/validate_modern_visual_state_coverage.py').read_text()
MANIFEST = json.loads((ROOT/'scripts/modern_map_parity_blocks.json').read_text())
CONTRACT = json.loads((ROOT/'docs/BACKPORTER_STATE_CONTRACT.json').read_text())
DOC = (ROOT/'docs/FIDELITY_PASS_35_MAP_STATE.md').read_text()

errors=[]
def need(ok,msg):
    if not ok: errors.append(msg)
def has(text,*parts): return all(p in text for p in parts)

need(str(CONTRACT.get('contract_revision')) == '35', 'Backporter contract revision must be 35')
need('PASS_35_VISIBLE_STATE' in AUDIT and 'PASS_35_VERIFIED' in AUDIT,
     'capability audit does not promote Pass 35 rows')
need('facingModels = new Model[64]' in BRIDGE, 'model-state array is too small for 48-state Crafter matrix')

# Copper Golem Statue family: 8 identities x 16 states.
statues=[
 'copper_golem_statue','waxed_copper_golem_statue','exposed_copper_golem_statue','waxed_exposed_copper_golem_statue',
 'weathered_copper_golem_statue','waxed_weathered_copper_golem_statue','oxidized_copper_golem_statue','waxed_oxidized_copper_golem_statue']
for name in statues:
    need(name.upper() in MPB, 'missing Copper Golem Statue identity '+name)
need(has(MPB,'isCopperGolemStatueIdentity()','ParityStatefulItemBlock','return meta & 15'),
     'Copper Golem Statue item/pick/drop state preservation missing')
need(has(BRIDGE,'entry.isCopperGolemStatueIdentity()','for (int pose=0; pose<4; pose++)','for (int facing=0; facing<4; facing++)','pose*4+facing'),
     'Copper Golem Statue 4x4 model matrix missing')
need(has(BRIDGE,'addCopperGolemStandingPose','addCopperGolemSittingPose','addCopperGolemRunningPose','addCopperGolemStarPose','addGolemPartBox'),
     'Copper Golem Statue authored standing/sitting/running/star geometry missing')
need('applyCopperGolemPose' not in BRIDGE,
     'Copper Golem Statue must not fall back to generic transformed Standing geometry')

# Trial Spawner 2x6.
need(has(BRIDGE,'"trial_spawner".equals(registryName)','waiting_for_players','waiting_for_reward_ejection','ejecting_reward','ominous*6+st'),
     'Trial Spawner 12-state model matrix missing')
need('if (isTrialSpawner()) return meta >= 0 && meta < 12 ? meta : 0;' in MPB,
     'Trial Spawner metadata mapping missing')

# Vault 4x2x4 hybrid state.
need(has(MPB,'ParityVaultStateTileEntity','VaultState','modern_parity_vault_state'), 'Vault state TE missing')
need(has(BRIDGE,'"vault".equals(registryName)','st*8+ominous*4+facing','getVaultState(world,x,y,z)'),
     'Vault 32-state model selection missing')

# Crafter 12x2x2.
for token in ('down_east','down_north','down_south','down_west','east_up','north_up','south_up','up_east','up_north','up_south','up_west','west_up'):
    need(token in BRIDGE, 'Crafter/Jigsaw orientation missing '+token)
need(has(MPB,'ParityCrafterStateTileEntity','Triggered','Crafting','modern_parity_crafter_state'), 'Crafter visual-state TE missing')
need(has(BRIDGE,'"crafter".equals(registryName)','crafting*24+triggered*12+o','getCrafterVisualFlags(world,x,y,z)'),
     'Crafter 48-state model matrix missing')

# Bell 4x4 plus bounded support.
need(has(BRIDGE,'"bell".equals(registryName)','attachments={"floor","ceiling","single_wall","double_wall"}','a*4+f'),
     'Bell 16-state model preparation missing')
need(has(MPB,'bellSupportAt','bellSupported','attachment == 0','attachment == 1','attachment == 2'),
     'Bell attachment support checks missing')
need(has(MPB,'attachment == 3','remainingFacing','8 + remainingFacing'),
     'Bell double-wall to single-wall support transition missing')

# Respawn Anchor five models/light states.
need(has(BRIDGE,'"respawn_anchor".equals(registryName)','charges<=4','prepared.facingModels[charges]'),
     'Respawn Anchor five-state model preparation missing')
need(has(MPB,'final int[] light = {0, 3, 7, 11, 15}','return light[Math.min(meta, 4)]'),
     'Respawn Anchor state-sensitive light mapping missing')

# Sculk states.
need(has(BRIDGE,'"sculk_sensor".equals(registryName)','{"inactive","active","cooldown"}','prepared.facingModels[phase]'),
     'Sculk Sensor phase matrix missing')
need(has(BRIDGE,'"calibrated_sculk_sensor".equals(registryName)','phase*4+f','{"north","east","south","west"}'),
     'Calibrated Sculk Sensor 12-state matrix missing')
need(has(BRIDGE,'"sculk_shrieker".equals(registryName)','state.put("can_summon"','state.put("shrieking"','(shriek<<1)|summon'),
     'Sculk Shrieker persistent state matrix missing')

# Jigsaw and technical blocks.
need(has(BRIDGE,'"jigsaw".equals(registryName)','prepared.facingModels[o]=loadBlockStateModel'),
     'Jigsaw 12-orientation matrix missing')
need(has(BRIDGE,'"repeating_command_block".equals(registryName)','"chain_command_block".equals(registryName)','conditional*6+f'),
     'Repeating/Chain Command Block 12-state matrices missing')
need(has(BRIDGE,'"structure_block".equals(registryName)','{"save","load","corner","data"}','prepared.facingModels[mode]'),
     'Structure Block four-mode matrix missing')

# Vanilla Command Block: existing block/TE, no duplicate registry identity.
need(has(PLUGIN,'commandblockstate.MixinTileEntityCommandBlock','commandblockstate.MixinBlockCommandBlock'),
     'vanilla Command Block Pass-35 mixins are not registered')
need(has(CMD_IFACE,'etfu$getModernFacing','etfu$isConditional','etfu$setModernState'),
     'vanilla Command Block state interface incomplete')
need(has(CMD_TE,'EFRModernFacing','EFRConditional','writeToNBT','readFromNBT','onDataPacket'),
     'vanilla Command Block persistent/client-synced TE extension incomplete')
need(has(CMD_BLOCK,'@Mixin(BlockCommandBlock.class)','RenderIDs.MODERN_MAP_PARITY','onBlockPlacedBy'),
     'vanilla Command Block renderer/placement extension incomplete')
need(has(RENDER,'block == Blocks.command_block','getVanillaCommandBlockWorldModel'),
     'modern JSON renderer does not route vanilla Command Block')
need(has(BRIDGE,'prepareVanillaCommandBlock','loadBlockStateModel("command_block", state)','IModernCommandBlockState'),
     'vanilla Command Block 12-state AssetDirector model bridge missing')

# Manifest and global audit.
manifest = {e.get('name'):e for e in MANIFEST.get('blocks',[])}
for name in ['trial_spawner','vault','crafter','bell','respawn_anchor','sculk_sensor','calibrated_sculk_sensor','sculk_shrieker','jigsaw','repeating_command_block','chain_command_block','structure_block'] + statues:
    need(name in manifest and bool(manifest[name].get('meta')), 'manifest lacks Pass 35 state mapping for '+name)
for name,prop in [
 ('trial_spawner','trial_spawner_state'),('vault','vault_state'),('crafter','orientation'),('bell','attachment'),
 ('respawn_anchor','charges'),('sculk_sensor','sculk_sensor_phase'),('calibrated_sculk_sensor','facing'),
 ('sculk_shrieker','can_summon'),('jigsaw','orientation'),('command_block','conditional'),('structure_block','mode')]:
    need(name in VIS and prop in VIS, 'global visual-state classifier lacks %s.%s'%(name,prop))
need('copper_golem_pose' in VIS and 'STORED_EXACTLY' in VIS, 'global audit lacks Copper Golem Statue pose')
need('"bell": {"powered": "VISUALLY_IRRELEVANT"}' in VIS, 'Bell powered visually-irrelevant audit row missing')
need('"waterlogged": "UNSUPPORTED"' in VIS, 'Pass 35 waterlogging exception is not explicit')

# Backporter contract exact mappings.
bykey={b.get('key'):b for b in CONTRACT.get('blocks',[]) if isinstance(b,dict)}
required=['copper_golem_statue_family','trial_spawner','vault','crafter','bell','respawn_anchor','sculk_sensor','calibrated_sculk_sensor','sculk_shrieker','jigsaw','vanilla_command_block','modern_command_block_variants','structure_block']
for key in required:
    entry=bykey.get(key)
    need(entry is not None, 'Backporter contract missing '+key)
    if entry:
        need(entry.get('classification')=='FULL_IMPORT', key+' contract is not FULL_IMPORT for state mapping')
        need(bool(entry.get('source_properties')), key+' contract lacks source_properties')
        need(bool(entry.get('visual_property_classification')), key+' contract lacks visual_property_classification')
need(bykey.get('vault',{}).get('tile_entity',{}).get('id')=='etfuturum:modern_parity_vault_state', 'Vault TE contract ID mismatch')
need(bykey.get('crafter',{}).get('tile_entity',{}).get('id')=='etfuturum:modern_parity_crafter_state', 'Crafter TE contract ID mismatch')
need(bykey.get('vanilla_command_block',{}).get('target_ids')==['minecraft:command_block'], 'vanilla Command Block contract must target minecraft:command_block')

need('Pass 35 is a **map-state fidelity** pass' in DOC and 'General waterlogging remains intentionally unsupported' in DOC,
     'Pass 35 scope/deferral documentation missing')

if errors:
    print('Fidelity Pass 35 validation FAILED')
    for e in errors: print(' - '+e)
    sys.exit(1)
print('Fidelity Pass 35 validation PASSED')
print(' - Copper Golem Statues preserve 4 facings x 4 poses across all 8 identities')
print(' - Trial Spawner, Vault and Crafter preserve complete state-only model matrices')
print(' - Bell, Respawn Anchor and Sculk families preserve exact visible/persistent state')
print(' - Jigsaw, all Command Block families and Structure Block preserve technical orientation/model state')
print(' - vanilla Command Block keeps 1.7 execution metadata while a synchronized TE extension stores facing/conditional')
print(' - Backporter contract revision 35 documents every Pass-35 importer mapping')
