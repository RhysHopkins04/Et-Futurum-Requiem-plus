# Fidelity Pass 36 — Modern Copper Lifecycle / Block Mechanics

Pass 36 is a runtime-mechanics extension of EFR's existing `IDegradable` copper system. It does not
change the established Pass 29–35 static Backporter metadata encodings.

## Covered families

The following registry families participate through copper, exposed, weathered, oxidized and the
matching four waxed identities:

- Copper Chest
- Copper Golem Statue
- Copper Bars
- Copper Chain
- Copper Lantern
- Lightning Rod (`etfuturum:lightning_rod` is the mature stage-0 unwaxed implementation)

Copper Torch and Copper Wall Torch are deliberately excluded. Minecraft Java 1.21.11's
`WeatheringCopper.NEXT_BY_BLOCK` contains the families above and does not contain Copper Torch.

## Shared lifecycle

Every covered unwaxed block implements the same `IDegradable` contract as mature EFR copper:

- natural oxidation: copper -> exposed -> weathered -> oxidized;
- honeycomb/wax material: current unwaxed stage -> matching waxed identity;
- axe on waxed: matching unwaxed identity;
- axe on exposed/weathered/oxidized unwaxed: exactly one stage backward.

Waxed identities do not naturally oxidize. The existing range-4 neighbourhood/age calculation counts
new and mature `IDegradable` copper together. Family metadata is preserved verbatim during registry
identity changes: Statue pose/facing, Chain axis, Lantern hanging and Lightning Rod facing all survive.
Copper Bars connections remain neighbour-derived and are recalculated after the identity transition.

## Copper Chest replacement safety

Minecraft 1.7 `BlockChest.breakBlock` drops its inventory when a block identity is replaced, whereas
modern Java keeps the block entity across Copper Chest identity changes. Pass 36 therefore snapshots
both paired chest TileEntities before changing either block, temporarily clears their inventories so
legacy break code cannot drop/duplicate them, installs the common target identity, then restores the
full NBT (Items, CustomName and `EFRPairDirection`) and refreshes adjacency/render state.

All Copper Chest oxidation/wax identities are compatible with one another, matching modern
`#copper_chests`. Normal and trapped chests remain incompatible. When a mixed-stage pair is formed,
Pass 36 converges the pair to the least oxidized stage; mixed wax/unwax state is normalized to unwaxed
before that common identity is selected.

## Copper Golem Statue block-local mechanics

Pass 35's exact `meta = pose * 4 + facing` contract is unchanged. Non-axe interaction cycles:

`standing -> sitting -> running -> star -> standing`

Comparator output is `pose + 1`, therefore 1, 2, 3 or 4. Lifecycle transitions preserve the complete
0..15 metadata value. An axe on a fully unoxidized/unwaxed statue is intentionally left unhandled:
modern Java uses that action for Copper Golem reanimation, which remains deferred to the entity phase.

## Lightning cleaning

Pass 36 injects only into the existing 1.7 `EntityLightningBolt`; it does not add weather or a new
lightning entity. On the initial server-side strike, if the strike position is `IDegradable` copper:

1. an unwaxed struck block is reset to the first oxidation stage of its family/variant;
2. a waxed struck block remains waxed;
3. either kind starts 3..5 random cleaning walks;
4. each walk has 1..8 steps;
5. each step tries up to ten positions in the surrounding 3x3x3 cube and may scrape an unwaxed
   `IDegradable` copper block backward by one stage.

This mirrors the 1.21.11 `LightningBolt.clearCopperOnLightningStrike` shape while reusing EFR's
state-preserving `IDegradable.setCopperBlock` hooks.

## Still deferred

- Copper Golem entity spawning/reanimation and all entity AI;
- general waterlogging;
- unrelated Trial Spawner/Vault/Crafter/Sculk gameplay;
- world generation and structures.

## Pass 36b runtime corrections

Runtime testing of Pass 36 exposed three legacy-bridge issues without changing the lifecycle contract:

- `BlockChest.onBlockAdded()` recalculates 1.7 chest facing whenever oxidation/waxing swaps the
  registry identity. Pass 36b reapplies the exact saved metadata after each Copper Chest replacement,
  including both halves of a reciprocal pair, before restoring TileEntity NBT.
- Copper Golem Statue pose cycling is routed through an explicit 16-state helper that advances only
  the two pose bits and preserves the two facing bits. The facing is also reasserted after legacy
  comparator/neighbour callbacks. AssetDirector now requests the ordinary 1.21.11
  `entity.copper_golem_become_statue` event and its four modern copper-statue OGGs.
- Lightning Rod floor/ceiling placement remains unchanged, while 1.7 wall-click sides are inverted
  at the modern-model compatibility boundary so the rod head points away from its support. Static
  Backporter/import facing metadata remains unchanged.

These are bounded runtime corrections only; no Pass 29-35 state mappings or Pass 36 lifecycle
transition tables are changed.


## Pass 36c sound-event closeout

Pass 36b requested the intended Copper Golem Statue pose-change sound through AssetDirector, and
Pass 36c added a matching entry to EFR's versioned `minecraft_1.21.11:sounds.json` overlay. Runtime
logging later proved that both passes used the wrong dotted event key, so AssetDirector rejected the
request and the locally-defined event had no backing OGG assets. Pass 36c1 below corrects that root
cause rather than adding another alias around the bad key.

Lightning Rod natural-strike attraction is not implemented by the current 1.7.10 rod. Pass 36's
lightning mixin only performs copper cleaning after an `EntityLightningBolt` already exists. Modern
1.21.11 redirects a natural strike candidate to a valid surface Lightning Rod within 128 blocks; that
weather-target-selection mechanic is explicitly not claimed as complete by Pass 36c and should be
handled as a separate bounded parity change if desired.

## Pass 36c1 Copper Golem Statue sound asset correction

Runtime testing of Pass 36c exposed the remaining sound failure. Mojang 1.21.11 registers
`COPPER_GOLEM_BECOME_STATUE` as `entity.copper_golem_become_statue` (underscore before
`become`), while Pass 36b/36c used the non-existent dotted key
`entity.copper_golem.become_statue`.

Because AssetDirector resolves requested sound events from Mojang's versioned `sounds.json`,
the incorrect event key was rejected and its backing OGGs were never fetched. The dynamic
versioned `sounds.json` then contained an event whose sound files were absent, producing
`Unable to play empty soundEvent`.

Pass 36c1 corrects the event key at all three boundaries (interaction, AssetDirector request,
and dynamic sounds.json) and explicitly requests
`minecraft/sounds/block/copper_statue/become_statue1..4.ogg` through AssetDirector. This
provides a direct asset-delivery invariant in addition to the correct Mojang event expansion.

