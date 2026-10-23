#!/usr/bin/env python3
"""Launch disposable localhost Earth/legacy servers and a GUI client; verify automatic profile switching."""
from pathlib import Path
import os
import shutil
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/profile-network-tests'


def main():
    eula = ROOT / 'run/server/eula.txt'
    if not eula.exists() or 'eula=true' not in eula.read_text():
        raise RuntimeError('Accept the development Minecraft EULA in run/server/eula.txt first.')
    OUT.mkdir(parents=True, exist_ok=True)
    processes = []
    streams = []
    try:
        for name, port, earth in [('earth', 25578, True), ('legacy', 25579, False)]:
            server = OUT / name
            server.mkdir(exist_ok=True)
            (server / 'PROFILE_STOP').unlink(missing_ok=True)
            if not (server / 'config').exists():
                shutil.copytree(ROOT / 'run/server/config', server / 'config')
            cfg = server / 'config/etfuturum'
            cfg.mkdir(parents=True, exist_ok=True)
            (cfg / 'mapcompat.cfg').write_text('"map compatibility mode" {\n B:importedEarthMode=' + str(earth).lower() + '\n B:mapCompatibilityMode=' + str(not earth).lower() + '\n}\n')
            if earth:
                (cfg/'importedbiomes.cfg').write_text('"biome ids" {\n I:cherry_grove=209\n}\n')
            shutil.copyfile(eula, server / 'eula.txt')
            (server / 'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nlevel-type=FLAT\ngenerator-settings=2;7,2x3,2;1;\nspawn-monsters=false\nspawn-animals=false\ngenerate-structures=false\nlevel-name=profile-world-remapped\nview-distance=2\nallow-flight=true\nspawn-protection=0\n')
        cfg = OUT / 'client/config/etfuturum'
        cfg.mkdir(parents=True, exist_ok=True)
        (OUT/'client/PROFILE_STOP').unlink(missing_ok=True)
        (cfg / 'mapcompat.cfg').write_text('"map compatibility mode" {\n B:importedEarthMode=false\n B:mapCompatibilityMode=false\n}\n')
        subprocess.run([str(ROOT/'gradlew'), '--offline', '-I', 'scripts/profile_network_tests.gradle', 'compileEarthImportTests'], cwd=ROOT, check=True, timeout=180, stdout=subprocess.DEVNULL)
        for name in ['earth', 'legacy']:
            stream = (OUT / (name+'.log')).open('w'); streams.append(stream)
            processes.append(subprocess.Popen([str(ROOT/'gradlew'), '--offline', '-I', 'scripts/earth_import_tests.gradle', '-PprofileNetworkServer='+name, 'runServer'], cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT))
        deadline = time.monotonic()+120
        while not all('PROFILE_NETWORK_SERVER READY' in (OUT/(name+'.log')).read_text(errors='replace') for name in ['earth','legacy']):
            if time.monotonic()>deadline or any(p.poll() is not None for p in processes):
                raise RuntimeError('Test server failed to start; inspect build/profile-network-tests/*.log')
            time.sleep(1)
        print('Local Earth and legacy servers ready.', flush=True)
        stream = (OUT/'client.log').open('w'); streams.append(stream)
        client = subprocess.Popen([str(ROOT/'gradlew'), '--offline', '-I', 'scripts/profile_network_tests.gradle', 'runClient'], cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT)
        processes.append(client)
        client.wait(timeout=240)
        output = (OUT/'client.log').read_text(errors='replace')
        if client.returncode or 'PROFILE_NETWORK_CLIENT PASSED:' not in output or 'PROFILE_NETWORK_CLIENT FAILED' in output:
            raise RuntimeError('Client login test failed; inspect build/profile-network-tests/client.log')
        print('PROFILE_NETWORK_TESTS PASSED: real TCP Earth -> legacy -> Earth, client Earth mode disabled.', flush=True)
    finally:
        for name in ['earth','legacy','client']:
            (OUT/name/'PROFILE_STOP').parent.mkdir(parents=True,exist_ok=True)
            (OUT/name/'PROFILE_STOP').touch()
        for process in processes:
            try: process.wait(timeout=20)
            except subprocess.TimeoutExpired: process.terminate()
        for stream in streams: stream.close()

if __name__ == '__main__':
    main()
