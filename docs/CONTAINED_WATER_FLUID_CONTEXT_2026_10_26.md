# Contained-water fluid context with Angelica — 26 October 2026

The earlier material-ID override correctly identifies EFR's water faces to a
shader pack, but Angelica 2.2.14 still prepared their enclosing kelp/coral/model
block as a non-fluid. Its completed-batch encoder uses a separate fluid render
type for water lighting and vertex attributes. Material-ID overrides alone do
not change that render type. This is a confirmed processing mismatch; BountifulStars
also uses the lightmap when its indoor wave restriction is enabled, so differences
can affect both colour and wave response.

A client-only optional late mixin scopes Angelica's native fluid render type
around encoding a batch only when every used vertex carries the current pack's
source-water override. Normal plant batches, mixed batches and unmapped packs
are excluded. The previous context is restored on normal return and exceptions.
The code uses public APIs through cached method handles and has no mandatory
Angelica linkage. Absent/older context APIs safely retain previous behaviour.
The hook is registered only on the client when Angelica's Celeritas mesher class
exists; the optional callsite has no required match on unsupported versions.

Shader material IDs are read dynamically, including flowing-water fallback when
the source-water map lacks metadata zero. No pack-specific material ID is baked
into the fix. The existing render-only water view already shares vanilla water
heights and face culling across these occupied cells, so this change does not
alter plant geometry, fluid heights, gameplay, world data, AssetDirector assets,
shader files, DH data or networking.

Validation:

- Java 8 adapter harness covers water-only and mixed batches, active vertex
  counts versus unused array capacity, unsigned short IDs, nested contexts,
  exception cleanup, shader reloads, missing maps and flowing-water fallback.
- Harness runs without Angelica and with the older material-only API.
- The binary target checker finds exactly one matching callsite in both the base
  and Java 17 mesher classes in the installed Angelica 2.2.14 jar, and verifies
  its public context API and native fluid render type.
- EFR offline check/build and existing Earth/profile contracts pass.

Visual acceptance is still manual. With the same shader pack and vertex waves
enabled, reload chunks/reconnect and compare kelp-top water with adjacent water
from above and just below the surface. Check one wet coral/seagrass patch and
one dry plant, then toggle shaders off. Arbitrary shader displacement amplitudes
cannot be certified from offline tests; report any remaining model/surface
intersection rather than assuming this fixes every shader-specific clipping case.

This is a client rendering fix. No map conversion, capability refresh, LOD rebuild
or server-side configuration change is required.
