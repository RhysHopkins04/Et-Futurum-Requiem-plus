#!/usr/bin/env python3
"""Build a registry-name catalog from the pinned vanilla recipes and reviewed item crosswalk."""
import argparse,hashlib,json,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SHA1='ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd'
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--vanilla-jar',type=Path,required=True)
    args=parser.parse_args()
    assert hashlib.sha1(args.vanilla_jar.read_bytes()).hexdigest()==SHA1,'Unexpected vanilla jar'
    crosswalk=ROOT/'docs/parity-audit/after-production-chains/ITEM_CROSSWALK.json'
    mapping=json.loads(crosswalk.read_text());recipes=[]
    with zipfile.ZipFile(args.vanilla_jar) as jar:
        for name in sorted(jar.namelist()):
            if not name.startswith('data/minecraft/recipe/') or not name.endswith('.json'):continue
            recipe=json.loads(jar.read(name))
            if recipe['type']!='minecraft:stonecutting':continue
            source=recipe['ingredient'];target=recipe['result']['id']
            assert isinstance(source,str) and source in mapping and target in mapping,(name,source,target)
            assert not mapping[source].get('byte_tags') and not mapping[target].get('byte_tags'),name
            recipes.append({'id':'minecraft:'+Path(name).stem,'modern_input':source,'modern_output':target,
                'input':{k:mapping[source][k] for k in ['item','meta']},
                'output':dict({k:mapping[target][k] for k in ['item','meta']},count=recipe['result'].get('count',1))})
    assert len(recipes)==254,len(recipes)
    result={'schema':1,'vanilla_sha1':SHA1,'crosswalk_sha256':hashlib.sha256(crosswalk.read_bytes()).hexdigest(),'recipes':recipes}
    output=ROOT/'src/main/resources/assets/etfuturum/recipes/stonecutting_1_21_11.json';output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(result,indent=2)+'\n');print('Generated',len(recipes),'pinned stonecutting recipes')
if __name__=='__main__':main()
