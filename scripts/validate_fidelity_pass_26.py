#!/usr/bin/env python3
"""Static contract guard for Minecraft 1.21.11 Fidelity Pass 26."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
failures = []


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


def require(text, token, label):
    if token not in text:
        failures.append(f"{label} missing: {token}")


def forbid(text, token, label):
    if token in text:
        failures.append(f"{label} returned: {token}")


parity = read("src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java")
model = read("src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java")
asset_pack = read("src/main/java/ganymedes01/etfuturum/client/ModernAssetResourcePack.java")
wall = read("src/main/java/ganymedes01/etfuturum/blocks/BaseWall.java")
wall_renderer = read("src/main/java/ganymedes01/etfuturum/client/renderer/block/BlockModernWallRenderer.java")
render_ids = read("src/main/java/ganymedes01/etfuturum/lib/RenderIDs.java")
client_proxy = read("src/main/java/ganymedes01/etfuturum/core/proxy/ClientProxy.java")
cave_tip = read("src/main/java/ganymedes01/etfuturum/blocks/BlockCaveVines.java")
cave_base = read("src/main/java/ganymedes01/etfuturum/blocks/BaseCaveVines.java")
cave_item = read("src/main/java/ganymedes01/etfuturum/items/ItemGlowBerries.java")
cave_worldgen = read("src/main/java/ganymedes01/etfuturum/world/generate/decorate/WorldGenCaveVines.java")
world_config = read("src/main/java/ganymedes01/etfuturum/configuration/configs/ConfigWorld.java")

# Axis families: metadata 0/1/2 is Y/X/Z, chosen from the clicked face and resolved
# through the real AssetDirector blockstate variants.
for token in (
    "if (isFroglight() || isCopperChain())",
    "if (side == 4 || side == 5) return 1; // X",
    "if (side == 2 || side == 3) return 2; // Z",
    'registryName.endsWith("_froglight") || registryName.endsWith("copper_chain")',
    'String[] axes = {"y", "x", "z"};',
    'state.put("axis", axes[axis]);',
):
    require(parity + model, token, "Froglight/Copper Chain axis contract")

# Copper Lantern has a stored hanging bit, matching authored standing/hanging models
# and validating the matching support after neighbour updates.
for token in (
    "private boolean isCopperLantern()",
    "return canCopperLanternStand(world, x, y, z) ? 0 : 1;",
    "canCopperLanternHang(world, x, y, z)",
    'state.put("hanging", Boolean.toString(hanging != 0));',
    'if (name.endsWith("copper_lantern"))',
):
    require(parity + model, token, "Copper Lantern hanging contract")

# Shelves keep facing in bits 0..1, powered in bit 2, and derive the visual chain
# position from matching powered neighbours instead of consuming registry identities.
for token in (
    "int powered = world.isBlockIndirectlyGettingPowered(x, y, z) ? 4 : 0;",
    'String[] chains = {"unconnected", "unconnected", "left", "center", "right"};',
    'state.put("side_chain", chains[shelfState]);',
    "private static int shelfState(",
    "connectedPoweredShelf(",
    "models.facingModels[state * 4 + facing]",
):
    require(parity + model, token, "Shelf powered/side-chain contract")

# Chiseled Bookshelf preserves one block identity while a six-slot tile inventory
# supplies all 64 visual occupancy masks to the authored slot models.
for token in (
    "class ParityChiseledBookshelfTileEntity extends TileEntity implements IInventory",
    "private final ItemStack[] books = new ItemStack[6];",
    "public int getOccupancyMask()",
    "chiseledBookshelfSlot(facing, hitX, hitY, hitZ)",
    "chiseledBookshelfSlots[facing * 12 + slot * 2 + occupied]",
    '"minecraft:block/chiseled_bookshelf_" + stateName + "_slot_"',
):
    require(parity + model, token, "Chiseled Bookshelf six-slot contract")

# Decorated Pot remains on the special entity-texture geometry path; do not collapse
# it to a generic static bookshelf/cube or bundle Mojang payloads.
for token in (
    'if ("decorated_pot".equals(name)) return decoratedPotModel();',
    'String side = "minecraft:entity/decorated_pot/decorated_pot_side";',
    'String base = "minecraft:entity/decorated_pot/decorated_pot_base";',
    "addModelBoxUv(out, 4, 14, 4, 12, 16.7, 12",
):
    require(model, token, "Decorated Pot special-model contract")

# Potted Torchflower is block-only, uses the exact modern model graph, and the existing
# Torchflower item performs pot insertion/removal without exposing a duplicate item.
for token in (
    'POTTED_TORCHFLOWER("1.20", Style.SMALL',
    '"potted_torchflower".equals(name)',
    "GameRegistry.registerBlock(entry.block, (Class<? extends ItemBlock>) null, name);",
    "class ParityTorchflowerItemBlock extends ItemBlock",
    "world.setBlock(x, y, z, POTTED_TORCHFLOWER.get(), 0, 3);",
    "entry == POTTED_TORCHFLOWER",
):
    require(parity, token, "Potted Torchflower technical-block contract")

# Mud Brick/Tuff and the rest of BaseWall keep their subtype identities/textures while
# gaining the modern 8px post, 6px/14px arms, sensible gate/solid connections, and
# independent collision prisms.
for token in (
    "public boolean hasModernPost(",
    "block instanceof BlockFenceGate || block instanceof BaseWall",
    "addWallCollision(mask, list",
    "RenderIDs.MODERN_WALL",
):
    require(wall, token, "modern BaseWall contract")
for token in (
    "class BlockModernWallRenderer extends BlockModelBase",
    "5.0/16.0, 0, 0, 11.0/16.0, 14.0/16.0",
    "wall.canConnectWallTo",
):
    require(wall_renderer, token, "modern wall renderer contract")
require(render_ids, "MODERN_WALL = RenderingRegistry.getNextAvailableRenderId()", "modern wall render ID")
require(client_proxy, "new BlockModernWallRenderer()", "modern wall renderer registration")

# Cave Vines remain a manually usable content family. Their legacy atlas requests are
# explicitly redirected to the singular modern path and the Pass 26-touched sources use
# Java-compatible explicit casts instead of pattern-matching instanceof.
for token in (
    'startsWith("textures/blocks/cave_vines")',
    'replace("textures/blocks/", "textures/block/")',
):
    require(asset_pack, token, "Cave Vines modern asset bridge")
require(cave_base, "iicons[meta & 1]", "Cave Vines berry-state icon mask")
for text, label in ((cave_tip, "tip"), (cave_item, "item"), (cave_worldgen, "worldgen helper")):
    forbid(text, "instanceof BlockCaveVines vine", f"Cave Vines {label} Java compatibility")
    forbid(text, "instanceof TileEntityCaveVines te", f"Cave Vines {label} Java compatibility")
require(read("src/main/java/ganymedes01/etfuturum/ModBlocks.java"),
        "CAVE_VINE(ConfigBlocksItems.enableGlowBerries", "Cave Vines content gate")
forbid(read("src/main/java/ganymedes01/etfuturum/ModBlocks.java"),
       "CAVE_VINE(ConfigWorld.lushCavesWorldgen", "Cave Vines/worldgen coupling")

# Explicit scope/safety guards.
for name in (
    "extendedWorldHeight", "modernOverworldGeneration", "modernOreGeneration",
    "modernLargeOreVeins", "lushCavesWorldgen", "dripstoneCavesWorldgen",
):
    if not re.search(rf'getBoolean\("{re.escape(name)}",\s*catGeneration,\s*false,', world_config):
        failures.append(f"world-safety default is not false: {name}")

for rel in (
    "src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java",
    "src/main/java/ganymedes01/etfuturum/blocks/BaseWall.java",
    "src/main/java/ganymedes01/etfuturum/blocks/BlockCaveVines.java",
):
    forbid(read(rel), "waterlogged", f"Pass 26 waterlogging scope in {rel}")

for path in (ROOT / "src/main/java").rglob("*.java"):
    text = path.read_text(encoding="utf-8", errors="ignore")
    if "roadhog360.hogutils" in text or "me.mrnavastar.r" in text:
        failures.append(f"removed helper package returned in {path.relative_to(ROOT)}")

if failures:
    print("Fidelity Pass 26 validation FAILED")
    for failure in failures:
        print(" -", failure)
    sys.exit(1)

print("Fidelity Pass 26 validation PASSED")
print(" - Froglights and all Copper Chain variants resolve X/Y/Z placement models")
print(" - all Copper Lantern variants resolve standing/hanging placement models and support")
print(" - BaseWall families use modern post/arm rendering, connections and collision")
print(" - Shelves preserve facing plus powered/side-chain authored states")
print(" - Chiseled Bookshelf has six persistent inventory-backed visual slots")
print(" - Decorated Pot remains on its AssetDirector entity-texture special model")
print(" - Potted Torchflower is a block-only modern identity with normal pot interaction")
print(" - Cave Vines use modern assets, safe berry metadata and compatible casts")
print(" - waterlogging stayed out of scope; worldgen defaults remain off; HogUtils remains absent")
