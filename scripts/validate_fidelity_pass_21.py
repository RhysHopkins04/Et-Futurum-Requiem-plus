#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def read(rel):
    p = ROOT / rel
    if not p.is_file():
        raise SystemExit('Fidelity Pass 21 validation FAILED: missing ' + rel)
    return p.read_text(encoding='utf-8', errors='replace')


def require(text, needle, label):
    if needle not in text:
        raise SystemExit('Fidelity Pass 21 validation FAILED: ' + label)

parity = read('src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java')
bridge = read('src/main/java/ganymedes01/etfuturum/client/model/ModernJsonModelBridge.java')
renderer = read('src/main/java/ganymedes01/etfuturum/client/renderer/block/BlockModernJsonModelRenderer.java')
proxy = read('src/main/java/ganymedes01/etfuturum/core/proxy/ClientProxy.java')
rod = read('src/main/java/ganymedes01/etfuturum/blocks/BlockLightningRod.java')
fence = read('src/main/java/ganymedes01/etfuturum/blocks/BlockWoodFence.java')
modern_fence = read('src/main/java/ganymedes01/etfuturum/blocks/BlockModernWoodFence.java')
en_us = read('src/main/resources/assets/etfuturum/lang/en_US.lang')
en_gb = read('src/main/resources/assets/etfuturum/lang/en_GB.lang')

# Default Lightning Rod: one historical registry identity, modern model reuse, six-way shape.
require(rod, 'setNames("lightning_rod")', 'historical lightning_rod registry identity changed')
require(rod, 'return RenderIDs.MODERN_MAP_PARITY;', 'default Lightning Rod is not routed through modern renderer')
require(rod, 'ModernMapParityBlocks.WAXED_LIGHTNING_ROD', 'default Lightning Rod does not reuse audited modern sibling model')
require(rod, 'return side >= 0 && side <= 5 ? side : 1;', 'default Lightning Rod lacks six-way placement metadata')
require(rod, 'facing == 2 || facing == 3', 'default Lightning Rod lacks N/S bounds')
require(rod, 'facing == 4 || facing == 5', 'default Lightning Rod lacks E/W bounds')
require(renderer, 'block == ModBlocks.LIGHTNING_ROD.get()', 'world renderer lacks default Lightning Rod compatibility route')
require(renderer, 'entry = ModernMapParityBlocks.WAXED_LIGHTNING_ROD;', 'world renderer does not reuse waxed-default rod JSON model')
require(proxy, 'new ItemModernJsonModelRenderer(ModernMapParityBlocks.WAXED_LIGHTNING_ROD)', 'default Lightning Rod item lacks modern JSON renderer')
for lang, data in [('en_US', en_us), ('en_GB', en_gb)]:
    require(data, 'tile.etfuturum.lightning_rod.name=Lightning Rod', lang + ' Lightning Rod translation missing')

# Pale Oak fence/gate atlas parity and connectivity.
require(parity, 'entry == PALE_OAK_FENCE || entry == PALE_OAK_FENCE_GATE', 'Pale Oak fence family does not share plank fallback')
require(parity, 'PALE_OAK_PLANKS.get().getIcon(side, 0)', 'Pale Oak fence family icon does not use Pale Oak Planks atlas entry')
require(parity, 'style == Style.FENCE && otherEntry.style == Style.FENCE_GATE', 'parity fence does not connect to parity gate')
require(bridge, 'entry.getStyle() == ModernMapParityBlocks.Style.FENCE', 'JSON connectivity path missing fence special-case')
require(bridge, 'otherEntry.getStyle() == ModernMapParityBlocks.Style.FENCE_GATE', 'JSON fence does not visually connect to parity gate')
require(fence, 'parity.getStyle() == ModernMapParityBlocks.Style.FENCE_GATE', 'legacy wood fences do not connect to Pale Oak gate')
require(modern_fence, 'parity.getStyle() == ModernMapParityBlocks.Style.FENCE_GATE', 'modern EFR fences do not connect to Pale Oak gate')

# Fence-gate mechanics: exact 1.7-compatible state contract + modern JSON states.
require(parity, 'private boolean isFenceGate()', 'FENCE_GATE mechanic discriminator missing')
require(parity, 'world.setBlockMetadataWithNotify(x, y, z, quadrant, 2);', 'Pale Oak gate placement orientation missing')
require(parity, 'meta | 4', 'Pale Oak gate OPEN state missing')
require(parity, 'meta & ~4', 'Pale Oak gate CLOSE state missing')
require(parity, 'world.isBlockIndirectlyGettingPowered(x, y, z)', 'Pale Oak gate redstone response missing')
require(parity, 'if ((meta & 4) != 0) return null;', 'open Pale Oak gate still exposes collision AABB')
require(parity, '0.625D, y + 1.5D', 'Pale Oak gate collision height/orientation guard missing')
require(parity, 'return (world.getBlockMetadata(x, y, z) & 4) != 0;', 'Pale Oak gate movement/open contract missing')
require(bridge, 'if (style == ModernMapParityBlocks.Style.FENCE_GATE)', 'modern bridge does not resolve gate states dynamically')
require(bridge, 'String[] facings = {"south", "west", "north", "east"};', 'gate facing mapping missing')
require(bridge, 'prepared.facingModels[direction | (open << 2)]', 'gate open/facing models are not cached independently')
require(bridge, 'p.put("in_wall", "false");', 'gate blockstate defaults omit in_wall')

# Pass 21 must not touch worldgen gates or resurrect removed helper dependencies.
combined = '\n'.join([parity, bridge, renderer, proxy, rod, fence, modern_fence])
for forbidden in ('roadhog360.hogutils', 'me.mrnavastar.r'):
    if forbidden in combined:
        raise SystemExit('Fidelity Pass 21 validation FAILED: forbidden helper dependency ' + forbidden)

print('Fidelity Pass 21 static validation PASSED')
print(' - default Lightning Rod keeps its registry identity but reuses the audited 1.21.11 JSON model/item path')
print(' - default Lightning Rod has six-way placement/bounds and en_US/en_GB naming')
print(' - Pale Oak fence/gate expose Pale Oak Planks as their legacy atlas fallback')
print(' - Pale Oak Fence Gate now has placement orientation, open/close, redstone and directional collision')
print(' - gate world rendering resolves all four facings and open/closed states from AssetDirector JSON')
print(' - parity and mature EFR fences connect to the Pale Oak gate')
