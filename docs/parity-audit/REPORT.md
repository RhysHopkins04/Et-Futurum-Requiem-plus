> Latest pass: [3.5.24 modern smithing and crafter](after-smithing-crafter/REPORT.md). Earlier reports remain historical snapshots.

> Latest pass: [3.5.23 lectern corrections, cartography and composting](after-cartography/REPORT.md). Earlier reports remain historical snapshots.

> Latest pass: [3.5.22 lectern, enchanting and grindstone](after-scholarly-workstations/REPORT.md), including AssetDirector correction. Earlier reports remain historical snapshots.

> Latest pass: [3.5.20 functional stonecutter](after-stonecutter/REPORT.md). Earlier reports remain historical snapshots.

> Latest pass: [3.5.19 production chains](after-production-chains/REPORT.md). This file remains the original audit baseline.

> Latest follow-up: [3.5.18 cooking and usage corrections](after-cooking-usage/REPORT.md). This file remains the original audit baseline.

# EFR+ crafting and functionality audit — Java 1.21.11

> Historical baseline. The subsequent crafting sweep and remaining work are tracked in
> [RESOLUTION_PLAN.md](RESOLUTION_PLAN.md) and `after-crafting-sweep/`.

Audited source commit: `2fe04e1fd4af52e578f04ade5245d77acd999b23`. Date: 16 October 2026.

**EFR+ is not fully functional against Minecraft Java 1.21.11.** This audit establishes complete registry/recipe accounting and confirms concrete gaps; it does not certify every mechanic. Gameplay source was not changed. The committed ore/import milestone remains separate from this audit.

## Reference and scope

The installed vanilla client matches Mojang's exact 1.21.11 client SHA-1, `ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd`. Recipe and item-tag comparisons read that jar directly, not a wiki or remembered historical recipes. REFERENCE.json records SHA-256, the official object URL, source commit and production-file hashes. No reference textures, models or bytecode are redistributed.

The runtime snapshot contains **623 EFR-owned/replaced blocks**, **171 other vanilla blocks**, **427 ModBlocks declarations**, **84 ModItems declarations**, **245 parity enum entries** (including the technical kelp-age helper), and **686 registered EFR items**. The other items in the snapshot belong to vanilla/dependencies. Fifty-one block enum entries are disabled/optional in this no-NTM profile; they are explicitly accounted for rather than labelled broken.

BLOCK_AUDIT.json contains every live block plus those disabled block declarations, including subtype names, item identity, class, recipe accounting and known findings. ITEM_AUDIT.json accounts for every runtime item. Grouped metadata variants and technical placement-only identities are not counted as independent modern survival items.

The main runtime profile is ordinary EFR with the copied development-server settings; no user world or normal run config was edited. PROFILE.json records settings and their hashes. The config matrix adds four feature-off cases and the legacy content-only profile. This is not an exhaustive configuration/full-modpack certification. Optional NTM processing retains its separately documented milestone tests; this audit does not impose vanilla recipes on NTM materials.

## Recipe results

Every one of the **1,470 vanilla recipe definitions** has an explicit audit row. **4,514 real recipe witnesses** were executed against the startup registries. Ingredient tags were expanded from the reference jar and translated through an explicit item/metadata/NBT crosswalk. The harness uses the actual crafting, furnace, smoker and blast-furnace query APIs; it never registers a substitute recipe to make an expected result pass.

| Status | Recipe definitions |
| --- | ---: |
| CAMPFIRE_REQUIRES_WORLD_TICK_TEST | 8 |
| COMPONENT_OUTPUT_REQUIRES_NBT_TEST | 16 |
| INPUT_NOT_RESOLVED | 7 |
| MODERN_TEMPLATE_SYSTEM_NOT_IMPLEMENTED | 9 |
| OUTPUT_NOT_RESOLVED | 114 |
| RUNTIME_DIFFERENCE_REQUIRES_REVIEW | 204 |
| SPECIAL_SERIALIZER_REQUIRES_BEHAVIOURAL_TEST | 30 |
| WITNESSES_MATCH | 828 |
| WORKSTATION_NOT_IMPLEMENTED | 254 |

`WITNESSES_MATCH` certifies the listed witnesses' result identity, quantity and represented colour NBT only. It does not certify all combinations, consumption, returned containers, XP, GUI behaviour, automation or survival supply. Low-cardinality mapped combinations are exhaustive; larger sets use a baseline and each-slot alternative witnesses. Each row records whether its mapped combinations were exhaustive and which modern ingredients could not be represented.

`RUNTIME_DIFFERENCE_REQUIRES_REVIEW` is a candidate category, not a count of independent bugs. Repeated family failures share causes. The confirmed findings below were reconciled with current source. `OUTPUT_NOT_RESOLVED` is not automatically a missing feature: items using special NBT/legacy aliases or not implemented at all need separate review. Examples are shields, bundles, modern smithing templates and newer equipment.

The **254 stonecutting rows** identify modern outputs that exist but have no stonecutting workstation route. Further stonecutting definitions may have unresolved outputs. The nine represented netherite upgrade definitions require the modern template system; EFR's nine older two-input upgrades are functioning but do not meet that newer contract.

## Confirmed findings

### A01 — Shulker recolouring accepts unrelated extra items

A box plus red dye returns a recoloured box with custom data intact. Adding a diamond to a third slot still returns a result. The custom matches() does not reject unrelated non-null slots; those items can consequently be consumed by crafting.

Evidence: [RecipeDyedShulkerBox.java:35](../../src/main/java/ganymedes01/etfuturum/recipes/crafting/RecipeDyedShulkerBox.java#L35).

Recommended: Require exactly one box and one recognized dye, reject every unrelated slot, and test container contents, names, all colours and extra-input rejection.

### A02 — Smithing output shares input NBT

Mutating the returned smithing preview tag changes the input tag. getCraftingResult() assigns the input compound directly rather than copying it. Earlier smithing tests verified retained values, not ownership isolation.

Evidence: [SmithingTableRecipes.java:116](../../src/main/java/ganymedes01/etfuturum/recipes/SmithingTableRecipes.java#L116).

Recommended: Copy the input NBT compound and add result/input mutation-isolation tests. Keep the existing damage-retention contract intact.

### A03 — Lime shulker recolouring produces white

All 17 mapped shulker input colours tested with lime dye produce Color=1 (white), whereas lime requires Color=6. Other 15 dye families match their tested colour witnesses. The dye-tag resolution path needs tracing before choosing the fix.

Recipe IDs: `lime_shulker_box`.

Evidence: [RecipeDyedShulkerBox.java:68](../../src/main/java/ganymedes01/etfuturum/recipes/crafting/RecipeDyedShulkerBox.java#L68).

Recommended: Resolve recognized colour-specific tags deterministically; test all colours and retain inventory/custom NBT.

### A04 — Copper trapdoor output quantity

Four ingots return two trapdoors; the pinned 1.21.11 recipe returns one.

Recipe IDs: `copper_trapdoor`.

Evidence: [ModRecipes.java:1353](../../src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java#L1353).

Recommended: Match the pinned modern quantity; add a real matching/result regression case.

### A05 — Granite output quantity

Diorite plus quartz returns two granite; the pinned recipe returns one.

Recipe IDs: `granite`.

Evidence: [ModRecipes.java:316](../../src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java#L316).

Recommended: Match the pinned modern quantity; add a real matching/result regression case.

### A06 — End rod output quantity

Blaze rod plus popped chorus fruit returns one end rod; the pinned recipe returns four.

Recipe IDs: `end_rod`.

Evidence: [ModRecipes.java:952](../../src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java#L952).

Recommended: Match the pinned modern quantity; add a real matching/result regression case.

### A07 — Bamboo mosaic slabs craft with noncanonical metadata

The six-slab recipe returns item damage 5 although this standalone slab has only one material subtype, whose canonical damage is 0. This is confirmed noncanonical output; a client rendering/placement failure is not claimed by this audit.

Recipe IDs: `bamboo_mosaic_slab`.

Evidence: [ModRecipes.java:663](../../src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java#L663), [ModBlocks.java:370](../../src/main/java/ganymedes01/etfuturum/ModBlocks.java#L370).

Recommended: Craft canonical metadata 0 and test placement, stacking and merging while keeping existing saves readable.

### A08 — Pale oak fails generic wood recipes

Pale-oak-specific recipes exist, but ordinary plank/slab consumers reject tested pale oak substitutions. Examples include chest, barrel, crafting table and wooden tools. ModTagging registers the five mature modern wood families but omits parity Pale Oak.

Recipe IDs: `chest`, `barrel`, `crafting_table`, `wooden_pickaxe`.

Evidence: [ModTagging.java:298](../../src/main/java/ganymedes01/etfuturum/recipes/ModTagging.java#L298), [ModRecipes.java:179](../../src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java#L179).

Recommended: Register appropriate wood tags under the relevant feature gates; check all existing generic consumers and charcoal inputs.

### A09 — Older shaped recipes reject modern shapeless layouts

Six beetroot plus a bowl in a valid modern shapeless arrangement fails, while EFR registers a fixed shaped pattern. Minecart combinations and rabbit stews also reject tested modern arrangements. This is recipe flexibility loss, not proof that these outputs are unobtainable.

Recipe IDs: `beetroot_soup`, `chest_minecart`, `furnace_minecart`, `hopper_minecart`, `tnt_minecart`, `rabbit_stew_from_brown_mushroom`, `rabbit_stew_from_red_mushroom`.

Evidence: [ModRecipes.java:922](../../src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java#L922).

Recommended: Reconcile shaped/shapeless definitions and test multiple arrangements plus extra-input rejection.

### A10 — Implemented modern plants lack expected dye recipes

The six listed modern dye recipes have mapped registered inputs and outputs, but real matching returns no result.

Recipe IDs: `orange_dye_from_torchflower`, `orange_dye_from_open_eyeblossom`, `gray_dye_from_closed_eyeblossom`, `cyan_dye_from_pitcher_plant`, `yellow_dye_from_wildflowers`, `pink_dye_from_cactus_flower`.

Evidence: [ModRecipes.java:265](../../src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java#L265).

Recommended: Add only the missing recipes with modern quantities, proper alternatives and feature gating.

### A11 — Registered block families lack modern crafting routes

Every listed recipe was queried with mapped modern ingredients and has no matching output for at least one tested witness. These are missing expected routes; alternative loot, generation or commands have not been disproved. Some blocks also require substantial gameplay work before their recipes alone make them complete.

Recipe IDs: `candle`, `white_candle`, `orange_candle`, `magenta_candle`, `light_blue_candle`, `yellow_candle`, `lime_candle`, `pink_candle`, `gray_candle`, `light_gray_candle`, `cyan_candle`, `purple_candle`, `blue_candle`, `brown_candle`, `green_candle`, `red_candle`, `black_candle`, `campfire`, `soul_campfire`, `calibrated_sculk_sensor`, `chiseled_bookshelf`, `copper_bars`, `copper_chest`, `crafter`, `grindstone`, `lectern`, `respawn_anchor`, `scaffolding`, `resin_block`, `resin_clump`, `resin_brick_slab`, `resin_brick_stairs`, `resin_brick_wall`, `chiseled_resin_bricks`.

Evidence: [ModernMapParityBlocks.java:557](../../src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java#L557), [ModRecipes.java:265](../../src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java#L265).

Recommended: Implement verified missing routes in bounded families, paired with each block's missing mechanics; do not label a recipe-only patch full parity.

### A12 — Existing families lack essential companion items

No dried kelp, resin brick, copper nugget, nautilus shell or heart-of-the-sea item is present in the EFR runtime item registry. This blocks the corresponding modern dried-kelp, resin, copper and conduit production chains. Companion feature work is required, not just extra crafting registrations.

Recipe IDs: `dried_kelp_block`, `resin_bricks`, `copper_chain`, `copper_lantern`, `copper_torch`, `conduit`.

Evidence: [ModItems.java:18](../../src/main/java/ganymedes01/etfuturum/ModItems.java#L18), [ModernMapParityBlocks.java:113](../../src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java#L113).

Recommended: Define companion items and legitimate production/loot routes. Dried kelp also needs food, fuel/storage and all three cooking routes; do not introduce replacement NTM raw materials.

### A13 — Several vanilla recipe revisions remain unported

The pinned lead recipe uses five string, lodestone uses an iron ingot with chiseled stone bricks, packed ice is shapeless, and a honey bottle produces three sugar. Tested modern inputs do not match. The registered lightning rod also has no match for its three-copper recipe.

Recipe IDs: `lead`, `lodestone`, `packed_ice`, `sugar_from_honey_bottle`, `lightning_rod`.

Evidence: [ModRecipes.java:265](../../src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java#L265).

Recommended: Update recipe policy deliberately against 1.21.11; preserve container bottles and test recipe precedence with existing 1.7/mod recipes.

### A14 — Modern metal-equipment recycling is missing

The mapped modern iron/gold equipment recycling inputs tested through real smelting and blasting queries return no output.

Recipe IDs: `gold_nugget_from_blasting`, `gold_nugget_from_smelting`, `iron_nugget_from_blasting`, `iron_nugget_from_smelting`.

Evidence: [BlastFurnaceRecipes.java:44](../../src/main/java/ganymedes01/etfuturum/recipes/BlastFurnaceRecipes.java#L44).

Recommended: Add the exact modern input sets and nugget yields; verify XP, damaged/enchanted inputs, consumption and optional mod overrides.

### A15 — Decorated-pot recipe bypasses legacy recipe suppression

The legacy content-only profile suppresses ordinary EFR recipes and smithing but still contains RecipeDecoratedPot, registered directly by the parity initializer. Confirm the intended profile policy, then make the registration consistent.

Evidence: [ModernMapParityBlocks.java:671](../../src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java#L671), [ModRecipes.java:85](../../src/main/java/ganymedes01/etfuturum/recipes/ModRecipes.java#L85).

Recommended: Apply the chosen profile gate to this direct registration; add assertions covering all EFR recipe classes under each profile.

### A16 — Older capability matrix has a stale powder-snow gap

The earlier capability generator describes powder snow as lacking sinking, leather-boots collision, freezing and bucket behaviour. Current source and fresh modern-state runtime/restart checks implement and exercise these. That old row must not drive a duplicate feature pass.

Evidence: [audit_modern_map_parity_capabilities.py:110](../../scripts/audit_modern_map_parity_capabilities.py#L110).

Recommended: Refresh the capability metadata against current scoped evidence without promoting it to blanket full parity.

### S01 — Stonecutting

Modern workstation, input/output recipe selection, slab/stair yields, consumption, shift-click and registry coverage are absent; current block is explicitly decorative.

Evidence: [BlockStonecutter.java:21](../../src/main/java/ganymedes01/etfuturum/blocks/BlockStonecutter.java#L21).

Recommended: Implement as a separate bounded system with inventory/state, interaction, server authority, automation where applicable, persistence, client sync and configuration tests.

### S02 — Loom

Modern banner/pattern workstation is absent. Existing crafting-grid banner patterns are useful legacy behaviour, not the modern loom interface.

Evidence: [BlockLoom.java:19](../../src/main/java/ganymedes01/etfuturum/blocks/BlockLoom.java#L19).

Recommended: Implement as a separate bounded system with inventory/state, interaction, server authority, automation where applicable, persistence, client sync and configuration tests.

### S03 — Cartography

Map cloning, scaling and locking workstation is absent; block is decorative.

Evidence: [BlockCartographyTable.java:15](../../src/main/java/ganymedes01/etfuturum/blocks/BlockCartographyTable.java#L15).

Recommended: Implement as a separate bounded system with inventory/state, interaction, server authority, automation where applicable, persistence, client sync and configuration tests.

### S04 — Grindstone

Orientation/models exist, but there is no repair/disenchant inventory, XP or result-slot system.

Evidence: [ModernMapParityBlocks.java:108](../../src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java#L108).

Recommended: Implement as a separate bounded system with inventory/state, interaction, server authority, automation where applicable, persistence, client sync and configuration tests.

### S05 — Lectern

The registered shell lacks book/page inventory, reading GUI, page redstone pulses and comparator mechanics.

Evidence: [ModernMapParityBlocks.java:108](../../src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java#L108).

Recommended: Implement as a separate bounded system with inventory/state, interaction, server authority, automation where applicable, persistence, client sync and configuration tests.

### S06 — Modern smithing

EFR implements the pre-1.20 two-input upgrade GUI. Modern templates, the third input, smithing-trim components, template consumption/duplication/loot and newer equipment are not implemented.

Evidence: [BlockSmithingTable.java:15](../../src/main/java/ganymedes01/etfuturum/blocks/BlockSmithingTable.java#L15).

Recommended: Implement as a separate bounded system with inventory/state, interaction, server authority, automation where applicable, persistence, client sync and configuration tests.

### S07 — Crafter

The tile entity persists model booleans only; it has no inventory, disabled-slot system or redstone crafting/ejection pipeline.

Evidence: [ModernMapParityBlocks.java:108](../../src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java#L108).

Recommended: Implement as a separate bounded system with inventory/state, interaction, server authority, automation where applicable, persistence, client sync and configuration tests.

### S08 — Trial spawner and vault

Imported visible states are represented, but trial encounter/reward/cooldown and vault key/per-player reward state machines are not implemented.

Evidence: [ModernMapParityBlocks.java:108](../../src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java#L108).

Recommended: Implement as a separate bounded system with inventory/state, interaction, server authority, automation where applicable, persistence, client sync and configuration tests.

### S09 — Conduit, bell, respawn anchor and lodestone

These shells preserve varying visual states but lack their major gameplay endpoints: conduit activation/effects/targets, bell ringing/raid interactions, anchor charging/respawn/explosion, and lodestone compass binding.

Evidence: [ModernMapParityBlocks.java:108](../../src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java#L108).

Recommended: Implement as a separate bounded system with inventory/state, interaction, server authority, automation where applicable, persistence, client sync and configuration tests.

### S10 — Entity-dependent lifecycle endpoints

Hatching/spawning/reanimation require their corresponding entities and systems. Warden/Darkness remain deferred. Stored hatch/hydration/active/pose/warning states are not proof of completed lifecycles.

Evidence: [ModernMapParityBlocks.java:108](../../src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java#L108).

Recommended: Implement as a separate bounded system with inventory/state, interaction, server authority, automation where applicable, persistence, client sync and configuration tests.

## Already implemented: do not redo these as missing features

- All 16 plain banner crafting colour witnesses match after resolving EFR's wool-order item metadata.
- Fifteen shulker dye families match all 17 represented input-colour witnesses; lime is the separate A03 failure. A valid recolouring preserves custom NBT, but A01 still allows unrelated extra inputs.
- Existing two-input smithing supports all nine upgrades, damage and custom data retention. Earth-mode initialization was fixed in the committed milestone; A02 is the separate NBT ownership defect. Modern templates/trims remain S06.
- Ordinary raw copper/iron/gold storage and smelting routes, and many existing stone, deepslate, wood and copper recipes have passing witnesses. Individual results are in RECIPE_AUDIT.json; this is not a blanket claim for those families.
- The current modern-state Forge suite passes real aquatic immersion/drowning, wet/dry pickle counts, water fill/drain, kelp age migration, powder-snow collision/freezing/damage/immunity/melting and fresh-process restart checks. The older powder-snow capability row is stale (A16).
- Kelp/seagrass/coral/pickle lifecycles, amethyst/dripstone states, dripleaf, copper weathering and Sculk have existing bounded implementations/contracts. A block preserving a model/state still requires its recipe and gameplay axes to be audited separately. General waterlogging and modern entity parity are not implied.
- Scaffolding has substantial placement, support and movement code; it should not be called an empty model shell. Its modern recipe still fails and its full mechanics require a dedicated equivalence review.
- Fletching tables do not need an invented player crafting GUI. Their modern villager job-site behaviour belongs to the shared villager/profession audit; it has not been certified here.

## Complex production systems and outstanding checks

| System | Current evidence | Required before full parity |
| --- | --- | --- |
| Crafting grid | Real recipe/result queries; several custom serializers | All permutations/alternatives, extra items, NBT, remainders, shift-click, conflict precedence |
| Furnace | Real recipe lookup snapshot | Every modern recipe, XP, fuel amounts, remainders, automation, save/reload |
| Blast furnace / smoker | Existing 100-tick tile implementations; real recipe queries | Explicit modern recipe coverage instead of assuming OreDictionary/food heuristics equal modern tags; full fuel/XP/hopper cycles |
| Campfire / soul campfire | Four-slot, 600-tick food-only cooking implementation, persistence and ejection code | Execute all eight modern cooking definitions through real ticks; wet/unlit timing, unloading, food vs kelp input restrictions and drops |
| Stonecutter | Decorative only | S01; all applicable modern stonecutting recipes and quantities |
| Smithing | Functional older two-input GUI, feature/profile gates | A02 and S06; templates/trims, independent copied state, all slots and consumption |
| Loom | Existing crafting-grid banner paths, decorative workstation | S02; GUI, patterns, modern limits and NBT preservation |
| Cartography | Decorative workstation; older map crafting remains | S03; scaling, cloning, locking, map data and result rules |
| Grindstone | Models/orientation | S04; repair/durability, enchantment/curses, XP, exact inventory rules |
| Lectern | Model identity | S05; books, pages, GUI, comparator/pulses and save/sync |
| Enchanting table | Custom lapis/fuel container, enchanting seed and level-cost implementation | Compare current enchanting tables/RNG/enchantment set, costs and multiplayer state; do not label absent or fully certified |
| Anvil | Custom repair/enchantment container and damage handling | Current compatibility, rename/repair costs, conflicts, caps, XP and NBT/state isolation |
| Brewing | Five-slot blaze-fuel stand, older PotionHelper-based brewing and lingering conversion | Enumerate all 1.21.11 potion transformations, splash/lingering/tipped states, modern effects, remainders, hoppers and persistence |
| Crafter | Visual-state TE | S07; automated crafting and redstone/inventory systems |
| Storage blocks / shelves / pots / bookshelves | Existing dedicated inventories and import contracts | Full slot rules, item limits, comparators, automation, break drops and restart/sync for every family |
| Composter / cauldrons / bee processing | Existing special interaction implementations | Modern item tags, probabilities, levels, bottles/buckets, automation and acquisition, independently of crafting recipes |

## Survival and compatibility closure rules

No block is declared unobtainable merely because it has no crafting output. For each family, finish the harvest/drop, Silk Touch/Fortune, growth/bonemeal, loot/trade, generation and interaction routes. Then distinguish legitimately technical/admin-only blocks, retired save identities, optional mod materials and actual survival gaps. Imported-world availability does not certify renewable survival supply.

The per-block ledger keeps full-functionality certification false unless every relevant axis is evidenced: placement/support/collision, breaking/tool/XP behaviour, interactions, redstone/comparator, fluids, inventories/NBT, automation, synchronization, persistence, entities and obtainability. Basic legacy vanilla behaviour is not assumed equal to 1.21.11 behaviour.

Five isolated configuration/profile startup cases pass. Disabling netherite leaves zero built-in smithing recipes. No unregistered crafting outputs were found in those cases. The legacy profile still leaks the decorated-pot recipe (A15). Arbitrary combinations, client GUI presence and mixed-mod recipe conflicts remain unverified.

## Recommended implementation order

1. **Crafting safety and small recipe corrections:** A01–A07. Focus on strict inputs, independent NBT, correct quantities and canonical metadata. Add regression cases before fixing each behaviour.
2. **Existing-content recipe/obtainability coverage:** A08–A15. Wood tags, shape flexibility, plant dyes, missing family recipes, cooking/recycling and companion items. Group passes by family; each must preserve configuration and mod compatibility.
3. **Functional stonecutting**, followed by **loom/cartography/grindstone/lectern** as separate systems, each with focused headless inventory/persistence tests and one small client check.
4. **Modern smithing/templates/trims and remaining production-system equivalence**, with an explicit migration/config policy for existing packs using old upgrades.
5. **Crafter and other partial gameplay systems**, then entity-dependent endpoints. Trial/vault, conduit/anchor/bell/lodestone, modern villagers and hatching/reanimation are separate projects. Warden/Darkness follows the existing roadmap, not a recipe patch.
6. **Backporter integration** consumes the resulting stable identities/states. Do not change IDs or conflate map-state fidelity with gameplay completion. Test a small converted area before a whole-world conversion.

An implementation pass has not been selected or applied by this audit. Existing ore/import gameplay is unchanged.

## Reproduction and evidence

From this repository, with the existing user-accepted `run/server/eula.txt` and cached build dependencies:

```sh
python3 scripts/run_parity_audit.py --vanilla-jar '/absolute/path/to/minecraft-1.21.11-client.jar'
python3 scripts/audit_feature_configs.py
python3 scripts/run_modern_state_tests.py
python3 scripts/build_parity_audit_report.py
```

The audit runner uses `build/parity-audit/`, bounded subprocess timeouts, marker checks (Forge can exit zero after a failed probe), the exact reference checksum and source-file guards. Normal runClient/runServer and the distributable jar do not receive the audit mod. Registry-name aliases are recorded and remain reviewable rather than hidden numerical-ID guesses. Re-running snapshots against changed source requires re-reviewing this report's confirmed assertions; it should refuse to silently keep already-fixed findings.

- `REFERENCE.json`: baseline commit/reference hashes and all production-file hashes.
- `PROFILE.json`, `CONFIG_MATRIX.json`: actual settings and bounded gating checks.
- `RUNTIME_INVENTORY.json`: startup block/item/enumeration/recipe snapshot and NBT/input-safety observations.
- `ITEM_CROSSWALK.json`: explicit modern-to-legacy identity/metadata/colour mapping.
- `RECIPE_AUDIT.json`, `RECIPE_SUMMARY.json`: all 1,470 definitions and actual witness results.
- `CONFIRMED_FINDINGS.json`: source-backed issues and implementation candidates.
- `BLOCK_AUDIT.json`, `ITEM_AUDIT.json`: full accounting, including optional/technical identities and uncertified mechanics.
- Local runtime logs: `build/parity-audit/snapshot.log`, `recipes.log`, and `config-*.log`.
- Current modern-state logs: `build/modern-state-tests/contracts-and-build.log`, `runtime-save.log`, `runtime-restart.log`.

Additional primary references: [Mojang's loom workflow](https://www.minecraft.net/en-us/article/block-week--loom), [cartography table](https://www.minecraft.net/en-us/article/block-week--cartography-table), [fletching table/job site](https://www.minecraft.net/en-us/article/block-month--fletching-table), [modern smithing/template introduction](https://www.minecraft.net/en-us/article/minecraft-snapshot-23w04a). Exact recipe claims use the pinned 1.21.11 jar; older explanatory articles are not used to override it.

Latest follow-up: [3.5.21 functional loom and stonecutter lighting](after-loom/REPORT.md).
The preserved baseline and earlier snapshots above are historical; they are not rewritten.
