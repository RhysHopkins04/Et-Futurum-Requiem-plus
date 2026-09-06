# Fidelity Pass 35 — complex / technical visible-state parity

Pass 35 is a **map-state fidelity** pass for Minecraft Java 1.21.11. It preserves persistent
block properties that select a visible model, orientation, light state, or other imported visual
state without implementing the corresponding large gameplay systems.

General waterlogging remains intentionally unsupported.

## Deterministic state layouts

### Copper Golem Statue family

Applies to all eight weathering/waxed registry identities.

`meta = pose * 4 + facing`

- facing: north=0, east=1, south=2, west=3
- pose: standing=0, sitting=1, running=2, star=3

All 16 combinations are preserved by item metadata for pick/drop/re-placement. The block renderer
uses the correct weathering texture and four separately authored 1.21.11 Copper Golem model layers:
Standing, Sitting, Running, and Star. Pass 35b flattens each layer's own cuboids, part transforms, cube
deformations, and 64x64 entity-texture UV origins; Sitting/Running/Star are **not** transformed copies
of Standing. Copper Golem entity behaviour and oxidation/waxing/scraping are deferred.

### Trial Spawner

`meta = ominous * 6 + trial_spawner_state`

- ominous: false=0, true=1
- state: inactive=0, waiting_for_players=1, active=2,
  waiting_for_reward_ejection=3, ejecting_reward=4, cooldown=5

All 12 persistent combinations are retained and resolve the 1.21.11 AssetDirector blockstates.
Static model sharing is intentional: for each normal/ominous family `inactive` shares with `cooldown`;
`waiting_for_players`, `active`, and `waiting_for_reward_ejection` share; `ejecting_reward` is distinct.
This visual reuse must never be treated as collapsed persistent state. Pass 35d also preserves the
1.21.11 emitted-light table for the stored state: inactive=0, waiting_for_players=4, active=8,
waiting_for_reward_ejection=8, ejecting_reward=8, cooldown=0. Ominous does not alter that table.
Spawning, rewards and challenge state-machine mechanics are deferred.

### Vault

Metadata stores facing and ominous:

- bits 0..1: north=0, east=1, south=2, west=3
- bit 2: ominous

`ParityVaultStateTileEntity` stores `VaultState`:

- inactive=0
- active=1
- unlocking=2
- ejecting=3

This covers all 32 combinations. Pass 35d makes emitted light follow the 1.21.11 VaultState
values: inactive=6 and active/unlocking/ejecting=12. VaultState setter/sync/load paths explicitly
recalculate 1.7.10 block light rather than relying on a render update. Key/reward/player tracking is
deferred.

### Crafter

Metadata 0..11 stores the modern FrontAndTop orientation in this stable order:

0. down_east
1. down_north
2. down_south
3. down_west
4. east_up
5. north_up
6. south_up
7. up_east
8. up_north
9. up_south
10. up_west
11. west_up

`ParityCrafterStateTileEntity` stores the booleans `Triggered` and `Crafting`. Together this covers
all 48 persistent orientation/boolean combinations. Static models may intentionally be shared: the
1.21.11 `crafter_crafting_triggered` model inherits `crafter_crafting` without an additional visual
override, so `crafting=true, triggered=false/true` can legitimately render identically. Both booleans
remain stored exactly. No Crafter inventory, recipes, or redstone crafting is added here.

### Bell

`meta = attachment * 4 + facing`

- facing: north=0, east=1, south=2, west=3
- attachment: floor=0, ceiling=1, single_wall=2, double_wall=3

All 16 models are preserved. Normal placement checks the correct floor/ceiling/wall support. A
runtime single-wall bell upgrades to double-wall when the opposite support appears; a double-wall
bell with one support removed degrades to the corresponding single-wall state, and with no remaining
support it drops. These reciprocal support transitions preserve the existing facing/attachment metadata
contract. `powered` is gameplay-only/visually irrelevant in 1.21.11 and is not consumed for map
appearance. Ringing and raid behaviour remain deferred.

### Respawn Anchor

Metadata is exactly `charges=0..4`. The five models are preserved and EFR emits light levels
0/3/7/11/15 for charges 0..4. Charging, respawn and explosion mechanics are deferred.

### Sculk Sensor

Metadata:

- inactive=0
- active=1
- cooldown=2

The persistent phase is retained even though active and cooldown intentionally reuse the active
1.21.11 static model. In 1.21.11 the block light level is **1 in every phase**; only the
`emissiveRendering` predicate is restricted to `ACTIVE`. Pass 35d deliberately preserves that
constant emitted-light behaviour rather than inventing a 0/1/0 phase-light table. `power` is visually
irrelevant and vibration/redstone behaviour is deferred. Waterlogging remains unsupported.

### Calibrated Sculk Sensor

`meta = phase * 4 + facing`

- facing: north=0, east=1, south=2, west=3
- phase: inactive=0, active=1, cooldown=2

All 12 combinations are preserved. The calibrated sensor inherits the normal Sculk Sensor block
properties in 1.21.11, including constant emitted block light level 1; facing and phase therefore do
not change emitted light. Filtering/vibration/redstone mechanics are deferred.

### Sculk Shrieker

- metadata bit 0: `can_summon`
- metadata bit 1: `shrieking`

`can_summon` selects the correct source model. `shrieking` is deliberately stored rather than
collapsed even though the 1.21.11 static blockstate model does not select a different model for it.
Waterlogging and Warden/shriek gameplay remain deferred.

### Jigsaw

Metadata 0..11 uses the same FrontAndTop ordering as Crafter. All twelve Mojang blockstate rotations
are preserved. Structure generation is deferred.

### Command blocks

`minecraft:command_block` remains the vanilla 1.7 block. Its existing metadata bit remains owned by
vanilla powered/execution behaviour. A minimal tile-entity mixin adds:

- `EFRModernFacing`: byte 0..5 = down/up/north/south/west/east
- `EFRConditional`: boolean

The normal Command Block renderer reads these fields through `IModernCommandBlockState`. Existing
command text/output NBT and 1.7 execution are untouched.

`etfuturum:repeating_command_block` and `etfuturum:chain_command_block` use:

`meta = conditional * 6 + facing`

with facing down/up/north/south/west/east = 0..5. Their modern command execution is not implemented.

### Structure Block

Metadata stores mode:

- save=0
- load=1
- corner=2
- data=3

Only exact visible model state is implemented. Structure save/load/template execution is deferred.
Structure Void has no additional model-selecting state in the audited 1.21.11 blockstate data.

## Pass 35d dynamic-light closeout

Pass 35d removes the old constant registration light from Trial Spawner and Vault and routes both
through `ParityModelBlock#getLightValue(IBlockAccess,...)`. Placement/import block insertion triggers
a block-light recalculation. Pass 35d1 keeps immediate relighting for real Vault state mutation and
installed client packet application, but defers validation/NBT-load relighting until the TE is fully
installed. Direct `updateLightByType` from `TileEntity.validate()` or `readFromNBT()` is forbidden: the
light query reads `VaultState` through `World#getTileEntity`, so relighting during TE installation can
re-enter validation and overflow the stack on world join. No gameplay state machines are added.

The Sculk Sensor families are intentionally **not** added to this dynamic-light path: the audited
Minecraft Java 1.21.11 `Blocks` registration gives Sculk Sensor a constant `lightLevel(state -> 1)`,
and Calibrated Sculk Sensor copies those properties. Their phase-dependent emissive rendering is a
separate rendering property, not emitted block light.

## Persistent state versus static model identity

Pass 35/35b treats these as separate concepts. A source property remains preserved when its static
model is intentionally shared with another state. Validators must therefore protect the persistent
encoding and the Mojang model reuse at the same time; they must not manufacture fake visual variants
for Trial Spawner, Crafter, Sculk Sensor, or Sculk Shrieker merely to make stored states look unique.

## Global audit classifications

Pass 35 extends `scripts/validate_modern_visual_state_coverage.py` for these families. Persistent
properties omitted from model-selection JSON are explicitly audited too, including Copper Golem
Statue facing/pose, Bell powered, Sculk power fields, and Sculk Shrieker shrieking.

Every Pass-35 model-affecting property resolves as `STORED_EXACTLY`. Gameplay-only properties are
reported as `VISUALLY_IRRELEVANT`, while general `waterlogged` remains the project-wide explicit
`UNSUPPORTED` exception.

## Deferred functional work

Pass 35 intentionally does not implement Trial Spawner spawning/rewards, Vault keys/rewards,
Crafter inventory/crafting, Bell ringing, Respawn Anchor gameplay, Sculk vibration/redstone/Warden
mechanics, modern command execution, structure generation, Copper Golem entities, oxidation, or
general waterlogging.
