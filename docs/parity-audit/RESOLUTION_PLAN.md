# Coordinated resolution programme

The original `REPORT.md` and JSON files are the historical baseline at commit
`2fe04e1fd4af52e578f04ade5245d77acd999b23`. They intentionally retain the failures
that motivated this work. The current milestone is 3.5.28; see
`COMMIT_CHECK_3_5_28.md`, the later per-pass reports, and their validation evidence.

## Sweep 1 — existing content and crafting safety (implemented, 3.5.17)

- A01–A03: strict shulker recipe matching, correct colour selection, retained inventory/custom
  NBT and independent smithing result NBT.
- A04–A07: correct copper trapdoor, granite and end-rod quantities; canonical bamboo mosaic
  slab metadata. Existing block/item IDs and old placed blocks are retained.
- A08: Pale Oak wood-family recipe tags and charcoal production from its log/wood forms.
- A09: shapeless beetroot/rabbit stews and native minecart combinations.
- A10: all six audited plant-to-dye recipes with modern quantities.
- A11, partially: recipes for the already implemented candles, campfires, calibrated sensor,
  chiseled bookshelf, copper chest/bars, scaffolding and pale moss carpet. Recipes do not
  certify every gameplay mechanic of those blocks.
- A13, partially: lead, packed ice, honey-to-sugar, lightning rod, saddle, purpur alternatives,
  daylight detector slab alternatives, sea-pickle dye and the native oak chest-boat route.
  Lodestone is reserved for its functional implementation.
- A14: native iron/gold equipment recycling, retaining a modpack's pre-existing furnace
  processing outputs. Blast furnace lookup inherits the registered recycling routes.
- A15–A16: decorated-pot suppression in the legacy profile; current scoped powder-snow
  capability evidence and removal of an incorrect lodestone registry hint.
- Additional shared causes found during the sweep: recolouring existing wool, carpet and
  beds; dyeing clear glass panes; missing copper waxing recipes. Source alternatives remain
  explicit and use actual registered items, metadata and colour-specific tags.

Validation is recorded in `after-crafting-sweep/COMPARISON.json` and `VALIDATION.json`.
The recipe count is an output-witness metric, not a full-functionality score. No new worldgen,
block/entity IDs, height encoders or backporter implementation are introduced by this sweep.

## Cooking and usage follow-up — implemented, 3.5.18

See [the follow-up report](after-cooking-usage/REPORT.md) for the corrected NEI categories,
candle-cake creation/eating, netherite smithing visibility and shift-click, explicit enabled
Iron Chests shulker-tier recipes, and the copper nugget/chain/torch/lantern production chain.
The copper ingredient row below is now implemented and tested against NTM 2.4.0 and Iron Chests;
future copper equipment remains a separate feature decision. This does not complete the
other companion chains, modern three-slot smithing or the partial gameplay endpoints.

## Production-chain follow-up — implemented, 3.5.19

See [the production report](after-production-chains/REPORT.md). Dried kelp now has its food,
200/100/600-tick cooking, storage, composting and 20-item block-fuel chain. Resin has its brick
ingredient and downstream crafting/smelting routes. Renewable resin through Creaking remains
part of the unfinished entity/world system; no alternate source was invented. The remaining
companion chains below stay open. The next substantial workstation implementation is stonecutting.

## Sweep 2 — close companion-item and production-chain gaps

Addresses A11/A12 and the remaining existing-content recipe candidates.

| Chain | Required implementation | Acceptance evidence |
| --- | --- | --- |
| Kelp | Dried-kelp item, food behaviour, storage/decompression, correct fuels, furnace/smoker/campfire routes | Actual cooking/consumption/fuel cycles; containers, quantities, save/reload, client sprite |
| Resin | Resin-brick item, smelting and brick/storage/slab/stair/wall/chiseled recipes | Exact input/output metadata, XP/time, drop and renewable-source audit |
| Copper | Modern copper nugget and its required recipes, including remaining chain/lantern/torch inputs | NTM OreDictionary/processing conflict checks; no replacement of native NTM ore/raw processing |
| Conduit ingredients | Nautilus shell/heart-of-the-sea identity and legitimate acquisition | Loot/fishing/source checks, stack behaviour and compatibility; not just creative registration |
| Leaf litter | Modern leaf cooking route, recipe-specific cooking-time support where needed | Real furnace ticks and fuel/XP verification rather than output lookup alone |

Use the current 1.21.11 recipe/tag data as the specification. Do not replace missing ingredients
with convenient substitutes and count the route as exact. Resolve the 114 unresolved-output
definitions individually; some belong to unimplemented features or special representations.

## Stonecutter follow-up — implemented, 3.5.20

The [stonecutter report](after-stonecutter/REPORT.md) records completion of the 254 represented
stonecutting routes, player-owned container, selection/scrolling UI, NEI display and extraction/
item-safety tests. The existing registry identity and metadata are unchanged. A focused visual
client check remains. The design/acceptance checklist below is retained as implementation context;
loom/cartography/grindstone/lectern are the next workstation group.

## Sweep 3 — common workstation foundation and stonecutting

Addresses S01 and the 254 stonecutting rows whose output identities already exist.

Keep the existing stonecutter registry identity. Replace its decorative tooltip/interaction with
a functional, player-owned `Container` and client GUI. Modern stonecutting does not require a
persisted block inventory, so do not introduce an unnecessary tile entity/import contract.

1. Define a gated recipe registry with stable recipe identities, exact input variants and output
   quantities, validated against the pinned modern definitions.
2. Implement server-authoritative selection and output consumption. Validate window, recipe
   index, current input, stack limits, player access and shift-click behaviour.
3. Synchronize selection/results through the container protocol. Close/drop input correctly,
   handle changed ingredients, and add optional NEI display without making NEI mandatory.
4. Test all represented stonecutting routes through actual container extraction, including
   one-input/two-slab yields, alternatives, repeated crafting, shift-click and invalid selections.
5. Perform one focused client check for selection, rendering and input/output movement.

Append new GUI IDs; preserve existing IDs and saved block/item names. This foundation can be
reused by later workstations without coupling their recipe logic to rendering.

## Sweep 4 — loom, cartography, grindstone and lectern

Addresses S02–S05. Each system needs its own completion gate within the same programme.

- **Loom:** banner/pattern inputs, modern pattern selection/limits, exact dye consumption,
  existing pattern/name preservation and client result preview.
- **Cartography:** clone/scale/lock rules, map-data ownership/IDs, output amounts, material
  consumption and multiplayer state. Retain legitimate old maps.
- **Grindstone:** repair/durability, removable enchantments versus curses, XP, names, invalid
  combinations, result ownership and shift-click.
- **Lectern:** stored book/page state, readable-book rules, GUI access, page pulses/comparator,
  book ejection, persistence and synchronization.

Anvil, enchanting, brewing, campfire, storage, shelves, pots, composters, bee processing and
cauldrons retain existing implementations. Their remaining checklist is equivalence testing,
not blanket reimplementation. Complete the corresponding production-system rows in the audit.

## Sweep 5 — modern smithing and item-state representation

Addresses S06, the nine represented modern upgrades, trim serializers and relevant unresolved
outputs/components. The old two-input upgrades currently work; that alone is not modern parity.

- Introduce the modern third/template slot, template identities, duplication and legitimate
  loot acquisition, plus trim pattern/material representation and rendering.
- Define an explicit migration/configuration policy for packs using old upgrades. Export that
  policy through runtime capabilities so clients and servers cannot silently disagree.
- Test exact consumption, output quantities, durability/enchantments/custom NBT, ownership
  isolation, rejected extra inputs, stack limits, automation expectations and restart/sync.
- Validate all special/component serializers separately. Do not flatten their NBT into a plain
  item result just to make the recipe counter pass.

## Sweep 6 — finish partial gameplay endpoints

| Audit IDs | Required systems |
| --- | --- |
| S07 | Crafter inventory, disabled slots, recipe evaluation, redstone timing, dispensing, remainders and comparator |
| S08 | Trial spawner encounter/cooldown/reward rules; vault keys and per-player reward state |
| S09 | Conduit structure/effects/targeting; bell interaction/events; respawn-anchor charging/respawn/explosion; lodestone compass binding |
| S10 | Turtle/sniffer/frog lifecycle endpoints, Creaking Heart, Dried Ghast, copper-golem reanimation, and Warden/Darkness |
| Shared world systems | Modern villager profession/job-site behaviour, renewable acquisition, biome-specific ecology and structures |

Preserve the existing state encodings while adding real behaviour. Treat these as complete
systems with acceptance tests, not recipe-only changes or substitutions such as a vanilla spawner
for a trial spawner. Warden/Darkness remains a substantial later implementation.

## Completion gates for every sweep

- Preserve/save the before state and review the exact source diff; never reset unrelated work.
- Run relevant headless gameplay/container tests, the recipe witness comparison and feature-off
  cases. Distinguish fully tested behaviour, sampled witnesses and remaining unknowns.
- Check harvesting, loot/trade, growth/generation, fluids, redstone, item/NBT state, automation,
  synchronization and persistence for each relevant block family.
- Recheck NTM compatibility when changing shared materials, fuel, recipes or hazards.
- Use one small client smoke at coherent UI/rendering milestones rather than testing each
  recipe manually. Version/build/checksum the tested result.
- Review and commit each recoverable milestone. No automatic commit, push or Git-history
  changes are part of this programme.
- Update Backporter mappings only against settled registrations/state contracts. Follow the
  required `dev` commit → download → distributed-build test workflow before release validation.

Existing imported-Earth biome storage and the later deterministic NTM resource/structure
population pipeline remain separate contracts. This crafting sweep does not certify a whole
converted Earth or activate blanket chunk repopulation.

## Loom and stonecutter display follow-up — implemented, 3.5.21

The loom now handles 32 free patterns, ten reusable paper templates, one-banner/one-dye
consumption, six-layer limits, preserved banner NBT and bulk extraction. Six template crafting
recipes match 1.21.11. Globe trading, Piglin bartering and Flow/Guster structure-loot acquisition
remain open. The stonecutter choice grid now sets the normal inventory lightmap explicitly.
See `after-loom/REPORT.md` and `VALIDATION.json`; a small real client appearance check remains.

## Scholarly workstations — implemented, 3.5.22

Lectern reading/persistence/redstone, enchanting purchase/seed/fuel/inventory safety, and
grindstone repair/disenchant/XP now have focused runtime contracts. Signed-book actions
are server opt-in and default off. Recent copied modern PNGs use AssetDirector aliases
instead. See [the pass report](after-scholarly-workstations/REPORT.md) for evidence, scope
and a brief client milestone check. Full modern enchantment/villager catalogues are not
claimed. Next: composter, fletching and cartography/maps; then smithing and crafter.

## Cartography and composting — implemented, 3.5.23

Lectern facing and book-signing input compatibility were corrected, enchanting cost
tooltips remain visible below the required level, and the cartography table now copies,
expands and locks persisted maps. Composter hopper transactions, comparator/collision,
first-item success, ripening delay and all 115 modern compost probabilities are checked.
The modern fletching table has no player crafting GUI; villager job sites remain separate.
See [the pass report](after-cartography/REPORT.md). Next planned workstation pass: modern
smithing and crafter, with the remaining original audit gaps still tracked independently.

## Modern smithing and crafter — implemented, 3.5.24

Three-input netherite upgrades, nineteen template variants, eighteen duplication recipes,
all eighteen trim patterns/eleven materials and default vanilla-only armor eligibility now
have focused contracts. Worn/GUI trim overlays use AssetDirector palette permutations.
The crafter gains persistent inventory, disabled slots, balanced automation, edge-triggered
crafting, remainders, output routing and comparator behavior while retaining its TE ID.
See [the pass report](after-smithing-crafter/REPORT.md) for acquisition and rendering limits.
Template loot sources, Flow duplication (breeze rod), modern source-item translation,
villager jobs and remaining original audit gaps remain independent follow-up work.

## 3.5.28 commit checkpoint — prioritize map backporting

The workstation/crafting milestone is closed for this checkpoint. Smithing layout,
preview facing, palette decoding, inventory overlays, template names/tooltips and
independent armor/material restrictions have been corrected and tested. Worn trims
were confirmed visually; generic inventory outlines intentionally match vanilla.
See [the final commit check](COMMIT_CHECK_3_5_28.md) for reviewed scope and final tests.

The user's next priority is Backporter integration and bounded world-conversion
testing against this EFR+ target. The remaining special-crafting and larger gameplay
sweeps above are deferred; they are not prerequisites for every next conversion test.
Keep concrete data/state preservation defects in scope and follow the Backporter
`dev` commit → download → distributed-build test workflow.
