#!/usr/bin/env python3
"""Verify the cooking/usage follow-up against the preserved 3.5.17 witness snapshot."""
from pathlib import Path
import collections
import hashlib
import json

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'docs/parity-audit/after-crafting-sweep'
AFTER=ROOT/'docs/parity-audit/after-cooking-usage'
def read(folder,name):return json.loads((folder/name).read_text())

def main():
    before={r['recipe']:r for r in read(BASE,'RECIPE_AUDIT.json')}
    after={r['recipe']:r for r in read(AFTER,'RECIPE_AUDIT.json')}
    assert before.keys()==after.keys() and len(after)==1470
    for path,digest in read(AFTER,'REFERENCE.json')['production_sha256'].items():
        assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,path
    regressed=[n for n in before if before[n]['status']=='WITNESSES_MATCH' and after[n]['status']!='WITNESSES_MATCH']
    assert not regressed,regressed
    old=read(BASE,'RUNTIME_INVENTORY.json');new=read(AFTER,'RUNTIME_INVENTORY.json')
    for kind in ['blockIds','itemIds']:
        prior=old['capabilities'][kind];current=new['capabilities'][kind]
        assert all(current.get(name)==number for name,number in prior.items()),kind
        assert set(current)-set(prior)==({'etfuturum:copper_nugget'} if kind=='itemIds' else set()),kind
    obs=new['behaviour_observations']
    assert obs.get('shulker_with_unrelated_diamond') is None
    assert obs['shulker_custom_nbt_preserved'] and not obs['smithing_preview_mutation_changes_input_nbt']
    fixed=[n for n in before if before[n]['status']!='WITNESSES_MATCH' and after[n]['status']=='WITNESSES_MATCH']
    summary={'version':'3.5.18','comparison_baseline':'after-crafting-sweep (3.5.17)',
        'newly_matching_recipe_definitions':fixed,'previously_matching_regressions':regressed,
        'before_status_counts':dict(collections.Counter(r['status'] for r in before.values())),
        'after_status_counts':dict(collections.Counter(r['status'] for r in after.values())),
        'existing_block_and_item_numeric_ids_unchanged':True,'new_item':'etfuturum:copper_nugget',
        'scope':'Recipe witnesses and separate focused runtime contracts; not complete modern gameplay parity.'}
    (AFTER/'COMPARISON.json').write_text(json.dumps(summary,indent=2)+'\n')
    print('COOKING_USAGE_COMPARISON PASSED:',len(fixed),'additional matching definitions; zero witness regressions; existing IDs retained')
if __name__=='__main__':main()
