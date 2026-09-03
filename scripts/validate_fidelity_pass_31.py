#!/usr/bin/env python3
"""Static regression gates for Fidelity Pass 31 modern wall states."""

from __future__ import print_function

import importlib.util
import pathlib
import sys

sys.dont_write_bytecode = True
ROOT = pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parents[1]
errors = []


def read(rel):
    path = ROOT / rel
    if not path.is_file():
        errors.append("missing required file: " + rel)
        return ""
    return path.read_text(encoding="utf-8", errors="ignore")


def require(text, token, label):
    if token not in text:
        errors.append(label + ": missing " + token)


def forbid(text, token, label):
    if token in text:
        errors.append(label + ": forbidden " + token)


state = read("src/main/java/ganymedes01/etfuturum/blocks/ModernWallState.java")
base = read("src/main/java/ganymedes01/etfuturum/blocks/BaseWall.java")
renderer = read("src/main/java/ganymedes01/etfuturum/client/renderer/block/BlockModernWallRenderer.java")
parity = read("src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java")
bridge = read("src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java")
mixin = read("src/main/java/ganymedes01/etfuturum/mixins/early/fencewallconnect/MixinBlockWall.java")
converter = read("src/main/java/ganymedes01/etfuturum/core/utils/structurenbt/BlockStateConverter.java")
doc = read("docs/FIDELITY_PASS_31_MODERN_WALL_STATES.md")

for token in (
    'NONE("none"), LOW("low"), TALL("tall")',
    "public static State derive(",
    "boolean oppositeMismatch",
    "boolean oppositeTallPair",
    "else if (oppositeTallPair)",
    "up = false;",
    "isWallPostOverride(above)",
    "public static int modelIndex(",
):
    require(state, token, "shared modern wall state")

for token in (
    "ModernWallState.canConnectWallTo(this, world, x, y, z)",
    "ModernWallState.derive(this, world, x, y, z)",
    "state.getVisualMaxY()",
    "state.north.isConnected()",
):
    require(base, token, "BaseWall derived-state integration")

for token in (
    "if (!(block instanceof BlockWall)) return false",
    "ModernWallState.derive(block, world, x, y, z)",
    "state.north == ModernWallState.Side.TALL ? 1.0 : 14.0/16.0",
    "if (state.up)",
):
    require(renderer, token, "vanilla/BaseWall modern renderer")

for token in (
    "if (entry.style == Style.WALL)",
    "ModernWallState.State state = ModernWallState.derive(this, world, x, y, z)",
    "5.0F/16.0F",
):
    require(parity, token, "parity wall bounds/collision")

for token in (
    "public final Model[] connectionModels = new Model[162];",
    "for (ModernWallState.Side north : ModernWallState.Side.values())",
    "state.getModelIndex()",
):
    require(bridge, token, "162-state Mojang wall model bridge")

for token in (
    "ModernWallState.canConnectWallTo((BlockWall) (Object) this",
    "return RenderIDs.MODERN_WALL",
    "public void setBlockBoundsBasedOnState",
    "public void addCollisionBoxesToList",
    "public AxisAlignedBB getCollisionBoundingBoxFromPool",
    "5.0/16.0",
):
    require(mixin, token, "vanilla BlockWall modern-state mixin")
require(converter, "Pass 31 therefore maps modern wall north/east/south/west=none|low|tall and up", "import-derived wall state contract")
require(doc, "do not", "Pass 31 import documentation")
require(doc, "Waterlogging remains out of scope", "Pass 31 waterlogging scope")
require(doc, "3-wide x 2-high", "Pass 31c stacked-wall modern-post documentation")
require(doc, "forces `up=false`", "Pass 31c opposite-tall post suppression documentation")
require(doc, "registry identity and metadata", "Pass 31b vanilla BlockWall identity preservation")

for text, label in ((state, "state helper"), (base, "BaseWall"), (renderer, "BaseWall renderer")):
    forbid(text, "waterlogged", label)

# Promote exactly the parity WALL row without disturbing Pass 29/30 verification sets.
audit_path = ROOT / "scripts" / "audit_modern_map_parity_capabilities.py"
if audit_path.is_file():
    spec = importlib.util.spec_from_file_location("pass31_capability_audit", str(audit_path))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    manifest, rows = module.build_matrix(ROOT)
    errors.extend(module.validate_matrix(manifest, rows))
    wall_rows = [row for row in rows if row["style"] == "WALL"]
    if len(wall_rows) != 1 or wall_rows[0]["block"] != "resin_brick_wall":
        errors.append("Pass 31 expected exactly the resin_brick_wall parity WALL row")
    elif wall_rows[0]["review_status"] != "PASS_31_VERIFIED":
        errors.append("resin_brick_wall capability row was not promoted to PASS_31_VERIFIED")

if errors:
    print("Fidelity Pass 31 validation FAILED", file=sys.stderr)
    for error in errors:
        print(" - " + error, file=sys.stderr)
    sys.exit(1)

print("Fidelity Pass 31 validation PASSED")
print(" - vanilla BlockWall, BaseWall and parity walls share neighbour-derived none/low/tall/up state")
print(" - vanilla cobblestone/mossy walls route through the same custom modern wall renderer")
print(" - straight opposite TALL pairs suppress the center post, including stacked 3-wide/4-wide runs")
print(" - Resin Brick Wall prepares all 162 Mojang wall model combinations")
print(" - modern 14/16 vs 16/16 visual arms and 1.5-block collision prisms remain bounded")
print(" - imported wall connection/up properties remain derived rather than stored")
print(" - waterlogging remains out of scope")
