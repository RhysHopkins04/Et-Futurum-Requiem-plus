# Smithing screen correction — 3.5.25

The old bundled `assets/minecraft/textures/gui/container/smithing.png` overrode
AssetDirector's Minecraft 1.21.11 background. Versioned resource locations alone do
not prevent this: MCLib 0.3.7.7's MultiVersionDefaultResourcePack.parseName checks
unversioned, non-built-in resource packs before returning the downloaded jar resource.
EFR's own mod resource pack supplied the historical two-input image.

Removed that obsolete PNG. The GUI continues requesting the versioned modern asset
through AssetDirector. No new PNG is stored in the repository or patch. The existing
three input slots (8/26/44,48), output (98,48), inventory and hotbar now align with the
modern background. The title moves to (44,15), matching the pinned SmithingScreen.
The empty template slot cycles the two official template hints.

The preview keeps its arms and hides the base plate. It now uses a fixed angled
view at the modern screen's scale/position with 210-degree body yaw and a 25-degree
view pitch. Generic GuiInventory rendering previously reset its yaw to a frontal
mouse-following pose. Equipment is cleared on every update, then only an armor
result is copied into its corresponding armor slot. Tools are never equipped.
World armor stand models, poses and rendering behavior are unchanged.

The error X previously appeared for any single input. Vanilla 1.21.11 SmithingMenu
marks an error only when all three inputs are occupied and the result is empty.
The corrected predicate follows that behavior. The supplied populated screenshot
already contained a netherite pickaxe result; automated tests independently verify
that results can be taken and that costs and NBT are correct.

Default upgrades still require a netherite upgrade template, diamond gear and one
netherite ingot. Trimming still defaults to vanilla armor and EFR netherite, with
the existing option to allow other mods' armor. No recipe, ID, config default or
Backporter mapping changes are included in this correction.

## Validation

Build/check and headless recipe/asset contracts verify the actual output resource
classpath contains no shadowing background; the release jar is checked too. The
new preview tests exercise all four armor slots, tool/empty-result cleanup and
copied stacks. Forge tests exercise partial/complete invalid input errors, nine
upgrade types with left/right output clicks, 4,752 trim combinations, inventory
constraints, hotbar/shift extraction, costs and NBT. Disposable save/restart tests
cover the other recent workstation passes as regressions. See VALIDATION.json for
recorded logs and hashes. These are CPU/server tests; they do not certify OpenGL
appearance in a particular pack.

## Minimal client check

1. Open an empty smithing table: three adjacent input holes, centered arrow/result,
   title beside the hammer and an angled stand on the right, with no overlap.
2. Insert an upgrade template, diamond pickaxe and netherite ingot. Take the result;
   the stand should stay empty. Partial inputs should not display an error X.
3. Trim a chestplate: verify the result appears on the stand, then remove an input
   and confirm the armor disappears. A complete invalid combination should show X.

The exact 3.5.24 working tree was captured before editing, including earlier
uncommitted work. The incremental ZIP validates hashes and backs up changes.
No Git history operations or user world/config changes were performed.
