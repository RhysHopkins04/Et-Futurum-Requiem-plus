# Imported Earth foundation — EFR first

This is the first EFR implementation milestone, not the NTM population system or a completed Earth release. Backporter implementation has not been changed. Existing uncommitted NTM ore compatibility work was preserved; no commit or push was made.

## Implemented

- `importedEarthMode` in `config/etfuturum/mapcompat.cfg`, default **false**, separate from the old `mapCompatibilityMode`. The modes are mutually exclusive and require restart. Earth mode retains the 384-height engine and configured deepslate ore/content support, while suppressing EFR automatic generators, provider replacement and chunk-load terrain replacement. Earth mode retains crafting/ore mappings; the old content-only profile still suppresses them.
- Sixteen opt-in modern **surface identities**: cherry grove, grove, snowy slopes, jagged/frozen/stony peaks, meadow, mangrove swamp, bamboo jungle, warm/lukewarm/cold ocean, deep lukewarm/cold/frozen ocean, pale garden. Defaults are configurable IDs 210..225, not guaranteed free in another pack. Registration validates the complete allocation before modifying the biome array and rejects conflicts. ID255 is rejected because legacy chunks use it as the unset sentinel. Existing vanilla identities remain available separately.
- Basic climate/water color and Forge family tags, no automatic terrain decoration, and explicit legacy-family natural-spawn fallbacks. This does not claim modern fauna/terrain parity. Logical altitude correction applies to the imported biome classes only. Native mod class/equality checks still need explicit adapters or the future population catalogue; tags alone cannot repair those checks.
- Server-side persistent named 3D biome data, with an independent legacy 2D surface fallback. Original cave/custom names survive even when they do not resolve to registered surface biomes. No implicit client synchronization or client F3 underground-biome display is claimed.
- World-scoped profile/biome-ID guard. An Earth world saved with the new profile refuses incompatible height/profile or changed biome IDs. Do not change an existing Earth's biome allocation; use an explicit future migration or a new conversion.
- Registration of the existing infested-deepslate implementation when deepslate is enabled.
- `/efrimportreport` (operator permission level 2) exports `efr-import-capabilities.json` into the Overworld save: actual active provider/manager, height, profile, mod versions, biome mapping, block/item IDs and data-schema capabilities. This is runtime evidence, not a blanket compatibility certificate or JAR-hash provenance.
- Shifted imported ground/cloud/horizon references without replacing NTM's provider. Third-party sky renderers may use their own reference model and need the one client check.

Both client and server must use matching EFR build, profile and biome-ID configuration. These are not per-world toggles that can be flipped safely while playing. The saved-world guard is not a substitute for client/server configuration agreement.

## Automated checks

Ordinary `./gradlew --offline build` now executes `earthImportContracts` through `check`; it no longer relies on a `test NO-SOURCE` message for these data contracts. No EULA, world or NTM JAR is needed for the data contracts.

Run the headless matrix with an already accepted `run/server/eula.txt`:

```sh
python3 scripts/run_earth_import_tests.py
python3 scripts/run_earth_import_tests.py --profile legacy
python3 scripts/run_earth_import_tests.py --ntm-jar '/absolute/path/to/installed-NTM.jar'
python3 scripts/run_modern_state_tests.py
python3 scripts/validate_plus_map_compat.py
python3 scripts/validate_fidelity_pass_37.py
python3 scripts/validate_fidelity_pass_38.py
```

The optional NTM JAR is deobfuscated locally through RFG and included only for that test invocation. The normal runClient/runServer dependency list is unchanged. Probe mods are outside the distributable source set. The tests use separate `build/earth-import-tests/` worlds/configs and do not launch a client or modify user worlds/modpack configs.

Coverage:

- All 1,536 quart samples across 24 sections, schema/type/index validation, sparse fallback, unknown names and immutable storage.
- Saved biome-ID validation and occupied-ID rejection without overwriting vanilla biomes.
- Earth and legacy profile behavior, incompatible profile rejection, and no automatic EFR IWorldGenerator registrations in Earth mode.
- Earth-mode smithing: all nine netherite upgrades, quantities, damage/custom NBT retention, unchanged inputs and rejection of wrong materials/reversed slots; legacy mode retains built-in recipe suppression.
- Physical blocks at Y0/64/255/256/383 through actual Forge Anvil save and a fresh process restart, including negative X coordinates.
- Surface biome bytes and simultaneous underground/surface source identities after disk reload.
- Transformed S21 and S26 codecs: high section masks, high block IDs, multiple chunks, compression/decompression payloads.
- With the installed NTM: native `com.hbm.dim.WorldProviderEarth` remains active, height is 384, other loaded dimensions retain their height; all 28 individual NTM deepslate ores/clusters and the hidden compatibility family retain deepslate self-drops and native resource drops through EFR's global harvest handler at Fortune 0 and 3. The ore suite also checks cluster/basalt metadata and drops, retirement of non-vanilla raw forms, mixed-mod menu visibility, and exact native-stack recovery from silk-picked variants. Both raw-as-ore settings are tested. The actual NTM smelter/shredder/centrifuge/crystallizer tool abilities and Silk Touch are tested for all twenty-eight independent ore/cluster identities; hidden legacy identities remain readable. Native uranium/asbestos gas hooks, coal/lignite mining dust and breathing exposure, and all twelve new native-overlay sprites also have automated checks. Only vanilla copper/iron/gold raw forms remain active; hidden legacy raw IDs have no material tags, recipes or drop mappings. See [the NTM ore review](NTM_ORES.md) and its pinned modern-source matrix.
- Existing modern-state contract/gameplay/save/restart suite (contained water, kelp state, powder snow/freezing and migration).

The cumulative static checker contained six pre-existing stale expectations (dripleaf constructors, wet identity, Crucible-compatible bulk compression and model lookup). Before/after comparison confirmed them; they now check the current implementations. The older Pass37/38 version gates now allow newer staging versions while still checking the original feature contracts/provenance. The staging build is now 3.5.16, with the in-game version aligned to the jar filename.

## Actual NTM run and limits

Tested optional NTM input: the locally built `HBM-NTM-[1.0.27_X5778_H261_WG2.4.0].jar` (earlier foundation checks also used WG2.3.0). See `VALIDATION.json` for its SHA-256, the built artifact hash and pass results. The test uses disposable development configs; it is not the entire installed modpack or production configs. Full native machine execution and all raw-item recipe paths, petroleum/mineral-node machinery, custom sky/Angelica rendering, multiplayer handshake, and full-map resource distribution are not certified by the drop/provider checks.

The development logs include non-fatal third-party FML module-info/version-check messages and NTM OreDictionary entries with invalid legacy metadata that EFR explicitly reports and ignores. Assertions verify the selected native ore wrappers despite these messages. This is not claimed to be a warning-free modpack launch.

## One bundled client smoke test, when EFR changes are ready

Do not boot repeatedly for individual data changes. Finish the automated matrix, then use a disposable small world with the intended EFR/NTM/client-renderer configuration:

1. Enable **only** `importedEarthMode=true`, with `mapCompatibilityMode=false`, and the same imported biome IDs on client/server. Preserve the current NTM/EFR ore config.
2. Boot via the usual `runClient` workflow with the intended NTM/renderer dependencies. Confirm no mixin/provider/biome collision error.
3. In creative mode check blocks near Y255/256 and Y383: render, place/break, relight and save/reload. A few blocks are sufficient, not a tour of the whole map.
4. Check a small wet coral/kelp patch and warm-vs-cold ocean water tint; use a prepared imported biome fixture, not a world generated by the new identities (they do not generate terrain).
5. Run `/efrimportreport`, save/quit and retain the JSON plus the startup log. Reopen once to confirm the fixture persists.

The automated world is a technical fixture; do not distribute it as Earth. No client was launched for this implementation. Do not use the four-million-chunk world for the smoke test.

## Before moving to the backporter

Keep saved entity migration excluded. Native deposit/container block entities remain in scope.

- Review the client smoke and target-pack capability export; freeze the biome/storage contract and refresh the saved Forge template after new registrations.
- Backporter changes then consume the capability export, preserve source 3D biome palettes, select surface IDs from local terrain, and fix basalt/redstone state encodings. They must report unknown/merged biome identities and incomplete source chunks.
- NTM resource/structure population remains a separate explicit, deterministic, resumable Earth-only pipeline as described by the v4 audit. Registering these EFR capabilities does not add NTM resources to existing chunks. Do not turn off TerrainPopulated to run legacy population.
- For backporter release verification, follow the user's required workflow: local automated tests first, commit reviewed changes to `dev`, then re-download and test the distributed build. A source-only run is not final release validation. No backporter commit or push is authorized/performed by this milestone.
