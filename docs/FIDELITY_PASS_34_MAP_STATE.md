# Fidelity Pass 34 — Remaining persistent / visible block state

Pass 34 closes the bounded Pale Garden, crop/growth-stage and aquatic visible-state gaps used by Minecraft Map Backporter Studio. General waterlogging and lifecycle/entity systems remain deferred.

## Exact states

- Pale Moss Carpet: `bottom` plus four `none/low/tall` sides in `ParityPaleMossCarpetTileEntity` (162 combinations). Normal item placement now requires a solid floor support; Backporter TE state remains unrestricted/exact.
- Pale Hanging Moss: `tip` metadata 0/1. Normal placement creates a visible bottom tip, extends chains downward, and converts the previous tip to the body model. Removing the chain end promotes the block above to the tip.
- Creaking Heart: 9 `axis × creaking_heart_state` metadata combinations (`0..2=dormant x/y/z`, `3..5=awake x/y/z`, `6..8=uprooted x/y/z`).
- Dried Ghast: 16 `facing × hydration` metadata combinations (`meta=hydration*4+facing`, N/E/S/W=`0/1/2/3`). Normal placement now follows player facing at hydration 0.
- Torchflower Crop: `age=0/1`.
- Pitcher Crop: `age=0..4 × half=lower/upper`, with safe paired break cleanup.
- Pitcher Plant: mature standalone block now preserves `half=lower/upper` as metadata 0/1; normal item placement creates both halves and paired breaking avoids duplicate drops.
- Sniffer Egg: `hatch=0..2`.
- Mangrove Propagule: extends the mature `etfuturum:sapling` metadata-0 block with synchronized `Hanging` + `Age` TE state. The old `etfuturum:mangrove_propagule` registry identity is retained only as a hidden compatibility alias so Pass-33 saves/server handshakes remain loadable; Backporter output must still target `etfuturum:sapling`.
- Sea Pickle: count 1..4 plus one bounded source-live visual bit. The bit selects the live/dead Mojang model and light level but is **not** a general waterlogging/fluid implementation. Normal placement defaults to the live/white-tipped model to preserve EFR's pre-Pass-34 presentation.
- Tall Seagrass: `half=lower/upper`. Normal placement creates both blocks and paired breaking removes the mate; Backporter can still place either exact metadata half directly.

## Runtime clarification

Pass 34 is map-state fidelity, not the later lifecycle pass. Pass 34c adds only the small, directly state-driven runtime interactions needed to exercise the implemented visual states: Torchflower/Pitcher bonemeal progression, Sea Pickle 1..4 stacking, and ordinary Pale Hanging Moss chain placement. Mangrove tree generation, Sniffer Egg hatching timers/entity spawning, Creaking behavior, Dried Ghast hydration progression, and general waterlogging remain intentionally deferred. Their visible source states are nevertheless stored/rendered exactly for imported maps.

## Audited as already exact / visually property-free

Turtle Egg (`eggs`, `hatch`) and Leaf Litter/Wildflowers (amount + facing) were already exact and remain regression-sensitive. Minecraft 1.21.11 Kelp/Kelp Plant and Open/Closed Eyeblossom blockstate JSON have no model-selecting properties, so no extra persistent state is needed for those identities.

Random/natural crop growth, Sniffer/Creaking/Happy-Ghast entity behavior, waterlogging, non-ancient-crop bonemeal/spreading and world generation remain later work.


## Pass 34c runtime corrections

- Pale Hanging Moss no longer rejects valid underside placement because of the legacy `side` argument after `ItemBlock` has already offset the target cell. Placement now keys from the actual support block above.
- Sea Pickles can be stacked in-place from 1 to 4 by using another Sea Pickle on the block. Metadata keeps the bounded live/dead bit while bits 0..1 advance the count; light and bounds update with the count. Normal placement also requires floor support.
- Bone Meal (`minecraft:dye` damage 15 in 1.7.10) now advances ancient crop state without introducing the later natural-growth system:
  - Torchflower Crop advances age 0 -> age 1 -> the existing Torchflower block.
  - Pitcher Crop advances by one age per use, creates the upper half at age 3, and keeps lower/upper ages synchronized through age 4.
- Creaking Heart state, Dried Ghast hydration, and Sniffer Egg hatch remain import/persistent visual states only in this pass. Their environment/entity lifecycle transitions are Pass-35-or-later functionality, not a Pass-34c prerequisite.


## Pass 34d runtime corrections

- Pale Hanging Moss now uses a dedicated `ParityPaleHangingMossItemBlock` for true downward-only placement. Clicking the underside of a non-replaceable support (including foliage) places a `tip=true` block below it; clicking the underside of the current moss chain extends downward and converts the former tip to body.
- Mature `minecraft:pitcher_plant` was found to have its own visible `half=lower/upper` source property, distinct from `minecraft:pitcher_crop`. EFR now stores this as metadata 0/1, prepares the exact `pitcher_plant_bottom` / `pitcher_plant_top` source models through AssetDirector, places both halves from the obtainable item, and removes the paired half safely on break.
- The global visual-state classifier and Backporter contract now explicitly cover `pitcher_plant.half`, closing the validator hole that allowed the one-block mature Pitcher Plant shell through Pass 34.
