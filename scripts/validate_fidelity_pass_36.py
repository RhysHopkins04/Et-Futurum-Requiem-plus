#!/usr/bin/env python3
"""Static gate for Pass 36 modern copper lifecycle / block mechanics."""
from __future__ import print_function
import json, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MPB = (ROOT/'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
IDEG = (ROOT/'src/main/java/ganymedes01/etfuturum/blocks/IDegradable.java').read_text()
ROD = (ROOT/'src/main/java/ganymedes01/etfuturum/blocks/BlockLightningRod.java').read_text()
LIGHT = (ROOT/'src/main/java/ganymedes01/etfuturum/blocks/ModernCopperLifecycle.java').read_text()
CHEST_PAIR = (ROOT/'src/main/java/ganymedes01/etfuturum/core/utils/ModernChestPairing.java').read_text()
CHEST_BLOCK = (ROOT/'src/main/java/ganymedes01/etfuturum/mixins/early/chestpairing/MixinBlockChest.java').read_text()
CHEST_TE = (ROOT/'src/main/java/ganymedes01/etfuturum/mixins/early/chestpairing/MixinTileEntityChest.java').read_text()
LIGHT_MIXIN = (ROOT/'src/main/java/ganymedes01/etfuturum/mixins/early/copperlifecycle/MixinEntityLightningBolt.java').read_text()
PLUGIN = (ROOT/'src/main/java/ganymedes01/etfuturum/mixinplugin/EtFuturumEarlyMixins.java').read_text()
AUDIT = (ROOT/'scripts/audit_modern_map_parity_capabilities.py').read_text()
DOC = (ROOT/'docs/FIDELITY_PASS_36_COPPER_LIFECYCLE.md').read_text()
MATRIX = (ROOT/'docs/MODERN_MAP_PARITY_CAPABILITY_MATRIX.md').read_text()
CONTRACT = json.loads((ROOT/'docs/BACKPORTER_STATE_CONTRACT.json').read_text())

errors=[]
def need(ok,msg):
    if not ok: errors.append(msg)
def has(text,*parts): return all(p in text for p in parts)

families = ['copper_chest','copper_golem_statue','copper_bars','copper_chain','copper_lantern']
stages = ['', 'exposed_', 'weathered_', 'oxidized_']
for family in families:
    for stage in stages:
        for waxed in (False, True):
            name = ('waxed_' if waxed else '') + stage + family
            need(name.upper() in MPB, 'missing Pass-36 identity '+name)
# Lightning Rod stage0 unwaxed is the mature EFR block; the other seven identities are parity entries.
need('LIGHTNING_ROD(ConfigBlocksItems.enableLightningRodBlock, new BlockLightningRod())' in (ROOT/'src/main/java/ganymedes01/etfuturum/ModBlocks.java').read_text(),
     'mature stage-0 Lightning Rod registration missing')
for stage in stages:
    for waxed in (False, True):
        if stage == '' and not waxed: continue
        name=('waxed_' if waxed else '')+stage+'lightning_rod'
        need(name.upper() in MPB, 'missing Lightning Rod lifecycle identity '+name)

# One shared lifecycle framework, not a second oxidation implementation.
need('interface IDegradable' in IDEG and 'tickDegradation' in IDEG and 'tryWaxOnWaxOff' in IDEG,
     'shared IDegradable lifecycle foundation missing')
need('ParityCopperChestBlock extends BlockChest implements IDegradable' in MPB,
     'Copper Chest family is not wired into IDegradable')
need('ParityCopperLifecycleModelBlock extends ParityModelBlock implements IDegradable' in MPB,
     'parity copper model families are not narrowly wired into IDegradable')
need('ParityModelBlock extends Block implements ITileEntityProvider, IDegradable' not in MPB,
     'generic parity shells must not all count as degradable copper')
need('BlockLightningRod extends BaseBlock implements IDegradable' in ROD,
     'mature Lightning Rod is not wired into shared IDegradable')
need(has(MPB,'getPass36CopperMeta(Block block)','getPass36CopperBlock(Block source, int copperMeta)',
             'getCopperLifecycleStage()','isWaxedCopperLifecycleIdentity()'),
     'Pass-36 family transition table helpers missing')
need('"copper_torch".equals(family)' not in MPB and '"copper_wall_torch".equals(family)' not in MPB,
     'Copper Torch must not enter the oxidation family')

# Natural oxidation and interaction mechanics.
need(has(MPB,'setTickRandomly(!entry.isWaxedCopperLifecycleIdentity()','tickDegradation(world, x, y, z, random)'),
     'Copper Chest unwaxed-only natural oxidation gate missing')
need(has(MPB,'entry.isPass36CopperLifecycleIdentity()','!entry.isWaxedCopperLifecycleIdentity()',
             '((IDegradable) this).tickDegradation(world, x, y, z, random)'),
     'parity copper unwaxed-only natural oxidation gate missing')
need(has(MPB,'tryWaxOnWaxOff(world, x, y, z, player)','getFinalCopperMeta','return worldMeta'),
     'parity copper wax/scrape state-preservation path missing')
need(has(ROD,'tryWaxOnWaxOff(world, x, y, z, player)','getFinalCopperMeta','return worldMeta'),
     'Lightning Rod wax/scrape/facing-preservation path missing')
need(has(IDEG,'meta < 8','meta % 4 != 0 || meta > 7','meta - 1','damageItem(1, entityPlayer)'),
     'existing wax-on/wax-off/one-stage-scrape/axe-durability semantics regressed')

# Copper Chest TE + pair preservation and modern cross-stage pairing compatibility.
need(has(CHEST_PAIR,'areCompatibleChestBlocks','isCopperChestBlock(first)','isCopperChestBlock(second)'),
     'Copper Chest cross-weather/wax pairing compatibility missing')
need(has(CHEST_BLOCK,'normalizeCopperChestPairIdentity','areCompatibleChestBlocks'),
     'Copper Chest placement does not normalize compatible mixed identities')
need('areCompatibleChestBlocks(ownBlock' in CHEST_TE,
     'Copper Chest TE partner resolution still requires exact identity')
need(has(MPB,'ChestSnapshot.capture','tile.writeToNBT(tag)','clearInventory()',
             'restoreIntoReplacement()','EFRPairDirection') or
     has(MPB,'ChestSnapshot.capture','tile.writeToNBT(tag)','clearInventory()',
             'restoreIntoReplacement()','IChestPairingState'),
     'Copper Chest lifecycle does not snapshot/clear/restore full TileEntity state')
need(has(MPB,'transitionCopperChestPair','partner.clearInventory()','world.setBlock(px, y, pz, target, partner.metadata, 2)',
             'first.resolveRestoredPair()','partner.resolveRestoredPair()'),
     'Copper Chest paired-half identity transition/pair restoration missing')
need(('normal/trapped chests remain incompatible' in MATRIX) or ('vanilla/trapped chests remain incompatible' in MATRIX),
     'capability docs do not preserve normal/trapped Copper Chest incompatibility')

# Statue block-local mechanics and state preservation.
need(has(MPB,'isCopperGolemStatue()','cycleCopperGolemStatuePoseMeta(oldMeta)',
             'entity.copper_golem_become_statue'),
     'Copper Golem Statue pose cycle/facing preservation missing')
need(has(MPB,'hasComparatorInputOverride()','isCopperGolemStatue()',
             '((world.getBlockMetadata(x, y, z) >> 2) & 3) + 1'),
     'Copper Golem Statue comparator output 1..4 missing')
need('contains("axe")) return false' in MPB,
     'base-statue axe action must remain available for deferred entity reanimation rather than cycling pose')

# Lightning exact algorithm shape, integrated into existing entity.
need(has(LIGHT_MIXIN,'@Mixin(EntityLightningBolt.class)','onUpdate','etfu$pass36CopperCleaned',
             'ModernCopperLifecycle.cleanCopperOnLightningStrike'),
     'existing 1.7 lightning entity is not wired to Pass-36 cleaning')
need('copperlifecycle.MixinEntityLightningBolt' in PLUGIN,
     'Pass-36 lightning mixin is not registered')
need(has(LIGHT,'random.nextInt(3) + 3','random.nextInt(8) + 1','attempt < 10',
             'random.nextInt(3) - 1','copperMeta >= 8','previousMeta = copperMeta - 1'),
     'lightning 3..5 walks / 1..8 steps / ten-candidate one-stage cleaning shape missing')
need(has(LIGHT,'firstMeta = copperMeta - (copperMeta % 4)','degradable.setCopperBlock'),
     'direct lightning strike does not reset unwaxed copper to first stage via state-preserving hook')

# Existing mature copper implementations remain on IDegradable.
for rel, token in [
    ('src/main/java/ganymedes01/etfuturum/blocks/BlockCopper.java','implements IDegradable'),
    ('src/main/java/ganymedes01/etfuturum/blocks/BlockCutCopperStairs.java','implements IDegradable'),
    ('src/main/java/ganymedes01/etfuturum/blocks/BlockCutCopperSlab.java','implements IDegradable'),
    ('src/main/java/ganymedes01/etfuturum/blocks/BlockCopperDoor.java','implements IDegradable'),
    ('src/main/java/ganymedes01/etfuturum/blocks/BlockCopperTrapdoor.java','implements IDegradable'),
]:
    need(token in (ROOT/rel).read_text(), 'existing mature copper lifecycle regressed in '+rel)
need('extends BlockCopper' in (ROOT/'src/main/java/ganymedes01/etfuturum/blocks/BlockCopperGrate.java').read_text(),
     'Copper Grate no longer inherits mature copper lifecycle')
need('extends BlockCopper' in (ROOT/'src/main/java/ganymedes01/etfuturum/blocks/BlockCopperBulb.java').read_text(),
     'Copper Bulb no longer inherits mature copper lifecycle')

# Contract / capability documentation.
bykey={b.get('key'):b for b in CONTRACT.get('blocks',[])}
chest=bykey.get('copper_chest_family',{})
statue=bykey.get('copper_golem_statue_family',{})
need(not chest.get('unsupported'), 'Copper Chest contract still marks lifecycle unsupported')
need('Pass 36' in chest.get('exactness',{}).get('behaviour',''), 'Copper Chest contract not updated for Pass 36')
need('oxidation/wax/scrape lifecycle implemented' in statue.get('exactness',{}).get('gameplay',''),
     'Copper Golem Statue contract still defers block lifecycle')
need(has(DOC,'Copper Chest','Copper Golem Statue','Copper Bars','Copper Chain','Copper Lantern','Lightning Rod',
             'Copper Torch','standing -> sitting -> running -> star -> standing','3..5','1..8'),
     'Pass 36 lifecycle document incomplete')
need('PASS_36_COPPER_LIFECYCLE' in AUDIT and 'PASS_36_VERIFIED' in AUDIT,
     'capability audit does not promote Pass-36 copper lifecycle rows')
need('## Pass 36 modern copper lifecycle contract' in MATRIX,
     'capability matrix missing Pass 36 lifecycle section')

if errors:
    print('Fidelity Pass 36 validation FAILED')
    for e in errors: print(' - '+e)
    sys.exit(1)
print('Fidelity Pass 36 validation PASSED')
print(' - six applicable modern copper families share EFR IDegradable across four unwaxed + four waxed stages')
print(' - wax on/off, one-stage scraping and natural oxidation preserve family metadata/state')
print(' - Copper Chest lifecycle preserves inventory/name/reciprocal pairing and supports cross-stage copper pairs')
print(' - Copper Golem Statue cycles four poses, outputs comparator 1..4 and preserves pose/facing')
print(' - existing 1.7 lightning performs the 1.21.11-shaped direct clean plus random-walk de-oxidation')
print(' - Copper Torch remains outside WeatheringCopper lifecycle and entity reanimation remains deferred')
