#!/usr/bin/env python3
from pathlib import Path
import json,re,sys
ROOT=Path(__file__).resolve().parents[1]
ERR=[]
def need(cond,msg):
    if not cond: ERR.append(msg)
def text(rel): return (ROOT/rel).read_text(errors='replace')

DISP=text('src/main/java/ganymedes01/etfuturum/core/handlers/SculkVibrationDispatcher.java')
RUN=text('src/main/java/ganymedes01/etfuturum/tileentities/TileEntitySculkRuntime.java')
HAND=text('src/main/java/ganymedes01/etfuturum/core/handlers/SculkEventHandler.java')
CAT=text('src/main/java/ganymedes01/etfuturum/tileentities/TileEntitySculkCatalyst.java')
SPREAD=text('src/main/java/ganymedes01/etfuturum/tileentities/SculkSpreaderLegacy.java')
PAR=text('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
BUILD=text('build.gradle')
CON=json.loads(text('docs/BACKPORTER_STATE_CONTRACT.json'))
AUD=text('scripts/audit_modern_map_parity_capabilities.py')

need('enum Event' in DISP and 'SCULK_SENSOR_TENDRILS_CLICKING' in DISP,'one shared vibration/event bridge must exist')
expected={1:['STEP','SWIM','FLAP'],2:['PROJECTILE_LAND','HIT_GROUND','SPLASH'],3:['ITEM_INTERACT_FINISH','PROJECTILE_SHOOT','INSTRUMENT_PLAY'],4:['ENTITY_ACTION','ELYTRA_GLIDE','UNEQUIP'],5:['ENTITY_DISMOUNT','EQUIP'],6:['ENTITY_MOUNT','ENTITY_INTERACT','SHEAR'],7:['ENTITY_DAMAGE'],8:['DRINK','EAT'],9:['CONTAINER_CLOSE','BLOCK_CLOSE','BLOCK_DEACTIVATE','BLOCK_DETACH'],10:['CONTAINER_OPEN','BLOCK_OPEN','BLOCK_ACTIVATE','BLOCK_ATTACH','PRIME_FUSE','NOTE_BLOCK_PLAY'],11:['BLOCK_CHANGE'],12:['BLOCK_DESTROY','FLUID_PICKUP'],13:['BLOCK_PLACE','FLUID_PLACE'],14:['ENTITY_PLACE','LIGHTNING_STRIKE','TELEPORT'],15:['ENTITY_DIE','EXPLODE']}
for freq,names in expected.items():
    for n in names: need(re.search(r'\b%s\(%d\)'%(n,freq),DISP) is not None,'frequency mapping missing %s=%d'%(n,freq))
need('int radius=calibrated()?16:8' in RUN and 'Math.floor(dist)' in RUN,'sensor ranges/travel delay missing')
need('isOccludedByDampening' in DISP and 'for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS)' in DISP and 'Blocks.wool' in DISP,'six-ray wool dampening path missing')
need('phaseEndsAt=worldObj.getTotalWorldTime()+(calibrated()?10:30)' in RUN and 'setPhase(2); phaseEndsAt=now+10' in RUN,'Sensor ACTIVE/COOLDOWN lifecycle missing')
need('redstoneStrength' in RUN and 'getPower()' in PAR and 'canProvidePower' in PAR,'Sensor redstone path missing')
need('calibrationInput()' in RUN and 'filter!=0 && filter!=v.event.frequency' in RUN and 'int f=facing()' in RUN,'Calibrated facing/input filter missing')
need('SCULK_SENSOR_TENDRILS_CLICKING' in RUN and 'canSummon()' in RUN and 'phaseEndsAt=worldObj.getTotalWorldTime()+90' in RUN,'Shrieker trigger/timing/can_summon path missing')
need('WARN_TAG="EFRSculkWarning"' in HAND and '12000L' in HAND and '200L' in HAND and 'Math.min(4' in HAND,'per-player warning persistence/cooldown missing')
need('no Warden entity exists' in RUN and 'no spawn/retry is attempted' in RUN,'explicit Warden-spawn boundary missing')
need('LivingDeathEvent' in HAND and 'findNearbyCatalyst' in HAND and 'registerCatalyst' in HAND,'bounded Catalyst death hook missing')
need('bloomEndsAt=worldObj.getTotalWorldTime()+8L' in CAT and 'setBlockMetadataWithNotify' in CAT,'Catalyst bloom timing/state missing')
need('TODO' not in CAT and 'bloomWithExperience' in CAT,'bloomWithExperience must be implemented')
need('MAX_CHARGE=1000' in SPREAD and 'MAX_CURSORS=32' in SPREAD and 'List<Cursor>' in SPREAD,'charge/spreader cursor algorithm missing')
need('chunkExists' in SPREAD and 'getChunkFrom' not in SPREAD,'spread must avoid unloaded-chunk force loads')
need('getTileEntity(x,y,z)' in SPREAD and 'te!=null' in SPREAD,'spread must avoid arbitrary TileEntity destruction')
need('getFaceMask()|mask' in SPREAD and 'setFaceMask' in SPREAD,'Sculk Vein FaceMask merge missing')
need('SCULK_SHRIEKER.get()' in SPREAD and 'SCULK_SENSOR.get()' in SPREAD and 'int meta=0;' in SPREAD,'special Sensor/Shrieker growth missing or spread-generated can_summon=false state wrong')
need('ParityMultifaceTileEntity' in PAR and 'isSculkVein()' in PAR,'Pass-30 FaceMask implementation missing')
need('meta=phase_index*4+facing_index' in text('docs/BACKPORTER_STATE_CONTRACT.json') and 'bit0=can_summon; bit1=shrieking' in text('docs/BACKPORTER_STATE_CONTRACT.json'),'Pass-35 Sculk visible-state encodings regressed')
need(str(CON.get('contract_revision')) in ('38','39'),'contract revision must be 38 or forward-compatible 39')
need(CON.get('implemented_in_version')=='3.5.10' and CON.get('implemented_in_git_ref') is None and CON.get('release_status')=='unreleased','3.5.10 unreleased provenance invalid')
version=re.search(r'String stagingVersion = "(\d+)\.(\d+)\.(\d+)"',BUILD)
need(version and tuple(map(int,version.groups())) >= (3,5,10),'stagingVersion predates Pass 38')
need('PASS_38_SCULK_GAMEPLAY' in AUD and 'PASS_38_VERIFIED' in AUD,'capability audit not promoted to Pass 38')
# Warden boundary: documentation/comments are fine; introducing an EntityWarden implementation is not.
alljava='\n'.join(p.read_text(errors='replace') for p in (ROOT/'src/main/java').rglob('*.java'))
need('class EntityWarden' not in alljava and 'new EntityWarden' not in alljava,'general Warden implementation accidentally introduced')

if ERR:
    print('Fidelity Pass 38 validation FAILED')
    for e in ERR: print(' - '+e)
    sys.exit(1)
print('Fidelity Pass 38 validation PASSED')
print(' - shared bounded vibration bridge and exact 1..15 frequency table are present')
print(' - Sensor/Calibrated lifecycle, redstone/comparator, travel and wool occlusion are gated')
print(' - Shrieker warning lifecycle reaches an explicit no-Warden boundary')
print(' - Catalyst bloom/death XP and bounded persisted Sculk charge spreading are implemented')
print(' - Pass-30 FaceMask and Pass-35 visible-state encodings remain authoritative')
print(' - contract revision 38/39 / unreleased 3.5.10 provenance is consistent')
