#!/usr/bin/env python3
"""Independent feature/profile startup smoke cases; never modify normal run configs."""
from pathlib import Path
import argparse
import json
import re
import shutil
from run_parity_audit import ROOT, SERVER, run


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output-dir',type=Path,default=ROOT/'docs/parity-audit')
    args=parser.parse_args()
    cases={
        'parity_disabled':{'enableModernMapParityBlocks':False},
        'bamboo_disabled':{'enableBambooBlocks':False},
        'netherite_disabled':{'enableNetherite':False},
        'modern_wood_disabled':{'enableCrimsonWoodFamily':False,'enableWarpedWoodFamily':False,
            'enableMangroveWoodFamily':False,'enableCherryBlocks':False},
        'legacy_content_only':{'mapCompatibilityMode':True,'importedEarthMode':False},
        'honey_disabled':{'enableHoney':False},
        'copper_disabled':{'enableCopper':False},
        'new_dyes_disabled':{'enableNewDyes':False},
        'dyed_beds_disabled':{'enableDyedBeds':False},
        'shulkers_disabled':{'enableShulkerBoxes':False},
        'stonecutter_disabled':{'enableStonecutter':False},
        'loom_disabled':{'enableLoom':False},
        'banners_disabled':{'enableBanners':False},
        'smithing_disabled':{'enableSmithingTable':False},
        'modded_armor_trim_enabled':{'allowModdedArmorTrims':True},
        'template_optional':{'requireNetheriteUpgradeTemplate':False},
        'cartography_disabled':{'enableCartographyTable':False},
        'automatic_composting_disabled':{'enableAutoAddComposter':False},
        'enchanting_disabled':{'enableNewEnchantingTable':False},
        'lectern_book_actions_enabled':{'allowLecternBookClickActions':True},
    }
    results=[]
    for name, options in cases.items():
        server=ROOT/'build/parity-audit/config-checks'/name
        server.mkdir(parents=True,exist_ok=True)
        shutil.copytree(SERVER/'config',server/'config',dirs_exist_ok=True)
        shutil.copyfile(SERVER/'eula.txt',server/'eula.txt')
        (server/'server.properties').write_text((SERVER/'server.properties').read_text().replace('25578','25579'))
        for filename in ['banned-players.json','banned-ips.json','ops.json','whitelist.json']:(server/filename).write_text('[]\n')
        changes={}
        for key,value in options.items():
            count=0
            for path in (server/'config/etfuturum').glob('*.cfg'):
                text=path.read_text()
                text,n=re.subn(r'(B:'+re.escape(key)+r'=)(?:true|false)',r'\g<1>'+str(value).lower(),text)
                if n:path.write_text(text);changes[key]=str(path.relative_to(server/'config'));count+=n
            if count!=1:raise RuntimeError('Expected exactly one existing config key '+key)
        row={'case':name,'changes':options,'config_files':changes,'log':'build/parity-audit/config-'+name+'.log'}
        try:
            run('config-'+name+'.log',server)
            data=json.loads((server/'runtime-inventory.json').read_text())
            for key in ['modern_smithing_enabled','crafter_enabled','allow_modded_armor_trims','require_upgrade_template']:row[key]=data[key]
            if name in {'smithing_disabled','legacy_content_only'} and data['modern_smithing_enabled']:raise RuntimeError('Modern smithing bypasses feature/profile gate')
            if name in {'parity_disabled','legacy_content_only'} and data['crafter_enabled']:raise RuntimeError('Crafter bypasses feature/profile gate')
            if data['allow_modded_armor_trims']!=(name=='modded_armor_trim_enabled'):raise RuntimeError('Mod armor trim default/opt-in incorrect')
            if data['require_upgrade_template']!=(name!='template_optional'):raise RuntimeError('Template requirement config incorrect')
            row['cartography_enabled']=data['cartography_enabled'];row['compost_entries']=data['compost_entries']
            if name in {'cartography_disabled','legacy_content_only'} and data['cartography_enabled']:raise RuntimeError('Cartography bypasses feature gate')
            if name=='automatic_composting_disabled' and (data['automatic_composting'] or data['compost_entries']):raise RuntimeError('Automatic composting bypasses disabled config')
            row['lectern_enabled']=data['lectern_enabled'];row['grindstone_enabled']=data['grindstone_enabled'];row['enchanting_enabled']=data['enchanting_enabled'];row['allow_lectern_book_actions']=data['allow_lectern_book_actions']
            if name in {'parity_disabled','legacy_content_only'} and (data['lectern_enabled'] or data['grindstone_enabled']):raise RuntimeError('New parity workstations bypass feature/profile gate')
            if name=='enchanting_disabled' and data['enchanting_enabled']:raise RuntimeError('Enchanting bypasses feature gate')
            if name=='lectern_book_actions_enabled' and not data['allow_lectern_book_actions']:raise RuntimeError('Book action option did not load')
            if name!='lectern_book_actions_enabled' and data['allow_lectern_book_actions']:raise RuntimeError('Book actions must default off')
            row['loom_enabled']=data['loom_enabled']
            if name in {'loom_disabled','banners_disabled','legacy_content_only'} and data['loom_enabled']:raise RuntimeError('Loom bypasses feature/profile gate')
            row['startup']='passed';row['live_efr_blocks']=len(data['blocks']);row['smithing_recipes']=data['smithing_recipes'];row['stonecutting_recipes']=data['stonecutting_recipes']
            row['disabled_enum_entries']=[e['enum'] for e in data['enums'] if not e['enabled']]
            row['unregistered_recipe_outputs']=[r for r in data['crafting_recipes'] if r.get('output') and r['output']['item']=='null']
            if name=='legacy_content_only':
                row['efr_crafting_classes_present']=[r for r in data['crafting_recipes'] if r['class'].startswith('ganymedes01.etfuturum.')]
            if name in {'stonecutter_disabled','legacy_content_only'} and data['stonecutting_recipes']!=0:
                raise RuntimeError('Stonecutting recipes bypass feature/profile gate')
            if name=='shulkers_disabled':
                row['dyed_shulker_recipes']=[r for r in data['crafting_recipes'] if r['class'].endswith('.RecipeDyedShulkerBox')]
                if row['dyed_shulker_recipes']:raise RuntimeError('Dyed shulker recipes bypass disabled shulker feature')
            if name=='netherite_disabled' and data['smithing_recipes']!=0:row['smithing_feature_gate']='FAILED'
            elif name=='netherite_disabled':row['smithing_feature_gate']='passed'
        except RuntimeError as ex:row['startup']='FAILED';row['error']=str(ex)
        results.append(row)
    out=args.output_dir/'CONFIG_MATRIX.json'
    out.write_text(json.dumps({'scope':'Twenty targeted configuration/profile startup and registry smoke cases. Not exhaustive combinations or gameplay certification.',
        'cases':results},indent=2)+'\n')
    print(json.dumps([(r['case'],r['startup']) for r in results]),flush=True)
    if any(r['startup']!='passed' or r.get('unregistered_recipe_outputs') or r.get('smithing_feature_gate')=='FAILED' for r in results):
        raise RuntimeError('One or more feature/profile cases failed; inspect '+str(out))


if __name__=='__main__':main()
