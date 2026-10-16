# NTM ore compatibility review

EFR build 3.5.16, reviewed 2026-10-16. Twenty-eight independent material block/item IDs cover ordinary ores, the six NTM vanilla-ore counterparts and four ordinary clusters. The old combined ID and subtype order remain registered only for compatibility with existing test saves.

## Source comparison

NTM Next reference: [commit 1b8c6c2](https://github.com/Warfactory-Official/ntm-next/tree/1b8c6c2d0936de9d9f85926293f7666a1e6aac0a), NeoForge implementation. The extracted block/loot matrix is in `NTM_NEXT_ORE_REFERENCE.json`.

The modern implementation registers 16 deepslate types: uranium, thorium, titanium, sulfur, aluminium, fluorite, niter, tungsten, lead, beryllium, rare earth, cinnabar, cobalt, lignite, asbestos and coltan. Its ordinary helper gives the host deepslate colour/sound and 1.5x hardness, with material/ore registration shared through `MATERIAL_BLOCKS`. Uranium and asbestos use a separate outgassing helper with random ticks. Their behaviour is not reproduced by an ordinary textured wrapper.

The five basalt types are sulfur, fluorite, asbestos, volcanic gem and molysite. They are a distinct deposit family, not basalt counterparts of every metal. Legacy WG2.4.0 already implements these as metadata 0–4 of `hbm:tile.ore_basalt`; asbestos has native gas behaviour. Preserve that block and metadata.

Modern configured cluster features place the same cluster block in both stone and deepslate replaceable hosts. Legacy clusters are four separate blocks (iron, titanium, aluminium, copper), dropping native crystal items. Legacy ore metadata also encodes planetary host textures; it must not be mistaken for cluster richness. EFR 3.5.15 adds dedicated deepslate counterparts for the four ordinary clusters, retaining their native crystal drops and processing. Other planetary metadata and the special depth-cluster family remain native.

## Identity and tool-path correction (3.5.14)

Each ordinary NTM deepslate material now has its own `deepslate_hbm_<material>_ore` registry ID, a normal ItemBlock with metadata zero, and a standard display name such as “Deepslate Titanium Ore”. The old `deepslate_hbm_ore` metadata family is hidden from menus and excluded from new terrain replacement mappings. It remains readable and processable in existing worlds. Internal namespaces identify the optional integration; player-facing ore names no longer contain “NTM”.

Metadata itself is valid in Forge 1.7.10. The actual compatibility gap was exact block/item lookup: ordinary-drop parity and a crafting conversion did not prove NTM tool processing. Three optional late mixins normalize only EFR NTM ore inputs at the native shredder, centrifuge and crystallizer recipe lookup boundaries. Both machine and tool callers use those lookups. Stack count and NBT survive normalization; the original stack is not mutated. Native inputs and unrelated mod inputs pass through unchanged. Recipes, outputs, fluids and configured balance remain owned by NTM. NTM’s smelter operates on native drops and does not require this adapter.

`NtmToolProbe` now executes all four actual processing abilities for each of the twenty-eight separate blocks, compares their spawned outputs with the corresponding native ore (allowing only the requested deepslate self-drop identity), and checks that mining removes the block. Real ordinary harvests at Fortune 0/3/9 also match native drops. It also uses NTM’s real Silk Touch ability toggle and Forge block harvesting to verify recovery of the correct independent item. Separate IDs and old subtype blocks are written and checked on a fresh server process. This supersedes the earlier drop-only evidence.

## Confirmed EFR defects and corrections

- Raw titanium item/meta 10 already exists. The English US translation had a malformed key. Fixed US and added GB fallback text.
- Generic material ores and the explicit NTM variants both appeared in menus. Hide generic entries only when the OreDictionary's external sources are exclusively NTM. Keep them when another mod supplies that material. Existing block IDs, stacks, recipes and saved worlds remain valid.
- The global harvest event could overwrite native NTM drops after `getDrops`. Protect native NTM item drops and the explicit NTM wrapper from that replacement. Native resource quantities, Fortune, metadata and hazards are retained. As of 3.5.16, self-drops retain the deepslate identity and recipe adapters supply native lookup compatibility.
- Imported-Earth mode retained raw items/recipes but skipped `RawOreRegistry.init`. Initialize those mappings in this profile; native NTM drops stay protected.
- The earlier generic raw-metal hazard integration is superseded by removal of all active non-vanilla raw forms in 3.5.16.
- Native Earth metadata zero of uranium/asbestos and the four ordinary clusters maps to the dedicated deepslate counterpart. Other host metadata stays native. Basalt and the special depth clusters stay native at all legal metadata. Gas hooks delegate to the actual NTM uranium/asbestos instance, preserving its identity-based gas selection and configured tick/break behaviour.
- Add one-way, one-item shapeless conversion from each silk-picked explicit NTM deepslate variant to its exact native ore stack for machines that match exact inputs. Retained deepslate inputs use native recipe lookups through the adapters.

## Current raw-item and drop policy (3.5.16)

Only vanilla raw copper, iron and gold remain active EFR raw resources and storage blocks. All non-vanilla EFR raw variants (including the generic metal family and SimpleOres adamantium) are retired: no creative/NEI entries, recipes, smelting, raw-drop mappings or OreDictionary material tags. The old registry IDs and metadata layouts remain hidden save-data shells so worlds created during testing do not lose their stored blocks/items or require missing-registry confirmation. They cannot be enabled through the old moddedRawOres option. Existing hidden shells are not a supported processing resource. Other mods' own resources are untouched.

The earlier EFR raw-metal hazard bridge has been removed with that active content. NTM retains its own radioactive/hazardous resource items; EFR does not add parallel NTM raw chunks.

Normal mining now keeps the deepslate identity whenever the native ore would drop itself. Native resource products (sulfur, lignite, gems, crystal clusters' crystal drops, cobalt/coltan fragments, etc.) remain native. Silk Touch retains the actual deepslate block, including all four ordinary clusters. The old combined ore family yields the corresponding active individual deepslate ore when it normally drops an ore block. Exact NTM recipe-query adapters and furnace aliases accept the retained forms without requiring manual conversion.

Reactive on-break outgassing remains native. A scoped, restored ThreadLocal identifies only the deepslate wrapper's own native harvest call; EFR's highest-priority harvest handler restores that wrapper's self-drop after the native gas hook. It does not alter native resource products or unrelated native mining.

## Coal/lignite mining dust (3.5.16)

NTM's `ModEventHandler.onBlockBreak` recognises only exact vanilla coal ore/block and native lignite identities. A narrowly scoped optional mixin normalises field reads for EFR deepslate coal and lignite within that handler, without editing or reposting the Forge event. Native NTM owns dust placement, particles, masks and breathing hazards. The probe compares actual gas placement with normal coal/lignite for a fixed seed, confirms the original event's identity/XP/cancellation are unchanged, and calls native gas collision to verify black-lung exposure.

## Validation scope

The expanded headless Forge probe and actual-tool probe check global EFR harvest processing for every explicit variant at Fortune 0/3, native special-block metadata, cluster/basalt drops, retirement of non-vanilla raw menus/tags/recipes/drop mappings and persistence of hidden save identities, one-way silk processing conversions, vanilla raw iron and mixed-mod menu visibility. See `VALIDATION.json` and the probe logs for actual run results. The three recipe-lookup adapters are tested via actual tool processing. Complete powered-machine cycles, other NTM processors, client rendering/gas visuals and production-pack interactions are not certified by these tests.

## Earth coverage audit (3.5.15)

Checked local WG2.4.0 source commit `ee241d23df6bf0003765997bc87f079a0baf4870`.

| Candidate | Stock Earth generation evidence | EFR action |
|---|---|---|
| Uranium / asbestos | `HbmWorldGen.java` lines 165 and 181 | Separate reactive deepslate blocks, native gas-hook parity tests |
| Four ordinary clusters | `HbmWorldGen.java` lines 185–188 | Four separate deepslate cluster blocks; native crystal yields |
| Coltan | `HbmWorldGen.java` lines 190–218, region/config-dependent | Existing deepslate coltan retained; installed 3.5.14 also contains it |
| Morkite, cadmium, palladium, arsenic, silicon ore | Registered in `ModBlocks.java`, but no stock generation reference found in Java/resources | No new Earth ore generation or deepslate registration added for these placeholders |
| Six NTM vanilla-ore counterparts | `ModBlocks.java` lines 1421–1427; celestial generator supports planetary hosts | Separate optional deepslate identities preserve NTM source/drop/processing semantics; vanilla EFR identities remain available |

The cadmium plant/contaminated-soil pathway is separate from a cadmium ore vein. Registered creative items do not establish Earth resource generation. Pack scripts or custom generation configs can change this stock-source result.

The twelve added textures are assembled at atlas load from the active deepslate host and installed NTM's transparent ore/cluster overlays. No borrowed asset copies are added. Pure CPU contracts exercise actual native overlay composition, transparency, resolution scaling and the custom sprite loader/frame data. In-game atlas rendering remains a short client check. NTM ore items have a hover note identifying their native drop/processing semantics so identically named vanilla-material ores remain distinguishable.

## Why NTM has vanilla-material ore copies (source review, 2026-10-16)

The checked WG2.4.0 implementation has a concrete planetary-host purpose:

- `blocks/generic/BlockOre.java` lines 183–213 registers one stone icon per `SolarSystem.Body`, then renders a host-rock pass selected by the block metadata and an ore-overlay pass. The class comment says any ore can be placed on any planet, using the planet enum as its stone type.
- `dim/WorldGeneratorCelestial.java` lines 163–183 returns immediately for dimension zero. Outside Earth, it gets the provider's stone and the celestial body's metadata, then generates its vanilla-material copies when the host is not vanilla stone. Lines 257–261 generate native NTM iron, gold, redstone and diamond for those hosts. Separate planet generators also place some of those ores.
- `main/ModEventHandler.java` lines 965–968 likewise guards the celestial ore-generation event with `dimensionId != 0`. The celestial coal suppression does not apply to Earth.
- `BlockOre.vanillaMap` is read by `main/ModEventHandlerClient.java` lines 881–895 to show planetary ore-location tooltips for vanilla ore items. It is not a global world-block replacement map.
- `inventory/OreDictManager.java` lines 380–387 shares material ore tags with the copies, and `crafting/SmeltingRecipes.java` lines 33–39 gives native stack smelting recipes.

The copies do not establish a second required vanilla-ore resource family for Earth. In this source, `lib/HbmWorldGen.java` does not generate those six NTM vanilla-material block copies in its Earth ore list. Registration, creative visibility or a “Kerbin” tooltip is not proof of actual placement.

They also are not perfectly identical vanilla aliases. NTM's generic `quantityDroppedWithBonus` (BlockOre lines 171–177) multiplies redstone drops, whereas Forge 1.7.10's vanilla BlockRedstoneOre adds a random Fortune bonus. NTM's generic block does not inherit vanilla redstone's XP/activation implementation. Compatibility tests intentionally compare EFR's NTM variants against native NTM behaviour, not vanilla behaviour.

Policy: retain the six optional NTM deepslate identities for actual NTM source blocks, preserve nonzero planetary metadata, and keep modern vanilla Earth source ores mapped to the existing vanilla EFR deepslate family. Do not globally substitute NTM ores, duplicate Earth veins, or infer new Earth resources from creative-menu entries. The backporter implementation remains unchanged; this is a constraint for its next mapping update.

## Release Testing config cross-check

A read-only search found old raw-metal references in `_hbmArcFurnace.json` and AE2's generated `items.csv`. NTM's `SerializableRecipe.initialize` reads the filename without the underscore; underscore-prefixed files are exported templates, not enabled overrides. No non-underscore NTM recipe override referencing these retired EFR raw IDs was found in the inspected Release Testing config. Those old exported lists do not establish active raw recipes after retirement.
