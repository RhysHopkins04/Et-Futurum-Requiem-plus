# Contained water shader material fix

See [the 26 October follow-up](CONTAINED_WATER_FLUID_CONTEXT_2026_10_26.md) for the additional Angelica fluid-context correction.

The kelp/coral/dripleaf renderers emit plant geometry in pass 0 and vanilla water geometry in pass 1. Angelica 2.2.14 otherwise attaches the enclosing plant's shader material ID to that water. BountifulStars ALPHA 0.0.3 assigns water ID 32000 and kelp ID 10017; water faces classified as kelp therefore bypass the shader's normal water treatment.

Scope Angelica's existing StateAwareTessellator per-vertex shader-ID override around contained-water renderBlockLiquid calls only. Look up the active shader pack's vanilla source-water material dynamically (flowing-water mapping fallback), never hard-code 32000 or change the plant's global block mapping. Use GTNHLib's current tessellator so worker-thread meshing writes the override to the tessellator actually collecting geometry. This includes parity kelp ages/plants, seagrass, wet coral/pickles/campfires/dripleaf and bubble columns through their common liquid renderer. Dry states and plant quads retain their existing classification. The fluid world-access view and override are restored in finally/try-with-resources, including exceptions; nested scopes restore the outer override.

The optional adapter uses cached method handles and public Angelica APIs, avoiding a mandatory Angelica runtime dependency. Without the relevant API or an active water mapping it does nothing. Shader reloads are respected by reading the current material map. Angelica's default -1 sentinel is restored on leaving the outer scope. This assumes no unrelated mod has an active explicit vertex-ID override around the contained-water invocation; the public API currently exposes a setter without a current-value getter.

No world state, water tint profiles, textures, DH database, network contracts or shader-pack files change. No world reconversion or LOD deletion is needed. This fixes the near-terrain material classification, not a DH-only rendering issue.

Validation: Java 8 adapter harness covers water ID lookup, nested scopes, changed shader maps, unsigned-short IDs, exception cleanup, absent/null mappings, flowing-water fallback, non-aware tessellators, and a runtime without Angelica. Offline EFR compilation/build pass. The installed Angelica jar's override capture and consumption were inspected: it records shader IDs per vertex and applies them during chunk buffer encoding. In-game appearance and actual shader rendering still require confirmation.

Minimal visual check: use the same BountifulStars pack at the reported kelp patch, reload chunks (F3+A) or reconnect, and compare kelp-top water against adjoining water. Check one wet coral/seagrass patch and toggle shaders off once to confirm normal rendering. No server-side installation is needed for this client-only change. This build has not been installed into Release Testing automatically.
