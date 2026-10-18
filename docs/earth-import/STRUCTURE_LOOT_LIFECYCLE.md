# Imported Earth structure loot

The backporter does not use modern Minecraft datapack loot tables for the generated NTM structures.

| Content | When inventory/content is generated | Effective source |
| --- | --- | --- |
| EFR imported dungeon chest actions | First server-side chunk load; action removed after successful fill and persisted on save | Running pack's Forge `ChestGenHooks.DUNGEON_CHEST` |
| EFR imported NTM inventory actions | First server-side chunk load | Running NTM `ItemPool.pools`, named pool and captured count; explicit fixed-slot overrides applied afterwards |
| NTM wand-loot placeholders | First server-side tile tick after loading | Running NTM named item pool; native replacement and loot generation, including the current `StructureConfig.lootAmountFactor` for inventories |
| AE2 meteor sky chests | During conversion | Native AE2 loot-table snapshot in the exported capabilities/resource plan |
| Fixed structure contents | During conversion or the deferred action, according to the reviewed structure contract | Captured canonical fixed item stacks; not automatically replaced by random-pool edits |

Chunk loading includes spawn chunks and server/plugin chunk loaders. Opening a chest is not the trigger for these imported random inventories. Once populated and saved, changing tables does not reroll the inventory. Use a fresh conversion for clean testing.

## NTM configuration

NTM initializes its built-in item pools, then loads `config/hbmConfig/hbmItemPools.json` if present. `config/hbmConfig/_hbmItemPools.json` is a generated reference, not the active override. The override replaces the pool map: retain all required named pools, including pools whose entries you revise. Restart the server to load changes.

NTM's structure configuration contains `5.03_lootAmountFactor`. It affects native spawned inventories, not every decorative loot block. Some imported action counts have already been captured; changing this setting alone is not a universal retroactive multiplier. Regenerate the export and resource plan after pack/worldgen configuration changes.

## Before removing registrations

First finalize the target pack's item and block registry. Audit item pools, Forge loot additions, fixed contents/slot overrides, structure palettes, machine inventories and recipes for removed entries. A structure can physically contain a block you intend to remove, independent of its loot. Merely removing an item from a random pool does not address these references.

After changing the NTM jar or registrations, regenerate the clean template and its genuine `/efrimportreport`, reanalyze changed mod catalogs, refresh the resource plan, and run a new preflight. Native captures/contracts may require updating when the NTM jar or fixed contents change. Do not reuse old converted worlds as proof that the revised pack is safe: saved inventories and placed blocks retain their old data.

## Native structure grounding under imported mode

Imported mode's provider ground reference remains Y128 for spawn behavior. Vanilla village and shared scattered-feature foundation sampling now use actual terrain height instead of clamping it to that reference in dimension zero. This prevents native terrain around Y64 from being raised to Y128 by the imported spawn setting. Other dimensions and non-imported profiles retain their original policy. The change does not reposition structures already saved, and does not flatten steep terrain or repair all legacy structure-placement limitations.

A headless Forge probe exercises the transformed village and temple height samplers on solid surfaces at Y64 and Y180 while checking that the provider spawn reference stays Y128.

NTM's legacy `Component.setAverageHeight` and static `getAverageHeight` independently apply the same provider-ground clamp. The optional late mixin redirects both helpers under imported-Earth mode in dimension zero. Outside that mode and in other dimensions the native provider result is retained. The native NTM probe checks both helpers against Y64 and Y180 surfaces and checks config-off behavior. NTM's separate NBT structure placement, water-specific anchors and existing saved structures are not repositioned by this patch.
