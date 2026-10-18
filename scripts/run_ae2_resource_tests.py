"""Isolated installed-AE2 contract, chest, spawn-data and compass save/restart probe."""
import argparse
import gzip
import hashlib
import os
from pathlib import Path
import shutil
import signal
import subprocess

ROOT=Path(__file__).resolve().parents[1]


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ae2-jar',type=Path,required=True)
    parser.add_argument('--gtnhlib-jar',type=Path,required=True)
    parser.add_argument('--ntm-jar',type=Path)
    parser.add_argument('--artifacts',type=Path,help='Converter payload bundle from create_ae2_native_artifacts.py')
    args=parser.parse_args();out=ROOT/'build/earth-ae2-tests';server=out/'server';server.mkdir(parents=True,exist_ok=True)
    eula=ROOT/'run/server/eula.txt'
    if not eula.is_file() or 'eula=true' not in eula.read_text():raise RuntimeError('An existing accepted Minecraft EULA is required')
    shutil.copyfile(eula,server/'eula.txt')
    if not (server/'config').exists():shutil.copytree(ROOT/'run/server/config',server/'config')
    imported=server/'config/etfuturum/mapcompat.cfg';imported.parent.mkdir(parents=True,exist_ok=True)
    imported.write_text('"map compatibility mode" {\n B:mapCompatibilityMode=false\n B:importedEarthMode=true\n}\n')
    if not (server/'server.properties').exists():
        (server/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25579\nonline-mode=false\nlevel-type=FLAT\ngenerator-settings=2;7,2x3,2;1;\nspawn-monsters=false\nspawn-animals=false\ngenerate-structures=false\nlevel-name=world\nview-distance=2\n')
    for name in ('banned-players.json','banned-ips.json','ops.json','whitelist.json'):
        if not (server/name).exists():(server/name).write_text('[]\n')
    props=['-PearthAe2Jar='+str(args.ae2_jar.resolve()),'-PearthGtnhLibJar='+str(args.gtnhlib_jar.resolve()),'-PearthAe2Only=true']
    if args.ntm_jar:props.append('-PearthNtmJar='+str(args.ntm_jar.resolve()))
    if args.artifacts:
        if not (args.artifacts/'chest.nbt.gz').is_file():raise RuntimeError('Missing converter artifact bundle')
        identity=hashlib.sha256(gzip.decompress((args.artifacts/'chest.nbt.gz').read_bytes())+(args.artifacts/'position.json').read_bytes()+(args.artifacts/'seed.txt').read_bytes()).hexdigest()
        marker=out/'artifact-identity.sha256'
        if not marker.exists() or marker.read_text()!=identity:
            (server/'ae2-artifacts-created').unlink(missing_ok=True)
            marker.write_text(identity)
        props.append('-PearthAe2Artifacts='+str(args.artifacts.resolve()))
    for phase in ('save','restart'):
        log=out/(phase+'.log')
        with log.open('w') as stream:
            process=subprocess.Popen([str(ROOT/'gradlew'),'--offline','--no-configuration-cache','-I','scripts/earth_import_tests.gradle',*props,'runServer'],cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,start_new_session=True)
            try:code=process.wait(timeout=300)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid,signal.SIGTERM)
                try:process.wait(timeout=15)
                except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait()
                raise RuntimeError('Stopped timed-out AE2 fixture: '+str(log))
        text=log.read_text(errors='replace')
        if code or 'AE2_NATIVE_RESOURCES PASSED' not in text or (args.artifacts and 'AE2_CONVERTER_ARTIFACTS PASSED' not in text):raise RuntimeError('AE2 '+phase+' failed: '+str(log))
        print('AE2 native '+phase+' passed: '+str(log),flush=True)


if __name__=='__main__':main()
