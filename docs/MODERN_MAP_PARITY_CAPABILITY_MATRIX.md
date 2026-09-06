# Modern Map Parity Capability Matrix

Et Futurum Requiem Plus exposes 244 stable modern registry identities for map conversion. A valid
AssetDirector-backed model is not, by itself, proof that every modern block state or mechanic is
represented.

`scripts/audit_modern_map_parity_capabilities.py` generates one machine-readable row for every
identity in `scripts/modern_map_parity_blocks.json`. Each row records the parity registry's creation
path, any already-resolved mature EFR equivalent, visual path, state storage, shape, mechanics, NBT
status, map-import status, review level and known difference. `parity_create_path` describes what the
parity registry would create; it does not claim that this path supersedes an older EFR block.

Run the complete JSON report:

```bash
python3 scripts/audit_modern_map_parity_capabilities.py --format json
```

Run a spreadsheet-friendly TSV report:

```bash
python3 scripts/audit_modern_map_parity_capabilities.py --format tsv
```

Validate that all manifest identities remain classified:

```bash
python3 scripts/audit_modern_map_parity_capabilities.py --check
python3 scripts/validate_fidelity_pass_29.py
python3 scripts/validate_fidelity_pass_30.py
python3 scripts/validate_fidelity_pass_31.py
```


## Pass 31 wall-state contract

Wall state is now resolved by the shared `ModernWallState` helper for both mature `BaseWall`
families and parity walls. Horizontal properties use the modern `none` / `low` / `tall` domain and
`up` is derived from the same neighbourhood. Resin Brick Wall prepares all 162 Mojang wall model
combinations (`2 * 3^4`) instead of reducing every connection to `low`.

These properties remain importer-derived rather than persisted: Backporter/structure input keeps the
wall identity and subtype metadata, discards `north/east/south/west/up`, and lets the placed world
recompute the natural modern state. Waterlogging and explicit impossible/debug-stick overrides remain
outside Pass 31. See `docs/FIDELITY_PASS_31_MODERN_WALL_STATES.md` for the bounded contract.

## Pass 30 multiface contract

Sculk Vein and Resin Clump share one six-face state implementation. The state is stored by
`ParityMultifaceTileEntity` as the integer NBT field `FaceMask`; metadata is normalized to zero.
Bits are stable and importer-facing: bit 0 `down`, bit 1 `up`, bit 2 `north`, bit 3 `south`, bit 4
`west`, and bit 5 `east`. Backporter imports must set one or more bits from the six modern boolean
blockstate properties. A zero mask is not a valid placed state.

Each active face requires a solid supporting face in its direction, renders the matching Mojang
blockstate combination, and contributes a 1/16-thick outline/ray target. Unsupported faces are
pruned after neighbour changes; the block is removed when none remain. Resin Clump drops one item
per active face. Sculk Vein drops one item per active face only under Silk Touch. Waterlogging and
the blocks' larger spreading/growth systems remain outside Pass 30.

Pass 30b keeps that state contract unchanged and corrects only the legacy JSON bridge boundary:
Mojang's `down=true`/`x=90` and `up=true`/`x=270` model rotations are exchanged when selecting the
legacy baked model because the bridge's positive X rotation convention is reversed. This does not
invert `FaceMask`, placement, support checks, bounds, drops, synchronization or Backporter input.

## Pass 29 axis contract

Pass 29 verifies the complete non-waterlogged `axis` representation for:

- `etfuturum:pale_oak_log`
- `etfuturum:stripped_pale_oak_log`

The deterministic mapping for placement and map conversion is:

| Modern state | EFR metadata | Placement faces |
| --- | ---: | --- |
| `axis=y` | `0` | top or bottom |
| `axis=x` | `1` | east or west |
| `axis=z` | `2` | north or south |

Metadata `3` is invalid/reserved and is rendered as the safe vertical/Y fallback. Inventory,
dropped-item and pick-block stacks always use canonical item damage `0`; the axis applies only to
the placed block.

## Pass 29b stripping contract

Using an item tagged as an EFR stripped-log tool converts every valid Pale Oak Log axis to the
matching Stripped Pale Oak Log axis. The conversion is metadata-preserving: `0 -> 0`, `1 -> 1`
and `2 -> 2`, so removing the bark cannot rotate a vertical, east-west or north-south log. Pale Oak
Wood continues to convert to Stripped Pale Oak Wood when bark-log support is enabled.

General waterlogging remains outside this contract. Later passes must promote additional rows only
after their exact 1.21.11 properties, storage, renderer behaviour and Backporter mapping are
implemented and validated.

## Pass 32 block-entity/import contract

Pass 32 closes the reverse-engineering gap for persistent parity families.  The machine-readable target mapping is `docs/BACKPORTER_STATE_CONTRACT.json`; it separates source properties from EFR registry identity, metadata and tile-entity NBT, and records explicit partial/unsupported state rather than treating implemented families as blanket `backport_close` approximations.

The capability audit now marks Chiseled Bookshelf, Decorated Pot, Campfire/Soul Campfire, Suspicious Sand/Gravel, every parity Shelf, parity Sign/Hanging Sign, and all eight Copper Chest identities as `PASS_32_CONTRACT_VERIFIED`.  Cave Vines, banners, shulkers, barrels, beehives, furnace variants and other mature tile entities are covered by the same contract/audit even though they are not all entries in the 243-block parity manifest.


## Pass 33 small visible-state contract

Pass 33 adds the technical `copper_wall_torch` identity and verifies exact map-state representation for Pale Oak Button, Pale Oak Pressure Plate, Copper Torch/Wall Torch, Decorated Pot `Cracked`, and all Candle Cake `lit` states. The new global state scanner is `scripts/validate_modern_visual_state_coverage.py`; strict mode treats previously-unclassified model-changing properties as `UNSUPPORTED` so Passes 34–36 can close the remaining debt systematically.

## Pass 34 remaining visible-state contract

Pass 34 promotes Pale Moss Carpet, Pale Hanging Moss, Creaking Heart, Dried Ghast, Torchflower Crop,
Pitcher Crop, Pitcher Plant, Sniffer Egg, Mangrove Propagule, Sea Pickle and Tall Seagrass to `PASS_34_VERIFIED`.
Pale Moss Carpet stores all 162 `bottom × north/east/south/west` combinations in a synchronized tile
entity; the other new parity-shell states fit in metadata except Mangrove Propagule, which extends the
existing mature `etfuturum:sapling` metadata-0 implementation with `Hanging` and `Age` tile data.

Sea Pickle metadata bits 0..1 store `pickles-1`; bit 2 is a deliberately bounded live/dead visual bit
copied from the source `waterlogged` property. It selects Mojang's live/dead model and 6/9/12/15 light
for one through four live pickles, but does not create water/fluid occupancy and is not general
waterlogging support.

The global model-state audit also confirmed that Tall Seagrass `half` must be stored, while Kelp,
Kelp Plant and the separate Open/Closed Eyeblossom identities have no model-selecting blockstate
properties in Minecraft 1.21.11. Turtle Egg and Leaf Litter/Wildflowers were already exact and were
left regression-sensitive rather than reworked.

## Pass 35 complex / technical visible-state contract

Pass 35 promotes Copper Golem Statues, Trial Spawner, Vault, Crafter, Bell, Respawn Anchor, Sculk
Sensor, Calibrated Sculk Sensor, Sculk Shrieker, Jigsaw, Repeating/Chain Command Blocks and Structure
Block to `PASS_35_VERIFIED`. The vanilla 1.7 Command Block is audited separately because it is not a
parity-manifest identity; a minimal TE/mixin extension preserves modern facing and conditional model
state without repurposing its powered metadata or rewriting command execution.

Vault and Crafter use tiny synchronized visual-state tile entities only where the complete modern
state matrix cannot fit in four metadata bits. Copper Golem Statue pose/facing, Bell attachment,
Respawn Anchor charges, Sculk phases, Jigsaw orientation, technical command-block orientation, and
Structure Block mode remain metadata-backed. `docs/BACKPORTER_STATE_CONTRACT.json` revision 35 is
the authoritative importer mapping.

Large functional systems and general waterlogging remain explicitly deferred.
