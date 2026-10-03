# Modern state fixes: local handoff

Changes are uncommitted, directly in the original checkout. Existing local work was preserved. No push, tag or version bump. Forge 1.7.10, Java 8 and the current UniMixins setup remain in use. The tested server has extendedWorldHeight=false and modernOverworldGeneration=false.

## Completed

Bounded contained source water covers kelp heads/stems, seagrass/tall seagrass, wet sea pickles and wet coral plants/fans/wall fans. Hooks implement shared water rendering, exposed surfaces, biome tint, body/eye immersion, swimming, drowning, fog, bucket fill/drain, source flow and lava cooling. Wet/dry bits, support, growth, lighting and restoration are retained. This does not add general waterlogging.

Powder snow implements sinking, conditional collision, slowing, leather boots/climbing, freezing/thawing/damage and immunity, buckets, burning/melting interactions, particles, frost overlay and fog. Exact Java 1.21.11 source verification and differences are documented in SOURCE_VERIFICATION.md.

Kelp ages 0–15 use etfuturum:kelp metadata; 16–25 use hidden etfuturum:kelp_age_16 metadata 0–9. New heads need no tile entities. Existing modern_parity_kelp_state BYTE Age data migrates before tile entity allocation and marks the chunk dirty for saving. Unrelated/stale records are preserved. Legacy reader registration remains available.

## Validation completed

- Offline Gradle build and Java 8 contracts: passed. NBT migration covers all 26 ages over three registry-ID pairs, nibble preservation, negative coordinates, stale records and idempotence.
- Twelve static regression checks: passed (static-regression-checks.txt).
- Actual transformed Forge dedicated server: save and restart passed. Tests cover all 26 ages without TEs; Anvil loader migration; entity freeze persistence; water surfaces/immersion/drowning/swimming; wet/dry buckets; water flow/lava; powder collision, leather handling, damage/immunity, movement and melting. Logs are beside this file.
- Graphical Minecraft client checks have NOT been run.

Repeat targeted checks with `python3 scripts/run_modern_state_tests.py`. The script uses an isolated build/modern-state-tests/server directory and an existing accepted EULA. Test-only code is outside the shipped jar.

## Local artifact and scale

Install build/libs/etfuturum-3.5.10-master.5-2e756a15b3-dirty.jar on both client and server with the existing dependencies. Do not install the dev/api/source jars as the mod.

SCALE_ASSESSMENT.json records a synthetic 4,096-head comparison: legacy raw chunk NBT 312,852 bytes versus compact 13,844 bytes. At 63 million heads, the old compounds alone represent about 4.6 GB before compression/object overhead. Compact ages add no per-head TE allocation or ticking. This is not a full-map benchmark; ordinary random growth remains. Back up worlds before upgrading; downgrading requires restoring a backup because the high-age block ID is new.

## Minimum manual check

1. Back up the world; install the new EFR jar on client and server. Keep both world-height/generation options false. Refresh the converter registry.
2. Convert a small sample. Check block states/positions; swim through aquatic plants and try water buckets. Check water tint and underwater fog.
3. Try powder snow barefoot and with leather boots; check sinking, freezing/thawing, fog and bucket pickup/placement.
4. Save, stop and restart. Confirm kelp ages and wet/dry states survived. If using an old world, include old kelp in this check.

## Remaining limitations

Client visual acceptance is pending. Modern frozen-heart art and entity shaking are not ported. The fork has no modern leather horse armor counterpart. Swimming uses Forge 1.7.10 movement, without the modern swimming pose. Unsupported waterlogged states remain approximate in the converter. No complete 63-million-head map conversion or benchmark was performed.

## Suggested commit

Title: Add bounded aquatic fluids and powder snow; compact kelp ages

Description: Implement contained water for the supported aquatic blocks and Java 1.21.11-derived powder snow mechanics. Store all kelp ages in block metadata across two IDs and migrate legacy BYTE Age tile entities before allocation. Preserve Forge 1.7.10/Java 8 compatibility and existing local changes. Offline build, Java NBT contracts, static regression checks and dedicated-server save/restart checks pass; graphical client acceptance remains manual.
