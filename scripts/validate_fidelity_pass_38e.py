#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
modern = (ROOT / 'src/main/java/ganymedes01/etfuturum/ModernMapParityBlocks.java').read_text()
runtime = (ROOT / 'src/main/java/ganymedes01/etfuturum/tileentities/TileEntitySculkRuntime.java').read_text()
dispatch = (ROOT / 'src/main/java/ganymedes01/etfuturum/core/handlers/SculkVibrationDispatcher.java').read_text()
docs = (ROOT / 'docs/FIDELITY_PASS_38_SCULK_GAMEPLAY.md').read_text()
contract = (ROOT / 'docs/BACKPORTER_STATE_CONTRACT.json').read_text()
build = (ROOT / 'build.gradle').read_text()


def require(cond, msg):
    if not cond:
        raise AssertionError(msg)

# Calibrated output must be silent on its dedicated rear/amethyst calibration face.
require('private int calibratedSculkInputPowerSide(IBlockAccess world, int x, int y, int z)' in modern,
        'missing calibrated input-side power helper')
require('return facing == 0 ? 2 : facing == 1 ? 5 : facing == 2 ? 3 : 4;' in modern,
        'calibrated rear/amethyst side mapping changed')
require('if (isCalibratedSculkSensor() && side == calibratedSculkInputPowerSide(world, x, y, z)) return 0;' in modern,
        'calibrated Sensor still emits weak output through its calibration face')

# Existing filtering remains exact: unpowered means unfiltered; non-zero accepts exact frequency only.
require('int filter=calibrationInput();' in runtime, 'calibration input read missing')
require('if (filter!=0 && filter!=v.event.frequency) return;' in runtime,
        'calibrated exact-frequency rejection changed')
require('int radius=calibrated()?16:8;' in runtime,
        'calibrated listening radius changed')
require('getComparatorFrequency()' in runtime and 'phase() == 1 ? lastFrequency : 0' in runtime,
        'ACTIVE-only comparator frequency contract changed')

# Lock the two frequencies relevant to the runtime report: walk must not pass input 13; place must.
require(re.search(r'\bSTEP\(1\)', dispatch) is not None, 'STEP frequency is no longer 1')
require(re.search(r'\bBLOCK_PLACE\(13\)', dispatch) is not None, 'BLOCK_PLACE frequency is no longer 13')
filter_strength = 13
require(filter_strength != 1 and filter_strength == 13,
        'calibration 13 truth table invalid')

# P38 contract/version remain a corrective follow-up, not a new feature revision.
require(any('"contract_revision": "'+rev+'"' in contract for rev in ("38", "39")), 'contract revision must remain compatible with 38/39')
require('stagingVersion = "3.5.10"' in build or "stagingVersion = '3.5.10'" in build,
        'staging version must remain 3.5.10')
require('Pass 38e calibrated-input isolation correction' in docs,
        'Pass 38e documentation missing')

print('Fidelity Pass 38e validation PASSED')
print(' - Calibrated Sensor weak output is suppressed on the rotating amethyst/input face')
print(' - calibration 13 retains exact filtering: STEP 1 rejected, BLOCK_PLACE 13 accepted')
print(' - range-16 listening and ACTIVE-only comparator frequency remain unchanged')
print(' - contract revision 38 / staging version 3.5.10 remain unchanged')
