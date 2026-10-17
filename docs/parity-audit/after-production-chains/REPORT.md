# Production-chain pass — 3.5.19

## Implemented

- Shortened the campfire instruction to `Lit campfire: 30s, no fuel`.
- Reordered the netherite ingot recipe's ingredient list for the familiar modern presentation
  in NEI's column-grouped shapeless slots: three scraps across the top, another at middle-left,
  and four gold ingots in the remaining occupied positions. Its matcher remains shapeless.
- Prefer campfire, candle-cake and smithing process categories to generic fuel usage by default.
  Legacy NEI uses registration order; the GTNH handler-order map receives defaults only where
  the user/pack has not supplied an order by overlay ID or handler class. Fuel uses are retained.
- Added the dried-kelp food item: one hunger point, 0.6 saturation and a 16-tick eating duration.
  Added furnace (200 ticks), smoker (100 ticks) and campfire (600 lit ticks) cooking, nine-item
  compression/decompression, and composting for kelp/dried kelp (30%) and its storage block (50%).
- The existing dried-kelp block now fuels a furnace for 4,000 ticks, enough to cook 20 items.
  The existing smoker/blast-furnace fuel scaling gives the same 20-item capacity at their
  faster cooking speed. Individual dried-kelp food is not added as a furnace fuel.
- Added resin-brick smelting (200 ticks, 0.1 XP) and recipes for resin storage/decompression,
  resin bricks, six slabs, four stairs, six walls and chiseled bricks. Existing block identities,
  multiface clump placement and clump drops are retained.
- Updated both campfire admission paths: the tile and block click handler accept cookable kelp,
  including insertion on an unlit campfire. Ores and other non-food cooking routes remain rejected.

## Evidence and limits

The exact installed Minecraft Java 1.21.11 jar remains checksum-pinned in `REFERENCE.json`.
All 1,470 recipe definitions were replayed using 4,526 runtime queries. **1,035 recipe definitions
now match their tested witnesses**, up from 1,023, with zero previously matching regressions.
`COMPARISON.json` lists the twelve newly matching definitions.

Existing numeric block/item IDs remain unchanged in the ordinary audited profile. The only new
identities in this pass are `etfuturum:dried_kelp` and `etfuturum:resin_brick`. Their textures are
from the pinned jar's corresponding `assets/minecraft/textures/item/` entries, following the
existing vanilla item-texture layout.

`ProductionChainProbe` exercises real registered recipes and real tiles, not replacement recipes:

- Correct crafting outputs/counts and the two smelting XP values.
- 199/200 furnace ticks, 99/100 smoker ticks, and 599/600 campfire lit ticks, with NBT restoration
  immediately before completion.
- Player insertion of four kelp items onto unlit normal and soul campfires, no cooking while
  unlit, exact food output and input consumption.
- Actual food consumption/hunger/saturation, eating duration and composting percentages.
- A 21-item furnace input with one dried-kelp block: exactly 20 cooked, one uncooked remaining.
- Rejection of kelp in a blast furnace and resin in both smoker and blast furnace.

The same tests run without NTM and with NTM 2.4.0 plus Iron Chests 6.1.6, alongside the existing
ore/tool/hazard, shulker, smithing and save/restart regressions. NEI query/ordering tests run
without a window. Feature-off/profile results and artifact checksums are in `VALIDATION.json`.
The presentation changes are compiled and data-tested; this pass does not claim a fresh visual
client check. The next workstation UI milestone is a useful time to check those small changes.

Resin production from harvested/imported clumps now works, but **renewable resin generation
through the Creaking is still outstanding**. No substitute loot or fake renewable source was
introduced. Stonecutting routes remain outstanding until the stonecutter is implemented.
Conduit ingredients/acquisition, recipe-specific leaf-litter cooking time, other workstations,
modern smithing templates/trims and partial gameplay endpoints remain in the coordinated plan.

No user world, PrismLauncher installation or backporter code was modified. Nothing was committed
or pushed. Official gameplay context for the kelp chain: [Minecraft's dried-kelp article](https://www.minecraft.net/en-us/article/block-month--dried-kelp).
