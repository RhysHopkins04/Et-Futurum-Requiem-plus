# Native NTM resource export and validation

The capability report now includes additive `ntmNativeResources` schema 1.
The optional reflection bridge reads registered mineral-resource item IDs,
native fluid IDs, Earth density categories, four actual native node NBT
templates and density samples. Missing NTM reports `available=false`; reflection
errors are reported and resource-plan preflight refuses an unavailable contract.
Biome-only capability exports remain compatible.

Rebuild EFR before producing a new imported-Earth template report with
`/efrimportreport`. The existing Release Testing pack has not been changed or
certified by this source build. Do not copy IDs from an automated fixture into
a production template. Backporter resource algorithm `earth-resources-v2`
requires the fresh runtime contract.

## Automated verification

`./gradlew --offline build` validates compilation and the Earth import contracts.
For the native runtime probe, run from the repository root:

```sh
python3 scripts/run_native_resource_tests.py --ntm-jar '/absolute/path/to/NTM.jar'
```

The runner uses only `build/earth-resource-tests/server`, the existing accepted
EULA, and disposable copied configuration. It runs a save and restart. All four
native mineral-node tiers must load, produce native resource output through
excavator collection with drill/fluid requirements, and persist. The probe also
checks ordinary/bedrock oil fluid and fracking contracts. This does not exercise
a complete powered drilling/fracking machine cycle.

An optional `--converted-chunk /absolute/path/to/chunk.nbt.gz` validates actual
Backporter-produced node tile entities through native NTM loading/writing,
including exact payload round-trip and resource-item identity. It loads those
records only; this does not certify all terrain or other mods in that chunk.
Six generated node records passed this test in both save and restart phases.

The native fixture is an isolated Forge/NTM test, not a full Release Testing
pack import. The later release gate remains a fresh target template, a reviewed
Backporter commit to dev, the downloaded distribution and a small imported-map
runtime check. No production saves, installed jars or Git history were changed
by this pass.

## Resource flora extension

The same native export now includes `floraContracts` schema 1: actual resource
flower metadata, canonical flower/reed/oily-soil names and the target world's
NTM impact flag. Reading that flag does not create or alter impact save data.
The optional Backporter resource-flora policy requires these fresh fields.
Older exports still support underground v2 when flora is disabled.

The isolated probe additionally tests actual native flower drops/metadata, reed
water support, willow irrigation and all growth stages, consumption of oily
soil, native leaf drops and the registered cadmium crystallizer recipe. A seed
willow on oily soil is saved and checked after process restart. Test-only plant
placement never runs in ordinary runServer/runClient or in a production save.

## AE2 meteorite extension

`ae2NativeResources` schema 1 exports effective meteorite flags, spacing/chance,
loot tables, actual OreDictionary ItemStacks and native shape/loot/grid samples.
The optional reflection bridge leaves AE2 absent/incompatible reports unavailable
without introducing a hard dependency or changing the export world's terrain.

`run_ae2_resource_tests.py` tests the installed AE2 and GTNHLib jars in a separate
headless build fixture, optionally alongside NTM. `--artifacts` accepts the bundle
extracted by the Backporter from a real converted test output. The probe checks
exact chest NBT and native loot, restart persistence, SpawnData readability,
offline compass data, a real compass rescan and its computed direction. Its
runtime classpath replaces the older development GTNHLib only for this opt-in
probe. Normal client/server tasks retain their configured dependencies.

The separate native structure catalogue, capture tooling and current partial
Backporter adapter are documented in [NATIVE_STRUCTURES.md](NATIVE_STRUCTURES.md).
