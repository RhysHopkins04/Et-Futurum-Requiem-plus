#!/usr/bin/env python3
"""Static gate for Pass 35b Copper Golem pose fidelity and Pass-35 runtime closeout."""
from __future__ import print_function
import json, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
def read(rel): return (ROOT/rel).read_text()

MPB = read('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
BRIDGE = read('src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java')
PLUGIN = read('src/main/java/ganymedes01/etfuturum/mixinplugin/EtFuturumEarlyMixins.java')
CMD_IFACE = read('src/main/java/ganymedes01/etfuturum/core/utils/IModernCommandBlockState.java')
CMD_BLOCK = read('src/main/java/ganymedes01/etfuturum/mixins/early/commandblockstate/MixinBlockCommandBlock.java')
CMD_TE = read('src/main/java/ganymedes01/etfuturum/mixins/early/commandblockstate/MixinTileEntityCommandBlock.java')
DOC = read('docs/FIDELITY_PASS_35_MAP_STATE.md')
CONTRACT = json.loads(read('docs/BACKPORTER_STATE_CONTRACT.json'))

errors=[]
def need(ok,msg):
    if not ok: errors.append(msg)
def has(text,*parts): return all(p in text for p in parts)

# Copper Golem Statue: exact authored pose definitions, no generic Standing transform fallback.
need(has(BRIDGE,'addCopperGolemStandingPose','addCopperGolemSittingPose','addCopperGolemRunningPose','addCopperGolemStarPose'),
     'four authored Copper Golem pose methods are required')
need('applyCopperGolemPose' not in BRIDGE and 'rotateQuadAround(q,0.33' not in BRIDGE,
     'Sitting/Running/Star must not be generic transformed Standing geometry')
need(has(BRIDGE,'addGolemPartBox','GolemPartPose','golemToBlockPixels','64.0D, 64.0D'),
     'Copper Golem hierarchical cuboid/64x64 UV flattener missing')
# Pose-specific sentinels are deliberately values that do not occur in Standing.
need(has(BRIDGE,'addCopperGolemSittingPose','-4.525D','3, 19','-3.1416D','-8.325D','-1.5708D'),
     'Sitting pose does not contain the authored 1.21.11 geometry/UV sentinels')
need(has(BRIDGE,'addCopperGolemRunningPose','-1.064D','0.1204D','-0.0064D','1.0036D','-0.8715D','0.7854D'),
     'Running pose does not contain the authored 1.21.11 part transforms')
need(has(BRIDGE,'addCopperGolemStarPose','1.9199D','-1.9199D','0.2618D','-0.2618D'),
     'Star pose does not contain the authored 1.21.11 limb transforms')
need(has(BRIDGE,'addCopperGolemStandingPose','0.015D','-0.015D','56, 0','37, 8','37, 0'),
     'Standing entity-sheet/deformation definition regressed')
need(has(BRIDGE,'if (name.contains("oxidized"))','oxidized_copper_golem','if (name.contains("weathered"))','weathered_copper_golem',
         'if (name.contains("exposed"))','exposed_copper_golem','copper_golem/copper_golem'),
     'Copper Golem weathering texture selection regressed')
statues=[
 'COPPER_GOLEM_STATUE','WAXED_COPPER_GOLEM_STATUE','EXPOSED_COPPER_GOLEM_STATUE','WAXED_EXPOSED_COPPER_GOLEM_STATUE',
 'WEATHERED_COPPER_GOLEM_STATUE','WAXED_WEATHERED_COPPER_GOLEM_STATUE','OXIDIZED_COPPER_GOLEM_STATUE','WAXED_OXIDIZED_COPPER_GOLEM_STATUE']
for name in statues: need(name in MPB, 'missing Copper Golem identity '+name)
need(has(BRIDGE,'for (int pose=0; pose<4; pose++)','for (int facing=0; facing<4; facing++)','pose*4+facing','rotateModelY(statue,facing*90)'),
     'Copper Golem pose*4+facing model matrix regressed')
need(has(MPB,'if (isCopperGolemStatue()) return meta & 15','int pose = (meta >> 2) & 3','(pose << 2)','damageDropped(int meta)','getDamageValue(World world'),
     'Copper Golem placement/pick/drop metadata preservation regressed')

# Bell closeout: 4 facing x 4 attachment source-state matrix, bounds and bounded support transitions.
need(has(BRIDGE,'"bell".equals(registryName)','{"north","east","south","west"}','{"floor","ceiling","single_wall","double_wall"}','a*4+f'),
     'Bell 4x4 model matrix regressed')
need(has(MPB,'bellSupportAt','bellSupported','attachment == 0','attachment == 1','attachment == 2','attachment == 3',
         'primary && opposite','12 + facing','remainingFacing','8 + remainingFacing'),
     'Bell reciprocal wall-support transition contract regressed')
need(has(MPB,'setBlockBoundsBasedOnState','isBell()','2.0F/16.0F','14.0F/16.0F'),
     'Bell attachment/facing selection bounds regressed')

# Command Block closeout: vanilla block stays vanilla-owned; repeating/chain retain 6x2 metadata matrices.
need(has(BRIDGE,'"repeating_command_block".equals(registryName)','"chain_command_block".equals(registryName)',
         '{"down","up","north","south","west","east"}','conditional*6+f'),
     'repeating/chain Command Block 12-state matrices regressed')
need(has(CMD_IFACE,'IModernCommandBlockState','etfu$getModernFacing','etfu$isConditional','etfu$setModernState'),
     'vanilla Command Block compatibility interface missing modern facing/conditional state')
need(has(CMD_BLOCK,'MixinBlockCommandBlock') and has(CMD_TE,'MixinTileEntityCommandBlock'),
     'vanilla Command Block minimal compatibility mixins missing')
need('commandblockstate' in PLUGIN.lower(), 'Command Block compatibility mixins are not registered')
need(has(MPB,'if (isModernCommandVariant())','return facing','conditional=false','importer may set 6..11 directly'),
     'modern command variant placement metadata contract regressed')

# Intentional shared visuals: persistent state must remain represented without fake model variants.
need(has(BRIDGE,'"trial_spawner".equals(registryName)','{"inactive","waiting_for_players","active","waiting_for_reward_ejection","ejecting_reward","cooldown"}','ominous*6+st'),
     'Trial Spawner 12 persistent states regressed')
need(has(BRIDGE,'"crafter".equals(registryName)','crafting*24+triggered*12+o'),
     'Crafter 48 persistent combinations regressed')
need(has(BRIDGE,'"sculk_sensor".equals(registryName)','{"inactive","active","cooldown"}','prepared.facingModels[phase]'),
     'Sculk Sensor three persistent phases regressed')
need(has(BRIDGE,'"sculk_shrieker".equals(registryName)','state.put("can_summon"','state.put("shrieking"','(shriek<<1)|summon'),
     'Sculk Shrieker shrieking persistence regressed')
need(has(DOC,'inactive` shares with `cooldown`','waiting_for_players`, `active`, and `waiting_for_reward_ejection` share',
         '`crafter_crafting_triggered` model inherits `crafter_crafting`','active and cooldown intentionally reuse the active',
         'does not select a different model for it','must not manufacture fake visual variants'),
     'Pass 35 documentation no longer distinguishes persistent state from intentional static-model sharing')

# Backporter contract mapping stays deterministic and revision stays Pass 35 because encoding did not change.
need(str(CONTRACT.get('contract_revision')) == '35', 'Pass 35b must not bump contract revision when encoding is unchanged')
bykey={b.get('key'):b for b in CONTRACT.get('blocks',[]) if isinstance(b,dict)}
copper=bykey.get('copper_golem_statue_family',{})
need(copper.get('metadata',{}).get('layout')=='meta=pose_index*4+facing_index', 'Copper Golem metadata layout changed')
need(copper.get('metadata',{}).get('pose_index')=={'standing':0,'sitting':1,'running':2,'star':3}, 'Copper Golem pose index mapping changed')
for key in ('trial_spawner','crafter','sculk_sensor','sculk_shrieker'):
    notes=' '.join(bykey.get(key,{}).get('notes',[])).lower()
    need(('share' in notes or 'reuse' in notes or 'distinct static' in notes or 'both intentionally resolve the active model' in notes), key+' contract does not document intentional model sharing')

if errors:
    print('Fidelity Pass 35b validation FAILED')
    for e in errors: print(' - '+e)
    sys.exit(1)
print('Fidelity Pass 35b validation PASSED')
print(' - Copper Golem Sitting/Running/Star use authored pose-specific geometry and 64x64 UVs')
print(' - all 8 statue identities retain pose*4+facing metadata and weathering texture selection')
print(' - Bell remains a complete 4x4 attachment/facing matrix with bounded support handling')
print(' - vanilla/repeating/chain Command Blocks retain facing+conditional compatibility state')
print(' - persistent state remains distinct from intentional Mojang static-model sharing')
