# Configurable Overworld build height

## Behaviour and configuration

`config/etfuturum/world.cfg`, `generation` category (the existing `extendedWorldHeight` entry's category):

```properties
B:extendedWorldHeight=true
I:extendedWorldHeightBlocks=400
```

Supported physical heights are 384 through 512 inclusive, in multiples of 16. Default: 384. Height is an exclusive upper boundary: 400 permits Y=0..399; use 416 to build at Y=400. Map Compatibility Mode still disables the engine; the imported-Earth profile enables it as before. A full server restart is required. Updated clients negotiate the server height without changing their own configuration.

The EFR ceiling overrides the old `server.properties` build limit while extended height is active; an existing `max-build-height=256` no longer prevents upper-world placement. Digging and placement still respect each dimension's provider height. Nether/End remain unchanged.

World storage arrays, Anvil loading, lighting bounds, renderer allocation, initial tile sync and chunk masks use the negotiated physical height. The existing 384-block modern terrain and +64 import offset remain fixed: raising the build ceiling adds headroom and does not regenerate or vertically move terrain. No map backporter changes are required.

Login profile v2 carries height and enables an unsigned-short Y in block effect packets. New extended servers require updated clients. New clients retain the old v1 acknowledgement/layout for old servers and recordings. Unsupported/malformed profiles fail through the existing guarded login path.

A world-scoped `etfuturum_world_height.dat` marker refuses reductions below a height already adopted by this version, including disabling extended height. Raising the height is allowed. This is a safeguard, not a way to undo tall builds. Old mod versions cannot enforce the new guard. Back up a production world before changing its engine/mod versions.

## Companion compatibility

- WGMap main already had dynamic 384-height snapshots and compatible cache migration. Its companion change raises the shared supported bound to 512 and widens only the internal run endpoint. Existing terrain tile, occlusion, bundle and receipt formats/identities remain unchanged. No blanket cache deletion/rebuild is required. Old snapshots remain useful below their captured height; above that, queries remain uncertain until that area is refreshed.
- DH uses the world's provider height and actual section array length. Its client/server level wrappers and chunk wrapper need no runtime change. Existing datasets remain in place; newly changed terrain needs the normal local LOD refresh.
- ReplayMod's existing integer masks preserve sections 24..31, including the signed top bit. EFR also preserves each decoded packet's negotiated wire width when ReplayMod serializes it, independently of the client's local height setting. Its existing save safeguard still skips automatic cuts/splits when ReplayStudio cannot represent extended sections, preserving the complete recording. No runtime change was necessary. Old v1 profile reads remain supported in EFR.

## Validation

- EFR offline `check build` passed, including profile round trips at 384/400/448/496/512 and malformed height rejection.
- Disposable transformed Forge servers at 384, 400 and 512 passed provider/build limits with `max-build-height=256`, actual placement packets across Y=256 and the top boundary, S21/S26 masks, S28 effect Y, client-config-independent packet reserialization, Anvil round trips, saved-world restart and Nether isolation.
- A disposable 400 -> 384 startup was refused; region file SHA-256 values remained identical.
- WGMap companion `check build`: 1,127 tests passed. New tests cover the 512 exclusive boundary, memory/network/disk round trips, the actual Forge S3F payload transport and uncertain upper coverage in an existing 384 snapshot.
- ReplayMod chunk-codec tests: six passed via direct Java 8-targeted compilation/JUnit. Its Gradle test task could not resolve the offline Shadow 5.2.0 plugin; no full ReplayMod build is claimed.
- Supplied Crucible dev-280 CraftWorld bytecode delegates `getMaxHeight` to its Minecraft world; no Bukkit/plugin/server jar was modified. The Crucible placement path retains the server build-limit check. Runtime probes above used Forge, not a live Crucible server with the installed plugins.

WGMap build dependencies: current WGCore development output and EFR development classes were used because the local ignored dependency jars predated main's new integrations. Wargames Trailer's WGCore X211 contains the required map API. Dedicated-server class boundaries remain unchanged in WGMap; EFR's common height/profile/storage changes reference no client-only types.

## Minimum live test

On a backup/test world with matching EFR and WGMap on server and client:

1. Start at height 400. Place and break stone/glowstone around Y=255/256, 383/384 and 399. Y=400 must refuse placement.
2. Put a chest above 384, use it, disconnect/rejoin and restart the server; verify the blocks and contents persist and render correctly with the existing shader/Angelica setup.
3. Confirm the local WGMap tile, upper-platform occlusion and DH LOD update when that area changes. Do not delete or regenerate the full cache.
4. Record and reopen a short replay of the upper platform. Existing automatic extended-height cut/split limitations still apply.
5. Confirm ordinary Nether placement is unchanged.

Do not test lowering a production world's ceiling. No commits, pushes, jar installation, world edits or cache migrations are performed by this source change.

## Accepted release, 27 October 2026

The user confirmed live building after height increases, plus ReplayMod playback compatibility. The final normal-named release is `etfuturum-3.6.0_X3.jar`; it supersedes the temporary descriptive test filenames above. Matching companion WGMap is `WGMap-0.1.2_X153.jar`. Current EFR offline contracts/check/build and all 1,144 WGMap tests plus production reobfuscation verification passed. No new live lowering test was required; the prior disposable-world refusal test remains the available evidence.
