# Imported foliage skylight repair

The Land Review conversion near the locked safe contained 638 leaf-litter cells
with zero SkyLight in the examined nine chunks. Vanilla repaired most on load,
but zero-light non-air plants remained. Population can discard source light for
an entire changed chunk; its conservative geometry seed treats every non-air
block, including transparent foliage, as a roof. Vanilla queued checks primarily
visit air cells, which can leave dark non-air foliage cells behind.

EFR now seeds direct skylight once per imported server Overworld chunk before
its initial packet. It uses the loaded pack's actual block opacity and vanilla
vertical attenuation through leaves/water. Only increases are applied: existing
lateral skylight and block light are preserved. It reconstructs the height map
from opacity, preserves sparse section storage and never explicitly requests a
neighbour chunk. Ordinary worlds, client chunks, other dimensions and disabled
import profiles are excluded. Vanilla still performs lateral reconciliation;
this is not a replacement full-world lighting engine or a texture change.

The saved chunk-root marker `etfuturum:imported_skylight_version=1` prevents
repeating the pass on later loads. Existing imported test worlds without it are
eligible, including chunks already saved with LightPopulated=true. Future marker
versions are preserved. No registry, catalog or capability-report refresh is needed.

Verification: headless Forge fixture exercises actual fern halves, leaf litter,
bush, leaf attenuation, solid roofs, existing lateral light, block light, physical
Y above 255, sparse sections, loaded-chunk count, config gates and marker save/load.
Run `./gradlew --offline -I scripts/earth_import_tests.gradle
-PearthSkyLightProbe=true runServer build` after preparing disposable
`build/earth-skylight-tests/server` with an existing accepted EULA and imported
profile configs. Require the `IMPORTED_SKYLIGHT PASSED` log marker; Gradle success
alone is insufficient because Forge may report a startup failure with exit zero.

Client validation remains necessary for any unrelated alpha/model issue: reopen
the review world and inspect the previously black plants near `/tp 1241 147 -4158`.
