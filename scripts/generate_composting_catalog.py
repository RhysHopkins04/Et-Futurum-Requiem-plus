#!/usr/bin/env python3
"""Read pinned vanilla compost probabilities from local javap output; resolve only audited identities."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess
ROOT=Path(__file__).resolve().parents[1]
SHA1='ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd'
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--vanilla-jar',type=Path,required=True);p.add_argument('--mappings',type=Path,required=True);p.add_argument('--javap',default='javap');p.add_argument('--check',action='store_true');args=p.parse_args()
 assert hashlib.sha1(args.vanilla_jar.read_bytes()).hexdigest()==SHA1
 mappings=args.mappings.read_text();items=mappings.split('net.minecraft.world.item.Items -> ',1)[1].split('\nnet.',1)[0];obf=items.split(':',1)[0]
 names={m.group(2):m.group(1).lower() for m in re.finditer(r'    net.minecraft.world.item.Item (\w+) -> (\w+)',items)}
 composter=mappings.split('net.minecraft.world.level.block.ComposterBlock -> ',1)[1].split(':',1)[0]
 code=subprocess.check_output([args.javap,'-p','-c','-classpath',str(args.vanilla_jar),composter],text=True)
 crosswalk=json.loads((ROOT/'docs/parity-audit/after-scholarly-workstations/ITEM_CROSSWALK.json').read_text())
 # These metadata identities are explicit 1.7 equivalents absent from the general recipe crosswalk.
 for modern,item_id,meta in [('dry_short_grass','etfuturum:short_dry_grass',0),('dry_tall_grass','etfuturum:tall_dry_grass',0),('tall_grass','minecraft:double_plant',2),('fern','minecraft:tallgrass',2),('large_fern','minecraft:double_plant',3),('mushroom_stem','minecraft:brown_mushroom_block',10)]:
  crosswalk['minecraft:'+modern]={'item':item_id,'meta':meta}
 rows=[];chance=None;item=None
 for line in code.splitlines():
  match=re.search(r'// float ([\d.]+)f',line)
  if match:chance=round(float(match[1])*100)
  if re.search(r'\bfconst_1\b',line):chance=100
  match=re.search(r'// Field '+re.escape(obf)+r'\.(\w+):',line)
  if match:item='minecraft:'+names[match[1]]
  if '// Method a:(FLdwn;)V' in line:
   assert item and chance in (30,50,65,85,100),(line,item,chance)
   target=crosswalk.get(item);row={'modern':item,'percent':chance}
   if target:row['target']={k:target[k] for k in ('item','meta')}
   rows.append(row);item=None
 assert len(rows)==115 and len({r['modern'] for r in rows})==115
 result={'schema':1,'vanilla_sha1':SHA1,'entries':sorted(rows,key=lambda r:r['modern'])};text=json.dumps(result,indent=2)+'\n';out=ROOT/'src/main/resources/assets/etfuturum/recipes/composting_1_21_11.json'
 if args.check:assert out.read_text()==text,'Composting catalog differs from pinned reference'
 else:out.write_text(text)
 print('COMPOSTING_REFERENCE PASSED:',len(rows),'modern identities;',sum('target' in r for r in rows),'audited targets;', [r['modern'] for r in rows if 'target' not in r])
if __name__=='__main__':main()
