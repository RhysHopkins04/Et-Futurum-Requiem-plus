#!/usr/bin/env python3
"""Extract template duplication facts and exact art routes from the pinned local modern client."""
from pathlib import Path
import argparse,hashlib,json,re,zipfile
ROOT=Path(__file__).resolve().parents[1];SHA1='ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd'
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--vanilla-jar',type=Path,required=True);p.add_argument('--check',action='store_true');a=p.parse_args();assert hashlib.sha1(a.vanilla_jar.read_bytes()).hexdigest()==SHA1
 names=re.findall(r'"(\w+_smithing_template)"',(ROOT/'src/main/java/ganymedes01/etfuturum/items/ItemSmithingTemplate.java').read_text());assert len(names)==19
 crosswalk=json.loads((ROOT/'docs/parity-audit/after-cartography/ITEM_CROSSWALK.json').read_text());entries=[];aliases={}
 with zipfile.ZipFile(a.vanilla_jar) as z:
  for meta,name in enumerate(names):
   definition=json.loads(z.read('data/minecraft/recipe/'+name+'.json'));center=definition['key']['C'];variants=center if isinstance(center,list) else [center]
   assert definition['pattern']==['#S#','#C#','###'] and definition['result']['count']==2
   targets=[{k:crosswalk[c][k] for k in ['item','meta']} for c in variants if c in crosswalk]
   entries.append({'name':name,'meta':meta,'centers':variants,'targets':targets});target='textures/item/'+name+'.png';assert 'assets/minecraft/'+target in z.namelist();aliases['textures/items/'+name+'.png']=target
 result={'schema':1,'vanilla_sha1':SHA1,'templates':entries};catalog={'schema':1,'aliases':aliases}
 for path,data in [('src/main/resources/assets/etfuturum/recipes/smithing_1_21_11.json',result),('src/main/resources/assets/etfuturum/asset_aliases/smithing_1_21_11.json',catalog)]:
  text=json.dumps(data,indent=2)+'\n';out=ROOT/path
  if a.check:assert out.read_text()==text,path
  else:out.write_text(text)
 print('SMITHING_REFERENCE PASSED:',len(entries),'templates;',sum(bool(r['targets']) for r in entries),'duplication centers resolved;', [r['name'] for r in entries if not r['targets']])
if __name__=='__main__':main()
