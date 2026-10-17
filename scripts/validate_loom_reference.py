#!/usr/bin/env python3
"""Cross-check the loom's pattern sets, craftable templates and assets against pinned 1.21.11."""
import argparse,hashlib,json,re,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SHA1='ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd'
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--vanilla-jar',type=Path,required=True)
    parser.add_argument('--output',type=Path)
    args=parser.parse_args();assert hashlib.sha1(args.vanilla_jar.read_bytes()).hexdigest()==SHA1
    source=(ROOT/'src/main/java/ganymedes01/etfuturum/tileentities/TileEntityBanner.java').read_text().split('public enum EnumBannerPattern {',1)[1].split('public static final',1)[0]
    enums={match[0]:match[1:] for match in re.findall(r'([A-Z_]+)\("([^"]+)", "([^"]+)"([^;]*?)(?=\),|\);)',source)}
    free={name for name,ident,extra in enums.values() if '"' in extra and 'new ItemStack' not in extra}
    catalog=(ROOT/'src/main/java/ganymedes01/etfuturum/recipes/LoomRecipes.java').read_text()
    templates=re.findall(r'EnumBannerPattern\.([A-Z_]+)',catalog.split('TEMPLATES={',1)[1].split('};',1)[0])
    names=['bordure_indented','creeper','field_masoned','flower','mojang','skull','globe','piglin','flow','guster']
    checks=[]
    with zipfile.ZipFile(args.vanilla_jar) as jar:
        modern=set(v.split(':')[1] for v in json.loads(jar.read('data/minecraft/tags/banner_pattern/no_item_required.json'))['values'])
        assert free==modern,(free-modern,modern-free)
        for item,enum in zip(names,templates):
            expected=json.loads(jar.read('data/minecraft/tags/banner_pattern/pattern_item/'+item+'.json'))['values']
            assert expected==['minecraft:'+enums[enum][0]],(item,enum)
        assert len(templates)==10 and len(free)==32
        aliases=json.loads((ROOT/'src/main/resources/assets/etfuturum/asset_aliases/workstations_1_21_11.json').read_text())['aliases']
        # Existing legacy masks are preserved; new art must resolve through AssetDirector.
        for name in ['globe','piglin','flow','guster']:
            path='assets/minecraft/textures/entity/banner/'+name+'.png'
            assert aliases[path.split('assets/minecraft/',1)[1]]==path.split('assets/minecraft/',1)[1]
            assert not (ROOT/'src/main/resources'/path).exists()
            assert path in jar.namelist()
        for name in names:
            local=ROOT/('src/main/resources/assets/minecraft/textures/items/'+name+'_banner_pattern.png')
            assert not local.exists()
            assert aliases['textures/items/'+name+'_banner_pattern.png']=='textures/item/'+name+'_banner_pattern.png'
            assert 'assets/minecraft/'+aliases['textures/items/'+name+'_banner_pattern.png'] in jar.namelist()
        craftable=[]
        for name in names:
            recipe='data/minecraft/recipe/'+name+'_banner_pattern.json'
            if recipe in jar.namelist():craftable.append(json.loads(jar.read(recipe)))
        assert len(craftable)==6
    report={'vanilla_sha1':SHA1,'free_patterns':sorted(free),'template_patterns':dict(zip(names,[enums[e][0] for e in templates])),
        'craftable_template_reference':craftable,'scope':'Pattern membership and runtime image aliases checked against official data. Recipe execution is checked separately by the Forge audit; GUI appearance needs a client check.'}
    if args.output:args.output.write_text(json.dumps(report,indent=2)+'\n')
    print('LOOM_REFERENCE PASSED: 32 free patterns, 10 template tags, 6 reference recipes and 14 AssetDirector image aliases')
if __name__=='__main__':main()
