# Functional loom and stonecutter lighting — 3.5.21

The stonecutter choice icons were drawn before GuiContainer supplied its inventory lightmap.
They now explicitly use the same 240/240 lightmap and rescaled normals as inventory slots.
The loom uses the same lighting setup. A real client check is still required to confirm the
appearance; dedicated-server tests cannot verify OpenGL output.

The exact official Minecraft 1.21.11 client jar (SHA-1
`ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd`) regenerates the unchanged 254-entry
stonecutting catalog byte-for-byte. Its ingredients, results and counts match vanilla.
The existing all-route extraction tests pass again.

## Loom behavior

The existing loom block opens a player-owned container with banner, dye, reusable template
and result slots. It offers 32 free patterns and 10 template patterns, matching the pinned
vanilla banner-pattern tags. Selection uses the standard window button packet and is
validated against current inputs, owner, block presence and distance. A template selects
its single pattern automatically. One extraction consumes one banner and one dye, while
retaining the template. The result copies the banner's base, name, custom data and existing
layers; six layers is the survival limit. Shift-click repeats while ingredients and inventory
space allow. Closing returns inputs and clears the preview without awarding it.

Existing persisted banner pattern IDs are retained. Globe, Piglin, Flow and Guster are
appended to the banner enum with their own modern masks and translations. Existing registry
names and block metadata are retained. Template variants use one new item with ten metadata
values, appended after existing ModItems entries. This avoids renumbering the existing enum
items. The backporter has not been changed by this pass.

The six craftable paper templates use the vanilla ingredients: vine, creeper head, bricks,
oxeye daisy, enchanted golden apple and wither skeleton skull. **Globe trading, Piglin
bartering, and Flow/Guster structure-loot acquisition remain unimplemented.** These four
items are available in creative and work in the loom; they have no invented crafting routes.
Existing crafting-grid banner routes remain available for compatibility.

NEI has a separate loom category with dye alternatives, reusable template and loom catalyst.
Queries for a patterned result reconstruct its previous layers rather than presenting an
unrelated white banner recipe. Banner usage queries preserve their base and existing layers.
There is no furnace fuel slot or misleading crafting-grid transfer.

## Evidence and limits

`LoomProbe` runs 672 real left/right extraction cases (42 patterns × 16 dyes), checks retained
templates and independent custom NBT, and round-trips the banner through tile-entity NBT with
its rendered pattern/color lists. Additional cases cover the six-layer limit, bulk shift-click,
full inventories, invalid selections/inputs, synchronization, repeat close, range and block
removal. The probe runs in a disposable Forge world, including with NTM 2.4.0 and Iron Chests
6.1.6, and after fresh-process restart. Legacy-profile suppression is checked separately.

The full modern recipe audit now makes 4,786 runtime queries: 1,295 recipe definitions match,
up from 1,289. All six newly matching definitions are template recipes. No previously matching
definition regressed. This metric is not certification of every existing feature.

`validate_loom_reference.py` independently checks free/template pattern membership and new
image bytes against the pinned official jar. Feature-off startup cases cover loom and banners
separately, alongside existing profiles. Detailed results and final hashes are in
`VALIDATION.json`, `CONFIG_MATRIX.json`, `LOOM_REFERENCE.json` and `COMPARISON.json`.

## Small client check

1. Open the stonecutter in the same location/lighting and compare recipe-icon brightness with
   input/output slots. The previously confirmed one-block counts and bulk extraction are
   also covered automatically.
2. Open the loom with a banner and dye, select a pattern, and take the result. Check the option
   thumbnails, scrolling and output appearance. Place the banner to confirm its design.
3. Insert one paper template, take one result and check that the template stays. A short
   shift-click test is sufficient; the exhaustive consumption/quantity cases run automatically.

No commit, push, history rewrite or changes to normal client/server worlds/configs were made.
