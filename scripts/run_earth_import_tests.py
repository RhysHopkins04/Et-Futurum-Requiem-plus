#!/usr/bin/env python3
"""Build + bounded headless Forge save/restart checks in build/, never a user's world."""
from pathlib import Path
import argparse
import os
import signal
import json
import shutil
import subprocess
import sys
import re

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/earth-import-tests'
SERVER = OUT / 'server'
EXTRA_ARGS = []
PROFILE = "earth"

def run(name, tasks, marker):
    log = OUT / ((('ntm-' if any(a.startswith('-PearthNtmJar=') for a in EXTRA_ARGS) else '') + ('ironchests-' if any(a.startswith('-PearthIronChestsDevJar=') for a in EXTRA_ARGS) else '') + (('legacy-' if PROFILE=='legacy' else '') + name)) + '.log')
    with log.open('w') as stream:
        process = subprocess.Popen([str(ROOT/'gradlew'), '--offline', '-I', 'scripts/earth_import_tests.gradle', *EXTRA_ARGS, *tasks],
                                   cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            code = process.wait(timeout=300)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGTERM)
            try: process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL); process.wait()
            raise RuntimeError('Timed out; stopped probe process group. Inspect ' + str(log))
    text = log.read_text(errors='replace')
    if code or marker not in text or 'EARTH_IMPORT_PROBE FAILED' in text:
        raise RuntimeError('Check failed; inspect ' + str(log))
    print(name + ' passed: ' + str(log), flush=True)
    return text

def main():
    global SERVER, PROFILE
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ntm-jar', type=Path, help='Optional installed NTM JAR, deobfuscated locally by RFG for a separate probe world')
    parser.add_argument('--iron-chests-dev-jar', type=Path, help='Optional deobfuscated Iron Chests development JAR for a separate probe world')
    parser.add_argument('--profile', choices=['earth','legacy'], default='earth')
    parser.add_argument('--raw-as-ore', choices=['true','false'], help='Override only the disposable probe raw-item OreDictionary setting')
    args = parser.parse_args()
    PROFILE = args.profile
    EXTRA_ARGS.append('-PearthProfile=' + PROFILE)
    if args.ntm_jar:
        if not args.ntm_jar.is_file(): raise RuntimeError('NTM JAR not found')
        EXTRA_ARGS.append('-PearthNtmJar=' + str(args.ntm_jar.resolve()))
        SERVER = OUT / 'ntm-server'
    if args.iron_chests_dev_jar:
        if not args.iron_chests_dev_jar.is_file(): raise RuntimeError('Iron Chests dev JAR not found')
        EXTRA_ARGS.append('-PearthIronChestsDevJar=' + str(args.iron_chests_dev_jar.resolve()))
        SERVER = SERVER.with_name(('ntm-' if args.ntm_jar else '') + 'ironchests-server')
    if PROFILE=='legacy': SERVER = SERVER.with_name(('ntm-' if args.ntm_jar else '') + 'legacy-server')
    SERVER.mkdir(parents=True, exist_ok=True)
    eula = ROOT/'run/server/eula.txt'
    if not eula.exists() or 'eula=true' not in eula.read_text():
        raise RuntimeError('No existing accepted Minecraft EULA at run/server/eula.txt; accept it yourself first.')
    shutil.copyfile(eula, SERVER/'eula.txt')
    if not (SERVER/'config').exists(): shutil.copytree(ROOT/'run/server/config', SERVER/'config')
    cfg = SERVER/'config/etfuturum'
    cfg.mkdir(parents=True, exist_ok=True)
    # Only the disposable probe config is changed, never run/client or run/server config.
    (cfg/'mapcompat.cfg').write_text('"map compatibility mode" {\n B:mapCompatibilityMode=' + ('true' if PROFILE=='legacy' else 'false') + '\n B:importedEarthMode=' + ('false' if PROFILE=='legacy' else 'true') + '\n}\n')
    if args.raw_as_ore is not None:
        functions = cfg/'functions.cfg'
        text = functions.read_text()
        text, count = re.subn(r'B:registerRawItemAsOre=(?:true|false)', 'B:registerRawItemAsOre=' + args.raw_as_ore, text)
        if count != 1: raise RuntimeError('Expected one existing raw-as-ore setting in probe config')
        functions.write_text(text)
    for name in ['banned-players.json','banned-ips.json','ops.json','whitelist.json']:
        if not (SERVER/name).exists(): (SERVER/name).write_text('[]\n')
    (SERVER/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25577\nonline-mode=false\nlevel-type=FLAT\ngenerator-settings=2;7,2x3,2;1;\nspawn-monsters=false\nspawn-animals=false\ngenerate-structures=false\nlevel-name=world\nview-distance=2\n')
    run('contracts-build', ['earthImportContracts', 'build'], 'EARTH_IMPORT_CONTRACTS PASSED')
    if PROFILE=='legacy':
        legacy = run('forge-legacy', ['runServer'], 'EARTH_IMPORT_LEGACY PASSED')
        if 'EARTH_IMPORT_SMITHING PASSED' not in legacy: raise RuntimeError('Legacy smithing suppression check did not run')
        if 'CRAFTING_SWEEP_LEGACY PASSED' not in legacy: raise RuntimeError('Legacy crafting sweep check did not run')
        if 'SCHOLARLY_WORKSTATIONS_LEGACY PASSED' not in legacy: raise RuntimeError('Scholarly workstation legacy checks did not run')
        if 'LOOM_LEGACY PASSED' not in legacy: raise RuntimeError('Loom legacy check did not run')
        if 'STONECUTTER_LEGACY PASSED' not in legacy: raise RuntimeError('Stonecutter legacy check did not run')
        if 'SMITHING_CRAFTER_LEGACY PASSED' not in legacy: raise RuntimeError('Smithing/crafter legacy gate did not run')
        if 'CARTOGRAPHY_LEGACY PASSED' not in legacy: raise RuntimeError('Cartography legacy gate not tested')
        print('Legacy content-only profile retains 256 height and suppression PASSED.'); return
    saved = run('forge-save', ['runServer'], 'EARTH_IMPORT_PROBE PASSED')
    if 'EARTH_IMPORT_SMITHING PASSED: all 9 upgrades' not in saved: raise RuntimeError('Earth smithing upgrade checks did not run')
    if 'CRAFTING_SWEEP PASSED' not in saved: raise RuntimeError('Crafting sweep regression checks did not run')
    if 'MODERN_SMITHING PASSED' not in saved or 'CRAFTER PASSED' not in saved: raise RuntimeError('Modern smithing/crafter checks did not run')
    if 'CARTOGRAPHY PASSED' not in saved or 'COMPOSTER PASSED' not in saved: raise RuntimeError('Cartography/composter checks did not run')
    if 'SCHOLARLY_WORKSTATIONS PASSED' not in saved: raise RuntimeError('Scholarly workstation checks did not run')
    if 'LOOM PASSED' not in saved: raise RuntimeError('Loom extraction checks did not run')
    if 'STONECUTTER PASSED' not in saved: raise RuntimeError('Stonecutter extraction checks did not run')
    if 'PRODUCTION_CHAINS PASSED' not in saved: raise RuntimeError('Production chain checks did not run')
    if 'COOKING_AND_USAGE PASSED' not in saved: raise RuntimeError('Cooking and usage checks did not run')
    if args.iron_chests_dev_jar and 'IRON_CHESTS_SHULKERS PASSED' not in saved: raise RuntimeError('Iron Chests shulker checks did not run')
    if args.ntm_jar and 'EARTH_IMPORT_NTM_ORES PASSED' not in saved: raise RuntimeError('NTM drop/provider checks did not run')
    if args.ntm_jar and 'EARTH_IMPORT_NTM_TOOLS PASSED' not in saved: raise RuntimeError('NTM actual tool ability checks did not run')
    restarted=run('forge-restart', ['runServer'], 'EARTH_IMPORT_RESTART PASSED')
    if 'LECTERN_RESTART PASSED' not in restarted: raise RuntimeError('Lectern fresh-process book/page check did not run')
    if 'CRAFTER_RESTART PASSED' not in restarted: raise RuntimeError('Crafter persistence did not run')
    if 'CARTOGRAPHY_RESTART PASSED' not in restarted: raise RuntimeError('Saved map restart check did not run')
    report = json.loads((SERVER/'world/efr-import-capabilities.json').read_text())
    if report['height'] != 384 or len(report['surfaceBiomeIds']) != 16: raise RuntimeError('Bad capability export')
    print('Earth import build, Forge load/save, fresh-process restart and capability export PASSED.', flush=True)

if __name__ == '__main__':
    try: main()
    except Exception as exc:
        print(str(exc), file=sys.stderr); sys.exit(1)
