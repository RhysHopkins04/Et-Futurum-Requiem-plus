# Modern Map Parity Capability Matrix

Et Futurum Requiem Plus exposes 243 stable modern registry identities for map conversion. A valid
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
