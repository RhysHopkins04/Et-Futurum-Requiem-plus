# Wargames versioning

Use `major.minor.patch_Xbuild` for release jar names and reported versions.

- Major changes only when substantially changing the entire mod.
- Minor changes for a new feature; reset patch to zero.
- Patch changes for fixes and small changes.
- Build advances on every successful packaging invocation, independently of the three release components. Never reset the counter for a release bump.

EFR and DH automatically persist the next build number after successful packaging (including CI); failed builds, checks, configuration and dry runs do not consume it. Run one packaging invocation at a time in a checkout. Build through the repository root wrapper. Do not add branch names, git hashes, dirty markers or descriptive feature suffixes to release versions. Standard dev/sources classifiers identify different artifact types.

The build counter is local to each checkout. Separate branches, worktrees, clones and CI jobs can package the same X number from the same committed counter. Before packaging a candidate, coordinate with other checkouts and reconcile the committed counter with the highest number already packaged. For End flashes, a local `3.6.0_X12` JAR had already been packaged while master still recorded X11. The first feature build used X13 and the clock correction fix used X14.

The three release components are deliberate edits in gradle.properties; the build does not guess feature versus fix from source changes. Configuration cache is disabled so a cached task graph cannot reuse a consumed build number.

WGMap keeps its existing version format and build policy. NTM keeps its upstream mod_version and mod_build_number untouched: only wdg_version is advanced manually, without a new automatic build counter. Leave disabled counters in other mods disabled.

## 27 October 2026 release validation

Released as `etfuturum-3.6.0_X3.jar`: configurable-height feature advances 3.5.28 to 3.6.0; three successful packaging checks consumed X1, X2 and X3. Offline `check build` passed, including world-profile and Earth-import contracts. Counter checks passed for read-only help, dry-run and failed task selection (no increment), followed by a successful CI-mode build (increment). Existing snapshot/update-check behaviour is retained independently of naming.

The user reports successful live height increases to 384 and 400, building at the raised height, and ReplayMod compatibility; map compatibility is believed working. This run did not repeat live multiplayer or lowering tests. Prior disposable-server and actual Crucible bytecode validation is recorded in the height documents.

## 22 November 2026 movement candidate

`3.9.0_X39` advances the minor component from 3.8.0 for modern player movement. The verified base had consumed X35. Candidate builds consumed X36 through X39. The deliverable filename is `etfuturum-3.9.0_X39.jar`, and its embedded version is `3.9.0_X39`. See [the movement verification notes](MODERN_PLAYER_MOVEMENT.md) for fixture results and pack coverage.

`3.9.1_X40` advances the patch for the crawl rendering correction. One successful `check build` consumed X40 after verifying X39 was the highest packaged build. The filename `etfuturum-3.9.1_X40.jar` and embedded version match. SHA-256 is `933093595a7f4d852dfa68a940fc70bade90cafbe1d6fea2dd87b3c755fde918`.

`3.9.2_X41` advances the patch for swim activation and first-person item lighting. One successful `check build` consumed X41. The filename `etfuturum-3.9.2_X41.jar` and embedded version match. SHA-256 is `82cf54b25f80ce92dd04eda9c4f6a8733106423431009ebc6d556e2e879898a1`.
