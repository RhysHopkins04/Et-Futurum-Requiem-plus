# Crucible interaction-height correction, v2

## Fault and failed first correction

The supplied dev-280 server jars are byte-identical (SHA-256 `bb44fe4ef0898716a614ce42be58debc4c9d93b04b674933572a6db86e3f5fd4`). Supplied EFR config correctly enables the imported-Earth 384-block engine.

CraftEventFactory's seven-argument block-interaction overload converts clicked blocks above Y=255 to air interactions. Its Bukkit/Forge event results govern both digging and placement, independently of world storage and MinecraftServer.getBuildLimit. Console /setblock succeeding above 255 is consistent with that fault.

The first corrective jar used an optional mixin. The 17:33 production crash proves that mixin found zero targets and prevented CraftEventFactory from loading. The development fixture did not reproduce production target selection and was insufficient. That crashing mixin and its fixture are removed; v1 must not be used.

## Final change

A narrow LaunchWrapper transformer patches the unique height-comparison branch in the actual CraftEventFactory class. Matching uses its static argument/return structure, Action parameter, Y-local slot, literal 255 and IF_ICMPLE. It supports raw and mapped Minecraft type names. No Bukkit API links or Minecraft class initialization are needed in the transformer.

Only that bound is replaced by a call to CrucibleHeightCompat. The helper uses actual height minus one only for LEFT_CLICK_BLOCK/RIGHT_CLICK_BLOCK in an extended Overworld. AIR actions retain Bukkit's deliberate Y=256 sentinel; null/unknown players/actions, other dimensions and legacy mode keep the original bound.

Every plugin-event, cancellation and protection instruction remains intact. Unknown target layouts are logged and left unchanged rather than causing a required-mixin crash. Transformation is idempotent. No Bukkit/Crucible jar, plugin, packet format, world data or cache format changes are made.

## Validation

`CrucibleHeightBytecodeContracts` loads the actual built/reobfuscated transformer from the release jar and applies it to the actual supplied server's CraftEventFactory.class. It verifies:

- exactly one method/branch changes;
- after removing the inserted helper call, every instruction matches the original (including event dispatch and protection);
- stack analysis succeeds;
- raw and mapped argument signatures work;
- repeated transformation is unchanged;
- unrelated classes and unrecognized layouts remain untouched.

Run:

```sh
./gradlew --offline -I scripts/crucible_height_tests.gradle \
  -PcrucibleHeightJar=/path/to/Crucible-server.jar \
  crucibleHeightBytecodeContracts check build
```

Passed: actual-release/actual-server bytecode contracts; ordinary EFR check/build; disposable transformed Forge 384-height startup, actual placement packets, chunk/effect packets, ReplayMod reserialization, Anvil/restart and Nether isolation. The helper is tested with real Forge players/worlds for block actions, AIR and legacy mode. There is no claim that a complete production Crucible server with the user's entire plugin/mod set was run locally.

Release hygiene confirms the crashing mixin is absent and no Bukkit API or test classes are bundled. No commits, pushes or installation were performed.

## Deployment and live test

Stop the server. Remove the v1 `etfuturum-3.5.28-crucible-interaction-height-fix.jar` and replace it with `etfuturum-3.5.28-crucible-height-v2.jar`, keeping only one EFR jar. Fully restart. Existing height-update clients remain protocol-compatible; WGMap does not need another update.

Keep height at 384 initially. The server log should contain:

`Patched exact Crucible block-interaction height branch; plugin event dispatch remains intact`

Test stone placement and breaking at Y=255/256 and around Y=260; reconnect to verify real server state. Test an upper chest before changing height to 400. No world or cache reset is required. A separate `Timed out` disconnect is not diagnosed by this correction.
