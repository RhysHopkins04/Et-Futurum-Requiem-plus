# Fidelity Pass 33 — Small Remaining Visible States

Pass 33 is deliberately bounded to map-state fidelity. It does not begin the Pass 34 vegetation work or the later complex functional-block passes.

## Implemented

- **Pale Oak Button**: metadata `0..11` stores `face × facing`; `ParityButtonTileEntity.Powered` stores the pressed state. AssetDirector resolves all 24 `face/facing/powered` models. The block has orientation-aware pressed/unpressed bounds, support checks, ordinary activation/redstone, the wooden 30-tick release delay, and wooden-button projectile activation that remains powered while an arrow is lodged in the button.
- **Pale Oak Pressure Plate**: a dedicated `BlockPressurePlate` implementation retains normal 1.7 wooden plate mechanics while metadata `0/1` selects the exact modern raised/depressed JSON model.
- **Copper Torch / Copper Wall Torch**: `etfuturum:copper_torch` is the single obtainable standing identity. `etfuturum:copper_wall_torch` is a technical no-ItemBlock identity using metadata sides `2=N,3=S,4=W,5=E`. Wall placement swaps to the technical identity; support loss picks/drops the standing Copper Torch item.
- **Decorated Pot**: existing Pass 32f facing, sherd order, one-stack inventory and item preservation are unchanged. `Cracked` is now persisted/synchronized and included in `BlockEntityTag` round trips. Mojang 1.21.11 blockstate/model data does not select a different model for `cracked`, so the visual-state audit classifies it as `VISUALLY_IRRELEVANT` while the Backporter contract still preserves it exactly.
- **Candle Cakes**: audited rather than rewritten. All 17 identities already store `lit=false/true` in metadata `0/1`, select the correct model, change light level, and support ignition/extinguishing. Cake eating/bite conversion remains deferred gameplay.

## Global visible-state coverage foundation

`scripts/validate_modern_visual_state_coverage.py` reads Mojang 1.21.11 blockstate JSON from an extracted client root or client JAR and classifies every property it discovers as exactly one of:

- `STORED_EXACTLY`
- `DERIVED_EXACTLY`
- `VISUALLY_IRRELEVANT`
- `UNSUPPORTED`

Strict mode fails when an unsupported property materially changes model selection. General `waterlogged` is reported as the project's explicit global exception rather than being confused with an accidental state-collapse. During the staged Pass 34–36 completion series, `--allow-unsupported` can be used to print the remaining debt without failing the command. `--self-test` validates the detector/classifier without requiring a downloaded Mojang client.

## Backporter contract

`docs/BACKPORTER_STATE_CONTRACT.json` revision `33` is authoritative for these mappings. Backporter Studio must use those metadata/TE fields directly rather than reverse-engineering the Java implementation.

## Deferred to Pass 34

Pale Moss Carpet, Pale Hanging Moss, Creaking Heart, Dried Ghast, Torchflower Crop, Pitcher Crop, Sniffer Egg, Mangrove Propagule, Sea Pickle and other vegetation-state debt discovered by the global validator remain intentionally outside this patch.

## Pass 33b runtime corrections

Runtime testing after Pass 33 found four presentation/interaction issues without changing the
Backporter state contract:

- Pale Oak Button wall variants now compensate for the legacy model bridge's wall-plane rotation,
  so the authored model occupies the same side of the cell as the canonical selection box/support.
- Manual Pale Oak Button activation now has a deterministic 30-tick TE countdown in addition to the
  scheduled block tick, notifies the neighbours of its attached support on both press and release,
  and only emits strong power toward that attached support. Imported `Powered=true` remains a valid
  persistent state when no manual countdown is present.
- Pale Oak Pressure Plate keeps the exact 14x14x1/0.5 world model, while its inventory-only
  presentation matches 1.7's established full-footprint, four-pixel-thick pressure-plate atlas icon.
  Its inherited `BlockPressurePlate` activation/release and directional strong-power mechanics remain
  unchanged.
- Standing and wall Copper Torches now emit smoke plus the real 1.21.9+ `copper_fire_flame` particle
  texture provided at runtime by AssetDirector. No Mojang particle texture is redistributed.


## Pass 33c runtime corrections

Runtime testing after Pass 33b found two remaining bounded interaction/presentation issues:

- Pale Oak Button now matches wooden-button projectile behaviour: it has no collision box, `EntityArrow`
  impacts activate it, and the TE tracks projectile-held activation separately from manual/imported
  `Powered` state. The button remains powered while an arrow overlaps its pressed bounds and releases
  when that arrow is gone.
- Copper Wall Torch smoke/green-flame offsets are mirrored to the rendered wall-torch tip for all four
  horizontal facings. Standing Copper Torch particle placement is unchanged.

The Backporter state contract remains revision `33`; neither correction changes imported metadata/TE
state mappings.
