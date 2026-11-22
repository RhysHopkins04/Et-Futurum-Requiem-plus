# Native waterlogging

Plus stores source water alongside the block state of stairs, single slabs, and leaves. These classes keep their existing block IDs and metadata. Campfires, dripleaf, and aquatic blocks keep their native water encodings. This uses the existing Plus runtime dependencies and works at both vanilla and extended world heights.

A water bucket fills a supported block and an empty bucket drains it. Placing a supported block into a source retains the water. Breaking a filled block releases a source. Water spreads through open faces using the Plus contained-water flow hooks.

The standard renderer and Angelica-WDG use the existing Plus contained-water rendering path. The Angelica adapter snapshots the water alongside the host block, adds the water mesh to the translucent pass, and retains the shader material and lighting path used by aquatic blocks.

Water in a leaf cell is visible through the foliage cutouts. The leaf cell holds a source without feeding outward flow, matching the Java Edition behavior described in [Minecraft Wiki’s Waterlogging reference](https://minecraft.wiki/w/Waterlogging). Eligibility uses the Forge leaf hook and the same leaf class or material check as Foliage+, including Plus mangrove and pale oak leaves. Foliage+ continues to decide which entities can enter each block. An entity that enters a wet leaf cell can be submerged.

## Saved chunks

The optional `Level.etfuturum_waterlogging_v1` byte array stores a chunk's wet cells as a little-endian bitset. Bit `(physicalY << 8) | (localZ << 4) | localX` refers to the host in the normal Anvil section arrays. The byte index is the bit index divided by eight and bit zero is the least significant bit. The tag is absent when the chunk has no overlay water. Height uses the target world's physical coordinates, with up to 512 levels.

For future conversion, write the overlay bit only when the mapped target host supports it. Continue writing the existing native states for aquatic blocks, campfires, and dripleaf. Verify a disposable converted chunk after loading and resaving it in Forge. A Distant Horizons integration can use the chunk's water snapshot alongside the host geometry when generating or invalidating LODs.

The earlier dependency-based prototype used [LiquidLogged](https://github.com/GTNewHorizons/LiquidLogged) and [upstream EFR's opt-in approach](https://github.com/GTNewHorizons/Et-Futurum-Requiem/pull/147). This implementation uses Plus's existing water and Forge chunk hooks. A save created with the earlier external provider stores water in that provider's format.

## Candidate validation

`3.8.0_X20` passed `check build`. Disposable Forge singleplayer fixtures passed at Y67, Y276, and Y500. They exercised Survival bucket use, placement into a source, source restoration on break, client sync, and the existing campfire and dripleaf states. The Y276 fixture passed save/reopen and an independent read of the stored Anvil tag. Angelica-WDG 2.2.14_X13 rendered the water at Y276 with shaders disabled and with the Bountiful Stars WDG shader pack active. Distant Horizons LOD rendering and a dedicated-server session remain untested.

### Dedicated-server startup fix: 3.8.0_X21

The reported Crucible `1.7.10-staging-667a28e2` startup crash came from reflective construction of `WaterloggedChunkHandler`. Its scheduled lambda caused the JVM to resolve `WorldClient` on the physical server. The handler's client entry point is now side stripped, and the scheduled work lives in a separate client-only receiver. The main-thread scheduling and connection/dimension checks are preserved.

`check build` passed for X21, including release mapping checks. The opt-in `scripts/waterlogged_server_tests.gradle` regression uses the side transformer from the supplied Crucible dev-280 server JAR against the production mod archives, with client classes forbidden. X20 reproduced `NoClassDefFoundError: net/minecraft/client/multiplayer/WorldClient`; X21 instantiated successfully. A disposable Forge dedicated development server also reached `Done` with the fix.

This validates startup and the reported classloading failure, not a full Crucible modpack session. Dedicated-server joining, waterlogging sync, reconnect/restart, and pack performance still need testing. The reported singleplayer slowdown, including with DH rendering and Angelica's deferred updates disabled, remains unresolved. No performance improvement is claimed by X21. Both master refs remain unchanged; this candidate belongs only to `feature/liquidlogged-waterlogging`.

### Wet leaves candidate — 3.8.0_X22

`check build` and the Crucible classloading regression passed. Disposable clients verified vanilla oak, Plus mangrove and pale oak, and NTM rubber leaves. Water showed through the cutouts with standard Forge and with Angelica-WDG X13, including the Bountiful Stars WDG preset. Fill, drain, client sync, water at eye level, and sealed outward flow passed. With Foliage+ X15 in player-only mode, dry and wet leaves let players through and kept collision for mobs at Y67 and Y276.

Foliage+ X15's separate contact scan for slowdown and rustling still stops at Y255. Slab and stair water heights are the same as X21.
