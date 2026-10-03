# Fidelity Pass 38 — Sculk gameplay

Pass 38 is the bounded Minecraft Java 1.21.11 Sculk gameplay layer for EFR+ 1.7.10. It preserves the
Pass-30 Sculk Vein FaceMask and Pass-35 Sensor/Calibrated Sensor/Shrieker metadata encodings.

Implementation anchors:

- `SculkVibrationDispatcher`: one frequency/event bridge, chunk-indexed listeners, travel delay,
  8/16-block sensor ranges, six-ray wool occlusion and modern distance redstone strength.
- `TileEntitySculkRuntime`: Sensor/Calibrated ACTIVE -> COOLDOWN -> INACTIVE timing, comparator
  frequency, facing-aware calibration, pending vibration persistence, Shrieker trigger/shriek timing.
- `SculkEventHandler`: bounded Forge hooks for 1.7-representable events, catalyst registry/death hook,
  XP suppression and per-player warning state.
- `TileEntitySculkCatalyst` + `SculkSpreaderLegacy`: 8-tick bloom and persisted bounded charge cursors,
  no forced chunks/TileEntity destruction, Sculk/Vein/special growth.

The frequency mapping follows the modern simplified 1..15 table. Sensor ACTIVE timings are 30/10
(normal/calibrated), cooldown is 10, Catalyst bloom is 8, Shrieker shriek is 90, warning maximum is 4,
warning-increase cooldown is 200 ticks and warning decay interval is 12000 ticks.

Warden creation is intentionally absent. Reaching the warning boundary performs no entity spawn and
schedules no repeated spawn attempt.

## Pass 38b runtime corrections

Runtime testing closed several bridge-specific gaps without changing the Pass-38 metadata/import contract or the 3.5.10 staging version:

- vibration-dampening now treats both 1.7 wool and carpet as source dampers as well as ray-occluders; walking on them, placing/breaking them, and projectile impacts into them do not emit Sensor vibrations;
- Sensor pending state is explicitly emptied across ACTIVE/COOLDOWN transitions, and an arriving vibration may activate only while the Sensor is still INACTIVE. Continuous movement after cooldown may legitimately create a *new* event, but a one-shot event performed during cooldown is not queued for later;
- the modern `block.sculk_sensor.clicking`, `block.sculk_sensor.clicking_stop`, `block.sculk_shrieker.shriek`, and `block.sculk_catalyst.bloom` events are now backed by the versioned dynamic `sounds.json` and explicitly requested OGG objects;
- comparator output remains the accepted vibration frequency (1..15) only while the Sensor is ACTIVE;
- Sculk-spread-generated Shriekers are `can_summon=false`, matching Mojang's 22w13a rule. Only world-generation/import state may represent `can_summon=true`.

A player-placed or Sculk-spread-generated Shrieker therefore does **not** enter the Warden-warning/Darkness response in modern Java; its purple shriek particles and shriek sound are still expected. The actual Warden entity/spawn endpoint remains outside Pass 38, and EFR currently has no reusable modern Darkness rendering/status-effect facility, so Pass 38b does not fake Darkness with legacy Blindness.


## Pass 38c chunk-load lifecycle correction

Runtime testing exposed a 1.7.10 TileEntity insertion hazard in the new Pass-38 Sculk runtime. `TileEntitySculkCatalyst.validate()` read block metadata while `AnvilChunkLoader` was still installing the TileEntity. That world read synchronously re-entered `getChunkFromChunkCoords()` for the same chunk and could recurse until `StackOverflowError`. The Sensor/Calibrated Sensor/Shrieker runtime used the same unsafe pattern through imported-state normalization.

Pass 38c makes both Sculk TileEntity `validate()` hooks installation-safe: they now set only local transient flags. Listener/Catalyst registration and all identity/metadata normalization are deferred to the first server `updateEntity()` after the TileEntity is fully installed. Unload/invalidate unregister only if registration actually occurred. Imported ACTIVE/COOLDOWN/Shrieking states and Catalyst bloom metadata are still normalized once after installation, so the Pass-35/38 persistence contract is unchanged.


## Pass 38d placement-event ownership correction

Forge 1.7.10 publishes `PlayerInteractEvent.RIGHT_CLICK_BLOCK` before an `ItemBlock`
use has resolved into a real placement. Pass 38 originally translated every such
right-click directly to modern `block_activate`, so one physical placement could
produce a false frequency-10 activation before the real frequency-13
`block_place`. Because a Sensor accepts only one in-flight vibration, the false
activation could win, distort distance-derived normal redstone output, and make
wool/carpet placement appear audible even though their later `PlaceEvent` was
correctly dampened.

Pass 38d gives the resolved `PlaceEvent` ownership of block-placement vibration.
A right-click performed while holding an `ItemBlock` is retained only as a
same-tick activation candidate. A successful `PlaceEvent` consumes that
candidate before posting exactly one `BLOCK_PLACE`; dampening wool/carpet
placements consume it and post no vibration. If no placement follows, the
candidate is emitted as `BLOCK_ACTIVATE` at the end of the server tick so
right-clickable blocks still work while a block item is held.

The normal Sensor power remains distance-derived with the modern
`max(1, 15 - floor(15 * distance / range))` shape (range 8 normal, range 16
calibrated). Comparator output remains a separate ACTIVE-only frequency output:
for the current 1.21.11 mapping, block destroy is 12 and block place is 13.
Pass 38d therefore changes event ownership/deduplication, not the established
redstone-strength or comparator formulas.


## Pass 38e calibrated-input isolation correction

Runtime testing confirmed the Calibrated Sensor's frequency filter and rotating
amethyst input face, but the 1.7 backport was still inheriting the normal
Sensor's weak redstone output on every horizontal side. Modern
`CalibratedSculkSensorBlock` overrides weak output specifically so the dedicated
calibration/amethyst face does not receive the Sensor's own output.

Pass 38e adds that sided-output isolation for all four Pass-35 facing states.
The frequency filter itself is intentionally unchanged: an unpowered input
accepts normal vibrations, while a non-zero input accepts only the matching
1..15 vibration frequency. In particular, calibration strength 13 accepts
`BLOCK_PLACE` (13) and rejects `STEP` (1). The correction prevents the Sensor's
own output from feeding back into its calibration line without changing the
range-16 listening radius, distance-derived normal output, comparator-frequency
output, metadata encoding, contract revision 38 or staging version 3.5.10.


## Pass 38f Shrieker pre-Warden closeout

Runtime testing showed that `can_summon=false` and `can_summon=true` Shriekers were audibly
indistinguishable because Pass 38 stopped after the normal shriek sound/particles. Pass 38f closes
the remaining block-local 1.21.11 behavior without implementing the Warden entity or Darkness.

- walking directly on a Shrieker now calls the Shrieker runtime immediately, matching modern
  `SculkShriekerBlock.onSteppedOn` / `SculkShriekerBlockEntity.tryShriek`;
- `can_summon=true` uses shared per-player warning state: any nearby player's active 200-tick
  cooldown suppresses a new warning/shriek, otherwise the highest nearby level is incremented and
  synchronized, capped at 4, with the established 12000-tick decay interval;
- after the exact 90-tick shriek, a summon-capable non-Peaceful Shrieker plays Mojang's warning
  reply at a random point within ±10 blocks: level 1 `entity.warden.nearby_close`, level 2
  `nearby_closer`, level 3 `nearby_closest`, level 4 `entity.warden.listening_angry`;
- the warning reply uses volume 5/pitch 1 and the exact modern backing OGGs fetched through
  AssetDirector and exposed by the versioned dynamic `sounds.json`;
- player-placed / spread-generated `can_summon=false` Shriekers still shriek normally but never
  advance warning state or play a Warden reply sound;
- Minecraft 1.7.10 has no `doWardenSpawning` gamerule, so the compatibility gate preserves
  `can_summon` plus non-Peaceful difficulty. No unrelated legacy gamerule is substituted.

The explicit Pass-38 boundary is unchanged: level 4 does **not** create a Warden, there is no
spawn/retry loop, and modern radius-40 Darkness is not faked with legacy Blindness. Contract
revision 38 and staging version 3.5.10 remain unchanged.
