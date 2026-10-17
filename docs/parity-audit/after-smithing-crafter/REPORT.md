# Modern smithing and operational crafter — 3.5.24

The existing smithing table now uses template/base/addition inputs. The existing crafter
registry and tile-entity identity gain a persistent automated crafting implementation.
The backporter is unchanged. All previous working-tree passes are retained.

## Smithing and templates

Nineteen template variants are appended as the new `etfuturum:smithing_template` item:
netherite upgrade at metadata 0, then Sentry, Dune, Coast, Wild, Ward, Eye, Vex, Tide,
Snout, Rib, Spire, Wayfinder, Shaper, Silence, Raiser, Host, Flow and Bolt at 1..18.
Existing numeric block/item/surface-biome IDs are unchanged. The actual variant names,
translations and nineteen texture aliases are recorded in the template catalogue.
All template art comes through AssetDirector; no copied PNG is added.

Built-in diamond upgrades require a netherite upgrade template and an ingot. Taking a
result consumes one template, one base and one addition. Damage, names, enchantments,
trim data and independent custom NBT carry into the upgraded equipment. Custom two-input
smithing recipes registered through the existing API/CraftTweaker remain usable with an
empty template slot. The old two-input query API remains available to integrations; the
block GUI uses the modern validation path.

All eighteen trim patterns work with eleven registered material identities: quartz, iron,
netherite, redstone, copper, gold, emerald, diamond, lapis, amethyst and resin. Matching ore
entries can supply compatible metal ingots. An identical existing pattern/material pair
produces no result, avoiding accidental consumption. Applying another trim replaces the
trim while retaining the rest of the equipment's data. Trimming changes appearance,
not armor protection or processing behavior.

The server validates current inputs, player, block, range and result on extraction.
Full-inventory shift-click does not consume inputs; hotbar extraction retains the result.
Closing returns inputs and clears the preview. Spectators and stale/distant block windows
cannot craft. NEI has three-input recipes and trim examples, with custom recipes showing
an empty template position. The GUI uses modern art and an EFR armor-stand preview when
the armor-stand feature is enabled.

## Armor eligibility and visuals

The default whitelist is the twenty native vanilla armor pieces plus EFR's four netherite
pieces. Other mods' armor does not accept trims by default. The server setting in
`config/etfuturum/functions.cfg` is:

```text
B:allowModdedArmorTrims=false
B:requireNetheriteUpgradeTemplate=true
```

`allowModdedArmorTrims=true` permits other `ItemArmor` implementations. This is an opt-in
for standard humanoid armor layouts, not a guarantee for arbitrary custom models, UVs,
special armor interfaces or custom item renderers. Trim NBT stays on the item and is not
removed if the setting is later disabled. Recognized existing trim data can still render.

Worn armor (players, ordinary armor-wearing mobs and EFR armor stands) and standard inventory
icons receive trim overlays. Worn overlays follow the native armor model/pass, including the
leather overlay and enchantment rendering. Pattern masks, item trim masks and color palettes
come from AssetDirector and are permuted in memory. Same-material iron/gold/diamond/netherite
use the darker palette; EFR's `Netherite_Armour` material name is translated explicitly.
Resource reload clears the generated textures. Tooltips identify the trim and material.

Headless tests verify all 640 actual pattern/layer/item-mask palette combinations, alpha
handling, darker-palette selection and all nineteen resource aliases. They cannot certify
OpenGL output or third-party render mixins. Held/dropped armor item sprites currently keep
their base icon; worn and GUI overlays are the supported visual paths in this pass.

## Acquisition boundary

The user explicitly chose functional templates ahead of implementing every source.
Templates are available in creative and can be used immediately. Eighteen duplication
recipes use the exact pinned modern layout, quantities and available center materials.
Bolt supports both plain and waxed copper blocks. **Flow duplication remains unavailable
because its required breeze rod is not implemented.** No substitute ingredient is invented.
The first-template loot routes, including bastions and other absent structures, remain
outstanding. They were not silently replaced with generic dungeon loot.

Strict template requirements stay enabled by default, as requested. Existing imported
survival worlds therefore need a supplied/imported initial template until acquisition is
implemented. `requireNetheriteUpgradeTemplate=false` is an explicit compatibility option
for retaining pre-1.20 netherite progression; it does not disable the template path.

## Crafter behavior

The existing `etfuturum:modern_parity_crafter_state` TE name, twelve FrontAndTop metadata
values and Triggered/Crafting fields are retained. Old visual-only NBT loads as an empty
inventory. Additional saved data includes nine item slots, `disabled_slots` and a bounded
`crafting_ticks_remaining` counter. Inventory and disabled slots survive an actual save and
fresh-process restart. Closing a GUI leaves this world-owned inventory intact.

Empty slots can be disabled and re-enabled; occupied slots cannot be disabled. The GUI
synchronizes the mask and triggered state, and its result is a read-only preview. Hopper
insertion distributes items across enabled slots, preferring empty/lower stacks. Sided
insertion queries have no side effects. Comparator output is the number of occupied or
disabled slots, from zero through nine.

A rising redstone edge schedules one craft after four ticks. Holding power does not repeat;
a second edge schedules another. The normal three-by-three Forge crafting manager runs on
copied inputs, including existing mod recipes. One item is consumed from each occupied
slot. Forge container-item remainders, such as cake's three buckets, use the output path.
A six-tick crafting visual resets afterward. Invalid recipes consume nothing.

Output enters the inventory in front from the appropriate side, or ejects the remaining
items into the world. Output into another crafter is inserted one item at a time to retain
its distribution rules. All twelve output directions are exercised; placement supports
horizontal and vertical orientations. Breaking drops the stored inventory once. Block,
owner, distance and spectator checks protect GUI actions. The legacy profile suppresses
these workstation interfaces without discarding their saved data.

The automated crafting contract uses Forge 1.7 recipe/container APIs. Arbitrary custom
inventory handlers and recipe implementations can require their own integration tests;
modern component-based recipe semantics are not synthesized. Modern source-world item
stacks still need translation by the backporter before importing a populated crafter.

## Validation and remaining client milestone

The exact reference client SHA-1 is `ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd`.
The recipe audit now matches **1,323 definitions across 4,814 queries**, up from 1,295.
All 28 improvements are accounted for: eighteen template duplication definitions, nine
modern netherite upgrade definitions and the crafter crafting recipe. There are no
previously matching recipe regressions. The runtime variant probe now checks all nineteen
template metadata values, correcting its former generic sixteen-variant ceiling.

Forge tests cover eighteen left/right upgrade extractions, 4,752 trim combinations,
default mod-armor rejection/explicit opt-in, costs and independent NBT, native API
compatibility, hotbar/shift/full inventory, and crafter timing, container remainders,
hopper balance, chained crafters, all twelve ejection directions, breaks and persistence.
The existing ore, smithing API, cooking, shulker, stonecutter, loom, lectern, enchanting,
grindstone, cartography and composting checks run alongside them. Optional NTM 2.4.0 and
Iron Chests and the legacy profile are tested separately. Twenty configuration startup
cases include smithing disabled, optional templates and mod-armor trimming enabled.
The historical Pass-35 static guard now accepts the already-existing contract revision 39;
no backporter encoding or contract revision was changed.

See `VALIDATION.json`, `CONFIG_MATRIX.json`, `COMPARISON.json` and `ASSET_REFERENCE.json`
for final hashes and results. No test classes are included in the release jar.

Only a short real client check remains:

1. In creative, check several template icons. Use a netherite upgrade template, diamond
   tool and ingot once. Check the modern GUI, preview and returned tool.
2. Apply one trim to vanilla armor, wear it and check the inventory icon and worn pattern.
   Check iron-on-iron or netherite-on-netherite once for contrast. Try one NTM armor piece:
   it should produce no trim result with the default configuration.
3. In a crafter, disable unused slots, insert a log and pulse it. Confirm four planks appear,
   held power does not repeat, and the GUI's preview cannot be taken manually.

These visual checks are still required for the client render hooks and full pack mixins;
a dedicated server and CPU asset tests cannot substitute for them. Nothing was committed,
pushed or applied to normal user saves/configs.
