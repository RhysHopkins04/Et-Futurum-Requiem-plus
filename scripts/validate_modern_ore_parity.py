#!/usr/bin/env python3
"""Deterministic P009d diagnostic for modern ore placement/cluster parity.

This is intentionally offline and stdlib-only. It validates the Java source signatures that encode
Minecraft 1.21-era placement counts/ranges, computes exact in-world origin expectations for the
logical -64..319 contract, and simulates the ported OreFeature ellipsoid geometry with java.util.Random
semantics. It does not pretend cave exposure/host replacement can be reproduced without a real world;
those remain runtime-test concerns.
"""

from __future__ import print_function

import math
import re
import statistics
import struct
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ORE_JAVA = ROOT / "src/main/java/ganymedes01/etfuturum/world/generate/terrain/ModernOverworldOreGenerator.java"
MODERN_MIN = -64
MODERN_MAX = 319


class JavaRandom(object):
    MULT = 0x5DEECE66D
    ADD = 0xB
    MASK = (1 << 48) - 1

    def __init__(self, seed):
        self.seed = (int(seed) ^ self.MULT) & self.MASK

    def _next(self, bits):
        self.seed = (self.seed * self.MULT + self.ADD) & self.MASK
        return self.seed >> (48 - bits)

    def next_int(self, bound):
        if bound <= 0:
            raise ValueError("bound must be positive")
        if (bound & -bound) == bound:
            return (bound * self._next(31)) >> 31
        while True:
            bits = self._next(31)
            value = bits % bound
            if bits - value + (bound - 1) >= 0:
                return value

    def next_float(self):
        return self._next(24) / float(1 << 24)

    def next_double(self):
        return ((self._next(26) << 27) + self._next(27)) / float(1 << 53)


# name, count model, provider, min, max, configured size, air discard
# count model is either a fixed int, ("uniform", lo, hi), or ("rarity", denominator).
PLACEMENTS = (
    ("coal_upper", 30, "uniform", 136, 319, 17, 0.0),
    ("coal_lower", 20, "triangle", 0, 192, 17, 0.5),
    ("iron_high", 90, "triangle", 80, 384, 9, 0.0),
    ("iron_middle", 10, "triangle", -24, 56, 9, 0.0),
    ("iron_small", 10, "uniform", -64, 72, 4, 0.0),
    ("gold_main", 4, "triangle", -64, 32, 9, 0.5),
    ("gold_deep", ("uniform", 0, 1), "uniform", -64, -48, 9, 0.5),
    ("redstone", 4, "uniform", -64, 15, 8, 0.0),
    ("redstone_lower", 8, "triangle", -96, -32, 8, 0.0),
    ("diamond_small", 7, "triangle", -144, 16, 4, 0.5),
    ("diamond_medium", 2, "uniform", -64, -4, 8, 0.5),
    ("diamond_large", ("rarity", 9), "triangle", -144, 16, 12, 0.7),
    ("diamond_buried", 4, "triangle", -144, 16, 8, 1.0),
    ("lapis_exposed", 2, "triangle", -32, 32, 7, 0.0),
    ("lapis_buried", 4, "uniform", -64, 64, 7, 1.0),
    ("copper", 16, "triangle", -16, 112, 10, 0.0),
    ("emerald_mountain", 100, "triangle", -16, 480, 3, 0.0),
    ("gold_badlands_extra", 50, "uniform", 32, 256, 9, 0.0),
)


def count_expectation(model):
    if isinstance(model, int):
        return float(model)
    if model[0] == "uniform":
        return (model[1] + model[2]) / 2.0
    if model[0] == "rarity":
        return 1.0 / model[1]
    raise AssertionError(model)


def uniform_pmf(lo, hi):
    n = hi - lo + 1
    return {y: 1.0 / n for y in range(lo, hi + 1)}


def triangle_pmf(lo, hi):
    span = hi - lo
    lower_half = span // 2
    upper_half = span - lower_half
    denom = float((upper_half + 1) * (lower_half + 1))
    counts = Counter()
    for a in range(upper_half + 1):
        for b in range(lower_half + 1):
            counts[lo + a + b] += 1
    return {y: c / denom for y, c in counts.items()}


def provider_pmf(kind, lo, hi):
    return uniform_pmf(lo, hi) if kind == "uniform" else triangle_pmf(lo, hi)


def expected_valid_origins(entry):
    _name, count_model, kind, lo, hi, _size, _discard = entry
    in_world = sum(p for y, p in provider_pmf(kind, lo, hi).items() if MODERN_MIN <= y <= MODERN_MAX)
    return count_expectation(count_model) * in_world, in_world


def f32(v):
    return struct.unpack('>f', struct.pack('>f', float(v)))[0]


def floor_java(v):
    i = int(v)
    return i - 1 if v < i else i


def ellipsoid_candidate_count(size, seed):
    rand = JavaRandom(seed)
    x = y = z = 0
    angle = f32(f32(rand.next_float()) * f32(math.pi))
    half_span = f32(size / 8.0)
    sin_a = math.sin(angle)
    cos_a = math.cos(angle)
    start_x, end_x = x + sin_a * half_span, x - sin_a * half_span
    start_z, end_z = z + cos_a * half_span, z - cos_a * half_span
    start_y = y + rand.next_int(3) - 2
    end_y = y + rand.next_int(3) - 2

    ellipsoids = []
    for node in range(size):
        t = node / float(size)
        random_scale = rand.next_double() * size / 16.0
        radius = ((math.sin(math.pi * t) + 1.0) * random_scale + 1.0) * 0.5
        ellipsoids.append([
            start_x + (end_x - start_x) * t,
            start_y + (end_y - start_y) * t,
            start_z + (end_z - start_z) * t,
            radius,
        ])

    for first in range(size - 1):
        if ellipsoids[first][3] <= 0.0:
            continue
        for second in range(first + 1, size):
            if ellipsoids[second][3] <= 0.0:
                continue
            dr = ellipsoids[first][3] - ellipsoids[second][3]
            dx = ellipsoids[first][0] - ellipsoids[second][0]
            dy = ellipsoids[first][1] - ellipsoids[second][1]
            dz = ellipsoids[first][2] - ellipsoids[second][2]
            if dr * dr > dx * dx + dy * dy + dz * dz:
                if dr > 0.0:
                    ellipsoids[second][3] = -1.0
                else:
                    ellipsoids[first][3] = -1.0

    visited = set()
    for cx, cy, cz, radius in ellipsoids:
        if radius <= 0.0:
            continue
        for px in range(floor_java(cx - radius), floor_java(cx + radius) + 1):
            xn = (px + 0.5 - cx) / radius
            if xn * xn >= 1.0:
                continue
            for py in range(floor_java(cy - radius), floor_java(cy + radius) + 1):
                yn = (py + 0.5 - cy) / radius
                if xn * xn + yn * yn >= 1.0:
                    continue
                for pz in range(floor_java(cz - radius), floor_java(cz + radius) + 1):
                    zn = (pz + 0.5 - cz) / radius
                    if xn * xn + yn * yn + zn * zn < 1.0:
                        visited.add((px, py, pz))
    return len(visited)


def percentile(values, p):
    values = sorted(values)
    if not values:
        return 0
    index = int(round((len(values) - 1) * p))
    return values[index]


def require_source_signatures(text):
    required = (
        # Counts / ranges / configured feature sizes.
        '30, 136, WorldHeightCompat.MODERN_MAX_Y, 0.0D',
        '20, 0, 192, 0.5D',
        '90, 80, 384, 0.0D',
        '10, -24, 56, 0.0D',
        '10, WorldHeightCompat.MODERN_MIN_Y, 72, 0.0D',
        '4, WorldHeightCompat.MODERN_MIN_Y, 32, 0.5D',
        'int deepAttempts = rand.nextInt(2);',
        '4, WorldHeightCompat.MODERN_MIN_Y, 15, 0.0D',
        '8, -96, -32, 0.0D',
        '7, -144, 16, 0.5D',
        '2, WorldHeightCompat.MODERN_MIN_Y, -4, 0.5D',
        'if (rand.nextInt(9) == 0)',
        '1, -144, 16, 0.7D',
        '4, -144, 16, 1.0D',
        '2, -32, 32, 0.0D',
        '4, WorldHeightCompat.MODERN_MIN_Y, 64, 1.0D',
        'int logicalY = sampleTriangle(rand, -16, 112);',
        'int size = regions.isDripstone(x, physicalY, z) ? 20 : 10;',
        '100, -16, 480, 0.0D',
        '50, 32, 256, 0.0D',
        # Modern OreFeature geometry / exposure RNG semantics.
        'final float angle = rand.nextFloat() * (float) Math.PI;',
        'final float halfSpan = size / 8.0F;',
        'final double[] ellipsoids = new double[size * 4];',
        'double randomScale = rand.nextDouble() * size / 16.0D;',
        'radiusDelta * radiusDelta > dx * dx + dy * dy + dz * dz',
        'Set<Long> visited = new HashSet<Long>',
        'rand.nextFloat() < (float) airDiscard',
        '&& isAdjacentToAir(world, px, py, pz)',
        # P009c re-entrancy hardening.
        '!visited.add(key) || !isChunkLoaded(world, px, pz)',
        'world.getChunkProvider().chunkExists(x >> 4, z >> 4)',
        # Large-vein ranges / matrices / raw blocks remain bounded and rare-system-specific.
        'generateNoiseVein(world, chunkX, chunkZ, 0, 50',
        'generateNoiseVein(world, chunkX, chunkZ, -60, -8',
        'Blocks.stone, 1, VEIN_COPPER_A, VEIN_COPPER_B',
        'ModBlocks.TUFF.isEnabled() ? ModBlocks.TUFF.get() : Blocks.stone',
        'ModBlocks.RAW_ORE_BLOCK',
    )
    missing = [token for token in required if token not in text]
    if missing:
        print("P009d modern ore parity diagnostic FAILED")
        for token in missing:
            print(" - missing source invariant: {}".format(token))
        return False

    exposure_roll = text.find('rand.nextFloat() < (float) airDiscard')
    adjacency = text.find('&& isAdjacentToAir(world, px, py, pz)', exposure_roll)
    if exposure_roll < 0 or adjacency < exposure_roll:
        print("P009d modern ore parity diagnostic FAILED")
        print(" - air-exposure RNG roll is not consumed before adjacency evaluation")
        return False
    return True


def main():
    text = ORE_JAVA.read_text(encoding="utf-8")
    if not require_source_signatures(text):
        return 1

    print("P009d modern ore parity diagnostic PASSED")
    print("Logical world: {}..{} (physical {}..{})".format(MODERN_MIN, MODERN_MAX, MODERN_MIN + 64, MODERN_MAX + 64))
    print("\nExpected valid feature origins per chunk (before biome/host/air replacement):")
    totals = Counter()
    for entry in PLACEMENTS:
        name = entry[0]
        expected, in_world = expected_valid_origins(entry)
        family = name.split('_', 1)[0]
        totals[family] += expected
        print(" {:21s} {:7.3f}  in-world-height probability {:6.2f}%  size {:2d} discard {:.1f}".format(
            name, expected, in_world * 100.0, entry[5], entry[6]))

    print("\nFamily origin totals (features in biome-specific rows are conditional):")
    for family in sorted(totals):
        print(" {:10s} {:7.3f}".format(family, totals[family]))

    print("\nAll-solid OreFeature candidate-cluster geometry (4096 deterministic Java-RNG samples):")
    for size in (3, 4, 7, 8, 9, 10, 12, 17, 20):
        values = [ellipsoid_candidate_count(size, 0x5EED0000 + size * 10000 + i) for i in range(4096)]
        print(" size {:2d}: mean {:6.2f}, median {:4.0f}, p95 {:4d}, min {:2d}, max {:3d}".format(
            size, statistics.mean(values), statistics.median(values), percentile(values, 0.95), min(values), max(values)))

    # Sanity assertions chosen to catch accidental return to the P009b oversized-blob failure mode,
    # not to assert cave-visible block counts (which depend on host terrain and exposure).
    means = {}
    for size in (4, 9, 17, 20):
        values = [ellipsoid_candidate_count(size, 0xC0FFEE + size * 8192 + i) for i in range(1024)]
        means[size] = statistics.mean(values)
    if not (means[4] < means[9] < means[17] < means[20] and means[20] < 80.0):
        print("\nFAILED: configured-size geometry scaling is implausible: {}".format(means))
        return 1

    diamond_total = totals["diamond"]
    if not (7.0 < diamond_total < 9.0):
        print("\nFAILED: diamond valid-origin expectation drifted unexpectedly: {:.4f}".format(diamond_total))
        return 1

    print("\nDiagnostic note: cluster counts are candidate blocks in solid host terrain; cave exposure, discard chance,")
    print("deepslate/stone replacement and overlapping world features reduce what a player actually sees.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
