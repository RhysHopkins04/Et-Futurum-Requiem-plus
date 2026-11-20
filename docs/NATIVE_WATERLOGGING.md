# Native waterlogging

Plus stores source water alongside the block state of stairs, single slabs, and leaves. These classes keep their existing block IDs and metadata. Campfires, dripleaf, and aquatic blocks keep their native water encodings. This uses the existing Plus runtime dependencies and works at both vanilla and extended world heights.

A water bucket fills a supported block and an empty bucket drains it. Placing a supported block into a source retains the water. Breaking a filled block releases a source. Water spreads through open faces using the Plus contained-water flow hooks.

The standard renderer and Angelica-WDG use the existing Plus contained-water rendering path. The Angelica adapter snapshots the water alongside the host block, adds the water mesh to the translucent pass, and retains the shader material and lighting path used by aquatic blocks.

## Saved chunks

The optional `Level.etfuturum_waterlogging_v1` byte array stores a chunk's wet cells as a little-endian bitset. Bit `(physicalY << 8) | (localZ << 4) | localX` refers to the host in the normal Anvil section arrays. The byte index is the bit index divided by eight and bit zero is the least significant bit. The tag is absent when the chunk has no overlay water. Height uses the target world's physical coordinates, with up to 512 levels.

For future conversion, write the overlay bit only when the mapped target host supports it. Continue writing the existing native states for aquatic blocks, campfires, and dripleaf. Verify a disposable converted chunk after loading and resaving it in Forge. A Distant Horizons integration can use the chunk's water snapshot alongside the host geometry when generating or invalidating LODs.

The earlier dependency-based prototype used [LiquidLogged](https://github.com/GTNewHorizons/LiquidLogged) and [upstream EFR's opt-in approach](https://github.com/GTNewHorizons/Et-Futurum-Requiem/pull/147). This implementation uses Plus's existing water and Forge chunk hooks. A save created with the earlier external provider stores water in that provider's format.

## Candidate validation

`3.8.0_X20` passed `check build`. Disposable Forge singleplayer fixtures passed at Y67, Y276, and Y500. They exercised Survival bucket use, placement into a source, source restoration on break, client sync, and the existing campfire and dripleaf states. The Y276 fixture passed save/reopen and an independent read of the stored Anvil tag. Angelica-WDG 2.2.14_X13 rendered the water at Y276 with shaders disabled and with the Bountiful Stars WDG shader pack active. Distant Horizons LOD rendering and a dedicated-server session remain untested.
