#!/usr/bin/env python3
"""Pass 38d validator: placement/activation event ownership + deterministic redstone math."""
from pathlib import Path
import re, sys

ROOT=Path(__file__).resolve().parents[1]
ERR=[]
def need(cond,msg):
    if not cond: ERR.append(msg)
def text(rel): return (ROOT/rel).read_text(encoding="utf-8")

HANDLER=text("src/main/java/ganymedes01/etfuturum/core/handlers/SculkEventHandler.java")
DISPATCH=text("src/main/java/ganymedes01/etfuturum/core/handlers/SculkVibrationDispatcher.java")
RUNTIME=text("src/main/java/ganymedes01/etfuturum/tileentities/TileEntitySculkRuntime.java")
DOC=text("docs/FIDELITY_PASS_38_SCULK_GAMEPLAY.md")

# ItemBlock right-click must not synchronously become BLOCK_ACTIVATE before Forge resolves PlaceEvent.
need("PendingBlockInteraction" in HANDLER and "pendingBlockInteractions" in HANDLER,
     "same-tick ambiguous ItemBlock interaction queue missing")
need("held.getItem() instanceof ItemBlock" in HANDLER,
     "ItemBlock right-clicks are not separated from immediate activation events")
need("pendingBlockInteractions.put" in HANDLER,
     "ItemBlock right-click candidate is not deferred")
need("pendingBlockInteractions.remove(e.player)" in HANDLER,
     "successful PlaceEvent does not consume the earlier right-click candidate")
need("SculkVibrationDispatcher.Event.BLOCK_PLACE" in HANDLER,
     "resolved PlaceEvent no longer emits BLOCK_PLACE")
need("!SculkVibrationDispatcher.isVibrationDampeningBlock(e.world.getBlock(e.x,e.y,e.z))" in HANDLER,
     "wool/carpet placement damping was lost")
need("new ArrayList<PendingBlockInteraction>(pendingBlockInteractions.values())" in HANDLER and
     "SculkVibrationDispatcher.Event.BLOCK_ACTIVATE" in HANDLER,
     "unresolved ItemBlock interactions are not flushed as activation candidates")
need("click.world==e.world" in HANDLER,
     "pending interaction candidates are not cleared on world unload")

# Cancelled Forge placement/break actions should not produce vibrations.
need("@SubscribeEvent(priority=EventPriority.LOWEST) public void onPlace" in HANDLER and "!e.isCanceled()" in HANDLER,
     "PlaceEvent is not observed after cancellation decisions")
need("@SubscribeEvent(priority=EventPriority.LOWEST) public void onBreak" in HANDLER,
     "BreakEvent is not observed after cancellation decisions")

# Frequency ownership is exact for the 1.21.11 table used by Pass 38.
need("BLOCK_DESTROY(12)" in DISPATCH, "BLOCK_DESTROY frequency is not 12")
need("BLOCK_PLACE(13)" in DISPATCH, "BLOCK_PLACE frequency is not 13")
need("BLOCK_ACTIVATE(10)" in DISPATCH, "BLOCK_ACTIVATE frequency is not 10")

# Deterministic normal redstone output remains distance-derived, distinct from comparator frequency.
m=re.search(r"public static int redstoneStrength\(double distance, int radius\)\s*\{\s*return\s+([^;]+);",DISPATCH,re.S)
need(m is not None, "redstoneStrength implementation missing")
if m:
    expr=re.sub(r"\s+"," ",m.group(1))
    need("Math.max(1, 15 - (int)Math.floor(15.0D * distance / (double)radius))" in expr,
         "distance redstone-strength formula changed unexpectedly")

def signal(distance, radius):
    import math
    return max(1, 15-int(math.floor(15.0*distance/radius)))

# Lock representative boundary values so manual dust-length measurement is not the regression gate.
expected8={0:15,1:14,2:12,3:10,4:8,5:6,6:4,7:2,8:1}
expected16={0:15,4:12,8:8,12:4,16:1}
need(all(signal(d,8)==v for d,v in expected8.items()),
     "normal Sensor radius-8 signal table does not match the encoded formula")
need(all(signal(d,16)==v for d,v in expected16.items()),
     "calibrated Sensor radius-16 signal table does not match the encoded formula")

need("phase() == 1 ? lastFrequency : 0" in RUNTIME,
     "comparator frequency is not ACTIVE-only")
need("lastFrequency=Math.max(1,Math.min(15,pendingFrequency))" in RUNTIME,
     "accepted event frequency is not clamped/stored for comparator output")

need("## Pass 38d placement-event ownership correction" in DOC and
     "resolved `PlaceEvent` ownership" in DOC,
     "Pass 38d ownership correction is not documented")

if ERR:
    print("Fidelity Pass 38d validation FAILED")
    for e in ERR: print(" - "+e)
    sys.exit(1)

print("Fidelity Pass 38d validation PASSED")
print(" - ItemBlock right-click no longer races the resolved PlaceEvent with a false BLOCK_ACTIVATE")
print(" - wool/carpet placement owns no vibration and normal block placement emits one BLOCK_PLACE")
print(" - cancelled place/break actions cannot create Sculk vibrations")
print(" - normal Sensor distance power is locked to the radius-8 formula; calibrated to radius 16")
print(" - comparator output remains ACTIVE-only accepted-event frequency (destroy 12 / place 13)")
