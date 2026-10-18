# Imported terrain boundaries, water and HarvestCraft — 2026-10-18

## Confirmed ore cause

The played test save put the player at X40.905 Y128.486 Z489.952, chunk (2,30),
outside the copied spawn patch (its south edge is Z479). The pristine converted
r.0.0.mca had 228 chunks; the played copy had 1,024. More importantly, neighboring
native generation changed **already imported chunks**: normal coal near deepslate
in that audit increased from 1 to 1,870, and normal iron from 62 to 1,760. NTM
ordinary ores and clusters also appeared. `TerrainPopulated=1` does not prevent
another chunk's decorators from crossing the boundary.

The imported-Earth profile now wraps both ChunkProviderServer's native population
and Forge GameRegistry generation. During those calls only, World block/metadata
writes, tile replacement/removal and access to imported tiles are blocked in
chunks carrying the imported-biome marker. Ordinary gameplay, machines and ticks
remain outside this generation scope. Finally-safe nesting restores the scope
after failure. This is a population boundary guard, not a general protection API
against generators that bypass World and mutate Chunk storage directly.

The imported marker persists in Anvil and is loaded before normal population.
Existing altered chunks are not repaired by installing this jar. Use a new
conversion/test copy. Walking outside a partial modern sample still generates
unrelated native target terrain, which is not a fidelity test of that sample.

Some stone-textured ores genuinely exist in the modern source (for example target
57,61,179 is source iron_ore). Those preserve their original identity. The
Backporter now clips new geological layers around the six source-host neighbors
of original ores, across section/chunk boundaries, rather than changing source
ore drops to an unrelated NTM gneiss ore.

## Water

`modernImportedWater=true`, under map compatibility mode, enables this profile
when importedEarthMode is active. Vanilla still/flow and underwater textures are
bridged to the existing AssetDirector 1.21.11 client assets, including animation
metadata. No raster assets were bundled.

The 65 biome profiles were extracted from the locally installed official 1.21.11
client biome JSON and its ceg environment-attribute defaults: water fog starts at
-8 and ends at 96 blocks; swamp visibility multiplies the end by 0.85. Surface
water colors blend over 3x3 columns; custom mod biome tints are retained. The
Overworld underwater handler uses the biome fog color/distance with linear fog,
limited by render distance, and retains blindness. Disable the option for shader
compatibility. This ports the profiles to the legacy renderer; it does not claim
the complete modern rendering pipeline or timed underwater adaptation.

## HarvestCraft handoff

`/efrimportreport` now exports actual installed HarvestCraft block IDs, flags,
rates and registered Forge-biome eligibility. The supported audited jar is Pam's
HarvestCraft 1.7.10Lb SHA-256
b0dc253702948d19d9c2d5191ae78ae022b65fac73210ca7313d7ef3b1cd7a57.

The optional native probe records four layouts for each of 34 native tree
entries (33 fruit types, coconut has two biome groups), plus live placement
supports for all 12 gardens. Mushroom gardens retain the native horizontal logs.
A late compatibility hook removes the native fruit/log tree generators' 256
height restriction so their saplings can grow on imported high terrain.

The separate Backporter native-projected policy verifies the jar, capture hash,
live config values, biome IDs and target registries. It plants only where the
original source permits the entire feature, reserves native structure footprints,
and never runs generic Forge population over converted chunks. It includes
native gardens, captured fruit/log trees, beehives and stone-host salt. Placement
uses independent deterministic feature streams, modern-surface projection with
native height-sampling probability, and conservative whole-tree air clearance.
It is not a byte-for-byte recreation of the mod's whole-world RNG order. Maple
retains the Lb generator's unusual tropical+coniferous nesting.

## Validation and next client check

- Native/Forge boundary attempts, gameplay writes, exception cleanup and saved
  imported markers passed a real headless server reopen.
- All 136 native tree captures also generated above Y255. A real server saved and
  reopened 2,350 tree/garden cells above Y255 without changes.
- `check build` verifies production SRG mappings for the boundary and water hooks,
  checks water profiles and excludes test classes from the release jar.
- Backporter: 791 tests; complete 2,888-chunk synthetic-contract integration
  conversion, zero failures, all regions round-trip verified. Serial replay of
  228 chunks matched parallel output byte-for-byte. The synthetic integration
  contract is restricted to ignored build fixtures, not a substitute for the
  real full-pack capability export.

Install the tested EFR jar, open **Backport Template Earth** in Release Testing,
run `/efrimportreport`, save and quit. Refresh the clean test template and build a
schema-4 resource plan with that genuine export before using the downloaded dev
Backporter. In the new test conversion, check water visibility and one garden,
fruit tree and beehive. Do not reuse the already altered played test save.

## Angelica water tint correction

The first water pass only hooked BlockLiquid.colorMultiplier. The installed
Angelica 2.2.14 wraps that method and uses SmoothBiomeColorCache.resolveColor,
whose WATER branch calls BiomeGenBase.getWaterColorMultiplier directly. Ordinary
legacy ocean biomes return white, leaving the modern grayscale asset untinted;
contained-water plants used the correctly tinted RenderBlocks path.

ClientProxy now registers a Forge GetWaterColor listener using the same profile
resolver as the block hook. The resolver accepts the existing event tint, avoids
recursive getter calls and retains custom mod tints. No optional-renderer API or
additional mixin is needed; Angelica's reusable biome event still runs through
the Forge event bus. No saved blocks, light arrays or biome IDs change.

ImportedWaterTintProbe exercises actual Forge biome getters for ordinary ocean,
river, swamp, frozen ocean and imported warm/lukewarm/cold oceans and mangrove
swamp. It checks agreement with the block path, custom tint retention and the
disabled-option fallback without mutating biome fields. Native probe and release
check/build passed. The remaining client check is to reopen the existing
converted world and confirm ordinary water matches the waterlogged-plant tint.
