#!/usr/bin/env python3
"""Verify every smithing/crafter art input exists in the pinned jar; no newly copied PNGs."""
from pathlib import Path
import argparse,hashlib,json,zipfile
ROOT=Path(__file__).resolve().parents[1];SHA1='ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd'
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--vanilla-jar',type=Path,required=True);p.add_argument('--jar',type=Path);p.add_argument('--output',type=Path);a=p.parse_args();assert hashlib.sha1(a.vanilla_jar.read_bytes()).hexdigest()==SHA1
 aliases=json.loads((ROOT/'src/main/resources/assets/etfuturum/asset_aliases/smithing_1_21_11.json').read_text())['aliases'];assert len(aliases)==19
 templates=json.loads((ROOT/'src/main/resources/assets/etfuturum/recipes/smithing_1_21_11.json').read_text())['templates']
 paths=set(aliases.values())|{'textures/gui/container/smithing.png','textures/gui/container/crafter.png','textures/gui/sprites/container/smithing/error.png','textures/gui/sprites/container/slot/smithing_template_netherite_upgrade.png','textures/gui/sprites/container/slot/smithing_template_armor_trim.png'}
 paths.update('textures/gui/sprites/container/crafter/'+name+'.png' for name in ['disabled_slot','powered_redstone','unpowered_redstone'])
 paths.update('textures/trims/entity/'+layer+'/'+row['name'].replace('_armor_trim_smithing_template','')+'.png' for row in templates if row['meta']>0 for layer in ['humanoid','humanoid_leggings'])
 paths.update('textures/trims/items/'+slot+'_trim.png' for slot in ['helmet','chestplate','leggings','boots'])
 palettes=['trim_palette','quartz','iron','netherite','redstone','copper','gold','emerald','diamond','lapis','amethyst','resin','iron_darker','gold_darker','diamond_darker','netherite_darker','copper_darker'];paths.update('textures/trims/color_palettes/'+name+'.png' for name in palettes)
 with zipfile.ZipFile(a.vanilla_jar) as reference:
  hashes={path:hashlib.sha256(reference.read('assets/minecraft/'+path)).hexdigest() for path in sorted(paths)}
 for path in set(paths)|set(aliases):
  assert not (ROOT/'src/main/resources/assets/minecraft'/path).exists(),'Copied art: '+path
 report={'reference_sha1':SHA1,'asset_sha256':hashes,'template_aliases':19,'new_copied_pngs':0,'scope':'Pinned art inputs and no new copied PNGs. Java contracts exercise 640 palette permutations and all 19 bridge aliases; OpenGL appearance needs a brief client check.'}
 if a.jar:
  with zipfile.ZipFile(a.jar) as release:
   for path in set(paths)|set(aliases):
    assert 'assets/minecraft/'+path not in release.namelist(),'Copied release art: '+path
   assert not any(n.startswith(('efrtest/','efraudit/','efrtweak/')) for n in release.namelist())
  report.update(release_jar=str(a.jar),release_sha256=hashlib.sha256(a.jar.read_bytes()).hexdigest())
 if a.output:a.output.write_text(json.dumps(report,indent=2)+'\n')
 print('SMITHING_CRAFTER_ASSETS PASSED:',len(paths),'pinned art inputs; no new copied PNGs')
if __name__=='__main__':main()
