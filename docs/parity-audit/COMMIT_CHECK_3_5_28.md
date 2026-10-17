# 3.5.28 crafting/workstation milestone: final commit check

This checkpoint collects the existing 3.5.17–3.5.28 working-tree passes. It is ready
to commit as the EFR+ foundation for the next map-backport integration phase. No new
feature pass is needed before proceeding. This is not a claim of complete 1.21.11
survival/gameplay parity or lossless Backporter conversion.

The current source was reviewed for container access/result ownership, inventory
consumption and persistence, server/client separation, retained registry identities,
and importer-facing visible/persistent state contracts. No blocker was found within
this milestone's scope. Existing GUI IDs remain unchanged; new GUI IDs are appended.
The operational crafter retains its imported `modern_parity_crafter_state` TE ID.
The obsolete smithing PNG override is removed; new modern art uses AssetDirector.

## Final validation

The final source passed ordinary Forge build/load/save/restart, NTM 2.4.0 plus Iron
Chests build/load/save/restart, and the legacy profile's suppression checks. The
suite exercises crafting/cooking/production chains, shulker compatibility, stonecutter,
loom, lectern, enchanting, grindstone, cartography, composter, smithing and crafter.

Crafter automation tests cover disabled slots, balanced insertion, redstone timing,
held-power behavior, recipe remainders, all twelve output orientations, chained
crafters, output safety, break drops and save/restart. The user accepted automated
evidence for this checkpoint; a manual crafter GUI smoke was not performed.

The actual client trim mixins apply in the isolated headless transformer test. All
640 palette permutations, 264 armor/material palette selections, 19 template names
and tooltips, and 72 template-to-renderer routes pass. The user confirmed worn trims
look correct; generic material-colored inventory trim outlines remain vanilla behavior.
The model/aquatic/sculk/import static contract checks and asset guards also pass.
No test classes or test-only resources leak into the release jar.

The source/log hashes and exact precommit tested jar are recorded in
`COMMIT_CHECK_3_5_28.json`. Earlier per-pass audit/configuration reports are historical
evidence, not claims that every feature's gameplay was exhaustively tested today.

## Next work: map conversion, with scope held

Use this committed EFR+ registry and state contract as the target for Backporter
integration. Recheck current mappings, state encoders and tile/item records against
this target; then convert a bounded, fully generated modern-world sample around the
saved player and compare seafloor blocks, aquatic population, biomes, height and
loss manifests. Follow the user's Backporter `dev` commit → download → distributed
build test workflow before treating the converter as verified.

Remaining full gameplay parity, template loot sources, breeze-rod progression,
held/dropped armor-item trim sprites and broader special-recipe coverage remain
follow-ups. Modern item/block-entity/entity NBT translation still needs explicit
Backporter handling; working EFR+ gameplay does not translate source records by itself.
