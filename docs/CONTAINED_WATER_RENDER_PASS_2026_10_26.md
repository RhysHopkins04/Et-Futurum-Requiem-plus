# Preserve contained-water shader render passes

## Evidence

The user reports rectangular differences on EFR waterlogged surfaces, gaps with
BountifulStars water height waving, and incorrect reflections on macOS and Windows.
Disabling waving removes the gaps but leaves the rectangular appearance difference.
Disabling Angelica Render Pass Optimization removes most of that difference.

Angelica 2.2.14 `AngelicaChunkBuildContext.selectMaterial` can promote its translucent
material into cutout when an eligible static atlas sprite is classified as opaque
or cutout. The shader-supplied render-pass override flag bypasses this promotion.
Water's visible transparency and vertex displacement can come from the shader;
atlas-pixel alpha alone is insufficient to choose its shader pass.

The existing EFR bridge already supplies water material IDs and native fluid render
context for batches whose every vertex explicitly carries the resolved water ID.
It previously forwarded the enclosing block's original pass-override flag unchanged.
That permits shader water attributes and render-pass selection to disagree.

## Change

For a successfully established, complete contained-water fluid scope, preserve the
existing translucent pass by setting the batch's pass-preservation flag. Forward
all other arguments, vertex positions, colours, UVs, IDs and lighting inputs unchanged.
Mixed plant/water batches, native water, ordinary blocks, missing shader mappings
and unavailable optional APIs retain their existing pass-selection behaviour.

The change is confined to the existing optional client bridge and Angelica copy hook.
No shaderpack, resource asset, gameplay, world data, biome tint, server or inventory
code is changed. Global render-pass optimization and water waving can stay enabled.
The former wildcard hypothesis was checked: Angelica also expands unrestricted
water mappings to metadata 0, so no mapping-lookup change was made in this patch.

## Validation

- `scripts/test_contained_water_shader_compat.py`: passed current/older/absent API,
  all-water versus mixed batches, scope restoration, nested and exception cases,
  signed IDs, shader reload and preservation-flag boundaries.
- `scripts/test_contained_water_native_pass.py`: executes the real selectMaterial
  method bytecode from the installed Angelica jar with controlled sprite fixtures.
  25 cases passed: opaque/cutout/translucent sprite classifications, static/animated,
  optimization on/off, pass preservation, and unrelated opaque-pass promotion.
  Fixtures do not replace the selection algorithm; the jar supplies that algorithm.
- `scripts/check_contained_water_angelica_target.py`: actual base/Java 17 mesher
  targets and fluid-context API passed for Angelica 2.2.14.
- `./gradlew --offline check build`: passed.

These checks establish routing and compatibility; they do not replace a real GPU
visual acceptance test. In particular, any residual plant shading, surface normal
or reflection differences must be checked in the scene rather than declared fixed
from headless tests.

## Test

Replace the client EFR jar while Minecraft is fully closed. Keep one EFR jar only.
No server update is required for this rendering change.

Re-enable both Angelica Render Pass Optimization and BountifulStars Waving Water
Height. Revisit the same kelp surface after a fresh launch (or rebuild chunks with
F3+A after changing settings). Compare ordinary water against kelp-top patches while
moving and rotating the camera, from above and underwater. Check reflections and
wave continuity, then one waterlogged coral/sea-pickle patch and shaders disabled.
Keep both optimizations enabled for the acceptance test. If rectangular differences
remain, report them without compensating by changing water height or biome colours.

## Runtime follow-up: the pass-only candidate did not resolve the screenshot

The user retested with the candidate jar and both settings enabled; isolated bright
kelp-top squares remain. Do not treat the pass-preservation change as a confirmed
complete fix. The loaded 1.21.11 kelp model was inspected: its parent is cross and
its elements are vertical crossed plant planes, not horizontal water caps. Logs
confirm the candidate jar and optional Angelica mixin loaded.

A temporary diagnostic build recorded one contained-water upper face and one native
water upper face in Wargames Trailer. Both used water ID 32000, fluid render type 1,
a translucent material and the same Y=14.887889. Contained-water vertices carried
the explicit water override; native water used its enclosing water context. The
contained scope preserved its pass. The diagnostic logging has now been removed.

After a fresh launch the user reported that the colour difference was resolved,
with only wave gaps remaining. Disabling BountifulStars' No Waving in Closed Areas
had hidden those gaps. Its water function scales wave amplitude by per-vertex
skylight, which can differ across adjoining faces. This is separate from EFR's
material-pass routing.

## Companion shader adjustment

A separately packaged BountifulStars ALPHA 0.0.3 shader ZIP removes the skylight
amplitude mask only inside DoWave_Water. It retains the WDG nighttime fix and
all foliage/leaves masks. Water under roofs consequently continues to wave.
Both water waves and Angelica Render Pass Optimization remain enabled.

Archive comparison confirms exactly one changed GLSL entry versus the installed
night-fix ZIP. A 504-case numerical check confirms the replacement water wave
amplitude is independent of skylight at the same position/time; the original can
differ by approximately 0.024 blocks in those samples. This does not constitute
GPU visual acceptance; the final combination still needs the same kelp scene test.

## Acceptance correction: colour mismatch remains

The user then retested the diagnostic EFR jar with the water-fix shader. The wave
fix held, but the colour/lighting mismatch remained. Removing diagnostic logging
is therefore not isolated as its cause. The earlier colour improvement was not
reproducible and must not be described as a completed colour fix.

The pre-copy trace does not establish the final material selection or GPU program.
The next diagnostic uses the pack's existing Color Coded Programs option, with
Render Pass Optimization enabled, to distinguish water-program faces (blue) from
terrain-program faces (green). No additional runtime modification has been made
on the assumption that colour differences alone identify the final render pass.

The user's two Color Coded Programs screenshots show blue water surfaces and green
kelp geometry. They do not show green water caps, so there is no visual evidence
here for a water-versus-terrain program mismatch. This debug mode overwrites the
fragment colour after material lighting and does not verify that both faces have
the same water material ID, normal, vertex colour or skylight.

A separate diagnostic-only shader ZIP retains the wave fix and adds four screen
bands to that existing debug function: water ID (32000 blue, others magenta),
world-space geometric normal, original vertex colour and original skylight.
Only colorCodedPrograms.glsl changes versus the water-fix shader. Actual GPU
compilation and the resulting surface screenshot remain pending; this probe is
not presented as a colour fix. The native selector regression test now executes
25 cases each against both base and Java 17 implementations using ASM 9.

## Water skylight candidate

The four-band screenshot is uniform in its material ID, geometric normal and
vertex-colour bands, with obvious face/triangle variation in the skylight band.
This supports a skylight-input investigation; it does not by itself establish
that all variation is erroneous or identify saved-light versus renderer causes.

Angelica copyRawBuffer recalculates fluid quad light after decoding the source
vertices. The candidate captures the original four skylight bytes immediately
before calculateQuadLight and restores only the skylight component in its output.
The original calculation still runs; computed block-light, AO and coloured-light
metadata remain in place. The rule applies to verified native water contexts and
verified contained-water fluid scopes, ensuring the two use the same skylight
policy. Lava/unrelated material IDs, plant batches, absent shader mappings and
unavailable optional APIs retain the existing path. No world light is rewritten.

This is a candidate policy change from recomputed fluid skylight to the legacy
water renderer's emitted skylight. It must be checked both at kelp seams and shaded
shorelines; do not describe the colour issue as fixed until the user confirms it.

Validation: adapter fixtures cover zero through full emitted sky (0,80,160,240),
preservation of independently calculated block-light bytes, exclusion/no-op paths,
native water recognition and existing nested/exception scope cleanup. The actual
base and Java17 Angelica distributions each contain both expected light-call
sites and the required data fields. Offline check/build and Earth import
contracts passed. The candidate has not been installed or visually accepted.

## User acceptance

On 26 October the user confirmed the water issue was solved with the skylight
candidate, then reported an unrelated multiplayer timeout. This is user visual
acceptance of the tested combination; automated checks alone did not establish it.
The timeout is being reviewed separately from the rendering fix.

On 27 October the user also confirmed shader visual testing on Windows and
authorised committing the EFR changes. The separate BountifulStars wave-mask
adjustment remains a shader-pack artifact; it is not a bundled EFR texture or
shader-pack asset. Networking configuration experiments are outside this patch.
