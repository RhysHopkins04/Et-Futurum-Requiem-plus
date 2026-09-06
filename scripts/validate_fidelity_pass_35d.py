#!/usr/bin/env python3
"""Static gate for Pass 35d state-dependent emitted-light fidelity."""
from __future__ import print_function
import pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
def read(rel): return (ROOT / rel).read_text()

MPB = read('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
BRIDGE = read('src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java')
DOC = read('docs/FIDELITY_PASS_35_MAP_STATE.md')

errors=[]
def need(ok,msg):
    if not ok: errors.append(msg)
def has(text,*parts): return all(p in text for p in parts)

def method_body(name, next_marker):
    start = MPB.find(name)
    end = MPB.find(next_marker, start + 1)
    return '' if start < 0 or end < 0 else MPB[start:end]

# Exact Minecraft Java 1.21.11 light tables.
need('private static final int[] TRIAL_SPAWNER_LIGHT = {0, 4, 8, 8, 8, 0};' in MPB,
     'Trial Spawner exact 1.21.11 light table must be 0/4/8/8/8/0')
need('private static final int[] VAULT_LIGHT = {6, 12, 12, 12};' in MPB,
     'Vault exact 1.21.11 light table must be 6/12/12/12')
need(has(MPB, 'if (isTrialSpawner()) return TRIAL_SPAWNER_LIGHT[meta % 6];',
         'if (isVault()) return VAULT_LIGHT[getVaultState(world, x, y, z)];'),
     'state-dependent Trial Spawner/Vault getLightValue routing missing')

# Static registration must not pin Trial Spawner/Vault to their old constructor light values.
dynamic = method_body('private boolean usesDynamicLight()', 'private Block createBlock()')
need('"trial_spawner".equals(name)' in dynamic and '"vault".equals(name)' in dynamic,
     'Trial Spawner/Vault are not routed through the dynamic-light registration path')
need('if (entry.lightLevel > 0 && !entry.usesDynamicLight()) entry.block.setLightLevel' in MPB,
     'dynamic-light static-registration bypass regressed')

# 1.7.10 needs an explicit block-light recalculation after TE/state mutations; rendering alone is insufficient.
need(has(MPB, 'if (isTrialSpawner() || isVault()) world.updateLightByType(EnumSkyBlock.Block, x, y, z);'),
     'placement/import block insertion does not explicitly relight Trial Spawner/Vault')
need(has(MPB, 'public void setVaultState(int state)', 'vaultState = (byte) clamped; sync();',
         'private boolean relightPending;', 'private void relight()',
         'worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord)',
         'private void sync()', '@Override public void updateEntity()',
         '@Override public void readFromNBT(NBTTagCompound tag)',
         'vaultState = (byte) Math.max(0, Math.min(3, tag.getByte("VaultState")));',
         '@Override public void onDataPacket'),
     'Vault state setter/load/sync relight coverage is incomplete')
# Lifecycle safety: direct relight during validate/readFromNBT re-enters getTileEntity while TE installation is in progress.
vault_te = method_body('public static final class ParityVaultStateTileEntity', '/** Pass 35 Crafter TE')
validate_match = re.search(r'@Override public void validate\(\) \{(.*?)\n        \}', vault_te, re.S)
read_match = re.search(r'@Override public void readFromNBT\(NBTTagCompound tag\) \{(.*?)\n        \}', vault_te, re.S)
need(validate_match is not None and 'relight();' not in validate_match.group(1) and 'worldObj.updateLightByType' not in validate_match.group(1),
     'Vault validate() must never relight directly (world-join recursion hazard)')
need(read_match is not None and 'relight();' not in read_match.group(1) and 'worldObj.updateLightByType' not in read_match.group(1),
     'Vault readFromNBT() must defer relighting until TE installation is complete')
need(has(vault_te, 'relightPending = true;', '@Override public void updateEntity()',
         'if (relightPending && worldObj != null)', 'relightPending = false;', 'relight();'),
     'Vault load/validation relight must be deferred to a safe post-install TE tick')

# Preserve the audited 1.21.11 Sculk Sensor behavior: emitted block light is CONSTANT 1.
# Only emissiveRendering is ACTIVE-phase-specific in modern Minecraft; 0/1/0 would be a fidelity regression.
need(re.search(r'SCULK_SENSOR\("1\.19"[^\n]*, 1\),', MPB) is not None,
     'Sculk Sensor constant emitted light level 1 regressed')
need(re.search(r'CALIBRATED_SCULK_SENSOR\("1\.20"[^\n]*, 1\),', MPB) is not None,
     'Calibrated Sculk Sensor inherited constant emitted light level 1 regressed')
need('"sculk_sensor".equals(name)' not in dynamic and '"calibrated_sculk_sensor".equals(name)' not in dynamic,
     'Sculk Sensor families must not be converted to a false phase-dependent block-light path')
light_body = method_body('public int getLightValue(IBlockAccess world, int x, int y, int z)', 'private int computeScaffoldingMeta')
need('isSculkSensor()' not in light_body and 'isCalibratedSculkSensor()' not in light_body,
     'Sculk Sensor emitted light must remain constant 1 rather than phase-dependent')

# No Pass-35 model/state matrix regressions.
need(has(BRIDGE, '"trial_spawner".equals(registryName)', 'ominous*6+st',
         '"vault".equals(registryName)', 'st*8+ominous*4+facing'),
     'Trial Spawner/Vault Pass-35 model matrices regressed')
need(has(BRIDGE, '"sculk_sensor".equals(registryName)', '{"inactive","active","cooldown"}',
         '"calibrated_sculk_sensor".equals(registryName)', 'phase*4+f'),
     'Sculk Sensor Pass-35 phase/facing matrices regressed')
need('meta = ominous * 6 + trial_spawner_state' in DOC and 'inactive=0, waiting_for_players=4, active=8' in DOC,
     'Pass 35d Trial Spawner light documentation missing')
need('inactive=6 and active/unlocking/ejecting=12' in DOC,
     'Pass 35d Vault light documentation missing')
need('block light level is **1 in every phase**' in DOC and 'constant emitted block light level 1' in DOC,
     'Pass 35d Sculk Sensor constant-light fidelity documentation missing')

if errors:
    print('Fidelity Pass 35d validation FAILED')
    for e in errors: print(' - ' + e)
    sys.exit(1)
print('Fidelity Pass 35d validation PASSED')
print(' - Trial Spawner emitted light follows exact 1.21.11 state values 0/4/8/8/8/0')
print(' - Vault emitted light follows exact 1.21.11 state values 6/12/12/12')
print(' - Vault setter/client sync relight immediately; load/validation relight is deferred safely past TE installation')
print(' - Sculk Sensor and Calibrated Sculk Sensor correctly retain constant emitted light level 1')
print(' - Pass-35 persistent state/model matrices remain intact')
