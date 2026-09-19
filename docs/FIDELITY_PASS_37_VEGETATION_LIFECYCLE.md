# Pass 37 — Vegetation / Plant Lifecycle Functional Parity

Pass 37 makes the non-entity vegetation identities already represented by Passes 34–35 behave as bounded
Minecraft Java 1.21.11 blocks when manually placed, randomly ticked or bonemealed. It does not enable biome
world generation, structures, general waterlogging, or any lifecycle endpoint which spawns an entity.

## Aquatic compatibility

Minecraft 1.7 `ItemBlock` cannot normally replace water, so Pass 37 uses `ParityAquaticItemBlock` only for the
covered aquatic parity blocks. Kelp, Seagrass and Tall Seagrass require source water; Sea Pickles and Coral
preserve whether their occupied cell contained source water using the smallest existing metadata-compatible bit.
Breaking/support removal restores source water where that bounded state says it should. Vanilla water itself is
not made globally replaceable.

## Kelp / Kelp Plant

Kelp heads retain modern `AGE=0..25` in `ParityKelpStateTileEntity`; body blocks require no tile entity. A new
head is initialized with age 0..24, random growth succeeds with probability 0.14 while age < 25, and Bone Meal
extends the current column by one block. Growth converts the old head to Kelp Plant and creates the new aged
head above. Exposing a Kelp Plant body at the top promotes it to a new Kelp head. Breaking through a column
restores water and promotes/recalculates the new tip without duplicating drops.

## Seagrass / Tall Seagrass

Seagrass requires source water, sturdy underwater support and excludes Magma Block support. Bone Meal converts
valid Seagrass into the canonical Tall Seagrass lower+upper pair. Tall Seagrass placement requires two source
water cells; loss/break of either half removes the counterpart through neighbour-update ownership and restores
water. Shears drop one Seagrass from the short form and two from Tall Seagrass; ordinary breaking has no drop.

## Coral families

All five colours are covered for coral block, coral, coral fan and coral wall fan. Live forms scan their modern
water requirement and schedule death after `60 + random.nextInt(40)` ticks when dry. On the scheduled tick they
become the matching dead identity if still dry. Wall fans preserve horizontal facing through death. Plant/fan
support and wall-fan backing support are validated; source water is restored for bounded aquatic placement.

## Sea Pickles

Metadata keeps Pass 34's `pickles=1..4` and bounded live/source-water bit. Same-item placement increments to four;
drops equal the pickle count. Live pickles emit 6/9/12/15 light for counts 1..4. Bone Meal is valid only for a
live pickle on a Coral Block and follows the bounded modern 5-wide spread algorithm, then sets the targeted
pickle cluster to four. Breaking a live/source-water pickle restores water.

## Torchflower / Pitcher crops

Torchflower Crop keeps visible age 0/1. It uses modern farmland growth-speed logic and the modern extra
`random.nextInt(3) != 0` gate, progressing age 0 -> age 1 -> the mature Torchflower block. Bone Meal advances one
logical age. Immature crops drop one Torchflower Seed; the new planting item places age 0 on farmland.

Pitcher Crop keeps age 0..4 and lower/upper representation. Growth uses the modern crop-speed probability and
Bone Meal adds exactly one age. The upper half first appears at age 3 and is synchronized with the lower half.
Age 4 remains Pitcher Crop, matching 1.21.11; the lower mature crop drops a Pitcher Plant item while ages 0..3
drop one Pitcher Pod. Breaking/removing either required half lets neighbour-update ownership remove the partner
without recreating a deliberately broken half or producing duplicate drops.

## Mangrove Propagule

The existing mature EFR Mangrove sapling implementation remains authoritative. Hanging propagules use the
existing `Hanging` + `Age` tile data, require Mangrove Leaves above, age 0..4 on random ticks, and advance one age
per Bone Meal. Mangrove Leaves are bonemealable when the cell below is air and create a hanging age-0 propagule.
A placement guard initializes the tile data before legacy sapling survival callbacks can incorrectly destroy the
new hanging propagule.

Planted propagules keep normal sapling stage behavior and may grow on the modern-compatible EFR dirt/clay/mud
set. Manual/bonemeal growth calls `WorldGenPass37MangroveTree`; the generator is never registered with biome
world generation. It preserves the modern 85% tall-tree selection bias and may attach hanging propagules to its
leaves.

## Pale Oak Sapling / Leaves

Pale Oak Saplings use standard modern sapling timing: sufficient light, 1-in-7 random advancement, stage 0 -> 1,
and 45% Bone Meal success. Tree growth requires a 2x2 sapling arrangement. `WorldGenPass37PaleOakTree` is a
manual-only 1.7 canopy-tree geometry compatibility generator using Pale Oak Log/Leaves and no Creaking/Pale
Garden decorators. Only leaves created by this generator carry the bounded decay marker; imported/player-placed
Pale Oak Leaves remain persistent.

## Pale Hanging Moss / Pale Moss Carpet

Pale Hanging Moss preserves Pass 34 `tip`. Support/chain neighbour updates derive the tip only when an actual
update occurs. There is no random downward growth. Bone Meal locates the chain tip and extends it downward one
block into air.

Pale Moss Carpet preserves its exact Pass-34 `bottom` and four none/low/tall side states. Placement and support
updates remove/derive only modern-update-owned sides; arbitrary supported imported representations are not
wholesale rebuilt. Bone Meal on a base creates the supported top side-only layer where possible.

## Leaf Litter / Wildflowers

Both keep amount 1..4 plus facing. Same-item interaction adds one segment up to four and does not consume in
Creative. Drops equal the stored amount. Leaf Litter has no Bone Meal behavior in Java 1.21.11. Wildflowers Bone
Meal increases amount through four and, when already at four, drops one additional Wildflowers item.

## Eyeblossom

Open and Closed Eyeblossom remain separate registry identities. In the ordinary Overworld timeline, the desired
state is open for `12600 <= dayTime < 23401`; legacy dimensions without a 1.21.11 environment-attribute analogue
retain their imported identity. Random transitions play the long open/close sound; propagated scheduled
transitions play the short sound. A transition schedules matching old-state neighbours in X/Z ±3 and Y ±2 with
delay chosen inclusively from distance*5 through distance*10 ticks. Transform particles use a bounded 1.7
particle approximation, and the versioned modern open/close/idle sound assets are explicitly requested.
Bee-specific poison/effect behavior is intentionally deferred.

## Frozen / deferred boundaries

Pass 34/35 static map-state mappings remain compatible. Pass 37 does not implement Turtle/Sniffer/Frogspawn/
Dried-Ghast/Creaking entity endpoints, Powder Snow entity physics, Copper Golem entities, Sculk vibration/Warden
systems, Crafter/Trial Spawner/Vault gameplay, biome or structure generation, or general waterlogging.

`docs/BACKPORTER_STATE_CONTRACT.json` is revision 37 with unreleased version 3.5.9 and no self-referential git ref.

## Pass 37b runtime corrections

Runtime testing of the first Pass 37 package exposed one legacy update-order defect and one
modpack chunk-loading compatibility defect. These corrections remain part of contract revision 37
and do not broaden the vegetation scope.

- Pitcher Crop growth now mirrors the modern two-block publication order: the lower age is
  synchronized with update flag 2 before the age-matched upper half is created/updated with flag 3.
  This prevents 1.7 neighbour callbacks from observing age 4 below an age-3 upper half and
  recursively deleting the pair. Bone Meal and random growth share the same `growPitcher` path.
- Pitcher growth/survival uses the modern sufficient-light threshold and mature Pitcher Plant uses
  vegetation soil rather than accepting arbitrary solid-top blocks.
- Mature Torchflower is constrained to vegetation soil and receives support-loss handling; the
  two crop states remain farmland-only and still mature after two one-stage Bone Meal applications.
- Dry Sea Pickles deliberately remain placeable on valid support with no emitted light/glowing tips;
  this is the modern `waterlogged=false` state, not a failure. Kelp and Seagrass remain source-water
  plants, while Coral retains its scheduled live-to-dead drying behaviour.
- `EtFuturumWorldListener.markBlockForUpdate` now refuses to dereference blocks in chunks which are
  not already loaded. Basalt/bubble-column neighbour probes use the same bounded rule. This prevents
  chunk-loader TileEntity activation callbacks from recursively forcing the chunk currently being
  loaded and does not alter chunk loading, tickets, or ChickenChunks itself.

Mangrove Propagules have no adjacency prohibition in the Pass 37 code. Manual tree growth keeps the
modern `TreeGrower.MANGROVE` 85% tall-feature selection bias, so a short test run is expected to show
mostly tall, similar-looking trees even though height, crown omissions, roots and propagule decoration
remain randomized by `World.rand`.

## Pass 37c aquatic/runtime follow-up

Runtime testing after 37b exposed a shared 1.7 liquid-flow incompatibility rather than four
independent survival bugs. Vanilla 1.7 `BlockDynamicLiquid` treats `Material.plants` as flow-through
space. The first Pass-37 aquatic bridge correctly replaced a source-water cell with Kelp, Seagrass,
Sea Pickles or coral foliage, but neighbouring water could then immediately flow back into that cell
and overwrite the plant. Pass 37c gives only the contained-water aquatic parity identities the
non-colliding vanilla `Material.coral` flow barrier. Their collision/rendering remains authored by the
parity block, and breaking them still restores source water. This is deliberately not a general
waterlogging implementation.

Additional runtime corrections in 37c:

- direct Kelp Plant placement, like Kelp/Seagrass, requires source water;
- Tall Seagrass direct placement and Seagrass -> Tall Seagrass Bone Meal publish the lower half with
  update flag 2 before publishing the upper half with flag 3, preventing 1.7 from validating the
  lower half before its mate exists;
- custom-rendered full-cube parity blocks (notably live/dead Coral Blocks) count as sturdy supports
  for aquatic floor/wall plants even though Forge's default `isSideSolid` rejects blocks rendered
  through the JSON parity renderer;
- Sea Pickles continue to allow a supported dry state with zero emitted light, while a source-water
  placement retains its bounded live/water bit and is protected from legacy water-flow replacement;
- Pitcher Crop keeps the exact modern lower-half-only loot contract: lower age 0..3 drops one Pitcher
  Pod, lower age 4 drops one Pitcher Plant, and an upper-half break drops nothing. Creative pick-block
  now resolves Pitcher Crop to the Pitcher Pod item instead of exposing the technical crop BlockItem;
- Pale Moss Block now owns its 1.21.11 block-local Bone Meal patch interaction using the modern
  PALE_MOSS_PATCH_BONEMEAL radius/edge/vegetation probabilities without registering biome generation.
  Pale Moss Carpet remains bonemealable only when a valid supported side-only topper can actually be
  created, matching `MossyCarpetBlock.isValidBonemealTarget`;
- Eyeblossoms receive a 20-tick compatibility schedule while loaded so 1.7 reacts reliably to
  `/time set` and normal day/night changes despite lacking 1.21.11 environment timelines. The
  transition still uses the exact Overworld 12600..23400 open interval and distance-delayed neighbour
  propagation. Transform particles are now sent by `WorldServer` so dedicated clients can see them.

Mangrove hanging propagules deliberately remain attached at age 4; modern `MangrovePropaguleBlock`
stops aging there and only removes the propagule when its Mangrove Leaves support is lost. Planted
Mangrove waterlogging is not broadened here because the mature EFR Mangrove/Cherry saplings share one
legacy block identity and Pass 37 still excludes a general waterlogging rewrite.

## Pass 37d Sea Pickle support follow-up

Runtime testing after 37c confirmed the shared aquatic flow fix, but exposed one remaining placement-only
mismatch for Sea Pickles on EFR's custom-rendered Coral Blocks. Survival updates already used the parity-aware
`canSupportPlantTop` bridge, while initial Sea Pickle placement still called Forge's raw `world.isSideSolid`.
Because parity Coral Blocks are full authored cubes rendered through the JSON bridge, Forge reports them
non-solid even though modern Java treats their top face as sturdy.

Pass 37d routes initial Sea Pickle placement through the same `canSupportPlantTop` helper used by survival.
This allows live underwater or dry supported Sea Pickles on Coral Blocks without changing their water/light
semantics. Bone Meal remains valid only for a live Sea Pickle directly above a Coral Block; when valid it sets
the source pickle count to four and performs the bounded modern nearby spread attempt.

The observed Pitcher behavior remains intentional: Pitcher Crop pick-block resolves to Pitcher Pod at every
crop age, only the lower crop half owns loot, ages 0..3 lower drop Pitcher Pod, and age 4 lower drops Pitcher
Plant. Breaking an upper crop or upper mature Pitcher Plant half does not independently duplicate the lower-half
drop. Pale Moss Carpet Bone Meal likewise remains conditional: a base carpet is a valid target only when a
side-only topper can actually survive one block above against continuing wall support.


## Pass 37e Coral Wall Fan placement follow-up

Runtime testing after 37d exposed a 1.7 placement-order race specific to the obtainable Coral Fan item.
Horizontal use correctly selected wall support, but the aquatic ItemBlock first published the floor-fan block
with a horizontal metadata marker and relied on `onBlockPlacedBy` to replace it with the technical wall-fan
identity. A flag-3 `setBlock` can run neighbour support validation before that callback; with no block below,
the temporary floor fan correctly failed its own floor-support rule and disappeared even though the clicked
wall was valid.

Pass 37e makes horizontal Coral Fan placement atomic: after validating the clicked wall face, the item publishes
the matching technical `_coral_wall_fan` identity directly with the same facing and bounded source-water bit.
Floor placement is unchanged, wall-fan map/import identities remain unchanged, drops/pick-block still resolve to
the single obtainable Coral Fan item, and no block beneath a valid wall-mounted fan is required. Subsequent
support loss continues to use the wall-fan's normal directional survival rule.

## Pass 37f Coral Fan client placement preflight follow-up

Runtime testing of 37e confirmed that atomic wall-fan publication alone was not sufficient on a
real 1.7 client. Before `ItemBlock.onItemUse` runs, `PlayerControllerMP` calls the inherited
`ItemBlock.func_150936_a` preflight. That path reaches `World.canPlaceEntityOnSide`, then
`Block.canReplace`, then the obtainable Coral Fan block's `canPlaceBlockOnSide`. Because the
Coral Fan had no side-aware override there, legacy `Block.canPlaceBlockOnSide` fell through to
`canPlaceBlockAt`, which correctly applied the floor-fan support rule. The client therefore refused
to send a placement packet for a valid wall-mounted fan whenever the target cell had no supporting
block below it.

Pass 37f exposes the same side-aware `canSurviveAtPlacement` predicate through
`canPlaceBlockOnSide` for the obtainable Coral Fan identity. Horizontal faces validate only the
clicked sturdy wall face; floor placement continues to validate its sturdy block below. The 37e
atomic technical `_coral_wall_fan` publication remains unchanged. This is a placement-preflight
correction only: registry identities, map-import state, wall facing, water bit, support-loss behavior,
drops and pick-block behavior are unchanged, and Pass 38 remains untouched.

