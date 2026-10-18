# Native structure import contracts

The Backporter's `native-overworld` policy now covers **35/35 modern NTM
families and 23/23 legacy Overworld entries**. Older policies retain their
smaller explicit scopes. Fresh `/efrimportreport` exports include native biome
predicates, rates/toggles, named fluid IDs, item IDs, loot-pool availability,
procedural child terrain rules and native save-codec registration.

The bounded test capture contains 274 cases: modern roots in four rotations,
all eight procedural children in four rotations, 16 connected bunker samples,
23 legacy entries in four samples and an additional high-terrain ancient tomb.
The tomb port matches all 24,209 native cells at a surface near Y287 as well as
all four low-terrain references. Captures are development evidence; production
adapters separately validate every supported block/tile/action contract.

`ImportedStructureActions` stores pending instructions in root chunk compound
`etfuturum:imported_structure_actions`. Version 1 uses live Forge dungeon
chest/spawner tables. Version 2 adds named native NTM inventory pools,
deterministic seeds and reviewed fixed-item overrides. Empty inventories are
required. All targets/results are validated before any mutation, changed or
nonempty targets are refused, and fulfilled requests disappear from the chunk
save carrying their resulting contents. The code runs only in server imported
Earth Overworld mode and invokes no generic decorators. Native inventory
setters retain normal comparator notifications.

The production exporter and action handler use optional reflection rather than
a hard NTM dependency. Capture-only loot observation records the native pool,
count, pre-roll RNG state and inventory changes without advancing the observed
RNG. It never ships in the release jar. Fixed items are rebound by canonical
registry identity; source numeric IDs are not copied into a different registry.
Empty machine/proxy states and scheduled updates remain strict typed contracts.

Actual converted modern proof: 3,076 full chunks, all 35 families/180 variants,
809,108 native block checks, 3,563 tile payloads, 7,782 scheduled updates and
180 native structure starts. Actual legacy proof: 1,446 full chunks, all 23
entries/92 samples, 311,785 block checks, 3,017 tile payloads and 4,790 updates.
The suites exercise native Anvil read/write, native factories, real Forge
load/save events, live loot/reference RNG, fixed overrides, refused overwrites,
one-shot completion and disk restart. Keyhole activation checks native cracked
key consumption, rooms/doors in all four directions and tile reload at Y12/Y272.
The finite codec world isolates comparator notifications; this is not redstone
network or full-pack certification.

Run `scripts/run_native_resource_tests.py --capture-structures` with installed
`--ntm-jar`, `--ae2-jar`, `--gtnhlib-jar` paths and the existing accepted EULA.
This uses the separate `build/earth-structure-combined-tests` fixture. In the
Backporter, run `scripts/verify_earth_overworld_native.py` and
`scripts/verify_earth_legacy_native.py` with `--native-world`, `--captures` and
a fresh `--output`. Validate each resulting artifact directory with EFR's
runner using `--converted-ruins /absolute/artifacts`. Both phases must pass.
Do not run capture and conversion concurrently against the same bundle.

The legacy coverage fixture enables the normally disabled Bosnia/explosive-gas
entries and suppresses modern placement weights only in its copied test
capabilities. Real imports honor authoritative pack settings. Procedural
imports select among captured native compositions and use deterministic
coordinate streams; they are not exact whole-mod decorator RNG replay.

Final validation requires the rebuilt EFR jar, a fresh capability export from
Release Testing, and a small import using the committed/downloaded `dev`
Backporter. Check coastal placement, bunker/tomb access and loot plus
representative NTM/AE2 resources. Existing workstation tests need not repeat.
Checkpoint/resume and a full Earth conversion remain outside this validation.
