#!/usr/bin/env python3
"""Compare the preserved audit baseline with the crafting sweep without claiming full parity."""
from pathlib import Path
import json
import hashlib
import collections

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'docs/parity-audit'
AFTER=BASE/'after-crafting-sweep'


def read(root,name):return json.loads((root/name).read_text())


def main():
    before={r['recipe']:r for r in read(BASE,'RECIPE_AUDIT.json')}
    after={r['recipe']:r for r in read(AFTER,'RECIPE_AUDIT.json')}
    assert before.keys()==after.keys() and len(after)==1470
    reference=read(AFTER,'REFERENCE.json')
    for path,digest in reference['production_sha256'].items():
        assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,path
    regressed=[n for n in before if before[n]['status']=='WITNESSES_MATCH' and after[n]['status']!='WITNESSES_MATCH']
    fixed=[n for n in before if before[n]['status']!='WITNESSES_MATCH' and after[n]['status']=='WITNESSES_MATCH']
    assert not regressed,regressed
    previous=read(BASE,'RUNTIME_INVENTORY.json');runtime=read(AFTER,'RUNTIME_INVENTORY.json')
    assert previous['capabilities']['blockIds']==runtime['capabilities']['blockIds'],'block registry IDs changed'
    assert previous['capabilities']['itemIds']==runtime['capabilities']['itemIds'],'item registry IDs changed'
    observations=runtime['behaviour_observations']
    assert observations.get('shulker_with_unrelated_diamond') is None
    assert observations['shulker_custom_nbt_preserved'] is True
    assert observations['smithing_preview_mutation_changes_input_nbt'] is False
    remaining=[n for n,r in after.items() if r['status']=='RUNTIME_DIFFERENCE_REQUIRES_REVIEW']
    summary={'version':'3.5.17','baseline_commit':read(BASE,'REFERENCE.json')['source_commit'],
        'newly_matching_recipe_definitions':len(fixed),'newly_matching_recipes':fixed,
        'previously_matching_recipe_regressions':regressed,'remaining_runtime_difference_recipes':remaining,
        'before_status_counts':dict(collections.Counter(r['status'] for r in before.values())),
        'after_status_counts':dict(collections.Counter(r['status'] for r in after.values())),
        'block_and_item_ids_unchanged':True,'shulker_extra_input_rejected':True,
        'shulker_custom_nbt_preserved':True,'smithing_result_nbt_isolated':True,
        'scope':'Witness comparison and focused safety fixes; complex systems and all-mechanic parity remain outstanding.'}
    (AFTER/'COMPARISON.json').write_text(json.dumps(summary,indent=2)+'\n')
    print('Crafting sweep comparison PASSED:',len(fixed),'newly matching definitions; zero prior-match regressions; unchanged block/item IDs; NBT/input safety fixed')


if __name__=='__main__':main()
