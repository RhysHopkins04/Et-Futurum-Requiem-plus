# Wargames versioning

Use `major.minor.patch_Xbuild` for release jar names and reported versions.

- Major changes only when substantially changing the entire mod.
- Minor changes for a new feature; reset patch to zero.
- Patch changes for fixes and small changes.
- Build advances on every successful packaging invocation, independently of the three release components. Never reset the counter for a release bump.

EFR and DH automatically persist the next build number after successful packaging (including CI); failed builds, checks, configuration and dry runs do not consume it. Run one packaging invocation at a time in a checkout. Build through the repository root wrapper. Do not add branch names, git hashes, dirty markers or descriptive feature suffixes to release versions. Standard dev/sources classifiers identify different artifact types.

The three release components are deliberate edits in gradle.properties; the build does not guess feature versus fix from source changes. Configuration cache is disabled so a cached task graph cannot reuse a consumed build number.

WGMap keeps its existing version format and build policy. NTM keeps its upstream mod_version and mod_build_number untouched: only wdg_version is advanced manually, without a new automatic build counter. Leave disabled counters in other mods disabled.

## 27 October 2026 release validation

Released as `etfuturum-3.6.0_X3.jar`: configurable-height feature advances 3.5.28 to 3.6.0; three successful packaging checks consumed X1, X2 and X3. Offline `check build` passed, including world-profile and Earth-import contracts. Counter checks passed for read-only help, dry-run and failed task selection (no increment), followed by a successful CI-mode build (increment). Existing snapshot/update-check behaviour is retained independently of naming.

The user reports successful live height increases to 384 and 400, building at the raised height, and ReplayMod compatibility; map compatibility is believed working. This run did not repeat live multiplayer or lowering tests. Prior disposable-server and actual Crucible bytecode validation is recorded in the height documents.
