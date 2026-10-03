#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
modern = (ROOT / 'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
runtime = (ROOT / 'src/main/java/ganymedes01/etfuturum/tileentities/TileEntitySculkRuntime.java').read_text()
handler = (ROOT / 'src/main/java/ganymedes01/etfuturum/core/handlers/SculkEventHandler.java').read_text()
sounds = (ROOT / 'src/main/java/ganymedes01/etfuturum/client/DynamicSoundsResourcePack.java').read_text()
main = (ROOT / 'src/main/java/ganymedes01/etfuturum/EtFuturum.java').read_text()
docs = (ROOT / 'docs/FIDELITY_PASS_38_SCULK_GAMEPLAY.md').read_text()
contract = (ROOT / 'docs/BACKPORTER_STATE_CONTRACT.json').read_text()
build = (ROOT / 'build.gradle').read_text()


def require(cond, msg):
    if not cond:
        raise AssertionError(msg)

# Modern direct-step entry point: player stepping a Shrieker reaches the TE immediately.
require('public void onEntityWalking(World world, int x, int y, int z, Entity entity)' in modern,
        'Shrieker direct-step block hook missing')
require('isSculkShrieker() && !world.isRemote && entity instanceof EntityPlayer' in modern,
        'Shrieker direct-step hook is not server/player bounded')
require('tryShriekFromPlayer((EntityPlayer) entity)' in modern,
        'direct player step does not converge on the Shrieker runtime')

# Both direct-step and vibration paths must converge on one tryShriek lifecycle.
require('public boolean tryShriekFromPlayer(EntityPlayer player)' in runtime,
        'shared Shrieker tryShriek entry point missing')
require('if(player!=null) tryShriekFromPlayer(player);' in runtime,
        'vibration-arrival path does not converge on tryShriek')
require('phaseEndsAt=worldObj.getTotalWorldTime()+90;' in runtime,
        'exact 90-tick shriek duration changed')

# can_summon warning flow: non-Peaceful, shared 200-tick cooldown, max 4, 12000 decay.
require('canSummon() && worldObj!=null && worldObj.difficultySetting!=EnumDifficulty.PEACEFUL' in runtime,
        'can_summon/non-Peaceful warning gate missing')
require('tryWarnPlayer(player,xCoord,yCoord,zCoord)' in runtime and 'if(warning<0)return false;' in runtime,
        'warning cooldown does not suppress a new summon-capable shriek')
require('public int tryWarnPlayer(EntityPlayer source,int x,int y,int z)' in handler,
        'shared warning tracker entry point missing')
require('now-warning.getLong("LastWarning")<200L' in handler,
        '200-tick shared warning cooldown missing')
require('now-lastDecay>=12000L' in handler,
        '12000-tick warning decay missing')
require('int next=Math.min(4,maxLevel+1);' in handler,
        'warning level is not capped at 4')
require('player.getDistanceSq(cx,cy,cz)<=256.0D' in handler,
        'nearby-player warning synchronization is not radius 16')

# Exact modern warning reply mapping after the 90-tick shriek.
for level, event in [
    (1, 'entity.warden.nearby_close'),
    (2, 'entity.warden.nearby_closer'),
    (3, 'entity.warden.nearby_closest'),
    (4, 'entity.warden.listening_angry'),
]:
    require(f'if(level=={level})return "{event}";' in runtime,
            f'warning level {level} sound mapping missing')
require('worldObj.rand.nextInt(21)-10' in runtime,
        'warning reply is not randomized within +/-10 blocks')
require('Tags.MC_ASSET_VER+":"+event,5.0F,1.0F' in runtime,
        'warning reply volume/pitch changed from modern 5.0/1.0')
require('respondAfterShriek();' in runtime,
        'warning response is not deferred until the shriek finishes')

# Dynamic versioned sounds.json + AssetDirector objects must cover all four events.
for event in ['entity.warden.nearby_close','entity.warden.nearby_closer','entity.warden.nearby_closest','entity.warden.listening_angry']:
    require(f'addSoundsToCategory("{event}"' in sounds,
            f'dynamic warning sound event missing: {event}')
    require(f'"{event}"' in main,
            f'AssetDirector warning event request missing: {event}')
for path in ['nearby_close_', 'nearby_closer_', 'nearby_closest_', 'listening_angry_']:
    require(f'minecraft/sounds/mob/warden/{path}' in main,
            f'warning OGG request family missing: {path}')

# Preserve explicit scope boundary: no Warden creation or fake Darkness/Blindness implementation.
require('no Warden entity' in runtime or 'no Warden entity' in docs,
        'explicit no-Warden boundary missing')
require('legacy Blindness' in runtime or 'legacy Blindness' in docs,
        'explicit no-fake-Darkness boundary missing')
require('Pass 38f Shrieker pre-Warden closeout' in docs,
        'Pass 38f documentation missing')

# Earlier P38e calibrated-Sensor closeout must remain untouched.
require('if (isCalibratedSculkSensor() && side == calibratedSculkInputPowerSide(world, x, y, z)) return 0;' in modern,
        'Pass 38e calibrated input isolation regressed')
require(any('"contract_revision": "'+rev+'"' in contract for rev in ("38", "39")), 'contract revision must remain compatible with 38/39')
require('stagingVersion = "3.5.10"' in build or "stagingVersion = '3.5.10'" in build,
        'staging version must remain 3.5.10')

print('Fidelity Pass 38f validation PASSED')
print(' - direct player stepping and vibration arrivals converge on one Shrieker tryShriek path')
print(' - can_summon warnings use radius-16 shared state, 200-tick cooldown, level 4 cap and 12000-tick decay')
print(' - 90-tick completion plays exact level 1..4 Warden reply sounds within +/-10 blocks')
print(' - warning sound aliases/backing OGG requests are present while Warden/Darkness remain deferred')
print(' - Pass 38e calibrated-input isolation, contract revision 38 and staging version 3.5.10 remain intact')
