# Flagged Earth state corrections

The Earth subset audit identified two target defects alongside missing Backporter
encoders. Infested deepslate now delegates face selection and placement to normal
deepslate's 0/4/8 pillar axis contract and uses renderer 31 for the corresponding
UV rotation. Old vertical instances remain metadata 0. Bamboo's stalk and stage
setters now clear the intended bits (1 and 8) without erasing leaf state or leaving
old stalk/stage bits behind. Existing saved metadata layouts are unchanged.

The matching Backporter corrections write these states and the already supported
basalt, bamboo, hanging moss, pitcher plant, dry mangrove roots, dry/uncracked pot
facing and canonical cave-vine age encodings. They do not add general waterlogging
or arbitrary source block-entity reconstruction. Campfires now support contained
water in bit 3, advertised as `blockStateEncodings.campfireWater=1` in the runtime
export. Incoming flow and buckets extinguish while preserving facing, draining
does not reignite, and breaking restores water. Rendering uses the existing
AssetDirector model and contained-water pass, with no new image assets.
The subset's 11 lit+waterlogged source campfires are explicitly normalized to
extinguished wet campfires; four waterlogged vanilla leaves remain unsupported.

`FlaggedStateProbe`, enabled only through the opt-in test init script with
`-PearthFlaggedStateProbe=true`, checks all 16 setter inputs, all native pillar
placements, all 12 bamboo states and all 26 canonical vine ages. The isolated
`build/earth-flagged-state-tests/server` fixture passes both SAVE and RESTART;
the probe is excluded from distributable jars. Normal build/contracts also pass.
