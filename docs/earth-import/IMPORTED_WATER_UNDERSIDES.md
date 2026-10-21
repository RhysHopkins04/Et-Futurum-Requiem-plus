# Imported water underside tint

The vanilla 1.7.10 RenderBlocks liquid renderer tints top and side vertices with
BlockLiquid.colorMultiplier, but gives the bottom face only neutral 0.5 shading.
Modern AssetDirector water sprites are neutral, so this leaves falling water pale
when viewed from below.

The imported-profile client mixin multiplies that bottom-face shading by the same
block/biome RGB used elsewhere. It applies to still and flowing vanilla water;
lava and disabled/legacy profiles retain their existing rendering. AssetDirector
continues to supply both water sprites; no replacement image was added.

Build/contract checks pass. Actual client visual confirmation remains required,
including Angelica/resource-pack interaction: inspect a waterfall from below and
from the side, then compare with nearby still water. No OpenGL screenshot test
was performed headlessly. Different face brightness from directional shading is
expected; an untinted pale face is not.

The independent missing-water-update issue is handled in the Backporter's new
conversion output. Installing this jar does not add queued ticks to old saves.
