# Cooking, survival interactions and recipe visibility — 3.5.18

This follow-up addresses the cooking/NEI, candle-cake, shulker-tier and netherite-use issues
reported after the 3.5.17 crafting sweep. It builds on the earlier uncommitted work; it is not
certification that every existing EFR+ feature now matches Minecraft 1.21.11.

## Changes and verified behaviour

| Area | Finding and correction |
| --- | --- |
| Smoker/blast furnace NEI | Their display caches were populated from a particular item lookup without being keyed by that item. They also delegated unrelated category requests to the generic furnace handler. Each lookup now reads the actual workstation registry, including explicit overrides and blacklists, with independent category dispatch. Actual NEI queries run in a window-free JVM regression test. |
| Campfire | The actual tile already rejected non-food and required no fuel. Added a distinct NEI category with a food input, cooking result and campfire catalyst, with no fuel slot or fuel button. The display and tile share one food-to-food admission rule. Both normal/soul campfires are tested through 599/600 lit ticks, four occupied slots, an unlit pause and NBT restoration. |
| Candle cakes | Registered visual blocks were missing the survival creation interaction and first-bite behaviour. Using any of the 17 candles on an uneaten placed cake now consumes one candle, or none in creative. The first bite restores a once-eaten vanilla cake, gives food and returns the candle. Breaking the block returns its candle, not a recoverable cake. Lighting and top-versus-side interaction are tested. A dedicated NEI interaction display explains placement; no inaccurate crafting-grid recipe is added. |
| Netherite | Ingot crafting remains the correct shapeless four scraps + four gold. All nine existing diamond-equipment upgrades already exist in the smithing registry. Added their missing NEI display, with equipment left and ingot right. Corrected shift-click routing into these slots and exercised result extraction/consumption through the real container. Three-slot modern template/trim parity is still outstanding. |
| Shulker dyes | Recolouring requires the colour-specific dye and exactly one box. Added explicit recipe outputs for enabled Iron Chests tiers so NEI can represent their coloured variants. Inventory, tier, names/custom data and input NBT ownership remain preserved. Recolouring without dye, with duplicate inputs or unrelated ingredients is rejected. |
| Iron Chests | Added matching enabled chest + two vertically arranged shulker shells -> the same tier of uncoloured shulker. Resolve the installed chest registration by tier name rather than assuming its numeric metadata equals EFR's tier ordinal. Disabled/unavailable tiers are omitted from the creative/NEI subitem list. The installed Iron Chests 6.1.6 configuration exposes six supported enhanced tiers: direct crafting and 1,904 colour/tier/NBT cases pass. Existing upgrade-item interactions remain. |
| Copper nuggets | Added the modern copper nugget, nine-nugget compression/decompression, copper chain, four copper torches and copper lantern. Recipes consume shared `ingotCopper`/`nuggetCopper` OreDictionary entries. Existing iron nugget recipes already use `nuggetIron`. Tests query registered copper ingots/nuggets, including the combined NTM/Iron Chests run. |
| Furnace fuel hooks | Corrected an inverted valid-item guard which skipped EFR's existing fuel remappings. Tested bamboo's 50-tick fuel value and rejection of nether stems as furnace fuel. This is separate from the fuel-free campfire cooking system. |
| Compatibility safety | Recipe-removal matching now skips absent target items and malformed third-party outputs instead of dereferencing a null item. This was exposed by the real Iron Chests startup test. |

The full pinned 1.21.11 witness replay now has **1,023 matching recipe definitions**, up from
1,018 in the prior sweep. The five newly matching definitions are the copper nugget/ingot,
chain, torch and lantern routes. No previously matching definition regressed. All prior block
and item numeric IDs remain unchanged in the ordinary audited profile; `etfuturum:copper_nugget`
is the one new item. The comparison is in `COMPARISON.json`.

The raw witness report intentionally still labels special serializers, component recipes,
stonecutting, campfire world ticks and modern smithing separately. Focused world-tick and
container evidence lives in `VALIDATION.json`; this does not turn the recipe metric into a
blanket functionality score.

## Compatibility scope

The supplied NTM 2.4.0 source has many specialised nuggets, but does not itself declare an iron
or copper nugget under those identities. No speculative aliases are added to its nuclear or
other material nuggets. Shared standard OreDictionary ingredients accept those metals from
other installed mods when registered appropriately. NTM's existing ore drops and processing
contracts are retained and tested. No new modded raw-ore forms are introduced.

Ordinary no-NTM, NTM 2.4.0, Iron Chests 6.1.6, their combined configuration, legacy profile and
feature-off cases use disposable worlds/configurations under `build/`. No user's world,
PrismLauncher mod folder or backporter implementation is changed. Nothing is committed or pushed.

## Small client check

Use the **3.5.18** candidate, replacing the older EFR+ jar rather than installing both.

1. In NEI, view beef then iron ore then sand. Campfire/smoker must show food routes, while the
   blast furnace shows eligible ore/metal routes. Verify the distinct campfire category has
   no fuel slot. A furnace **fuel-usage** page is a different query from campfire cooking.
2. Check a netherite equipment item's smithing page and one coloured enhanced shulker page.
   Confirm the ingredient icons, dye, tier and instructions are legible.
3. Place an uneaten cake, use a candle, light it, then eat a side while hungry. The candle should
   pop off and a bitten cake remain. This is also covered by the headless interaction test.

No complete ore/biome/map retest is needed for this follow-up. The remaining visual/layout check
cannot be replaced by recipe queries or a server test.

## Work still required

The coordinated programme remains in `../RESOLUTION_PLAN.md`. Copper's companion ingredient
and these interaction/visibility faults are now covered, but dried kelp, resin production,
conduit acquisition, functional stonecutting/loom/cartography/grindstone/lectern, modern
smithing templates/trims and the larger partial gameplay endpoints still need implementation
and acceptance tests. Technical block states and interaction-produced blocks should not all
receive arbitrary crafting recipes just to eliminate missing-recipe rows.

Reference data: the checksum-pinned installed Minecraft Java 1.21.11 jar recorded in
`REFERENCE.json`. The copper nugget sprite comes from that jar's
`assets/minecraft/textures/item/copper_nugget.png`, following the existing vanilla-item texture
layout. Official context: [candles](https://www.minecraft.net/de-de/article/taking-inventory--candle)
and [copper torches](https://feedback.minecraft.net/hc/en-us/articles/38580678455437-Minecraft-Java-Edition-Snapshot-25w32a).
