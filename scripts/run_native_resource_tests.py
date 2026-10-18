"""Isolated native NTM save/restart/extraction test; never loads a user's world."""
import argparse
from pathlib import Path
import shutil
import subprocess
import os
import signal
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ntm-jar', type=Path, required=True)
    parser.add_argument('--converted-chunk',type=Path,help='Optional gzip NBT chunk produced by the Backporter, for native payload validation')
    parser.add_argument('--ae2-jar',type=Path,help='Optional combined native fixture; requires --gtnhlib-jar')
    parser.add_argument('--gtnhlib-jar',type=Path)
    parser.add_argument('--converted-dungeons',type=Path,help='Validate actual converted underground chunks and live-table actions')
    parser.add_argument('--converted-ruins',type=Path,help='Validate actual converter chunks and native ruin loot')
    parser.add_argument('--capture-structures', action='store_true', help='Run bounded native structure capture development probes instead of resource tests')
    args = parser.parse_args()
    if args.converted_dungeons and (args.capture_structures or args.converted_chunk or args.converted_ruins):
        parser.error("Converted dungeon validation is a separate mode")
    if args.converted_ruins and (args.capture_structures or args.converted_chunk):
        parser.error("Converted ruin validation is a separate test mode")
    if args.capture_structures and args.converted_chunk:
        parser.error("Structure capture and converted-resource validation are separate test modes")
    if bool(args.ae2_jar)!=bool(args.gtnhlib_jar):parser.error('AE2 and GTNHLib jars must be provided together')
    out = ROOT/('build/earth-structure-combined-tests' if args.ae2_jar else 'build/earth-resource-tests')
    server = out/'server'; server.mkdir(parents=True, exist_ok=True)
    eula = ROOT/'run/server/eula.txt'
    if not eula.is_file() or 'eula=true' not in eula.read_text():
        raise RuntimeError('Use the existing accepted Minecraft EULA first')
    shutil.copyfile(eula,server/'eula.txt')
    if not (server/'config').exists():
        config=ROOT/'build/earth-import-tests/ntm-server/config'
        shutil.copytree(config if config.exists() else ROOT/'run/server/config',server/'config')
    imported=server/'config/etfuturum/mapcompat.cfg'
    imported.parent.mkdir(parents=True,exist_ok=True)
    imported.write_text('"map compatibility mode" {\n B:mapCompatibilityMode=false\n B:importedEarthMode=true\n}\n')
    if not (server/'server.properties').exists():
        (server/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25578\nonline-mode=false\nlevel-type=FLAT\ngenerator-settings=2;7,2x3,2;1;\nspawn-monsters=false\nspawn-animals=false\ngenerate-structures=false\nlevel-name=world\nview-distance=2\n')
    # Keep combined and NTM-only fixtures independently runnable.
    if args.ae2_jar:
        properties=server/'server.properties'
        properties.write_text(properties.read_text().replace('server-port=25578\n','server-port=25580\n'))
    for name in ('banned-players.json','banned-ips.json','ops.json','whitelist.json'):
        if not (server/name).exists():(server/name).write_text('[]\n')
    if args.converted_ruins:
        manifest=args.converted_ruins/'manifest.json'
        if not manifest.is_file():raise RuntimeError('Missing converted ruin artifact manifest')
        identity=hashlib.sha256(manifest.read_bytes()).hexdigest()
        marker=out/'converted-ruins-identity.sha256'
        if not marker.exists() or marker.read_text()!=identity:
            (server/'ntm-converted-ruin-loot.nbt').unlink(missing_ok=True)
            marker.write_text(identity)
        target=server/'world/data/NBTStructures.dat'
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(args.converted_ruins/'structure-starts.nbt.gz',target)
    for phase in ('save','restart'):
        log = out/(('converted-ruins-' if args.converted_ruins else 'dungeons-' if args.converted_dungeons else 'structures-' if args.capture_structures else '')+phase+'.log')
        with log.open('w') as stream:
            extra=['-PearthConvertedChunk='+str(args.converted_chunk.resolve())] if args.converted_chunk else []
            if args.converted_dungeons:extra.append('-PearthConvertedDungeons='+str(args.converted_dungeons.resolve()))
            if args.capture_structures: extra.append('-PearthStructureCapture=true')
            if args.converted_ruins:extra.append('-PearthConvertedRuins='+str(args.converted_ruins.resolve()))
            extra.append('-PearthResourceWorkdir='+str(server))
            if args.ae2_jar:extra.extend(['-PearthAe2Jar='+str(args.ae2_jar.resolve()),'-PearthGtnhLibJar='+str(args.gtnhlib_jar.resolve())])
            process=subprocess.Popen([str(ROOT/'gradlew'),'--offline','--no-configuration-cache','-I','scripts/earth_import_tests.gradle',
                '-PearthNtmJar='+str(args.ntm_jar.resolve()),'-PearthResourcesOnly=true',*extra,'runServer'],
                cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,start_new_session=True)
            try:code=process.wait(timeout=300)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid,signal.SIGTERM)
                try:process.wait(timeout=15)
                except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait()
                raise RuntimeError('Native resource test timed out; stopped its process group: '+str(log))
        text = log.read_text(errors='replace')
        required = (['NTM_CONVERTED_DUNGEONS PASSED'] if args.converted_dungeons else ['NTM_CONVERTED_RUINS PASSED'] if args.converted_ruins else ['NTM_STRUCTURE_CAPTURE PASSED'] if args.capture_structures else
                    ['NTM_NATIVE_STRUCTURES PASSED', 'NTM_NATIVE_FLORA PASSED', 'NTM_RESOURCE_ONLY PASSED'])
        if args.capture_structures or args.converted_dungeons:
            if 'IMPORTED_STRUCTURE_ACTIONS_SAVE PASSED' not in text and 'IMPORTED_STRUCTURE_ACTIONS_RESTART PASSED' not in text:
                raise RuntimeError('Native imported structure action probe did not pass: '+str(log))
        if args.converted_ruins:
            proof=json.loads((args.converted_ruins/'manifest.json').read_text())
            if proof.get('expectedVariants'):required.append('NTM_CONVERTED_INVENTORIES PASSED')
            if proof.get('legacy'):required.append('NTM_KEYHOLE_ACTIVATION PASSED')
        if args.converted_chunk and not args.capture_structures: required.append('NTM_CONVERTED_NBT PASSED')
        if code or any(marker not in text for marker in required):
            raise RuntimeError('Native '+('converted dungeons ' if args.converted_dungeons else 'converted ruins ' if args.converted_ruins else 'structure capture ' if args.capture_structures else 'resource ')+phase+' failed: '+str(log))
        if args.capture_structures:
            capture_dir=out/'structure-captures'
            report_path=capture_dir/'report.json'
            report=json.loads(report_path.read_text())
            report['ntmJarSha256']=hashlib.sha256(args.ntm_jar.read_bytes()).hexdigest()
            report['scope']='Native capture evidence; production placement is not enabled'
            for row in report['structures']:
                artifact=capture_dir/row.get('captureFile',row['name']+'.nbt')
                if row.get('captured') and artifact.is_file():
                    row['captureSha256']=hashlib.sha256(artifact.read_bytes()).hexdigest()
            report_path.write_text(json.dumps(report,indent=2)+'\n')
        print('Native '+('converted dungeons ' if args.converted_dungeons else 'converted ruins ' if args.converted_ruins else 'structure capture ' if args.capture_structures else 'resource ')+phase+' passed: '+str(log),flush=True)


if __name__ == '__main__': main()
