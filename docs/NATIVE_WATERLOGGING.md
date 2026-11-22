# Native waterlogging

Plus stores source water alongside the supported host block state. The [Java 1.21.11 coverage matrix](WATERLOGGING_JAVA_1_21_11.csv) lists the tested hosts and their legacy IDs. Campfires, dripleaf, decorated pots, and aquatic blocks keep their native water encodings. This uses the existing Plus runtime dependencies and works at both vanilla and extended world heights.

A water bucket fills a supported block and an empty bucket drains it. Placing a supported block into a source retains the water. Breaking a filled block releases a source. Water spreads through open faces using the Plus contained-water flow hooks.

The standard renderer and Angelica-WDG use the existing Plus contained-water rendering path. The Angelica adapter snapshots the water alongside the host block, adds the water mesh to the translucent pass, and retains the shader material and lighting path used by aquatic blocks.

Water in leaves and regular mangrove roots is visible through the cutouts. These blocks hold a source without feeding outward flow. Muddy mangrove roots reject waterlogging. Leaf eligibility uses the Forge leaf hook and the same leaf class or material check as Foliage+, including Plus mangrove and pale oak leaves. Foliage+ continues to decide which entities can enter each leaf block. An entity that enters a wet leaf cell can be submerged. [Minecraft Wiki’s Waterlogging reference](https://minecraft.wiki/w/Waterlogging)

Kelp and seagrass carry inherent water. An empty bucket leaves them intact and a successful break leaves a source. Kelp accepts source and downward-flowing water at placement. Sea pickles, coral plants and fans, campfires, and dripleaf use their own wet states. [Minecraft Wiki’s Kelp reference](https://minecraft.wiki/w/Kelp)

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

### BetterFoliage and aquatic behavior — 3.8.0_X24

In a disposable copy of Wargames Event 2.0.16 with shaders off, X22 stored water in oak, mangrove, azalea, and NTM plastic leaves while BetterFoliage drew the foliage in place of the water overlay. The corrected render path, first packaged in X23 and carried into X24, showed water in all four, pale oak, NTM dead leaves, and regular mangrove roots with Angelica-WDG X15. The host still uses its usual renderer. Native water heights for stairs and slabs are the same as in X22.

X24 passed `check build`, the Crucible classloading regression, and the Angelica-WDG X15 material and shader contract checks. Headless Forge fixtures at Y276 covered fill, drain, source placement, break, and persistence. Aquatic checks included kelp, seagrass, tall seagrass, sea pickles, tube coral, and both the campfire and big dripleaf wet states. The pack’s NTM JAR passed bucket fill and drain for plastic leaves, dead leaves, concrete stairs, and concrete slabs. In the confined ordinary-water test, the first step took five game ticks and the first cell cleared after 35 ticks once the source was removed. The previously reported singleplayer wall-clock slowdown remains unresolved.

Pack testing on Wargames Event 2.0.16 confirmed leaf, stair, and slab water rendering with shaders on and off.

### Fences, panes, and automation — 3.8.0_X25

A supported dry partial block can become waterlogged between two accessible source cells, including sources held in other waterlogged blocks. The scheduled update checks that both sources are still present. Sealed faces, flowing water, and missing support can prevent source formation. [Minecraft Wiki’s Slab reference](https://minecraft.wiki/w/Slabs)

An empty bucket in a dispenser now collects removable contained water. The underlying block stays in place, and the filled bucket goes into the inventory or is ejected if the other slots are full. Ordinary water, lava, and other targets use the pack's existing bucket behavior. [Minecraft Wiki’s Dispenser reference](https://minecraft.wiki/w/Dispenser)

Headless Forge checks at Y67 and Y276 passed source formation, blocked-face behavior, normal five-tick water spread, and dispenser pickup for both general and aquatic water. The Y276 fixture retained the new waterlogged blocks after restart. The packaged JAR passed `check build` and the Crucible classloading regression.

Pack testing subsequently confirmed vanilla and modded glass panes with shaders on and off.

### Java block-state coverage — 3.8.0_X30

Mojang's Java 1.21.11 registry report identifies 410 block IDs with a `waterlogged` property. X30 passed fill, drain, source restoration on break, and wet-state reload for every mapped ID in a disposable Forge world at Y276 using the pack's EFRP block settings. All 409 obtainable waterloggable forms also passed placement into a source. The big dripleaf stem has no separate item. A further five inherent-water blocks and fifteen non-waterloggable controls passed their bucket checks. Three of the inherent-water plants also passed direct item placement.

Normal, trapped, Ender, and all eight copper chest variants were covered. Additional chest checks used the regular player interaction path, tested a denied item-use event, and checked wet and dry halves side by side. Pairing and both inventories survived a copper state transition and a restart. The audit also checked extinguishing a lit candle without losing its count, preserving a decorated pot's facing and contents, and releasing both sources when wet small dripleaf breaks.

The fixture uses the registered 1.7.10 variant or documented metadata alias for each Java ID. The report was generated from the official server JAR with `-DbundlerMainClass=net.minecraft.data.Main -jar server.jar --reports`. Placement results include wall signs whose obtainable item produces the wall form through metadata. [Official Mojang Java 1.21.11 server JAR](https://piston-data.mojang.com/v1/objects/64bb6d763bed0a9f1d632ec347938594144943ed/server.jar)

The matrix covers block-state and bucket behavior. Dried Ghast hydration and Conduit activation have their own gameplay requirements. The new render families still need a visual pack check. Distant Horizons water in generated LODs remains unverified. [Minecraft Wiki’s Waterlogging reference](https://minecraft.wiki/w/Waterlogging)

### Sponge absorption — 3.8.0_X31

Sponges drain removable contained water while keeping the host and its inventory. They absorb inherent water by removing kelp and seagrass, including both tall seagrass halves where reachable. A waterlogged barrier retains its Creative-only restriction and blocks traversal. Absorption follows connected wet cells up to six steps and 64 drained cells.

Headless fixtures at Y67 and Y276 passed mixed sources, chest and decorated-pot inventory preservation, retained panes and dripleaf, both tall seagrass halves, dry gaps, barriers, range, and capacity. [Minecraft Wiki’s Sponge reference](https://minecraft.wiki/w/Sponge)

### Java world interactions — 3.8.0_X34

Water prevents ignition and fire spread from a compatible host. The explosion calculation also accounts for the fluid's resistance. Bucket lava and downward lava flow leave an occupied waterlogged cell intact. Nearby lava still cools against contained water. Headless wet and dry controls covered fire, environmental and entity explosions, and both lava paths.

Normal piston push and sticky pull clear the moved block's waterlogged state. When a sticky piston retracts while its push is still in progress and leaves the pushed block in place, the pushed block keeps its water. Normal and short-pulse movement passed with both vanilla pistons and Better Pistons at Y276, including a moving-tile NBT round trip. The transition byte is stored on the moving piston tile. Settled water remains in the same chunk overlay format. [Minecraft Wiki’s piston and waterlogging rules](https://minecraft.wiki/w/Waterlogging)

The 430-row coverage matrix was rerun on X34. Its fill, break, placement, and reload results match X30. X34 passed `check build`, headless Forge startup, and the packaged Crucible classloading regression.
