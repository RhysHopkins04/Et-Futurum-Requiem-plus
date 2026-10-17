#!/usr/bin/env python3
"""Run an isolated headless registry/recipe audit against a supplied vanilla jar."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import signal
import subprocess

ROOT=Path(__file__).resolve().parents[1]
SERVER=ROOT/'build/parity-audit/server'


def run(name, server=SERVER):
    for filename in ['runtime-inventory.json','recipe-results.json']:
        (server/filename).unlink(missing_ok=True)
    path=ROOT/'build/parity-audit'/name
    with path.open('w') as stream:
        process=subprocess.Popen([str(ROOT/'gradlew'),'--offline','-I','scripts/parity_audit.gradle','-PparityAuditDir='+str(server),'runServer'],
            cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,start_new_session=True)
        try:code=process.wait(timeout=300)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid,signal.SIGTERM)
            try:process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid,signal.SIGKILL);process.wait()
            raise RuntimeError('Audit timed out; inspect '+str(path))
    text=path.read_text(errors='replace')
    if code or 'EFR_PARITY_AUDIT PASSED' not in text or 'EFR_PARITY_AUDIT FAILED' in text:
        raise RuntimeError('Audit failed; inspect '+str(path))
    print(name+' passed',flush=True)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--vanilla-jar',type=Path,required=True)
    parser.add_argument('--output-dir',type=Path,default=ROOT/'docs/parity-audit')
    args=parser.parse_args()
    if not args.vanilla_jar.is_file():raise RuntimeError('Vanilla jar does not exist')
    eula=ROOT/'run/server/eula.txt'
    if not eula.is_file() or 'eula=true' not in eula.read_text():
        raise RuntimeError('Requires the existing user-accepted run/server/eula.txt')
    SERVER.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(eula,SERVER/'eula.txt')
    if not (SERVER/'config').exists():shutil.copytree(ROOT/'run/server/config',SERVER/'config')
    config=SERVER/'config/etfuturum';config.mkdir(parents=True,exist_ok=True)
    (config/'mapcompat.cfg').write_text('"map compatibility mode" {\n B:mapCompatibilityMode=false\n B:importedEarthMode=false\n}\n')
    (SERVER/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25578\nonline-mode=false\nlevel-type=FLAT\ngenerator-settings=2;7,2x3,2;1;\nspawn-monsters=false\nspawn-animals=false\ngenerate-structures=false\nlevel-name=world\nview-distance=2\n')
    for name in ['banned-players.json','banned-ips.json','ops.json','whitelist.json']:
        if not (SERVER/name).exists():(SERVER/name).write_text('[]\n')
    (SERVER/'recipe-requests.json').unlink(missing_ok=True)
    run('snapshot.log')
    script=ROOT/'scripts/audit_crafting_functionality.py'
    subprocess.run(['python3',str(script),'prepare','--vanilla-jar',str(args.vanilla_jar.resolve()),'--output-dir',str(args.output_dir.resolve())],cwd=ROOT,check=True)
    run('recipes.log')
    subprocess.run(['python3',str(script),'finish','--output-dir',str(args.output_dir.resolve())],cwd=ROOT,check=True)
    hashes={str(p.relative_to(SERVER/'config')):hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted((SERVER/'config').rglob('*')) if p.is_file()}
    (args.output_dir/'PROFILE.json').write_text(json.dumps({'profile':'ordinary EFR, copied development server settings; not a full-pack/config matrix',
        'config_sha256':hashes,'etfuturum_config':{p.name:p.read_text() for p in sorted(config.glob('*.cfg'))}},indent=2)+'\n')
    print('Audit complete: '+str(args.output_dir)+'. Differences are review candidates, not automatic defect verdicts.',flush=True)


if __name__=='__main__':main()
