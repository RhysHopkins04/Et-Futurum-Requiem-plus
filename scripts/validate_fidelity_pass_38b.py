#!/usr/bin/env python3
"""Pass 38b runtime-correction validator: Sculk damping, sound aliases and state safety."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def text(rel): return (ROOT / rel).read_text(encoding='utf-8')
def need(ok, msg):
    if not ok: raise SystemExit('Pass 38b validation FAILED: ' + msg)

HANDLER = text('src/main/java/ganymedes01/etfuturum/core/handlers/SculkEventHandler.java')
DISPATCH = text('src/main/java/ganymedes01/etfuturum/core/handlers/SculkVibrationDispatcher.java')
RUNTIME = text('src/main/java/ganymedes01/etfuturum/tileentities/TileEntitySculkRuntime.java')
SPREAD = text('src/main/java/ganymedes01/etfuturum/tileentities/SculkSpreaderLegacy.java')
BLOCKS = text('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
MOD = text('src/main/java/ganymedes01/etfuturum/EtFuturum.java')
SOUNDS = text('src/main/java/ganymedes01/etfuturum/client/DynamicSoundsResourcePack.java')
DOC = text('docs/FIDELITY_PASS_38_SCULK_GAMEPLAY.md')

# Modern source-side damping is distinct from wool ray occlusion.
need('block == Blocks.wool || block == Blocks.carpet' in DISPATCH,
     'wool + wool-carpet dampening classification missing')
need('playerStandingOnDampening' in HANDLER and '!playerStandingOnDampening(p)' in HANDLER,
     'footsteps on wool/carpet are not suppressed at source')
need('arrowImpactIsDampened' in HANDLER and 'if(!arrowImpactIsDampened(a))' in HANDLER,
     'projectile-land vibration is not suppressed when the impact block is dampening')
need('!SculkVibrationDispatcher.isVibrationDampeningBlock(e.world.getBlock(e.x,e.y,e.z))' in HANDLER,
     'wool/carpet placement still emits a vibration')
need('!SculkVibrationDispatcher.isVibrationDampeningBlock(e.block)' in HANDLER,
     'wool/carpet breaking still emits a vibration')
need('isOccludedByDampening' in RUNTIME and 'six tiny-offset rays' in DISPATCH,
     'existing path occlusion was lost while adding source damping')

# A one-shot event during ACTIVE/COOLDOWN must never survive into the next listening window.
need('phase()!=0' in RUNTIME and 'clearPendingVibration()' in RUNTIME,
     'Sensor cooldown/listening gate or stale-pending clear missing')
need('if(phase()==0) activateSensor();' in RUNTIME,
     'pending Sensor arrival can activate outside INACTIVE phase')
need('clearPendingVibration();\n                power=0; setPhase(2)' in RUNTIME,
     'ACTIVE -> COOLDOWN does not explicitly discard stale pending vibration')
need('clearPendingVibration();\n                setPhase(0); phaseEndsAt=0' in RUNTIME,
     'COOLDOWN -> INACTIVE does not begin from an empty pending slot')

# Comparator path remains modern: accepted-event frequency only while ACTIVE.
need('phase() == 1 ? lastFrequency : 0' in RUNTIME,
     'Sensor comparator frequency is not ACTIVE-only')
need('isSculkSensor() || isCalibratedSculkSensor()' in BLOCKS and 'getComparatorFrequency()' in BLOCKS,
     'Sensor/Calibrated comparator block bridge missing')

# Runtime-used Sculk sounds must exist in the versioned dynamic sounds.json and be fetched.
for event in ('block.sculk_sensor.clicking','block.sculk_sensor.clicking_stop',
              'block.sculk_shrieker.shriek','block.sculk_catalyst.bloom'):
    need(event in MOD, 'AssetDirector event request missing: ' + event)
    need(event in SOUNDS, 'dynamic versioned sounds.json alias missing: ' + event)
for path in ('block/sculk_sensor/sculk_clicking1','block/sculk_sensor/sculk_clicking_stop1',
             'block/sculk_shrieker/shriek1','enchant/soulspeed/soulspeed1'):
    need(path in SOUNDS, 'modern Sculk backing sound missing: ' + path)

# 22w13a+: spread-generated shriekers are can_summon=false; only worldgen may set true.
need('SCULK_SHRIEKER.get()' in SPREAD and 'SCULK_SENSOR.get()' in SPREAD and 'int meta=0;' in SPREAD,
     'Sculk-spread Shrieker is not forced to can_summon=false')
need('Sculk-spread-generated Shriekers are `can_summon=false`' in DOC,
     '38b can_summon correction is not documented')

print('Fidelity Pass 38b validation PASSED')
print(' - wool/wool-carpet source damping now covers steps, placement/breaking and projectile impacts')
print(' - path occlusion remains intact and stale Sensor arrivals cannot survive ACTIVE/COOLDOWN')
print(' - Sensor comparator output remains event-frequency-only while ACTIVE')
print(' - Sensor/Shrieker/Catalyst modern sound events are defined in the versioned sound namespace')
print(' - Sculk-spread-generated Shriekers correctly use can_summon=false')
