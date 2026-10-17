# Lectern, enchanting table and grindstone — 3.5.22

This pass completes the scoped workstation interactions and fixes enchanting purchase,
seed, fuel and inventory handling. It also corrects the previous passes' copied modern
textures to use AssetDirector. The backporter and normal user saves/configs are untouched.

## Lectern

Writable and signed books can be placed, read, paged and taken, with their full NBT retained.
Readers share the server page. The book survives an actual world save and fresh-process
restart. Comparator output follows page progress; page changes produce a two-tick redstone
pulse. The book drops once when the block breaks. The stand has multipart collision,
sloped selection bounds, facing-specific model selection and an open-book renderer.
Like the modern block entity, it does not expose a hopper inventory.

Signed pages support styled text, hover text and page links. The server-side
`config/etfuturum/functions.cfg` setting `B:allowLecternBookClickActions=false` defaults to
**off**. Reading and page navigation continue to work with it off. Set it to `true` and
restart to permit explicit command, URL, suggested-command and clipboard clicks. URLs
accept only HTTP(S) and retain the client's link settings and confirmation behavior.
Commands are validated against the current server book, page, text span, player, window,
block and distance, then enter the player's normal chat command handler. Permission,
filter and spam checks remain native. The 1.7 chat protocol limits commands to 100 chars.
Network threads queue requests; execution occurs on the server tick. Automated tests use
a recording connection and never execute a real command or open a browser.

This is a 1.7-compatible text renderer, not an implementation of every modern font,
component or RGB text feature. Modern source book block-entity translation remains work
for the separate backporter integration pass.

## Enchanting table

Offers are revalidated before purchase. Invalid selections, non-fuel inputs, insufficient
levels, unusable items, oversized stacks and an empty eligible enchantment pool cannot
charge the player or create an output. All three offers retain their expected level/fuel
costs; creative and no-fuel configurations work independently. Successful purchases roll
the persisted player seed, and the GUI receives its full value through bounded progress
updates. Shift-click inserts one enchantable item and retains the rest of the source stack;
closing conserves inputs. Book conversion preserves independent custom NBT.

Mending, Frost Walker, Swift Sneak and registered treasure/curse enchantments are filtered
before table selection, including books. Loot and anvil availability is unchanged. This
uses the enchantments registered in the 1.7 mod environment; it does not add Minecraft's
entire later enchantment catalogue.

## Grindstone

One enchanted input can be disenchanted. Two matching damageable items repair with the
five-percent durability bonus. Ordinary enchantments are removed; registered curses are
retained and combined, and retained curses rebuild the prior-work penalty. A book without
retained enchantments becomes a normal book. Unknown enchantment data is preserved
conservatively. Custom names/data remain independent of the inputs. Two identical
stackable eligible items can return two outputs, following the pinned modern menu logic.

The result is revalidated at extraction, consumes inputs once and awards randomized XP
only on actual take. Full-inventory shifts cannot consume inputs or award XP. Closing
returns inputs; block removal/range checks prevent stale use. All twelve floor/wall/ceiling
and facing combinations use five-part collision and selection shapes. NEI has its own
repair/disenchant category using actual queried stack NBT; no furnace fuel is shown.
The curse registration API supports existing mod curses; this pass adds no new curses.

## AssetDirector correction

Thirty-one byte-verified copies of Mojang PNGs introduced in these recent workstation,
item and loom passes have been removed. A curated catalogue routes 32 texture paths to
the pinned modern jar through the existing AssetDirector resource pack, including the
standalone grindstone error sprite. No replacement modern PNGs are bundled in this patch.
Existing unrelated legacy/custom art is outside this correction's scope. Automated Java
contracts exercise all 32 actual resource-pack aliases; a separate reference check verifies
the target assets and absence of those PNGs from source and release.

## Evidence and remaining checks

The pinned official Java 1.21.11 jar has SHA-1
`ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd`. Recipe data and relevant workstation bytecode
were inspected directly. See `VALIDATION.json`, `CONFIG_MATRIX.json`, `COMPARISON.json`
and `ASSET_REFERENCE.json` for final evidence and hashes.

Focused Forge tests cover lectern persistence, comparator/pulse timing, protected book
actions, 72 grindstone repair combinations, curse/NBT/XP handling, six enchanting purchases,
seed synchronization and 256 treasure-filter seeds. Existing ore, cooking, shulker,
stonecutter and loom probes run alongside them. A runtime witness audit still matches
1,295 modern recipe definitions across 4,786 queries with no previously matching recipe
regression. These counts are not a full-functionality score for all EFR content.

A short client check is still needed because headless tests cannot certify GUI rendering:

1. Place/read a signed book on a lectern, turn pages and take it back. Check the open book,
   page layout and comparator. Leave book actions off unless you want to test that option.
2. Enchant one item with lapis, then grind it to confirm the visible offers/output and XP.
3. Open the loom and stonecutter briefly and check that their migrated icons/GUI textures
   still resolve through AssetDirector. The exhaustive consumption tests run automatically.

After that milestone, the next planned pass is composter, fletching and cartography/maps,
followed by modern smithing and the crafter. No Git commit or push was made.
