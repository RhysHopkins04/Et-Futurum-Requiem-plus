# Trim colors, inventory overlays and material policy — 3.5.27

## Confirmed causes and corrections

ImageIO converts grayscale PNG samples to sRGB when getRGB is used. The vanilla
trim palette is an 8-bit grayscale PNG, while some trim masks (for example Dune)
are indexed PNGs. Reading both with getRGB changed the palette keys but left the
indexed mask colors unchanged. Those patterns therefore failed to match the
material palette. Grayscale masks happened to match the converted keys, which
made the issue dependent on the selected pattern.

ArmorTrimRenderer now decodes grayscale samples without that conversion, matching
Minecraft's NativeImage/STB values. Alpha is retained, transparent palette-key
entries are ignored and unmapped source colors remain unchanged, as in vanilla.
The same corrected decoder feeds worn armor, the smithing stand and GUI icons.
All textures still come through AssetDirector; no Mojang PNG is bundled.

The inventory overlay hook targeted vanilla's five-argument renderItemIntoGUI
wrapper. Forge's normal renderItemAndEffectIntoGUI calls the six-argument boolean
overload directly, bypassing that wrapper. The hook now targets the Forge overload
explicitly, with remap=false because this Forge-added method has no vanilla SRG
mapping. Its zLevel shadow is still remapped normally. Both entry paths now receive
one overlay. The overlay is drawn just in front of the current item's depth instead
of at depth zero, with its matrix and render state restored afterward.

Darker material variants now follow canonical armor equipment identities rather
than the 1.7 ArmorMaterial enum. Chainmail shares IRON internally but is a distinct
vanilla equipment asset; iron trim on chainmail must use the normal iron palette,
whereas iron trim on iron armor uses iron_darker. Gold, diamond and EFR netherite
follow the corresponding vanilla overrides. Arbitrary mod armor does not inherit
a darker override merely because it reuses a vanilla armor-material enum.

## Independent restrictions

`functions.cfg`, settings:

- `allowModdedArmorTrims=false`: existing default; vanilla armor and EFR netherite.
- `allowModdedTrimMaterials=false`: new default; vanilla ingredients and enabled
  EFR copper, netherite, amethyst and resin equivalents. Vanilla iron/gold are
  identified directly, independent of OreDictionary order.

Turning on allowModdedTrimMaterials accepts OreDictionary ingot equivalents for
iron, gold, copper and netherite, using those existing vanilla trim colors. It does
not invent colors for uranium, lead or other materials, and does not enable mod
armor. Either restriction can be enabled independently. In a pack that disables
EFR copper and substitutes another mod's ingot, this option is needed for that
copper ingot to supply trims. Existing trimmed items continue rendering regardless
of whether their acquisition options are subsequently disabled.

The restrictions apply to trim creation, not unrelated custom two-input smithing
API recipes. Netherite upgrade rules, templates, IDs, the front-facing stand fix,
and Backporter mappings are unchanged.

## Validation

- Build/check and recipe display contracts passed.
- 640 official texture/palette permutations now check exact visible pixel colors,
  alpha, unmapped pixels and actual recoloring, rather than just shape/alpha.
- All 264 combinations of the 24 supported armor items and 11 materials select
  palettes according to the pinned vanilla trim-material JSON definitions.
- A grayscale regression checks decoded values against the original PNG raster.
- A new isolated headless Mixin test applies the production mixins to the real
  Forge RenderItem and RendererLivingEntity classes. Exported bytecode verifies
  that exactly one trim hook is in the boolean overload and none in its wrapper.
  The production worn/stand armor hooks also apply successfully.
- Disposable Forge save/restart tests passed alone and with NTM + Iron Chests:
  4,752 trim combinations per setup, exact pattern/material NBT, nine upgrade types
  with left/right extraction, costs, independent material opt-in and existing
  workstation/crafting regressions. Test-only OreDictionary fixtures are removed.
- Asset checks verify the reference jar and release contents; no new PNGs or test
  classes are distributed. Source hashes and test logs are recorded in VALIDATION.json.

These checks do not create a GL context. A brief client check remains necessary to
confirm the final drawing in the user's pack. This correction covers worn/stand
armor and GUI icons; held/dropped armor-item sprites retain the previously noted
base-sprite limitation.

## Minimal client check

Use the Dune trim on an iron chestplate and change the addition between gold,
redstone and lapis. The stand and inventory/output icon should follow the selected
material (gold, red and blue). Collect one result and verify its inventory icon
keeps its trim. Iron trim on chainmail should be lighter than iron trim on iron armor.

With both restriction options off, another mod's armor and ingot equivalents should
not create trims. Enable only allowModdedTrimMaterials to permit compatible mod
ingots while keeping mod armor restricted.

Existing working-tree changes are preserved. No commits, pushes, resets, stashes,
user-world changes or normal game-config edits were performed. The incremental ZIP
uses the exact pre-pass 3.5.26 working tree, source/payload hashes and safe backups.
