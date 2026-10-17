# Lectern corrections, cartography and composting — 3.5.23

The lectern now places facing the player. All four placement quadrants have a real Forge
regression check. The enchanting screen continues to use its existing 1.7-compatible
layout, but hovering any available offer now shows its lapis and actual level costs even
when the required offer level is unmet. Invalid clue IDs are handled safely, and alternate
fuel-name cycling no longer picks the same fuel again. No new GUI art is bundled.

## Books and redstone

A book on a lectern is deliberately read-only. Write in a book and quill while holding it,
then place it on the stand to read and turn its existing pages.

The installed lwjgl3ify 3.0.28 development build was inspected directly. Its committed-text
hook always calls the page-append method, including while the book-signing title editor is
open; its ordinary-input suppression only covers the page editor. The compatibility mixin
routes committed text to the title while signing and suppresses the competing printable
key path when that hook is actually applied. Backspace and Enter remain native; older
LWJGL clients retain their ordinary input path. The title limit remains 16 characters.
Committed text also cannot modify a signed read-only book. The injected handlers are
exercised without opening a window; a brief real client signing check is still needed to
verify the installed input stack and actual mixin application together.

Ordinary lectern redstone output is a **two-tick page-turn pulse**. It is expected to end
quickly. A comparator reports the page continuously: a three-page book gives strengths
1, 8 and 15. The scheduled-tick tests check that the pulse clears after two ticks and that
the page/comparator value survives that transition. No persistent-output behavior was
invented for the ordinary pulse output. Signed-book click actions retain the preceding
pass's default-off server setting and permission/window/content checks.

## Cartography table

The existing registered table now opens a player-owned map/addition/result container:

- Filled map + empty map produces two copies sharing the original saved map ID.
- Filled map + paper expands one scale, up to level 4. A new saved map ID is created with
  an aligned center and blank terrain pixels; the source map and its other copies remain.
- Filled map + glass pane locks a copy under a new map ID. Its pixels and map extent are
  retained, and later exploration cannot repaint it. Locked maps can be copied, but cannot
  be expanded or locked again. Native crafting-grid expansion also rejects locked maps.

Inputs consume one map and one addition per take. Names and independent custom item NBT
are retained. Previews allocate no map IDs and do not initialize missing map records.
Actual extraction revalidates the current saved map. Shift-click checks full result capacity
before allocating or consuming; hotbar swaps finalize the map identity before installing
it. Closing returns inputs and clears the preview. Removed/distant tables and spectators
cannot craft. The legacy profile and disabled cartography setting suppress the interface.

Lock state belongs to MapData, persists in its NBT, and is respected by the actual map
painting method. Save/restart probes verify a locked map's pixels and state in a fresh
process. Client output slots use the server's result, including when the client has not
yet downloaded that map's pixels. The preview uses those pixels if available; a held map
still uses normal 1.7 map-packet synchronization.

The GUI, operation sprites and lock/error indicators request six original modern textures
through AssetDirector. NEI has a separate cartography tab with copy/expand/lock examples,
counts, catalyst and explicit same/new-ID captions. Examples allocate no saved maps;
known live map state filters usage queries. Existing map names and IDs are preserved.
This adds neither modern map-banner decorations nor modern component-backed item storage;
map storage and item-ID limits remain those of the 1.7 engine. Translating source-world
map records is separate backporter work.

## Composter

Eligibility queries no longer fill or empty a composter. Input changes occur only when a
stack is actually inserted. Extraction waits for the hopper's commit; tentative extraction
and rollback leave the compost intact. Stale proxies cannot repeatedly insert or extract.
Both native hopper transfer calls and explicit transaction-stage checks are exercised.

The first positive-chance item fills an empty composter, ripening takes exactly 20 ticks,
and comparator strength follows all levels 0..8. The inner collision floor follows the
visible fill level. Player extraction changes metadata server-side; spectators/adventure
players cannot modify contents. Existing configurable multi-layer additions and compost
loot extensions remain available.

The pinned 1.21.11 ComposterBlock registration contains **115 item identities**. Their
probabilities are recorded in a reproducible data catalogue and applied to corresponding
registered 1.7 items/metadata. All 115 active mappings are checked against the runtime
registry in the full test profile. Existing wildcard entries needed expansion before exact
variants could take precedence (notably roots and mushroom stems). Disabled identities
are skipped, and disabling automatic composting skips the modern defaults as well.
The existing CraftTweaker registration phase remains able to override the defaults.

## Fletching table scope

The pinned 1.21.11 block registry constructs the fletching table as a plain block; it has
**no player crafting interface**. Its existing crafting recipe and placed-block behavior
are retained. Modern villager professions/job-site claiming are not implemented by these
workstation containers and remain a separate feature gap for the fletching table and other
job-site blocks. This pass does not claim complete modern villager parity.

## Validation and short client check

The reference client SHA-1 is `ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd`.
The recipe audit remains 1,295 matching definitions across 4,786 runtime queries, with
no previous match lost and unchanged existing block/item/surface-biome IDs. The new map
operations and compost probabilities are behavioral contracts, not inflated crafting
recipe counts. See `VALIDATION.json`, `CONFIG_MATRIX.json`, `ASSET_REFERENCE.json` and
`COMPARISON.json` for final checks, hashes and their limits.

Only a brief client check is needed:

1. Place a lectern and check it faces you. Hold a book and quill, write two or three pages,
   sign it with a short title, then read it on the lectern. Check the title contains exactly
   what you typed and the page text was not changed while signing. A comparator should
   hold the page value after the ordinary pulse ends.
2. Hover an enchanting offer with insufficient levels; check that its lapis and level costs
   are still listed. The enchant/grind behavior was already confirmed in game and is
   covered by the automated regression suite.
3. Open the cartography table with a filled map. Briefly try an empty map, paper and a glass
   pane; check the GUI textures, operation previews and returned maps. A short visual
   check is sufficient: counts, saved identities, locking, rollback and restart are automated.

No normal user saves/configs, backporter source or Git history were modified. The next
planned major workstation work is modern smithing and the crafter; villager/job-site and
map-import conversion remain explicitly separate.
