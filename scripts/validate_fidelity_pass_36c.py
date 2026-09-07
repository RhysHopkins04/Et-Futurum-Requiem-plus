#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
DYN = (ROOT / 'src/main/java/ganymedes01/etfuturum/client/DynamicSoundsResourcePack.java').read_text()
ETFU = (ROOT / 'src/main/java/ganymedes01/etfuturum/EtFuturum.java').read_text()
MPB = (ROOT / 'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
DOC = (ROOT / 'docs/FIDELITY_PASS_36_COPPER_LIFECYCLE.md').read_text()
MIXIN = (ROOT / 'src/main/java/ganymedes01/etfuturum/mixins/early/copperlifecycle/MixinEntityLightningBolt.java').read_text()
ROD_TE = (ROOT / 'src/main/java/ganymedes01/etfuturum/tileentities/TileEntityLightningRod.java').read_text()

errors = []
def need(cond, msg):
    if not cond:
        errors.append(msg)

need('ImmutableSet.of("minecraft", Tags.MC_ASSET_VER)' in DYN,
     'dynamic sounds pack no longer exposes the versioned AssetDirector namespace')
need('addSoundsToCategory("entity.copper_golem_become_statue"' in DYN,
     'Copper Golem Statue pose sound event is not defined in the versioned sounds.json')
for n in range(1, 5):
    need(f'"block/copper_statue/become_statue{n}"' in DYN,
         f'Copper Golem Statue modern sound become_statue{n} is missing')
need('config.addSoundEvent(ver, "entity.copper_golem_become_statue", "block")' in ETFU,
     'AssetDirector request for Copper Golem Statue sound resources regressed')
need('Tags.MC_ASSET_VER + ":entity.copper_golem_become_statue"' in MPB,
     'Statue pose interaction no longer plays the versioned sound event')
need('ModernCopperLifecycle.cleanCopperOnLightningStrike' in MIXIN,
     'Pass 36 lightning copper-cleaning hook regressed')
need('class TileEntityLightningRod extends TileEntity' in ROD_TE,
     'Lightning Rod tile entity baseline changed unexpectedly')
need('Lightning Rod natural-strike attraction is not implemented' in DOC and 'within 128 blocks' in DOC,
     'Pass 36c documentation does not explicitly classify Lightning Rod attraction status')

if errors:
    print('Fidelity Pass 36c validation FAILED')
    for e in errors:
        print(' -', e)
    sys.exit(1)
print('Fidelity Pass 36c validation PASSED')
print(' - versioned dynamic sounds.json now defines entity.copper_golem_become_statue')
print(' - all four modern copper-statue pose OGG paths remain bound to that event')
print(' - AssetDirector fetch request and versioned interaction ResourceLocation remain intact')
print(' - Lightning Rod natural-strike attraction is explicitly not falsely claimed by Pass 36c')
