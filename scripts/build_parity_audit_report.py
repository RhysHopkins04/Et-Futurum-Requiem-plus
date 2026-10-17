#!/usr/bin/env python3
"""Build a coverage ledger and human report from the reviewed parity audit evidence."""
from pathlib import Path
import collections
import hashlib
import json
import subprocess

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/parity-audit'
JAVA='src/main/java/ganymedes01/etfuturum/'


def dump(name,value):
    (OUT/name).write_text(json.dumps(value,indent=2,sort_keys=True)+'\n')


def evidence(path,needle):
    text=(ROOT/path).read_text();line=text[:text.index(needle)].count('\n')+1
    return {'path':path,'line':line,'needle':needle}


def main():
    runtime=json.loads((OUT/'RUNTIME_INVENTORY.json').read_text())
    recipes=json.loads((OUT/'RECIPE_AUDIT.json').read_text())
    summary=json.loads((OUT/'RECIPE_SUMMARY.json').read_text())
    reference=json.loads((OUT/'REFERENCE.json').read_text())
    crosswalk=json.loads((OUT/'ITEM_CROSSWALK.json').read_text())
    configs=json.loads((OUT/'CONFIG_MATRIX.json').read_text())
    for path,digest in reference['production_sha256'].items():
        assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest, path
    assert len(recipes)==1470 and len(runtime['blocks'])==len({b['registry'] for b in runtime['blocks']})
    observations=runtime['behaviour_observations']
    assert observations['shulker_with_unrelated_diamond'] is not None
    assert observations['smithing_preview_mutation_changes_input_nbt'] is True
    by_recipe={r['recipe']:r for r in recipes}
    findings=[]
    def finding(id,title,kind,detail,source,recipe_names=(),blocks=(),fix=''):
        findings.append({'id':id,'title':title,'classification':kind,'detail':detail,
            'evidence':source,'recipes':list(recipe_names),'blocks':list(blocks),'recommended_change':fix})

    finding('A01','Shulker recolouring accepts unrelated extra items','confirmed_runtime_defect',
        'A box plus red dye returns a recoloured box with custom data intact. Adding a diamond to a third slot still returns a result. The custom matches() does not reject unrelated non-null slots; those items can consequently be consumed by crafting.',
        [evidence(JAVA+'recipes/crafting/RecipeDyedShulkerBox.java','public boolean matches')],blocks=['shulker_box'],
        fix='Require exactly one box and one recognized dye, reject every unrelated slot, and test container contents, names, all colours and extra-input rejection.')
    finding('A02','Smithing output shares input NBT','confirmed_runtime_defect',
        'Mutating the returned smithing preview tag changes the input tag. getCraftingResult() assigns the input compound directly rather than copying it. Earlier smithing tests verified retained values, not ownership isolation.',
        [evidence(JAVA+'recipes/SmithingTableRecipes.java','stack.setTagCompound(toCopy.getTagCompound())')],blocks=['smithing_table'],
        fix='Copy the input NBT compound and add result/input mutation-isolation tests. Keep the existing damage-retention contract intact.')
    finding('A03','Lime shulker recolouring produces white','confirmed_runtime_defect',
        'All 17 mapped shulker input colours tested with lime dye produce Color=1 (white), whereas lime requires Color=6. Other 15 dye families match their tested colour witnesses. The dye-tag resolution path needs tracing before choosing the fix.',
        [evidence(JAVA+'recipes/crafting/RecipeDyedShulkerBox.java','public ItemStack getCraftingResult')],['lime_shulker_box'],['shulker_box'],
        'Resolve recognized colour-specific tags deterministically; test all colours and retain inventory/custom NBT.')
    for id,recipe,title,needle,detail in [
        ('A04','copper_trapdoor','Copper trapdoor output quantity','ModBlocks.COPPER_TRAPDOOR.newItemStack(2)','Four ingots return two trapdoors; the pinned 1.21.11 recipe returns one.'),
        ('A05','granite','Granite output quantity','newItemStack(2, 1)','Diorite plus quartz returns two granite; the pinned recipe returns one.'),
        ('A06','end_rod','End rod output quantity','ModBlocks.END_ROD.newItemStack()','Blaze rod plus popped chorus fruit returns one end rod; the pinned recipe returns four.')]:
        row=by_recipe[recipe];assert any(r['status']=='OUTPUT_QUANTITY_DIFFERS' for r in row['results'])
        finding(id,title,'confirmed_recipe_difference',detail,[evidence(JAVA+'recipes/ModRecipes.java',needle)],[recipe],[recipe],
            'Match the pinned modern quantity; add a real matching/result regression case.')
    finding('A07','Bamboo mosaic slabs craft with noncanonical metadata','confirmed_recipe_difference',
        'The six-slab recipe returns item damage 5 although this standalone slab has only one material subtype, whose canonical damage is 0. This is confirmed noncanonical output; a client rendering/placement failure is not claimed by this audit.',
        [evidence(JAVA+'recipes/ModRecipes.java','ModBlocks.BAMBOO_MOSAIC_SLAB.newItemStack(6, 5)'),
         evidence(JAVA+'ModBlocks.java','BAMBOO_MOSAIC_SLAB(')],['bamboo_mosaic_slab'],['bamboo_mosaic_slab'],
        'Craft canonical metadata 0 and test placement, stacking and merging while keeping existing saves readable.')
    finding('A08','Pale oak fails generic wood recipes','confirmed_recipe_difference',
        'Pale-oak-specific recipes exist, but ordinary plank/slab consumers reject tested pale oak substitutions. Examples include chest, barrel, crafting table and wooden tools. ModTagging registers the five mature modern wood families but omits parity Pale Oak.',
        [evidence(JAVA+'recipes/ModTagging.java','ModRecipes.modernWoodTypesEnabled.length'),
         evidence(JAVA+'recipes/ModRecipes.java','private static void registerModernWoodParityRecipes')],
        ['chest','barrel','crafting_table','wooden_pickaxe'],['pale_oak_planks','pale_oak_slab'],
        'Register appropriate wood tags under the relevant feature gates; check all existing generic consumers and charcoal inputs.')
    finding('A09','Older shaped recipes reject modern shapeless layouts','confirmed_recipe_difference',
        'Six beetroot plus a bowl in a valid modern shapeless arrangement fails, while EFR registers a fixed shaped pattern. Minecart combinations and rabbit stews also reject tested modern arrangements. This is recipe flexibility loss, not proof that these outputs are unobtainable.',
        [evidence(JAVA+'recipes/ModRecipes.java','ModItems.BEETROOT_SOUP.newItemStack()')],
        ['beetroot_soup','chest_minecart','furnace_minecart','hopper_minecart','tnt_minecart','rabbit_stew_from_brown_mushroom','rabbit_stew_from_red_mushroom'],
        fix='Reconcile shaped/shapeless definitions and test multiple arrangements plus extra-input rejection.')
    flora=['orange_dye_from_torchflower','orange_dye_from_open_eyeblossom','gray_dye_from_closed_eyeblossom',
        'cyan_dye_from_pitcher_plant','yellow_dye_from_wildflowers','pink_dye_from_cactus_flower']
    finding('A10','Implemented modern plants lack expected dye recipes','confirmed_recipe_difference',
        'The six listed modern dye recipes have mapped registered inputs and outputs, but real matching returns no result.',
        [evidence(JAVA+'recipes/ModRecipes.java','private static void registerRecipes')],flora,
        ['torchflower','open_eyeblossom','closed_eyeblossom','pitcher_plant','wildflowers','cactus_flower'],
        'Add only the missing recipes with modern quantities, proper alternatives and feature gating.')
    shells=['candle']+[c+'_candle' for c in ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']]
    shells+=['campfire','soul_campfire','calibrated_sculk_sensor','chiseled_bookshelf','copper_bars','copper_chest',
        'crafter','grindstone','lectern','respawn_anchor','scaffolding','resin_block','resin_clump','resin_brick_slab','resin_brick_stairs','resin_brick_wall','chiseled_resin_bricks']
    for name in shells:assert by_recipe[name]['status']=='RUNTIME_DIFFERENCE_REQUIRES_REVIEW', name
    finding('A11','Registered block families lack modern crafting routes','confirmed_recipe_difference',
        'Every listed recipe was queried with mapped modern ingredients and has no matching output for at least one tested witness. These are missing expected routes; alternative loot, generation or commands have not been disproved. Some blocks also require substantial gameplay work before their recipes alone make them complete.',
        [evidence(JAVA+'ModernMapParityBlocks.java','public static void init()'),evidence(JAVA+'recipes/ModRecipes.java','private static void registerRecipes')],shells,shells,
        'Implement verified missing routes in bounded families, paired with each block\'s missing mechanics; do not label a recipe-only patch full parity.')
    finding('A12','Existing families lack essential companion items','confirmed_registry_gap',
        'No dried kelp, resin brick, copper nugget, nautilus shell or heart-of-the-sea item is present in the EFR runtime item registry. This blocks the corresponding modern dried-kelp, resin, copper and conduit production chains. Companion feature work is required, not just extra crafting registrations.',
        [evidence(JAVA+'ModItems.java','public enum ModItems'),evidence(JAVA+'ModernMapParityBlocks.java','DRIED_KELP_BLOCK(')],
        ['dried_kelp_block','resin_bricks','copper_chain','copper_lantern','copper_torch','conduit'],
        ['dried_kelp_block','resin_bricks','copper_chain','copper_lantern','copper_torch','conduit'],
        'Define companion items and legitimate production/loot routes. Dried kelp also needs food, fuel/storage and all three cooking routes; do not introduce replacement NTM raw materials.')
    finding('A13','Several vanilla recipe revisions remain unported','confirmed_recipe_difference',
        'The pinned lead recipe uses five string, lodestone uses an iron ingot with chiseled stone bricks, packed ice is shapeless, and a honey bottle produces three sugar. Tested modern inputs do not match. The registered lightning rod also has no match for its three-copper recipe.',
        [evidence(JAVA+'recipes/ModRecipes.java','private static void registerRecipes')],
        ['lead','lodestone','packed_ice','sugar_from_honey_bottle','lightning_rod'],['lodestone','lightning_rod'],
        'Update recipe policy deliberately against 1.21.11; preserve container bottles and test recipe precedence with existing 1.7/mod recipes.')
    finding('A14','Modern metal-equipment recycling is missing','confirmed_recipe_difference',
        'The mapped modern iron/gold equipment recycling inputs tested through real smelting and blasting queries return no output.',
        [evidence(JAVA+'recipes/BlastFurnaceRecipes.java','public ItemStack getSmeltingResult')],
        ['gold_nugget_from_blasting','gold_nugget_from_smelting','iron_nugget_from_blasting','iron_nugget_from_smelting'],
        fix='Add the exact modern input sets and nugget yields; verify XP, damaged/enchanted inputs, consumption and optional mod overrides.')
    legacy=next(r for r in configs['cases'] if r['case']=='legacy_content_only')
    assert any('RecipeDecoratedPot' in r['class'] for r in legacy['efr_crafting_classes_present'])
    finding('A15','Decorated-pot recipe bypasses legacy recipe suppression','confirmed_profile_difference',
        'The legacy content-only profile suppresses ordinary EFR recipes and smithing but still contains RecipeDecoratedPot, registered directly by the parity initializer. Confirm the intended profile policy, then make the registration consistent.',
        [evidence(JAVA+'ModernMapParityBlocks.java','if (!decoratedPotRecipeRegistered'),evidence(JAVA+'recipes/ModRecipes.java','if (ConfigMapCompatibility.mapCompatibilityMode)')],blocks=['decorated_pot'],
        fix='Apply the chosen profile gate to this direct registration; add assertions covering all EFR recipe classes under each profile.')
    finding('A16','Older capability matrix has a stale powder-snow gap','confirmed_audit_metadata_gap',
        'The earlier capability generator describes powder snow as lacking sinking, leather-boots collision, freezing and bucket behaviour. Current source and fresh modern-state runtime/restart checks implement and exercise these. That old row must not drive a duplicate feature pass.',
        [evidence('scripts/audit_modern_map_parity_capabilities.py','"powder_snow": "No sinking')],blocks=['powder_snow'],
        fix='Refresh the capability metadata against current scoped evidence without promoting it to blanket full parity.')

    systems=[
        ('S01','Stonecutting',['stonecutter'],'BlockStonecutter.java','Modern workstation, input/output recipe selection, slab/stair yields, consumption, shift-click and registry coverage are absent; current block is explicitly decorative.'),
        ('S02','Loom',['loom'],'BlockLoom.java','Modern banner/pattern workstation is absent. Existing crafting-grid banner patterns are useful legacy behaviour, not the modern loom interface.'),
        ('S03','Cartography',['cartography_table'],'BlockCartographyTable.java','Map cloning, scaling and locking workstation is absent; block is decorative.'),
        ('S04','Grindstone',['grindstone'],None,'Orientation/models exist, but there is no repair/disenchant inventory, XP or result-slot system.'),
        ('S05','Lectern',['lectern'],None,'The registered shell lacks book/page inventory, reading GUI, page redstone pulses and comparator mechanics.'),
        ('S06','Modern smithing',['smithing_table'],'BlockSmithingTable.java','EFR implements the pre-1.20 two-input upgrade GUI. Modern templates, the third input, smithing-trim components, template consumption/duplication/loot and newer equipment are not implemented.'),
        ('S07','Crafter',['crafter'],None,'The tile entity persists model booleans only; it has no inventory, disabled-slot system or redstone crafting/ejection pipeline.'),
        ('S08','Trial spawner and vault',['trial_spawner','vault'],None,'Imported visible states are represented, but trial encounter/reward/cooldown and vault key/per-player reward state machines are not implemented.'),
        ('S09','Conduit, bell, respawn anchor and lodestone',['conduit','bell','respawn_anchor','lodestone'],None,'These shells preserve varying visual states but lack their major gameplay endpoints: conduit activation/effects/targets, bell ringing/raid interactions, anchor charging/respawn/explosion, and lodestone compass binding.'),
        ('S10','Entity-dependent lifecycle endpoints',['turtle_egg','sniffer_egg','frogspawn','creaking_heart','dried_ghast','copper_golem_statue','sculk_shrieker'],None,'Hatching/spawning/reanimation require their corresponding entities and systems. Warden/Darkness remain deferred. Stored hatch/hydration/active/pose/warning states are not proof of completed lifecycles.'),
    ]
    for id,title,blocks,cls,detail in systems:
        source=[evidence(JAVA+'blocks/'+cls,'public class')] if cls else [evidence(JAVA+'ModernMapParityBlocks.java','public enum ModernMapParityBlocks')]
        finding(id,title,'confirmed_functionality_gap',detail,source,blocks=blocks,
            fix='Implement as a separate bounded system with inventory/state, interaction, server authority, automation where applicable, persistence, client sync and configuration tests.')
    dump('CONFIRMED_FINDINGS.json',findings)

    # Every live block and disabled block enum receives a ledger row. No model,
    # recipe witness or old classification can promote a row to full functionality.
    capability=json.loads(subprocess.check_output(['python3',str(ROOT/'scripts/audit_modern_map_parity_capabilities.py'),'--format','json'],text=True))
    cap={b['block']:b for b in capability['blocks']}
    ledger=[]
    for scope,blocks in [('EFR_OWNED_OR_REPLACED',runtime['blocks']),('LEGACY_VANILLA_BASELINE',runtime['vanilla_blocks'])]:
        for b in sorted(blocks,key=lambda b:b['registry']):
            name=b['registry'].split(':')[1]
            aliases=sorted(n for n,v in crosswalk.items() if v['item']==b['item'])
            related=[r for r in recipes if r['modern_output'] in aliases]
            ids=[f['id'] for f in findings if name in f['blocks']]
            role='technical_or_no_item' if b['item']=='null' or name.startswith(('double_','lit_')) else 'item_backed_block'
            retired=name in {'modded_raw_ore_block','raw_adamantium_block','deepslate_hbm_ore'}
            ledger.append({**b,'scope':scope,'role':'retired_save_identity' if retired else role,
                'modern_item_aliases':aliases,'recipe_status_counts':dict(collections.Counter(r['status'] for r in related)),
                'registered_crafting_output_count':sum(r.get('output') is not None and r['output']['item']==b['item'] for r in runtime['crafting_recipes']) if b['item']!='null' else 0,
                'confirmed_finding_ids':ids,
                'functionality_status':'NOT_APPLICABLE_RETIRED_IDENTITY' if retired else ('CONFIRMED_PARTIAL' if any(i.startswith('S') for i in ids) else 'REQUIRES_MECHANIC_CERTIFICATION'),
                'older_capability_hint':cap.get(name,{}).get('implementation_profile'),
                'survival_status':'INTENTIONALLY_RETIRED' if retired else ('TECHNICAL_NO_STANDALONE_ITEM_EXPECTED' if role=='technical_or_no_item' else 'REQUIRES_HARVEST_LOOT_GROWTH_INTERACTION_AND_RECIPE_ROUTE_REVIEW'),
                'full_1_21_11_functionality_certified':False})
    for b in runtime['enums']:
        if 'superclass' in b and not b['enabled']:
            ledger.append({**b,'scope':'DISABLED_OR_OPTIONAL_BLOCK_ENUM','role':'conditional_content',
                'functionality_status':'NOT_LOADED_IN_THIS_PROFILE','full_1_21_11_functionality_certified':False})
    assert sum(b['scope']=='EFR_OWNED_OR_REPLACED' for b in ledger)==len(runtime['blocks'])
    dump('BLOCK_AUDIT.json',ledger)
    item_ledger=[{**i,'scope':'EFR' if i['registry'].startswith('etfuturum:') else 'LEGACY_OR_DEPENDENCY',
        'modern_aliases':sorted(n for n,v in crosswalk.items() if v['item']==i['registry']),
        'full_functionality_certified':False} for i in runtime['items']]
    item_ledger.extend({**i,'scope':'DISABLED_OR_OPTIONAL_ITEM_ENUM','full_functionality_certified':False}
        for i in runtime['enums'] if 'superclass' not in i and not i['enabled'])
    dump('ITEM_AUDIT.json',item_ledger)
    status_rows='\n'.join('| '+k+' | '+str(v)+' |' for k,v in summary['status_counts'].items())
    findings_text=[]
    for f in findings:
        links=', '.join('['+Path(e['path']).name+':'+str(e['line'])+'](../../'+e['path']+'#L'+str(e['line'])+')' for e in f['evidence'])
        names='\n\nRecipe IDs: '+', '.join('`'+r+'`' for r in f['recipes'])+'.' if f['recipes'] else ''
        findings_text.append('### '+f['id']+' — '+f['title']+'\n\n'+f['detail']+names+'\n\nEvidence: '+links+'.\n\nRecommended: '+f['recommended_change'])
    report='''# EFR+ crafting and functionality audit — Java 1.21.11

Audited source commit: `COMMIT`. Date: 16 October 2026.

**EFR+ is not fully functional against Minecraft Java 1.21.11.** This audit establishes complete registry/recipe accounting and confirms concrete gaps; it does not certify every mechanic. Gameplay source was not changed. The committed ore/import milestone remains separate from this audit.

## Reference and scope

The installed vanilla client matches Mojang's exact 1.21.11 client SHA-1, `ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd`. Recipe and item-tag comparisons read that jar directly, not a wiki or remembered historical recipes. REFERENCE.json records SHA-256, the official object URL, source commit and production-file hashes. No reference textures, models or bytecode are redistributed.

The runtime snapshot contains **623 EFR-owned/replaced blocks**, **171 other vanilla blocks**, **427 ModBlocks declarations**, **84 ModItems declarations**, **245 parity enum entries** (including the technical kelp-age helper), and **686 registered EFR items**. The other items in the snapshot belong to vanilla/dependencies. Fifty-one block enum entries are disabled/optional in this no-NTM profile; they are explicitly accounted for rather than labelled broken.

BLOCK_AUDIT.json contains every live block plus those disabled block declarations, including subtype names, item identity, class, recipe accounting and known findings. ITEM_AUDIT.json accounts for every runtime item. Grouped metadata variants and technical placement-only identities are not counted as independent modern survival items.

The main runtime profile is ordinary EFR with the copied development-server settings; no user world or normal run config was edited. PROFILE.json records settings and their hashes. The config matrix adds four feature-off cases and the legacy content-only profile. This is not an exhaustive configuration/full-modpack certification. Optional NTM processing retains its separately documented milestone tests; this audit does not impose vanilla recipes on NTM materials.

## Recipe results

Every one of the **1,470 vanilla recipe definitions** has an explicit audit row. **QUERYCOUNT real recipe witnesses** were executed against the startup registries. Ingredient tags were expanded from the reference jar and translated through an explicit item/metadata/NBT crosswalk. The harness uses the actual crafting, furnace, smoker and blast-furnace query APIs; it never registers a substitute recipe to make an expected result pass.

| Status | Recipe definitions |
| --- | ---: |
STATUSROWS

`WITNESSES_MATCH` certifies the listed witnesses' result identity, quantity and represented colour NBT only. It does not certify all combinations, consumption, returned containers, XP, GUI behaviour, automation or survival supply. Low-cardinality mapped combinations are exhaustive; larger sets use a baseline and each-slot alternative witnesses. Each row records whether its mapped combinations were exhaustive and which modern ingredients could not be represented.

`RUNTIME_DIFFERENCE_REQUIRES_REVIEW` is a candidate category, not a count of independent bugs. Repeated family failures share causes. The confirmed findings below were reconciled with current source. `OUTPUT_NOT_RESOLVED` is not automatically a missing feature: items using special NBT/legacy aliases or not implemented at all need separate review. Examples are shields, bundles, modern smithing templates and newer equipment.

The **254 stonecutting rows** identify modern outputs that exist but have no stonecutting workstation route. Further stonecutting definitions may have unresolved outputs. The nine represented netherite upgrade definitions require the modern template system; EFR's nine older two-input upgrades are functioning but do not meet that newer contract.

## Confirmed findings

FINDINGS

## Already implemented: do not redo these as missing features

- All 16 plain banner crafting colour witnesses match after resolving EFR's wool-order item metadata.
- Fifteen shulker dye families match all 17 represented input-colour witnesses; lime is the separate A03 failure. A valid recolouring preserves custom NBT, but A01 still allows unrelated extra inputs.
- Existing two-input smithing supports all nine upgrades, damage and custom data retention. Earth-mode initialization was fixed in the committed milestone; A02 is the separate NBT ownership defect. Modern templates/trims remain S06.
- Ordinary raw copper/iron/gold storage and smelting routes, and many existing stone, deepslate, wood and copper recipes have passing witnesses. Individual results are in RECIPE_AUDIT.json; this is not a blanket claim for those families.
- The current modern-state Forge suite passes real aquatic immersion/drowning, wet/dry pickle counts, water fill/drain, kelp age migration, powder-snow collision/freezing/damage/immunity/melting and fresh-process restart checks. The older powder-snow capability row is stale (A16).
- Kelp/seagrass/coral/pickle lifecycles, amethyst/dripstone states, dripleaf, copper weathering and Sculk have existing bounded implementations/contracts. A block preserving a model/state still requires its recipe and gameplay axes to be audited separately. General waterlogging and modern entity parity are not implied.
- Scaffolding has substantial placement, support and movement code; it should not be called an empty model shell. Its modern recipe still fails and its full mechanics require a dedicated equivalence review.
- Fletching tables do not need an invented player crafting GUI. Their modern villager job-site behaviour belongs to the shared villager/profession audit; it has not been certified here.

## Complex production systems and outstanding checks

| System | Current evidence | Required before full parity |
| --- | --- | --- |
| Crafting grid | Real recipe/result queries; several custom serializers | All permutations/alternatives, extra items, NBT, remainders, shift-click, conflict precedence |
| Furnace | Real recipe lookup snapshot | Every modern recipe, XP, fuel amounts, remainders, automation, save/reload |
| Blast furnace / smoker | Existing 100-tick tile implementations; real recipe queries | Explicit modern recipe coverage instead of assuming OreDictionary/food heuristics equal modern tags; full fuel/XP/hopper cycles |
| Campfire / soul campfire | Four-slot, 600-tick food-only cooking implementation, persistence and ejection code | Execute all eight modern cooking definitions through real ticks; wet/unlit timing, unloading, food vs kelp input restrictions and drops |
| Stonecutter | Decorative only | S01; all applicable modern stonecutting recipes and quantities |
| Smithing | Functional older two-input GUI, feature/profile gates | A02 and S06; templates/trims, independent copied state, all slots and consumption |
| Loom | Existing crafting-grid banner paths, decorative workstation | S02; GUI, patterns, modern limits and NBT preservation |
| Cartography | Decorative workstation; older map crafting remains | S03; scaling, cloning, locking, map data and result rules |
| Grindstone | Models/orientation | S04; repair/durability, enchantment/curses, XP, exact inventory rules |
| Lectern | Model identity | S05; books, pages, GUI, comparator/pulses and save/sync |
| Enchanting table | Custom lapis/fuel container, enchanting seed and level-cost implementation | Compare current enchanting tables/RNG/enchantment set, costs and multiplayer state; do not label absent or fully certified |
| Anvil | Custom repair/enchantment container and damage handling | Current compatibility, rename/repair costs, conflicts, caps, XP and NBT/state isolation |
| Brewing | Five-slot blaze-fuel stand, older PotionHelper-based brewing and lingering conversion | Enumerate all 1.21.11 potion transformations, splash/lingering/tipped states, modern effects, remainders, hoppers and persistence |
| Crafter | Visual-state TE | S07; automated crafting and redstone/inventory systems |
| Storage blocks / shelves / pots / bookshelves | Existing dedicated inventories and import contracts | Full slot rules, item limits, comparators, automation, break drops and restart/sync for every family |
| Composter / cauldrons / bee processing | Existing special interaction implementations | Modern item tags, probabilities, levels, bottles/buckets, automation and acquisition, independently of crafting recipes |

## Survival and compatibility closure rules

No block is declared unobtainable merely because it has no crafting output. For each family, finish the harvest/drop, Silk Touch/Fortune, growth/bonemeal, loot/trade, generation and interaction routes. Then distinguish legitimately technical/admin-only blocks, retired save identities, optional mod materials and actual survival gaps. Imported-world availability does not certify renewable survival supply.

The per-block ledger keeps full-functionality certification false unless every relevant axis is evidenced: placement/support/collision, breaking/tool/XP behaviour, interactions, redstone/comparator, fluids, inventories/NBT, automation, synchronization, persistence, entities and obtainability. Basic legacy vanilla behaviour is not assumed equal to 1.21.11 behaviour.

Five isolated configuration/profile startup cases pass. Disabling netherite leaves zero built-in smithing recipes. No unregistered crafting outputs were found in those cases. The legacy profile still leaks the decorated-pot recipe (A15). Arbitrary combinations, client GUI presence and mixed-mod recipe conflicts remain unverified.

## Recommended implementation order

1. **Crafting safety and small recipe corrections:** A01–A07. Focus on strict inputs, independent NBT, correct quantities and canonical metadata. Add regression cases before fixing each behaviour.
2. **Existing-content recipe/obtainability coverage:** A08–A15. Wood tags, shape flexibility, plant dyes, missing family recipes, cooking/recycling and companion items. Group passes by family; each must preserve configuration and mod compatibility.
3. **Functional stonecutting**, followed by **loom/cartography/grindstone/lectern** as separate systems, each with focused headless inventory/persistence tests and one small client check.
4. **Modern smithing/templates/trims and remaining production-system equivalence**, with an explicit migration/config policy for existing packs using old upgrades.
5. **Crafter and other partial gameplay systems**, then entity-dependent endpoints. Trial/vault, conduit/anchor/bell/lodestone, modern villagers and hatching/reanimation are separate projects. Warden/Darkness follows the existing roadmap, not a recipe patch.
6. **Backporter integration** consumes the resulting stable identities/states. Do not change IDs or conflate map-state fidelity with gameplay completion. Test a small converted area before a whole-world conversion.

An implementation pass has not been selected or applied by this audit. Existing ore/import gameplay is unchanged.

## Reproduction and evidence

From this repository, with the existing user-accepted `run/server/eula.txt` and cached build dependencies:

```sh
python3 scripts/run_parity_audit.py --vanilla-jar '/absolute/path/to/minecraft-1.21.11-client.jar'
python3 scripts/audit_feature_configs.py
python3 scripts/run_modern_state_tests.py
python3 scripts/build_parity_audit_report.py
```

The audit runner uses `build/parity-audit/`, bounded subprocess timeouts, marker checks (Forge can exit zero after a failed probe), the exact reference checksum and source-file guards. Normal runClient/runServer and the distributable jar do not receive the audit mod. Registry-name aliases are recorded and remain reviewable rather than hidden numerical-ID guesses. Re-running snapshots against changed source requires re-reviewing this report's confirmed assertions; it should refuse to silently keep already-fixed findings.

- `REFERENCE.json`: baseline commit/reference hashes and all production-file hashes.
- `PROFILE.json`, `CONFIG_MATRIX.json`: actual settings and bounded gating checks.
- `RUNTIME_INVENTORY.json`: startup block/item/enumeration/recipe snapshot and NBT/input-safety observations.
- `ITEM_CROSSWALK.json`: explicit modern-to-legacy identity/metadata/colour mapping.
- `RECIPE_AUDIT.json`, `RECIPE_SUMMARY.json`: all 1,470 definitions and actual witness results.
- `CONFIRMED_FINDINGS.json`: source-backed issues and implementation candidates.
- `BLOCK_AUDIT.json`, `ITEM_AUDIT.json`: full accounting, including optional/technical identities and uncertified mechanics.
- Local runtime logs: `build/parity-audit/snapshot.log`, `recipes.log`, and `config-*.log`.
- Current modern-state logs: `build/modern-state-tests/contracts-and-build.log`, `runtime-save.log`, `runtime-restart.log`.

Additional primary references: [Mojang's loom workflow](https://www.minecraft.net/en-us/article/block-week--loom), [cartography table](https://www.minecraft.net/en-us/article/block-week--cartography-table), [fletching table/job site](https://www.minecraft.net/en-us/article/block-month--fletching-table), [modern smithing/template introduction](https://www.minecraft.net/en-us/article/minecraft-snapshot-23w04a). Exact recipe claims use the pinned 1.21.11 jar; older explanatory articles are not used to override it.
'''
    report=report.replace('COMMIT',reference['source_commit']).replace('QUERYCOUNT',format(summary['query_count'],',')).replace('STATUSROWS',status_rows).replace('\nFINDINGS\n','\n'+'\n\n'.join(findings_text)+'\n')
    (OUT/'REPORT.md').write_text(report)
    dump('COVERAGE.json',{'live_efr_blocks':len(runtime['blocks']),'legacy_baseline_blocks':len(runtime['vanilla_blocks']),
        'disabled_block_declarations':sum(b['scope']=='DISABLED_OR_OPTIONAL_BLOCK_ENUM' for b in ledger),
        'block_ledger_rows':len(ledger),'all_live_blocks_accounted_for':True,'recipe_definitions_accounted_for':len(recipes),
        'confirmed_findings':len(findings),'full_gameplay_certification':False,'production_source_changed_by_audit':False})
    print('Report built:',len(ledger),'block ledger rows;',len(findings),'confirmed findings; no blanket gameplay certification')


if __name__=='__main__':main()
