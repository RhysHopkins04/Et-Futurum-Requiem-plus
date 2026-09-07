# Fidelity Pass 32 — Block-entity / import contract

Pass 32 establishes the stable data boundary between **Minecraft 1.21.11** map data and Et Futurum Requiem Plus on Forge 1.7.10.

The machine-readable source of truth is:

`docs/BACKPORTER_STATE_CONTRACT.json`

That file intentionally separates **block registry identity**, **1.7 metadata**, and **tile-entity id/NBT**.  Backporter implementations should consume the JSON rather than infer EFR internals from Java classes.

## Pass 32 implementation changes

### Chiseled Bookshelf

`ParityChiseledBookshelfTileEntity` now persists `LastInteractedSlot` as an integer.  `-1` means a legacy/never-interacted bookshelf and imported modern values map directly to `0..5`.  Successful player insertion and removal update the field and notify comparator neighbours.  Comparator output is `slot + 1` (`1..6`), or `0` when no slot has ever been selected.

The existing `Items` list and `OccupancyMask` remain unchanged.  An occupied bit without translatable source item NBT still creates the existing placeholder `minecraft:book`, preserving the imported visible occupancy.

### Cave Vines

`TileEntityCaveVines` now persists canonical `Age` in the modern `0..25` range.  Natural growth increments Age and cannot naturally extend a tip at Age 25.  `berries` remains the block metadata bit and therefore remains independent of Age.

`MaxLength` and `TipSheared` are retained for old EFR saves.  If an old tile has no `Age`, Pass 32 derives a bounded deterministic Age from its legacy `MaxLength`; missing legacy fields also receive safe defaults.  The tile now sends its full state in the standard TE update packet.

## Import rules worth calling out

- **Shelf:** metadata stores facing plus powered; the three slots are `Items`; `side_chain` is never imported because EFR derives it from neighbouring powered shelves with the same facing.
- **Decorated Pot:** the four `sherds` strings are in the actual EFR/modern order **back, left, right, front**.  The one stored stack is `item`.  EFR has no persistent cracked property.  Its metadata bit 2 is an EFR stored-water compatibility extension and is **not** a substitute for modern `waterlogged`.
- **Campfire:** four entries are stored in `CookingItems`; each entry contains a `Slot` byte, `CookTime` integer and ItemStack fields.  EFR uses a fixed total cook time of 600 ticks.  Signal-fire smoke is derived from hay directly below.
- **Suspicious Sand/Gravel:** `brush_count` is `0..3` and should match the imported dusted metadata.  `item` is the concrete buried stack.  EFR copies the full TE NBT into falling suspicious blocks; modern loot tables must be resolved by the Backporter to a concrete item before import.
- **Signs:** EFR stores front text in vanilla `Text1..Text4`, back text in `BackText1..BackText4`, plus per-side RGB/glow and `Waxed`.  Lines are plain strings with a 15-character EFR limit.  Rich JSON formatting is therefore explicitly partial fidelity.
- **Copper Chest:** the eight weathering/waxed identities are separate target block IDs. Inventory/name are vanilla `TileEntityChest` NBT, facing is vanilla chest metadata, and Pass 32c persists `EFRPairDirection` for exact single/left/right import. Oxidation/wax/scrape transitions remain Pass 34.
- **Banners:** all modern colour-specific banner IDs collapse to `etfuturum:banner`; colour is TE `Base`, standing/wall is `IsStanding` plus metadata, and patterns must be converted to EFR's legacy short IDs listed in the JSON contract.
- **Shulker Boxes:** all colour-specific IDs collapse to `etfuturum:shulker_box`; `Color`, `Facing`, `Items`, `Type=0` and optional `CustomName` are TE state, not merely a texture conversion.

## Exactness boundary

`FULL_IMPORT` means the persistent state described by the contract can be deterministically represented in EFR (assuming referenced items/entities themselves are translatable).  `PARTIAL_IMPORT` means the JSON explicitly lists the remaining loss.  `VISUAL_ONLY_IMPORT` means EFR presently has the block visual/identity but no equivalent persistent block-entity mechanics.

Waterlogging remains outside Pass 32 unless a block already had an unrelated EFR compatibility extension.

## Deliberately deferred

Pass 32 does not add the Crafter inventory/redstone system, Lectern book/page mechanics, full Sculk Sensor family, Copper oxidation/wax/scrape transitions, Trial Spawner/Vault, Conduit, Respawn Anchor, Lodestone, or a general waterlogging system.

## Pass 32b regression hardening

Runtime acceptance after Pass 32 exposed two assumptions that the original static contract did not gate strongly enough.

- Chiseled Bookshelf slot columns are front-relative for every facing. East/west hit coordinates are mirrored so visual slot 0..5 remains `top_left`, `top_mid`, `top_right`, `bottom_left`, `bottom_mid`, `bottom_right` as seen by the player facing the shelf. Inventory synchronization remains owned by the inventory mutation; `LastInteractedSlot` only dirties persistent/comparator state.
- Copper Chest pairing is exact-identity-only. Pass 32c stores the reciprocal relationship as `EFRPairDirection`; vanilla 1.7 raw type-based adjacency must not pair Copper Chests with normal/trapped chests or with another oxidation/wax block identity. The renderer uses the persisted pair plus facing to select the 1.21.11 single/left/right 64x64 chest texture contract.
  - **Pass 36 supersession:** runtime Copper Chest pairing now follows 1.21.11 copper-chest family semantics across oxidation/wax identities while still rejecting normal/trapped chests. `EFRPairDirection` remains the persisted reciprocal relation and lifecycle replacements preserve the TileEntity NBT.

The bookshelf rule remains an implementation invariant. Pass 32c promotes chest pairing to an explicit Backporter field because modern adjacent singles cannot be reconstructed safely from raw 1.7 adjacency alone.

## Pass 32c runtime repair — explicit chest pairing and bookshelf hand safety

Pass 32c replaces 1.7's raw same-type chest adjacency with an explicit persisted relationship for
normal Chests, Trapped Chests and the eight Copper Chest identities. `TileEntityChest` now carries
`EFRPairDirection` (`0=single`, `1=west`, `2=east`, `3=north`, `4=south`), where a double chest is
valid only when both halves point reciprocally at each other, have the same facing, and are the exact
same block identity. This is now part of the Backporter contract for vanilla/trapped and Copper
Chests rather than an adjacency-derived implementation detail. The mixin is deliberately bounded to
`minecraft:chest`, `minecraft:trapped_chest`, and EFR's Copper Chest block class; unrelated modded
`BlockChest` subclasses retain their own 1.7/mod-specific placement and inventory behaviour.

The runtime placement rules follow modern Java behaviour within the 1.7 interaction model: an
ordinary placement forms one compatible lateral pair; sneaking while placing keeps the new chest
single; a differently-facing adjacent chest remains independent; and additional single chests may
sit beside an existing single or double chest. Breaking either half immediately clears the surviving
half's pair state and forces its bounds/renderer/inventory back to single. Pre-Pass-32c saves without
`EFRPairDirection` migrate only when there is one unambiguous same-identity, same-facing lateral
partner, avoiding accidental pairing of modern adjacent singles.

Chiseled Bookshelf removal remains allowed while the player is holding another item, matching the
modern interaction model. Pass 32c now performs an explicit server inventory-container resync after
both insertion and removal so the held hotbar stack cannot be lost or left client/server-desynchronised
when the retrieved book is inserted into another inventory slot.

## Pass 32d chest placement/render responsiveness

Pass 32d tightens the remaining runtime behaviour without changing the persisted chest schema.

- **Secondary-use placement now follows modern Java's targeted rule.** Sneaking while placing on the
  horizontal face of a compatible *single* chest targets that exact chest, inherits its facing and
  creates a reciprocal double chest. Sneaking while placing against the floor, top face, a
  non-chest block, an incompatible chest identity, or an already-paired chest leaves the new chest
  independent.
- **Ordinary placement remains auto-pairing.** The clockwise lateral single candidate is preferred
  before the counter-clockwise candidate, matching modern `ChestBlock#getChestType` when a placement
  has two otherwise eligible adjacent singles.
- **Copper Chest unlink rendering is immediate.** Neighbour changes invalidate and resolve the
  explicit pair on the same client block update. If the partner is already absent, the client
  locally collapses `EFRPairDirection` to single while the server's normal TE packet remains the
  authoritative persisted state.

These rules apply only to vanilla normal/trapped chests and EFR Copper Chest identities managed by
Pass 32c/32d. They do not alter unrelated modded `BlockChest` subclasses.

## Pass 32e finalization

- Shelf comparator output is the occupied-slot bitmask: slot 0 = 1, slot 1 = 2, slot 2 = 4, for outputs 0..7 independent of stack size.
- Powered Shelf chaining and 3/6/9 hotbar grouping accept adjacent powered, same-facing Shelves of any wood variant; each Shelf keeps its own wood model.
- Pass 32e introduced the stable 3.5.5/tag provenance fields; Pass 32f supersedes its contract revision while retaining `implemented_in_version=3.5.5` and `refs/tags/3.5.5`. The stable tag is used instead of embedding the final commit SHA because a commit cannot contain its own hash.
- Waterlogging and unrelated later-pass mechanics remain outside Pass 32.

## Pass 32f Shelf hotbar synchronization

- Powered Shelf groups keep the modern rightmost-slot mapping: one Shelf swaps hotbar slots 6..8, two connected Shelves swap 3..8, and three connected Shelves swap 0..8. Two powered Shelves separated by an unpowered/differently-facing Shelf are independent one-Shelf groups, so either one still targets the rightmost three slots.
- Pass 32f makes the 1.7 exchange server-authoritative and synchronizes the complete player inventory after the direct hotbar mutation. This fixes the client-side ghost/duplication state where only the selected held slot visibly left the hotbar while the other swapped slots remained stale.
- The Shelf tile entity now exposes a no-update swap primitive so each Shelf publishes one block-entity update after all three of its slots have been exchanged, matching the modern mutate-then-update pattern more closely.
- Powered group swaps have the same 3/6/9 exchange in Survival and Creative. Unpowered single-slot Shelf placement preserves modern Creative behaviour when placing into an empty Shelf slot.
- The Backporter state contract revision is `32f`; the implementation version remains `3.5.5`.

