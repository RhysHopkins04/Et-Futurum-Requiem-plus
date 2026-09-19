#!/usr/bin/env python3
"""Static gate for Pass 37 vegetation / plant lifecycle functional parity."""
from __future__ import print_function
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]

def text(rel):
    return (ROOT / rel).read_text()

MPB = text('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
ITEMS = text('src/main/java/ganymedes01/etfuturum/ModernVegetationItems.java')
SAPLING = text('src/main/java/ganymedes01/etfuturum/blocks/BlockModernSapling.java')
LEAVES = text('src/main/java/ganymedes01/etfuturum/blocks/BlockModernLeaves.java')
MANGROVE_GEN = text('src/main/java/ganymedes01/etfuturum/world/generate/decorate/WorldGenPass37MangroveTree.java')
PALE_GEN = text('src/main/java/ganymedes01/etfuturum/world/generate/decorate/WorldGenPass37PaleOakTree.java')
ETF = text('src/main/java/ganymedes01/etfuturum/EtFuturum.java')
SOUNDS = text('src/main/java/ganymedes01/etfuturum/client/DynamicSoundsResourcePack.java')
AUDIT = text('scripts/audit_modern_map_parity_capabilities.py')
MATRIX = text('docs/MODERN_MAP_PARITY_CAPABILITY_MATRIX.md')
DOC = text('docs/FIDELITY_PASS_37_VEGETATION_LIFECYCLE.md')
BUILD = text('build.gradle')
CONTRACT = json.loads(text('docs/BACKPORTER_STATE_CONTRACT.json'))
ENUS = text('src/main/resources/assets/etfuturum/lang/en_US.lang')
ENGB = text('src/main/resources/assets/etfuturum/lang/en_GB.lang')

errors = []
def need(ok, msg):
    if not ok:
        errors.append(msg)
def has(src, *parts):
    return all(p in src for p in parts)

# Revision / unreleased provenance.
need(str(CONTRACT.get('contract_revision')) == '37', 'Backporter contract revision must be 37')
need(CONTRACT.get('implemented_in_version') == '3.5.9', 'Pass 37 contract must target unreleased 3.5.9')
need(CONTRACT.get('implemented_in_git_ref') is None, 'unreleased Pass 37 contract must not self-reference a git ref')
need(CONTRACT.get('release_status') == 'unreleased', 'Pass 37 contract must remain unreleased')
need('String stagingVersion = "3.5.9"' in BUILD, 'build.gradle stagingVersion must be 3.5.9')
need(CONTRACT.get('implemented_in_version') != '3.5.5' and CONTRACT.get('implemented_in_git_ref') != 'refs/tags/3.5.5',
     'stale 3.5.5 provenance remains')

# Kelp head/body + AGE + bounded aquatic bridge.
need(has(MPB, 'ParityKelpStateTileEntity', 'Math.min(25', 'tag.setByte("Age"', 'modern_parity_kelp_state'),
     'Kelp AGE 0..25 synchronized state is incomplete')
need(has(MPB, 'ParityAquaticItemBlock', 'isSourceWater(world', 'parity.isKelp() || parity.isKelpPlant() || parity.isSeagrass() || parity.isTallSeagrass()'),
     'bounded aquatic ItemBlock bridge is missing')
need(has(MPB, 'growKelpOne', 'KELP_PLANT.get()', 'setKelpAge', 'random.nextDouble()<0.14D'),
     'Kelp tip/body/random growth implementation is incomplete')
need(has(MPB, 'if (isKelp() || isKelpPlant())', 'growKelpOne(world, x, y, z)'), 'Kelp bonemeal growth hook missing')
need('make water globally replaceable' in MPB or 'does not make water' in MPB,
     'bounded aquatic compatibility must explicitly reject general water replacement')

# Seagrass/Tall Seagrass.
need(has(MPB, 'isSeagrass()', 'TALL_SEAGRASS.get()', 'world.setBlock(x,y+1,z,TALL_SEAGRASS.get(),1,3)'),
     'Seagrass -> Tall Seagrass bonemeal conversion missing')
need(has(MPB, 'parity.isTallSeagrass() && !isSourceWater(world, x, y + 1, z)'),
     'Tall Seagrass two-source-water placement gate missing')
need(has(MPB, 'if (isSeagrass() || isTallSeagrass())', 'tool.getItem() == Items.shears', 'int count = isTallSeagrass() ? 2 : 1'), 'Tall Seagrass shears harvest hook missing')
need('Shears' in DOC and 'two' in DOC, 'Seagrass/Tall Seagrass drops are not documented')

# Coral all five colours/forms, drying and facing preservation.
for colour in ('TUBE','BRAIN','BUBBLE','FIRE','HORN'):
    for suffix in ('_CORAL_BLOCK(', '_CORAL(', '_CORAL_FAN(', '_CORAL_WALL_FAN('):
        need(colour + suffix in MPB, 'Coral lifecycle source lacks %s%s' % (colour.lower(), suffix.lower().rstrip('(')))
need(has(MPB, 'scheduleCoralDeath', '60 + world.rand.nextInt(40)', 'deadCoralCompanion', 'coralHasWater'),
     'Coral 60..99 tick live/dead lifecycle missing')
need(has(MPB, 'deadCoralCompanion()', 'world.getBlockMetadata(x, y, z)', 'dead.get()'),
     'Coral death conversion/state preservation hook missing')
need('wall fan' in DOC.lower() and 'facing' in DOC.lower(), 'Coral wall-fan facing preservation not documented')

# Sea Pickle.
need(has(MPB, 'if (isSeaPickle()) return (meta & 3) + 1', '6 + (meta & 3) * 3'),
     'Sea Pickle count-aware drops/light mapping regressed')
need(has(MPB, 'bonemealSeaPickles', 'world.rand.nextInt(6)==0', 'world.rand.nextInt(4)'), 'Sea Pickle bounded bonemeal spread missing')
need(has(MPB, 'count >= 4', '(meta & 4) | count'), 'Sea Pickle same-block stacking 1..4 missing')

# Torchflower / Pitcher crops + actual planting items.
need(has(ITEMS, 'torchflower_seeds', 'pitcher_pod', 'Blocks.farmland', 'isCreativeMode'),
     'Torchflower Seed/Pitcher Pod planting items missing')
need('item.etfuturum.torchflower_seeds.name=Torchflower Seeds' in ENUS and 'item.etfuturum.pitcher_pod.name=Pitcher Pod' in ENUS,
     'Pass 37 planting-item en_US names missing')
need('item.etfuturum.torchflower_seeds.name=Torchflower Seeds' in ENGB and 'item.etfuturum.pitcher_pod.name=Pitcher Pod' in ENGB,
     'Pass 37 planting-item en_GB names missing')
need(has(MPB, 'if (isTorchflowerCrop())', 'random.nextInt(3)!=0', 'ancientCropGrowthSpeed', 'growTorchflower(world,x,y,z)', 'TORCHFLOWER.get()'),
     'Torchflower modern random growth/mature transition missing')
need(has(MPB, 'private boolean growPitcher', 'int next = Math.min(4, age + increase)', 'if (next >= 3)', '5+next',
             'getPitcherPod()', 'PITCHER_PLANT.get()'),
     'Pitcher age/half/drop lifecycle incomplete')
need('Age 4 remains Pitcher Crop' in DOC, 'Pitcher age-4 identity rule not documented')
need('recreate' not in MPB[MPB.find('private void growPitcher'):MPB.find('private void growPitcher')+2500] if 'private void growPitcher' in MPB else True,
     'Pitcher growth path contains suspicious counterpart recreation wording')

# Mangrove hanging + planted growth.
need(has(SAPLING, 'PASS37_HANGING_PLACEMENT', 'placeHangingPropagule', 'setMangroveVisualState(world, x, y, z, true, age)'),
     'Mangrove hanging placement ordering guard missing')
need(has(SAPLING, 'getMangroveAge', 'age < 4', 'age + 1', 'isMangroveLeaves(world, x, y + 1, z)'),
     'Mangrove hanging AGE/support lifecycle incomplete')
need(has(LEAVES, 'IGrowable', 'placeHangingPropagule(world, x, y - 1, z, 0)'),
     'Mangrove Leaves bonemeal -> hanging age-0 propagule missing')
need(has(SAPLING, 'new WorldGenPass37MangroveTree(true)', 'tree = mangrove'), 'planted Mangrove manual tree hook missing')
need(has(MANGROVE_GEN, 'random.nextFloat() < 0.85F', 'placeHangingPropagule', 'random.nextInt(5)'),
     'manual Mangrove tree tall bias/decorator lifecycle missing')

# Pale Oak 2x2 + bounded generated leaf decay.
need(has(MPB, 'private void advancePaleOak', 'for (int ox=0; ox>=-1; ox--)', 'random.nextInt(7)==0',
             'world.rand.nextFloat() < 0.45F', 'PASS37_PALE_OAK_TREE.generate'),
     'Pale Oak stage/random/bonemeal/2x2 growth lifecycle incomplete')
need(has(MPB, 'PASS37_PALE_OAK_TREE', 'isPaleOakLeaves()', '(meta & 8)'),
     'Pale Oak generated-leaf bounded decay marker missing')
need('WorldGenCanopyTree' in PALE_GEN and 'PALE_OAK_LOG' in PALE_GEN and 'PALE_OAK_LEAVES' in PALE_GEN,
     'manual Pale Oak generator missing')

# Pale Hanging Moss / Pale Moss Carpet.
need(has(MPB, 'isPaleHangingMoss()', 'while (tipY>0', 'world.setBlock(x,tipY-1,z,this,1,3)'),
     'Pale Hanging Moss tip bonemeal extension missing')
need('no random downward growth' in DOC.lower(), 'Pale Hanging Moss no-random-growth rule not documented')
need(has(MPB, 'updatePaleMossCarpet', 'maybeCreatePaleMossTopper', 'ParityPaleMossCarpetTileEntity'),
     'Pale Moss Carpet update/topper lifecycle missing')

# Leaf Litter / Wildflowers.
need(has(MPB, 'isLeafLitter()', 'isWildflowers()', 'amount<4'), 'Leaf Litter/Wildflowers stacking missing')
need(has(MPB, 'if (isWildflowers())', 'dropBlockAsItem'), 'Wildflowers bonemeal amount/extra-drop path missing')
need('Leaf Litter has no Bone Meal behavior' in DOC, 'Leaf Litter non-bonemealable rule not documented')
need('if (isLeafLitter())' not in MPB[MPB.find('if (isBoneMeal(held))'):MPB.find('if (isBoneMeal(held))')+5000],
     'Leaf Litter must not acquire Bone Meal behavior')

# Eyeblossom exact timeline, propagation and sounds.
need(has(MPB, 'eyeblossomShouldBeOpen', 'time>=12600L && time<23401L', 'tickEyeblossom'),
     'Eyeblossom standard Overworld timeline missing')
need(has(MPB, 'for(int dx=-3;dx<=3;dx++)', 'for(int dy=-2;dy<=2;dy++)', 'for(int dz=-3;dz<=3;dz++)', 'scheduleBlockUpdate'),
     'Eyeblossom ±3/±2 propagation scheduling missing')
for event in ('block.eyeblossom.open_long','block.eyeblossom.open','block.eyeblossom.close_long','block.eyeblossom.close','block.eyeblossom.idle'):
    need(event in ETF and event in SOUNDS, 'Eyeblossom sound event missing: '+event)
need('Bee' not in MPB[MPB.find('private void tickEyeblossom'):MPB.find('private void tickEyeblossom')+5000],
     'Pass 37 Eyeblossom path must not add Bee/entity effects')

# Scope / no new worldgen registration or entity endpoints.
for generator in ('WorldGenPass37MangroveTree', 'WorldGenPass37PaleOakTree'):
    need(('GameRegistry.registerWorldGenerator(' + generator) not in ETF,
         generator + ' must remain manual-only and not worldgen-registered')
changed_runtime = '\n'.join((MPB, ITEMS, SAPLING, LEAVES, MANGROVE_GEN, PALE_GEN))
for forbidden in ('EntityTurtle', 'EntitySniffer', 'EntityTadpole', 'HappyGhast', 'EntityCreaking'):
    need(forbidden not in changed_runtime, 'entity-phase behavior leaked into Pass 37: '+forbidden)
# Item drops legitimately use spawnEntityInWorld elsewhere in this large legacy class. Gate the
# actual Pass-37 lifecycle helpers instead of banning the generic 1.7 entity API globally.
pass37_start = MPB.find('private boolean growKelpOne')
pass37_end = MPB.find('private boolean isSegmentedGroundDecal')
pass37_helpers = MPB[pass37_start:pass37_end] if pass37_start >= 0 and pass37_end > pass37_start else ''
need('spawnEntityInWorld' not in pass37_helpers, 'entity-phase spawning leaked into Pass 37 lifecycle helpers')
need('general waterlogging' in DOC.lower() and 'world generation' in DOC.lower(), 'Pass 37 scope exclusions not documented')

# Capability/contract/documentation and Pass34 visible-state preservation.
need(has(AUDIT, 'PASS_37_VEGETATION_NAMES', 'PASS_37_VEGETATION_LIFECYCLE', 'PASS_37_VERIFIED'),
     'Pass 37 capability audit classification missing')
need('## Pass 37 vegetation / plant lifecycle contract' in MATRIX, 'Pass 37 capability matrix section missing')
need('Pass 37 — Vegetation / Plant Lifecycle Functional Parity' in DOC, 'Pass 37 lifecycle documentation missing')
bykey = {b.get('key'): b for b in CONTRACT.get('blocks', [])}
for key in ('kelp','kelp_plant','seagrass','tall_seagrass','sea_pickle','torchflower_crop','pitcher_crop','mangrove_propagule','pale_hanging_moss','pale_moss_carpet'):
    need(key in bykey, 'Backporter contract missing Pass 37 key '+key)
for key in ('pass37_coral_family','pass37_pale_oak_lifecycle','pass37_eyeblossom_lifecycle'):
    need(key in bykey, 'Backporter contract missing aggregate '+key)
# Old Pass34 phrases intentionally stay regression-sensitive.
contract_text = text('docs/BACKPORTER_STATE_CONTRACT.json')
for phrase in ('compatibility alias', 'must not target', 'defaults to the live visual bit',
               'Pass 34c allows in-place stacking from 1..4 while preserving bit2',
               'Normal placement creates the canonical lower+upper two-block pair',
               'Pass 34c supports Bone Meal progression age 0 -> 1 -> the existing Torchflower block',
               'Pass 34c Bone Meal advances one age per use and creates/synchronizes the upper half from age 3'):
    need(phrase in contract_text, 'Pass 34 contract explanation regressed: '+phrase)

if errors:
    print('Fidelity Pass 37 validation FAILED')
    for error in errors:
        print(' -', error)
    sys.exit(1)

print('Fidelity Pass 37 validation PASSED')
print(' - aquatic Kelp/Seagrass/Coral/Sea Pickle lifecycle is bounded without general waterlogging')
print(' - Torchflower, Pitcher, Mangrove and Pale Oak growth paths are represented without biome worldgen')
print(' - Pale Moss, Leaf Litter, Wildflowers and Eyeblossom block-local mechanics are gated')
print(' - entity-dependent lifecycle endpoints remain deferred')
print(' - Backporter contract revision 37 / unreleased 3.5.9 and Pass 34 visible-state contracts remain intact')
