# Template names and pattern identity — 3.5.28

The template item translations still used the older generic Smithing Template
heading for all nineteen metadata variants, and their tooltip only described the
smithing operation. This made the patterns indistinguishable by name.

The titles now match the official 1.21.11 `.new` item translations: Netherite
Upgrade, Sentry Armor Trim, Dune Armor Trim, and the other named patterns. The
subtitle is Smithing Template, followed by the vanilla Applies to and Ingredients
sections. Upgrades describe Diamond Equipment and Netherite Ingot; trim templates
describe Armor and Ingots & Crystals. Heading/description colors match vanilla.
Title rarities also match the pinned Items initialization: Silence is epic; Ward,
Eye, Vex and Spire are rare; the remaining templates are uncommon.

No metadata numbers, item IDs, templates, recipe materials or config rules changed.
Existing template stacks retain their identity. Armor names remain the armor's
normal name; applied pattern and material descriptions already match the vanilla
trim_pattern and trim_material translations, and the new test checks those too.

The renderer's actual source-path selection was extracted into a testable helper
without changing its selection behavior. A new test follows every metadata value
through the actual template item, recipe result and renderer path. The result is
compared with the official trim_pattern asset_id, not just a guessed path.

## Validation

Build/check, NEI contracts, actual renderer Mixin application and asset tests passed.
The naming test verifies all nineteen actual localized titles, rarities and six-line
tooltips, all eighteen applied-pattern descriptions and all eleven material labels.
It also verifies seventy-two template/armor-slot routes, thirty-six worn texture
paths and eighteen distinct official pattern-image hashes. The existing 640 exact
pixel/palette tests still pass, including the grayscale/indexed-PNG regression.
The pinned template/asset catalogue check passes with all original metadata intact.
No PNGs or test classes/configuration are included in the release.

This pass changes client descriptions and extracts client texture selection; it
uses the existing build/renderer tests and does not repeat the server save/restart
suite already completed for 3.5.27. Actual OpenGL appearance in the user's pack
still needs the small client check below.

## Minimal client check

Hover Sentry, Dune and Silence: each should have its own name above the Smithing
Template subtitle. Silence's title should be purple, while Sentry/Dune are yellow.
Use Sentry and Silence on the same type of chestplate with the same material and
compare the stand: the worn patterns should differ. Inventory trim icons share
vanilla's generic armor-slot outline, so they do not show each unique pattern.

All art remains supplied by AssetDirector. The previously noted held/dropped
armor-item sprite limitation is unchanged. The Backporter and existing working-tree
changes are preserved; no Git operations or user-world/config edits were performed.

The incremental ZIP applies to the exact pre-pass 3.5.27 working tree and validates
source/payload hashes, backs up changed files and refuses unexpected edits.
