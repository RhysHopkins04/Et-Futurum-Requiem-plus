#!/usr/bin/env python3
"""Pass 35d1: prevent Vault dynamic-light recursion during tile-entity installation."""
from __future__ import print_function
import pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MPB = (ROOT / 'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
DOC = (ROOT / 'docs/FIDELITY_PASS_35_MAP_STATE.md').read_text()
errors = []

def need(ok, msg):
    if not ok:
        errors.append(msg)

def block_between(start_marker, end_marker):
    start = MPB.find(start_marker)
    end = MPB.find(end_marker, start + 1)
    return '' if start < 0 or end < 0 else MPB[start:end]

vault = block_between('public static final class ParityVaultStateTileEntity', '/** Pass 35 Crafter TE')
need(bool(vault), 'ParityVaultStateTileEntity block missing')

# Preserve Pass 35d light fidelity.
need('private static final int[] TRIAL_SPAWNER_LIGHT = {0, 4, 8, 8, 8, 0};' in MPB,
     'Trial Spawner 0/4/8/8/8/0 light table regressed')
need('private static final int[] VAULT_LIGHT = {6, 12, 12, 12};' in MPB,
     'Vault 6/12/12/12 light table regressed')
need('if (isVault()) return VAULT_LIGHT[getVaultState(world, x, y, z)];' in MPB,
     'Vault dynamic light getter regressed')

# The world-join crash was validate -> relight -> getLightValue -> getVaultState -> getTileEntity -> validate.
validate_match = re.search(r'@Override public void validate\(\) \{(.*?)\n        \}', vault, re.S)
need(validate_match is not None, 'Vault validate() override missing')
if validate_match:
    body = validate_match.group(1)
    need('relight();' not in body and 'worldObj.updateLightByType' not in body,
         'Vault validate() must never directly relight')
    need('relightPending = true;' in body,
         'Vault validate() must only arm a deferred relight')

read_match = re.search(r'@Override public void readFromNBT\(NBTTagCompound tag\) \{(.*?)\n        \}', vault, re.S)
need(read_match is not None, 'Vault readFromNBT() override missing')
if read_match:
    body = read_match.group(1)
    need('relight();' not in body and 'worldObj.updateLightByType' not in body,
         'Vault readFromNBT() must not relight while TE attachment may be incomplete')
    need('relightPending = true;' in body,
         'Vault NBT load must arm deferred relighting')

need('private boolean relightPending;' in vault,
     'Vault deferred-relight guard missing')
need('@Override public void updateEntity()' in vault and
     'if (relightPending && worldObj != null)' in vault and
     'relightPending = false;' in vault and 'relight();' in vault,
     'Vault deferred relight is not executed after TE installation')

# Real state mutation still has to relight immediately.
need('vaultState = (byte) clamped; sync();' in vault,
     'Vault setter no longer routes through sync()')
need('private void sync()' in vault and
     'worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);' in vault and
     'relightPending = false;' in vault and 'relight();' in vault,
     'Vault real state mutation no longer relights immediately')

# Client state packet arrives after TE installation, so immediate client relight is safe and desired.
packet_match = re.search(r'@Override public void onDataPacket\(NetworkManager network, S35PacketUpdateTileEntity packet\) \{(.*?)\n        \}', vault, re.S)
need(packet_match is not None, 'Vault onDataPacket() override missing')
if packet_match:
    body = packet_match.group(1)
    need('readFromNBT(packet.func_148857_g());' in body and
         'relightPending = false;' in body and 'relight();' in body,
         'Vault installed client packet path must relight after state application')

need('Direct `updateLightByType` from `TileEntity.validate()` or `readFromNBT()` is forbidden' in DOC,
     'Pass 35d1 lifecycle-safety documentation missing')

if errors:
    print('Fidelity Pass 35d1 validation FAILED')
    for error in errors:
        print(' - ' + error)
    sys.exit(1)

print('Fidelity Pass 35d1 validation PASSED')
print(' - Vault validate/readFromNBT cannot directly relight or re-enter World#getTileEntity')
print(' - Vault chunk-load relight is deferred until the TE is fully installed')
print(' - Vault setter and installed client packet paths still relight immediately')
print(' - Trial Spawner/Vault exact Pass-35d light tables remain unchanged')
