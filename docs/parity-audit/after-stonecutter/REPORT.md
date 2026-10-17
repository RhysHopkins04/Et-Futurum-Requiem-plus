# Functional stonecutter — 3.5.20

The existing stonecutter now opens a working selection interface and consumes one input per
recipe operation. Its registered block/item names, facing metadata and numeric IDs are retained.
GUI ID 8 is appended to the existing IDs. No tile entity, world migration or backporter change
is introduced: the input inventory belongs to the player while the screen is open.

## Recipes and interface

- All **254 Minecraft Java 1.21.11 stonecutting recipes** are included, with exact registered
  item identities, metadata and output quantities. The catalog is generated from the pinned
  vanilla jar and the reviewed item crosswalk; it never guesses numeric block IDs.
- Missing/disabled input or output registry identities are omitted in feature-off configurations.
  Stonecutter-disabled and legacy content-only profiles expose no stonecutting recipes.
- The interface uses the modern background and selection sprites: four columns, three visible
  rows, output tooltips, mouse-wheel scrolling and a draggable scrollbar. Selection starts empty
  and resets when the input's identity/metadata/NBT changes.
- Selection uses the vanilla window/button packet and is checked against the current recipe
  choices, player ownership, block presence and distance. Recipe counts are bounded for that
  packet's signed-byte selection field.
- Native NEI recipe/usage views show stonecutting inputs, outputs/counts and the stonecutter
  catalyst. They are separate from crafting-grid and fuel categories. No inaccurate grid
  transfer button is added.
- The decorative-only tooltip is suppressed for an enabled functional stonecutter, while the
  remaining decorative workstations keep their existing descriptions.

## Item ownership and extraction

Left/right extraction takes a complete recipe result and consumes one input. Shift-clicking
can repeat the operation while inventory space remains. A two-slab output requires space for
both slabs; a nearly-full inventory cannot consume a block for only half the result. Invalid
selections and stale previews cannot craft. Double-click collection/dragging cannot inject into
or collect the generated result as an ordinary storage slot.

Closing returns the remaining input to the player's inventory, or drops it when the inventory
is full. The preview result is cleared without being awarded. Players using the same block have
separate input inventories. Destroying the workstation or moving out of range prevents extraction.

## Validation

`StonecutterProbe` tests every route through actual left- and right-click extraction, then tests
bulk shift-click, completely/full-partial inventories, input routing, invalid button/slot IDs,
changed inputs, distance/block removal, remaining input NBT, repeated close, drop fallback,
slot/progress synchronization and player isolation. It runs without NTM and with NTM 2.4.0 plus
Iron Chests 6.1.6 alongside the existing crafting, cooking, ore/tool/hazard and save/restart tests.

The complete witness audit now uses **4,780 runtime queries** and has **1,289 matching recipe
definitions**, up from 1,035. The 254 new matches are stonecutting routes queried through actual
container selection/extraction. No previously matching recipe regressed. This remains a witness
and focused-contract metric, not certification of every mechanic in the mod.

Feature/profile results, source hashes and the distributable checksum are in `VALIDATION.json`.
The new interface compiles and its assets/layout coordinates were checked against the source
textures. A real visual client check is still required; a server cannot verify its rendering.

## Focused client check (about one minute)

1. Place/open a stonecutter. Put stone in the input, scroll the choices and select a slab.
   Confirm the output count is two and taking it consumes one stone.
2. Shift-click a small batch, then change the input to a different material. The old selection
   should clear. Close with input remaining and confirm it returns to your inventory.
3. View a stonecutting result in NEI and confirm the stonecutting category displays its input,
   output count and stonecutter icon legibly.

The wider implementation programme remains in `../RESOLUTION_PLAN.md`. Loom/cartography,
grindstone/lectern, modern smithing templates/trims and the other production/gameplay gaps are
not completed by this patch. Nothing was committed or pushed.
