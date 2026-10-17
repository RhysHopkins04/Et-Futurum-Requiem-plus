#!/usr/bin/env python3
"""Validate exact AssetDirector targets and refusal to bundle the migrated modern textures."""
import argparse,hashlib,json,zipfile,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SHA1='ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd'
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--vanilla-jar',type=Path,required=True)
    parser.add_argument('--jar',type=Path)
    parser.add_argument('--output',type=Path)
    args=parser.parse_args();assert hashlib.sha1(args.vanilla_jar.read_bytes()).hexdigest()==SHA1
    catalog=ROOT/'src/main/resources/assets/etfuturum/asset_aliases/workstations_1_21_11.json'
    aliases=json.loads(catalog.read_text())['aliases'];assert len(aliases)==32
    report={'reference_client_sha1':SHA1,'asset_catalog_sha256':hashlib.sha256(catalog.read_bytes()).hexdigest(),'aliases':[]}
    with zipfile.ZipFile(args.vanilla_jar) as reference:
        for legacy,modern in sorted(aliases.items()):
            assert legacy.startswith('textures/') and modern.startswith('textures/') and '..' not in legacy and '..' not in modern
            target='assets/minecraft/'+modern;assert target in reference.namelist(),target
            assert not (ROOT/'src/main/resources/assets/minecraft'/legacy).exists(),'Copied modern PNG: '+legacy
            report['aliases'].append({'legacy':legacy,'modern':modern,'reference_sha256':hashlib.sha256(reference.read(target)).hexdigest()})
    gui=(ROOT/'src/main/java/ganymedes01/etfuturum/client/gui/inventory/GuiCartographyTable.java').read_text()
    direct=sorted(set(re.findall(r'new ResourceLocation\(Tags\.MC_ASSET_VER,"([^"]+)"\)',gui)));assert len(direct)==6
    report['direct_cartography_targets']=[]
    with zipfile.ZipFile(args.vanilla_jar) as reference:
        for path in direct:
            entry='assets/minecraft/'+path;assert entry in reference.namelist();assert not (ROOT/'src/main/resources'/entry).exists()
            report['direct_cartography_targets'].append({'path':path,'sha256':hashlib.sha256(reference.read(entry)).hexdigest()})
    if args.jar:
        with zipfile.ZipFile(args.jar) as release:
            for path in direct:assert 'assets/minecraft/'+path not in release.namelist(),'Copied cartography PNG in release'
            for legacy in aliases:assert 'assets/minecraft/'+legacy not in release.namelist(),'Copied PNG in release: '+legacy
            assert release.read('assets/etfuturum/asset_aliases/workstations_1_21_11.json')==catalog.read_bytes()
        report['release_jar']=str(args.jar);report['release_sha256']=hashlib.sha256(args.jar.read_bytes()).hexdigest()
    report['scope']='Exact reference assets exist, migration source/release PNGs are absent, and catalog bytes match. Runtime Java contracts separately exercise the actual bridge routing; OpenGL/client visual checks are separate.'
    if args.output:args.output.write_text(json.dumps(report,indent=2)+'\n')
    print('WORKSTATION_ASSET_REFERENCE PASSED:',len(aliases)+len(direct),'pinned targets; no migrated PNGs bundled')
if __name__=='__main__':main()
