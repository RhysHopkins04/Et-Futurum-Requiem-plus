# Et Futurum Requiem Plus fork notes

This repository is a compatibility-focused fork of **Et Futurum Requiem** for Minecraft 1.7.10 / Forge 10.13.4.1614. The upstream `etfuturum` mod ID, existing registry names, package structure, configuration keys and saved-data identities are intentionally retained wherever possible.

The original Et Futurum Requiem `LICENSE` remains in place. Fork-specific changes are recorded here so they can be reviewed independently of upstream work.

## P001 — Map Compatibility + Lush Roots

This stage deliberately does three things only:

1. establishes a restart-required **Map Compatibility Mode** / content-provider profile intended for imported maps and RTG-based packs;
2. records the requested missing-content audit against the attached upstream source tree;
3. adds the first small, self-contained missing lush-cave batch: `rooted_dirt` and `hanging_roots`.

Large systems such as campfires, aquatic plants/coral, dripleaf, hanging signs and container/tile-entity-heavy blocks are deferred rather than added as decorative placeholders.

## Map Compatibility Mode

New config file:

`config/etfuturum/mapcompat.cfg`

New master option:

`mapCompatibilityMode=false`

The default is **false**, preserving ordinary upstream Et Futurum behaviour. Changing this option requires a full game/server restart because some gates are evaluated during early-mixin selection and Forge pre-initialisation.

When enabled, the mode leaves normal content registration available but suppresses the progression/world-generation paths which are unsafe or unwanted for map-import / RTG use:

- does not register Et Futurum's early, normal or late `IWorldGenerator` instances;
- does not register Et Futurum's terrain-generation event handler;
- does not load the deepslate ore-replacement chunk mixin;
- forces the Et Futurum Nether and End dimension-provider replacements off;
- forces End City generation off;
- forces tile/chunk replacement mode to disabled (`-1`);
- suppresses automatic Et Futurum crafting/smelting progression registration performed by `ModRecipes`;
- suppresses Et Futurum additions to normal dungeon/stronghold/Nether-fortress/End-City loot tables while retaining the internal composter output table used by the functional composter;
- suppresses raw-ore and deepslate-ore integration initialisation, modded raw/deepslate ore support and raw-item-as-ore OreDictionary registration;
- disables Backlytra/Elytra mixins;
- disables Shulkers and Shulker Boxes;
- disables dragon-respawn progression entities;
- suppresses Et Futurum natural spawn-table additions/reweights and Husk/Stray special-spawn replacement;
- suppresses automatic replacement of loaded vanilla boats, villager zombies and snow golems with Et Futurum replacements.

Functional registries which make already-present blocks behave correctly are intentionally retained. Examples include composting inputs, stripped-log behaviour, bee-plant recognition and piston behaviour. The profile is intended to make Et Futurum a content provider, not to turn functional blocks into inert decoration.

### RTG boundary

With Map Compatibility Mode enabled, this patch removes the Et Futurum generation/provider hooks found in the audited source which could directly compete with RTG terrain generation. RTG integration still requires an actual client/server smoke test in the target pack before this should be treated as production-verified.

No dedicated Et Futurum retro-generation subsystem was found in this source tree; the relevant terrain mutation paths are the world generators, terrain events, dimension providers, deepslate chunk-generation mixin and tile replacement path gated above.

## Missing-content audit

The audit covered `ModBlocks`, `ModItems`, configuration, renderers, tile entities, recipes, world generation, resources and the source classes matching the requested content families. The list below is intentionally about *usable exposed implementations*, not merely filenames or translation strings.

### 1. Already implemented and usable

Selected requested/closely-related content already present upstream and therefore **not duplicated**:

- cave vines plant;
- cave vines;
- glow berries item/behaviour and cave-vine growth state;
- glow lichen;
- moss block and moss carpet;
- azalea / flowering azalea and azalea leaves;
- mangrove roots / muddy mangrove roots;
- pointed dripstone and existing dripstone-family support;
- standing/wall signs for the existing supported modern wood families (these are not hanging signs).

### 2. Implemented but incomplete, disabled, or not properly exposed

- **Lodestone**: `BlockLodestone`, `ItemLodestoneCompass`, an item renderer and the required textures exist, but the block/item are not registered in `ModBlocks`/`ModItems`. The compass renderer is effectively empty and no complete lodestone-target binding/needle behaviour is present. This should be finished as one coherent feature rather than merely registering the orphan classes.
- **Stonecutter**: registered upstream, but its own configuration describes it as decoration-only with no functionality. It needs a separate functional pass if modern behaviour is desired.
- **Sculk / Sculk Catalyst**: existing unfinished/experimental source is present. Per Et Futurum Requiem Plus scope, no additional sculk work is planned here.

### 3. Genuinely missing and worth implementing

#### Lush caves / vegetation

- small dripleaf;
- big dripleaf;
- big dripleaf stem/state handling;
- spore blossom;
- **rooted dirt — added in this patch**;
- **hanging roots — added in this patch**.

#### Aquatic

- kelp;
- kelp plant;
- seagrass;
- tall seagrass;
- sea pickle;
- live/dead coral blocks;
- coral plants/fans;
- wall coral fans.

These should be implemented with deliberate 1.7.10 underwater placement/survival rules rather than fake full cubes. Waterlogging does not exist natively in 1.7.10, so each family needs a compatibility design before implementation.

#### Decorative / building / functional

- candles and coloured candles;
- candle cakes;
- froglights;
- reinforced deepslate;
- powder snow;
- scaffolding;
- hanging signs, including supported modern wood families;
- dried kelp block;
- suspicious sand and suspicious gravel;
- decorated pot;
- respawn anchor;
- bell;
- lectern;
- grindstone;
- campfire;
- soul campfire.

Translation strings alone were not counted as implementations; in particular, lectern/grindstone language entries do not correspond to usable registered block implementations in the audited source.

### 4. Intentionally excluded/deferred

- all new sculk work requested for exclusion;
- natural generation for newly added Et Futurum Requiem Plus content unless explicitly requested later;
- recipes/loot/progression for Et Futurum Requiem Plus content while Map Compatibility Mode is enabled;
- broad world-generation recreation of modern biomes/caves;
- campfire source integration from the attached Campfire Backport project in this patch.

## First new block batch

### `etfuturum:rooted_dirt`

- full block with vanilla-style rooted dirt texture;
- rooted-dirt sound set already present in upstream Et Futurum's modern sound definitions;
- dirt map colour;
- shovel harvest class;
- hardness `0.5F`;
- bonemeal can create hanging roots directly below when space is available.

### `etfuturum:hanging_roots`

- crossed-plant rendering / no full-cube collision inherited from the 1.7 plant block path;
- vanilla-style hanging-roots texture;
- hanging-roots sound set already present upstream;
- ceiling support check against the downward solid face of the block above;
- neighbour updates naturally drop the roots when support disappears;
- Cave plant type;
- included in automatic composter inputs at the upstream 30% tier.

Both blocks are controlled by a new `enableLushCaveBlocks=true` content toggle. The toggle does **not** enable any lush-cave world generation.

## Campfire Backport licence boundary

The attached Et Futurum Requiem source carries the GNU LGPL v3 licence. The attached Campfire Backport source carries GNU GPL v3. No Campfire Backport Java source, assets or package code has been copied into this Et Futurum Requiem Plus patch.

For later campfire work, the conservative path for keeping this fork's current licensing model is an independent implementation based on vanilla behaviour and general behavioural observation. If direct GPL-covered Campfire Backport code is ever desired instead, the distribution/licensing consequences for the combined work should be decided explicitly before code is incorporated.

## Validation status for this patch

P001 has now passed its local development validation gates on the fork checkout:

- `scripts/validate_plus_map_compat.py` passes;
- `./gradlew clean build --no-configuration-cache` completes successfully after supplying the legacy optional GTNH compile-time artifacts required by the upstream build;
- `runClient` boots successfully on Java 8 with Map Compatibility Mode disabled;
- `rooted_dirt` and `hanging_roots` are present and functional in-game;
- bonemealing rooted dirt creates one hanging-roots block directly below when space is available;
- `runClient` also boots with `mapCompatibilityMode=true`;
- a new integrated-server world creates, loads, saves and shuts down successfully with Map Compatibility Mode enabled.

A full target-pack dedicated-server + RTG integration smoke test is still intentionally deferred until the compatibility/content profile is closer to final, and should be completed before a production release.
