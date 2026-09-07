#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
ETFU = (ROOT / "src/main/java/ganymedes01/etfuturum/EtFuturum.java").read_text()
MPB = (ROOT / "src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java").read_text()
DYN = (ROOT / "src/main/java/ganymedes01/etfuturum/client/DynamicSoundsResourcePack.java").read_text()
DOC = (ROOT / "docs/FIDELITY_PASS_36_COPPER_LIFECYCLE.md").read_text()

errors = []

def need(cond, msg):
    if not cond:
        errors.append(msg)

EVENT = "entity.copper_golem_become_statue"
OLD_EVENT = "entity.copper_golem.become_statue"

need(f'config.addSoundEvent(ver, "{EVENT}", "block")' in ETFU,
     "AssetDirector does not request Mojang's exact Copper Golem become-statue event")
need(f'config.addSoundEvent(ver, "{OLD_EVENT}", "block")' not in ETFU,
     "obsolete dotted Copper Golem sound event is still requested from AssetDirector")
need(f'Tags.MC_ASSET_VER + ":{EVENT}"' in MPB,
     "Copper Golem Statue interaction does not play the exact Mojang 1.21.11 event")
need(f'Tags.MC_ASSET_VER + ":{OLD_EVENT}"' not in MPB,
     "Copper Golem Statue interaction still uses obsolete dotted event name")
need(f'addSoundsToCategory("{EVENT}"' in DYN,
     "versioned dynamic sounds.json does not define the exact Mojang event")
need(f'addSoundsToCategory("{OLD_EVENT}"' not in DYN,
     "versioned dynamic sounds.json still defines obsolete dotted event key")

for i in range(1, 5):
    need(f'config.addObject(ver, "minecraft/sounds/block/copper_statue/become_statue{i}.ogg")' in ETFU,
         f"Copper Golem OGG become_statue{i} is not explicitly requested through AssetDirector")
    need(f'"block/copper_statue/become_statue{i}"' in DYN,
         f"dynamic sound event is missing become_statue{i}")

need("Pass 36c1 Copper Golem Statue sound asset correction" in DOC,
     "Pass 36c1 sound correction is not documented")
need("entity.copper_golem_become_statue" in DOC,
     "documentation does not record the exact Mojang event key")
need("Unable to play empty soundEvent" in DOC,
     "documentation does not record the observed empty-event failure mode")

if errors:
    print("Fidelity Pass 36c1 validation FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("Fidelity Pass 36c1 validation PASSED")
print(" - exact Mojang event key entity.copper_golem_become_statue is used end-to-end")
print(" - obsolete dotted event key is absent from executable sound wiring")
print(" - all four copper statue become-statue OGGs are explicitly requested")
print(" - dynamic versioned sounds.json binds all four OGG paths")
