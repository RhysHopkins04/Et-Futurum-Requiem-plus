# Optional waterlogging prototype

Install LiquidLogged 0.2.4 and ChunkAPI 0.8.4 alongside Plus in a 256-block Forge world. Use matching mod versions on the client and server. LiquidLogged's published build uses UniMixins 0.3.1. Both waterlogging mods are optional for Plus.

LiquidLogged provides the vanilla holders, including leaves, single slabs, and stairs. Plus opts its enabled prismarine, prismarine-brick, and dark-prismarine stairs into water-only storage. The campfire, dripleaf, and aquatic blocks continue using Plus's native water states. This prototype follows the optional predicate registration in [GTNH Et Futurum Requiem PR #147](https://github.com/GTNewHorizons/Et-Futurum-Requiem/pull/147) and uses the implementation from [GTNH LiquidLogged](https://github.com/GTNewHorizons/LiquidLogged).

LiquidLogged registers its storage managers with ChunkAPI during Forge initialization. ChunkAPI owns region serialization and the chunk and block-update packets when installed. ChunkAPI 0.8.4 loads 16 Anvil sections and uses 16-bit section masks. LiquidLogged 0.2.4 also serializes 16 sections in its chunk packets. Plus leaves those packet and chunk-loading methods with ChunkAPI and refuses extended-height hosts and connections before world or chunk loading. This excludes the imported Earth profile.

## Candidate validation

`3.8.0_X17` passed the Gradle check and build and disposable Forge client tests for source placement, bucket fill and drain, source restoration on breaking a wet leaf, save/reload and client sync through Y255. The fixture checked both vanilla leaf blocks, a single slab, all three supported Plus stairs, and the double-slab and lava exclusions. With the provider absent, ordinary water and the native Plus water states still worked. The legacy kelp age migration persisted its converted state to disk with both mod sets.

## Future map conversion

For these provider versions, each Anvil section stores the host block in its usual block and metadata arrays. The optional fluid payload is at `Level.Sections[i].liquidlogged.fluids`. Version 1 has a byte `v` of 1, a compound list `palette` with string entries `t = "fluid"` and `n = "water"` for water, and a byte array `data` of length 4096. The cell index is `(localY << 8) | (localZ << 4) | localX`. Zero means empty and nonzero unsigned values index the palette starting at 1. A payload without `v` is empty. Retain unfamiliar payload versions when round-tripping a save.

A future Map Backporter Studio profile can map source `waterlogged=true` to this payload after translating positions and confirming that the target block opts in. Continue writing the native Plus state for campfires, dripleaf, and aquatic blocks. The tested save also records the provider's `liquidlogged:fluids` and `liquidlogged:ticks` managers under `chunkapicore.managers` in `level.dat`. Validate a disposable generated chunk against Forge and inspect its save/reload before emitting provider data for a map. Extended-height conversion needs support in both dependencies first.

## Renderer and pack checks

The standard Forge renderer draws provider fluids alongside the host geometry. The Angelica-WDG 2.2.14_X12 smoke test started and received the stored fluid, but its screenshot lacked the water surface inside the slab. The mesher needs to copy `LiquidLoggedAPI.getSection` data into the chunk snapshot, expose it through `LiquidLoggedBlockAccess`, and emit fluid faces with `LiquidLoggedRenderAPI`. Test the translucent water pass, shader material and tint, light, occlusion, and rebuilds after bucket changes. Plus's existing contained-water renderer handles the native states.

Distant Horizons needs the stored fluid in its chunk snapshot or generator input so it can resolve the visible liquid surface with the host geometry. Validate near-to-LOD seams, tint and occlusion, and LOD invalidation when a cell is filled or drained. Check the actual pack with matching client and server dependencies on a disposable 256-block world, including reconnect and a server restart.
