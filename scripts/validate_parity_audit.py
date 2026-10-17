#!/usr/bin/env python3
"""Validate coverage/provenance of this audit, without pretending gaps are passing gameplay tests."""
from pathlib import Path
import ast
import collections
import hashlib
import json
import argparse
import io
import subprocess
import tarfile

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/parity-audit'


def read(name):return json.loads((OUT/name).read_text())


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline',action='store_true',help='Validate pinned evidence against its Git commit, allowing subsequent production fixes')
    args=parser.parse_args()
    reference=read('REFERENCE.json')
    pinned={}
    if args.baseline:
        archive=subprocess.check_output(['git','archive',reference['source_commit'],'src/main'],cwd=ROOT)
        with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
            pinned={m.name:tar.extractfile(m).read() for m in tar if m.isfile()}
    def source(path):
        return pinned[path] if path in pinned else (ROOT/path).read_bytes()
    assert reference['version']=='1.21.11'
    assert reference['official_client_sha1']=='ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd'
    for path,digest in reference['production_sha256'].items():
        assert hashlib.sha256(source(path)).hexdigest()==digest,path
    for path in OUT.glob('*.json'):json.loads(path.read_text())
    for name in ['audit_crafting_functionality.py','run_parity_audit.py','audit_feature_configs.py','build_parity_audit_report.py','validate_parity_audit.py']:
        ast.parse((ROOT/'scripts'/name).read_text())
    runtime=read('RUNTIME_INVENTORY.json');blocks=read('BLOCK_AUDIT.json');recipes=read('RECIPE_AUDIT.json');summary=read('RECIPE_SUMMARY.json')
    assert len(recipes)==1470 and len({r['recipe'] for r in recipes})==1470
    assert dict(collections.Counter(r['status'] for r in recipes))==summary['status_counts']
    assert sum(len(r['query_ids']) for r in recipes)==summary['query_count']
    assert dict(collections.Counter(x['status'] for r in recipes for x in r.get('results',[])))==summary['query_status_counts']
    for row in recipes:
        if row['query_ids']:
            assert set(row['query_ids'])=={r['query'] for r in row['results']}
            assert 'QUERY_ERROR' not in row['witness_counts']
    actual={b['registry'] for b in runtime['blocks']}
    assert actual=={b['registry'] for b in blocks if b['scope']=='EFR_OWNED_OR_REPLACED'}
    assert {b['registry'] for b in runtime['vanilla_blocks']}=={b['registry'] for b in blocks if b['scope']=='LEGACY_VANILLA_BASELINE'}
    assert {b['enum'] for b in runtime['enums'] if 'superclass'in b and not b['enabled']}=={b['enum'] for b in blocks if b['scope']=='DISABLED_OR_OPTIONAL_BLOCK_ENUM'}
    assert not any(b['full_1_21_11_functionality_certified'] for b in blocks)
    mapping=read('ITEM_CROSSWALK.json')
    # Source-reviewed, pre-flattening aliases: do not let shared old names create false gaps.
    for modern,item,meta in [('granite','etfuturum:stone',1),('stone_stairs','etfuturum:stone_stairs',0),
            ('cobblestone_stairs','minecraft:stone_stairs',0),('stone_slab','etfuturum:stone_slab',0),
            ('smooth_stone_slab','minecraft:stone_slab',0),('melon','minecraft:melon_block',0),
            ('melon_slice','minecraft:melon',0),('white_banner','etfuturum:banner',0),('black_banner','etfuturum:banner',15)]:
        assert (mapping['minecraft:'+modern]['item'],mapping['minecraft:'+modern]['meta'])==(item,meta),modern
    for f in read('CONFIRMED_FINDINGS.json'):
        for e in f['evidence']:
            # The old capability script is outside the production manifest; read its
            # committed revision as well when validating this historical baseline.
            data=source(e['path'])
            if args.baseline and e['path'].startswith('scripts/'):
                data=subprocess.check_output(['git','show',reference['source_commit']+':'+e['path']],cwd=ROOT)
            assert e['needle'] in data.decode().splitlines()[e['line']-1],e
    configs=read('CONFIG_MATRIX.json')['cases'];assert len(configs)==5
    assert all(c['startup']=='passed' and not c['unregistered_recipe_outputs'] for c in configs)
    assert next(c for c in configs if c['case']=='netherite_disabled')['smithing_recipes']==0
    report=(OUT/'REPORT.md').read_text()
    assert 'CONFIRMED_FINDINGS.json' in report and '\nFINDINGS\n' not in report
    assert report.count('### A01 ')==1
    print('Parity audit validation PASSED: 1,470 definitions, complete live/disabled block accounting, source hashes, aliases, evidence anchors and five config cases')


if __name__=='__main__':main()
