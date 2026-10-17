#!/usr/bin/env python3
"""Compare an opt-in Forge registry snapshot to an exact vanilla 1.21.11 client jar.

This is a witness audit, not full gameplay certification. Never infer obtainability
from absence of a crafting recipe, or interpret a passing witness as all-tag parity.
"""
from pathlib import Path
import argparse
import collections
import hashlib
import itertools
import json
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / 'build/parity-audit/server'
OUT = ROOT / 'docs/parity-audit'
COLORS = ['white','orange','magenta','light_blue','yellow','lime','pink','gray',
          'light_gray','cyan','purple','blue','brown','green','red','black']
VANILLA_SHA1 = 'ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd'


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + '\n')


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def crosswalk(runtime, modern_names):
    """Conservative item identity crosswalk; explicit 1.7 metadata aliases outrank labels."""
    items = {i['registry']: i for i in runtime['items']}
    blocks = {b['registry']: b for b in runtime['blocks']}
    found = {}

    def add(modern, legacy, meta=0, reason='explicit legacy identity/metadata alias', priority=100):
        modern = modern if ':' in modern else 'minecraft:' + modern
        if modern not in modern_names or legacy not in items:
            return
        if modern not in found or priority > found[modern]['priority']:
            found[modern] = dict(item=legacy, meta=meta, count=1, reason=reason, priority=priority)

    # Legacy subtype names come from the actual ItemStack naming API. They are
    # evidence of an identity, never evidence that its mechanics are implemented.
    for registry, item in sorted(items.items()):
        if not registry.startswith(('minecraft:', 'etfuturum:')) or registry.startswith('etfuturum:double_'):
            continue
        if registry.startswith('etfuturum:'):
            for v in item['name_variants']:
                prefix = 'tile.etfuturum.' if v['unlocalized'].startswith('tile.') else 'item.etfuturum.'
                if not v['unlocalized'].startswith(prefix):
                    continue
                label = v['unlocalized'][len(prefix):]
                types = blocks.get(registry, {}).get('subtype_names')
                # E.g. stone meta 0 has an empty type but a clamped granite label;
                # only the actual granite slot is a canonical material identity.
                if types is not None and (v['meta'] >= len(types) or not types[v['meta']]):
                    continue
                add(label, registry, v['meta'], 'runtime EFR subtype label', 20)
        types = blocks.get(registry, {}).get('subtype_names')
        if types is None or (types and types[0] == registry.split(':')[1]):
            add(registry.split(':')[1], registry, reason='exact registry path', priority=30 if registry.startswith('minecraft:') else 25)

    wood = ['oak','spruce','birch','jungle','acacia','dark_oak']
    for i, name in enumerate(wood):
        for suffix, registry, meta in [('planks','planks',i),('sapling','sapling',i),
                ('log','log' if i<4 else 'log2',i%4),('leaves','leaves' if i<4 else 'leaves2',i%4),
                ('slab','wooden_slab',i)]:
            add(name+'_'+suffix,'minecraft:'+registry,meta)
    for i, color in enumerate(COLORS):
        for suffix, registry in [('wool','wool'),('stained_glass','stained_glass'),
                ('stained_glass_pane','stained_glass_pane'),('terracotta','stained_hardened_clay'),('carpet','carpet')]:
            add(color+'_'+suffix,'minecraft:'+registry,i)
        if color not in {'white','blue','brown','black'}:
            add(color+'_dye','minecraft:dye',15-i)
    for modern, legacy, meta in [
        ('stone_bricks','stonebrick',0),('mossy_stone_bricks','stonebrick',1),
        ('cracked_stone_bricks','stonebrick',2),('chiseled_stone_bricks','stonebrick',3),
        ('sand','sand',0),('red_sand','sand',1),('sandstone','sandstone',0),
        ('chiseled_sandstone','sandstone',1),('cut_sandstone','sandstone',2),
        ('smooth_stone_slab','stone_slab',0),('sandstone_slab','stone_slab',1),
        ('cobblestone_slab','stone_slab',3),('brick_slab','stone_slab',4),
        ('stone_brick_slab','stone_slab',5),('nether_brick_slab','stone_slab',6),
        ('quartz_slab','stone_slab',7),('cobblestone_wall','cobblestone_wall',0),
        ('mossy_cobblestone_wall','cobblestone_wall',1),('quartz_block','quartz_block',0),
        ('chiseled_quartz_block','quartz_block',1),('quartz_pillar','quartz_block',2),
        ('dirt','dirt',0),('coal','coal',0),('charcoal','coal',1),('cod','fish',0),('salmon','fish',1),
        ('tropical_fish','fish',2),('pufferfish','fish',3),('cooked_cod','cooked_fished',0),
        ('cooked_salmon','cooked_fished',1),('ink_sac','dye',0),('cocoa_beans','dye',3),
        ('lapis_lazuli','dye',4),('bone_meal','dye',15),('golden_apple','golden_apple',0),
        ('enchanted_golden_apple','golden_apple',1),('coarse_dirt','dirt',1),('podzol','dirt',2)]:
        add(modern,'minecraft:'+legacy,meta)
    for modern, legacy in {
        'bricks':'brick_block','nether_bricks':'nether_brick','nether_brick':'netherbrick',
        'oak_stairs':'oak_stairs','oak_fence':'fence','oak_fence_gate':'fence_gate',
        'oak_pressure_plate':'wooden_pressure_plate','oak_button':'wooden_button',
        'oak_sign':'sign','oak_door':'wooden_door','oak_trapdoor':'trapdoor',
        'terracotta':'hardened_clay','grass_block':'grass','lily_pad':'waterlily',
        'wheat_seeds':'seeds','sugar_cane':'reeds','melon_slice':'melon',
        'glistering_melon_slice':'speckled_melon','slime_block':'slime',
        'firework_rocket':'fireworks','firework_star':'firework_charge',
        'nether_quartz_ore':'quartz_ore','experience_bottle':'experience_bottle',
        'cooked_porkchop':'cooked_porkchop','cooked_beef':'cooked_beef',
        'snow_block':'snow','snow':'snow_layer','filled_map':'filled_map',
        'red_bed':'bed','dandelion':'yellow_flower',
        'poppy':'red_flower','carved_pumpkin':'pumpkin','jack_o_lantern':'lit_pumpkin',
    }.items():
        add(modern,'minecraft:'+legacy)
    add('short_grass','minecraft:tallgrass',1)
    add('cobblestone_stairs','minecraft:stone_stairs')
    for modern, legacy in {'bone_block':'bone','slime_block':'slime','end_stone_bricks':'end_bricks',
            'end_stone_brick_slab':'end_brick_slab','end_stone_brick_wall':'end_brick_wall',
            'end_stone_brick_stairs':'end_brick_stairs','netherite_shovel':'netherite_spade',
            'iron_chain':'chain','oak_boat':'boat','anvil':'anvil'}.items():
        add(modern,'etfuturum:'+legacy)
    add('oak_boat','minecraft:boat',priority=90)
    add('chipped_anvil','etfuturum:anvil',1)
    add('damaged_anvil','etfuturum:anvil',2)
    for name, meta in {'blue_orchid':1,'allium':2,'azure_bluet':3,'red_tulip':4,
            'orange_tulip':5,'white_tulip':6,'pink_tulip':7,'oxeye_daisy':8}.items():
        add(name,'minecraft:red_flower',meta)
    for name, meta in {'sunflower':0,'lilac':1,'rose_bush':4,'peony':5}.items():
        add(name,'minecraft:double_plant',meta)
    # Disambiguate pre-flattening names that still exist with another modern meaning.
    for modern,legacy in {'stone_slab':'stone_slab','stone_stairs':'stone_stairs',
            'coarse_dirt':'coarse_dirt'}.items():
        add(modern,'etfuturum:'+legacy,priority=110)
    add('melon','minecraft:melon_block',priority=110)
    if runtime['tileReplacementMode'] != -1:
        add('anvil','minecraft:anvil',priority=110)
    for index,color in enumerate(COLORS):
        add(color+'_banner','etfuturum:banner',index)
        add(color+'_shulker_box','etfuturum:shulker_box')
        name='minecraft:'+color+'_shulker_box'
        if name in found:found[name]['byte_tags']={'Color':index+1}
    for index,name in enumerate(['bordure_indented','creeper','field_masoned','flower','mojang','skull','globe','piglin','flow','guster']):
        add(name+'_banner_pattern','etfuturum:banner_pattern',index)
    add('creeper_head','minecraft:skull',4)
    add('wither_skeleton_skull','minecraft:skull',1)
    add('note_block','minecraft:noteblock')
    add('powered_rail','minecraft:golden_rail')
    add('magma_block','etfuturum:magma')
    # Returned direct recipe outputs may use a different valid spelling. Keep the
    # reverse map many-to-one and compare canonical item+meta, never numeric IDs.
    return {name:{k:v for k,v in value.items() if k!='priority'} for name,value in sorted(found.items())}


def prepare(jar_path):
    if hashlib.sha1(jar_path.read_bytes()).hexdigest() != VANILLA_SHA1:
        raise ValueError('Reference jar does not match the pinned official 1.21.11 client SHA-1')
    runtime = json.loads((SERVER/'runtime-inventory.json').read_text())
    with zipfile.ZipFile(jar_path) as jar:
        version = json.loads(jar.read('version.json'))
        if version['id'] != '1.21.11':
            raise ValueError('Expected exactly Minecraft Java 1.21.11')
        modern_names = {'minecraft:'+Path(n).stem for n in jar.namelist() if n.startswith('assets/minecraft/items/') and n.endswith('.json')}
        blocks = {'minecraft:'+Path(n).stem for n in jar.namelist() if n.startswith('assets/minecraft/blockstates/') and n.endswith('.json')}
        recipes = {Path(n).stem:json.loads(jar.read(n)) for n in jar.namelist() if n.startswith('data/minecraft/recipe/') and n.endswith('.json')}
        tags = {n:json.loads(jar.read(n)) for n in jar.namelist() if n.startswith('data/minecraft/tags/item/') and n.endswith('.json')}
    mapping = crosswalk(runtime, modern_names)

    def expand(ingredient, visiting=()):
        if isinstance(ingredient, list):
            return sorted({x for value in ingredient for x in expand(value,visiting)})
        if isinstance(ingredient, dict):
            ingredient = ingredient.get('item') or ('#'+ingredient['tag'] if 'tag'in ingredient else '')
        if not isinstance(ingredient,str):
            return []
        if not ingredient.startswith('#'):
            return [ingredient]
        if ingredient in visiting:
            raise ValueError('Cycle in vanilla item tags: '+ingredient)
        ns,path=ingredient[1:].split(':',1)
        tag=tags.get('data/'+ns+'/tags/item/'+path+'.json')
        if tag is None:
            return []
        return sorted({x for value in tag['values'] for x in expand(value.get('id') if isinstance(value,dict) else value,visiting+(ingredient,))})

    requests=[];rows=[]
    for name, recipe in sorted(recipes.items()):
        typ=recipe['type'].removeprefix('minecraft:')
        result=recipe.get('result'); output=result.get('id') if isinstance(result,dict) else result
        count=result.get('count',1) if isinstance(result,dict) else 1
        row={'recipe':name,'type':typ,'modern_output':output,'modern_count':count,'definition':recipe,'query_ids':[]}
        rows.append(row)
        if not output:
            row['status']='SPECIAL_SERIALIZER_REQUIRES_BEHAVIOURAL_TEST';continue
        if isinstance(result,dict) and result.get('components'):
            row['status']='COMPONENT_OUTPUT_REQUIRES_NBT_TEST';continue
        if output not in mapping:
            row['status']='OUTPUT_NOT_RESOLVED';continue
        row['expected']={k:mapping[output][k] for k in ['item','meta']};row['expected']['count']=count
        if 'byte_tags' in mapping[output]:row['expected']['byte_tags']=mapping[output]['byte_tags']
        if typ=='smithing_trim':
            row['status']='SMITHING_TRIM_REQUIRES_NBT_TEST';continue
        if typ=='campfire_cooking':
            row['status']='CAMPFIRE_REQUIRES_WORLD_TICK_TEST';continue
        if typ=='smithing_transform':
            ingredients=[recipe['template'],recipe['base'],recipe['addition']];query_type='modern_smithing'
        elif typ=='crafting_shaped':
            pattern=recipe['pattern'];ingredients=[recipe['key'][c] if c!=' ' else None for line in pattern for c in line.ljust(3)]
            ingredients += [None]*(9-len(ingredients));query_type='crafting'
        elif typ=='crafting_shapeless':
            ingredients=recipe['ingredients']+[None]*(9-len(recipe['ingredients']));query_type='crafting'
        elif typ=='crafting_transmute':
            ingredients=[recipe['input'],recipe['material']]+[None]*7;query_type='crafting'
        elif typ in {'smelting','blasting','smoking','stonecutting'}:
            ingredients=[recipe['ingredient']];query_type=typ
        else:
            row['status']='SERIALIZER_REQUIRES_BEHAVIOURAL_TEST';continue
        choices=[];missing=[];unrepresented=set()
        for ingredient in ingredients:
            if ingredient is None:
                choices.append([None]);continue
            values=expand(ingredient);available=[v for v in values if v in mapping]
            unrepresented.update(set(values)-set(available))
            if not available:missing.append(ingredient)
            choices.append(available)
        row['unrepresented_ingredient_members']=sorted(unrepresented)
        if missing:
            row['status']='INPUT_NOT_RESOLVED';row['unresolved_inputs']=missing;continue
        base=tuple(c[0] for c in choices);witnesses={base}
        for index, options in enumerate(choices):
            for option in options:
                witness=list(base);witness[index]=option;witnesses.add(tuple(witness))
        combinations=1
        for c in choices:combinations*=len(c)
        if combinations<=64:witnesses.update(itertools.product(*choices))
        row['possible_mapped_combinations']=combinations
        row['exhaustive_mapped_combinations']=len(witnesses)==combinations
        for index,witness in enumerate(sorted(witnesses,key=repr)):
            query_id=name+'@'+str(index);row['query_ids'].append(query_id)
            inputs=[{k:mapping[v][k] for k in ['item','meta','count','byte_tags'] if k in mapping[v]} if v is not None else None for v in witness]
            requests.append({'id':query_id,'type':query_type,'inputs':inputs,'modern_ingredients':witness,**({'recipe':'minecraft:'+name} if query_type=='stonecutting' else {})})
        row['status']='AWAITING_RUNTIME_QUERY'
    dump(SERVER/'recipe-requests.json',requests)
    production={str(p.relative_to(ROOT)):sha(p) for p in sorted((ROOT/'src/main').rglob('*')) if p.is_file()}
    dump(OUT/'REFERENCE.json',{'version':version['id'],'source_jar':str(jar_path.resolve()),'source_jar_sha256':sha(jar_path),
        'official_client_sha1':VANILLA_SHA1,'official_client_url':'https://piston-data.mojang.com/v1/objects/'+VANILLA_SHA1+'/client.jar',
        'source_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        'production_diff_sha256':hashlib.sha256(subprocess.check_output(['git','diff','--binary','--','src/main','build.gradle'],cwd=ROOT)).hexdigest(),
        'recipe_count':len(recipes),'modern_item_count':len(modern_names),'modern_blockstate_count':len(blocks),
        'production_sha256':production,'scope':'Recipe witnesses and source review; no blanket gameplay certification. No reference textures, models or bytecode are redistributed.'})
    dump(OUT/'ITEM_CROSSWALK.json',mapping)
    dump(OUT/'RECIPE_AUDIT.json',rows)
    dump(OUT/'RUNTIME_INVENTORY.json',runtime)
    print('Prepared',len(requests),'recipe queries;',len(mapping),'resolved modern item identities;',len(runtime['blocks']),'live EFR-owned/overridden blocks')


def finish():
    reference=json.loads((OUT/'REFERENCE.json').read_text())
    for path,digest in reference['production_sha256'].items():
        if sha(ROOT/path)!=digest:raise RuntimeError('Production source changed during audit: '+path)
    rows=json.loads((OUT/'RECIPE_AUDIT.json').read_text())
    answers={r['id']:r for r in json.loads((SERVER/'recipe-results.json').read_text())}
    for row in rows:
        if not row['query_ids']:continue
        results=[]
        for query_id in row['query_ids']:
            answer=answers[query_id];actual=answer.get('output');expected=row['expected']
            if 'error'in answer:status='QUERY_ERROR'
            elif actual is None:status='NO_MATCH_FOR_MODERN_INPUTS'
            elif actual['item']!=expected['item'] or actual['meta']!=expected['meta']:status='OUTPUT_IDENTITY_DIFFERS'
            elif actual['count']!=expected['count']:status='OUTPUT_QUANTITY_DIFFERS'
            elif any(actual.get('byte_tags',{}).get(k)!=v for k,v in expected.get('byte_tags',{}).items()):status='OUTPUT_NBT_STATE_DIFFERS'
            else:status='WITNESS_MATCH'
            results.append({'query':query_id,'status':status,'actual':actual,**({'error':answer['error']} if 'error'in answer else {})})
        row['results']=results
        counts=collections.Counter(r['status'] for r in results);row['witness_counts']=dict(counts)
        row['status']='WITNESSES_MATCH' if set(counts)=={'WITNESS_MATCH'} else 'RUNTIME_DIFFERENCE_REQUIRES_REVIEW'
    dump(OUT/'RECIPE_AUDIT.json',rows)
    counts=collections.Counter(r['status'] for r in rows)
    dump(OUT/'RECIPE_SUMMARY.json',{'recipes':len(rows),'status_counts':dict(counts),
        'query_count':sum(len(r['query_ids']) for r in rows),'query_status_counts':dict(collections.Counter(x['status'] for r in rows for x in r.get('results',[]))),
        'interpretation':'Differences require source/alias/config review before labeling defects. Matching witnesses do not certify all mechanics, alternatives, recipes, containers, XP, consumption, automation or config combinations.'})
    print(json.dumps(dict(counts),indent=2))


def main():
    global OUT
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('phase',choices=['prepare','finish'])
    parser.add_argument('--vanilla-jar',type=Path)
    parser.add_argument('--output-dir',type=Path,default=OUT)
    args=parser.parse_args()
    OUT=args.output_dir.resolve()
    if args.phase=='prepare':
        if args.vanilla_jar is None:parser.error('--vanilla-jar is required for prepare')
        prepare(args.vanilla_jar)
    else:finish()


if __name__=='__main__':main()
