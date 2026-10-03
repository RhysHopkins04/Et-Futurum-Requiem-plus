#!/usr/bin/env python3
from pathlib import Path
import re, sys
ROOT=Path(__file__).resolve().parents[1]
ERR=[]
def need(cond,msg):
    if not cond: ERR.append(msg)
def text(rel): return (ROOT/rel).read_text(errors='replace')

def method_body(src, name):
    # Bounded brace parser so the gate examines the actual method rather than a nearby comment.
    m=re.search(r'\b(?:public|private|protected)\s+(?:[\w<>\[\]]+\s+)?'+re.escape(name)+r'\s*\([^)]*\)\s*\{',src)
    if not m: return None
    start=src.find('{',m.start())
    depth=0
    for i in range(start,len(src)):
        if src[i]=='{': depth+=1
        elif src[i]=='}':
            depth-=1
            if depth==0: return src[start+1:i]
    return None

RUN=text('src/main/java/ganymedes01/etfuturum/tileentities/TileEntitySculkRuntime.java')
CAT=text('src/main/java/ganymedes01/etfuturum/tileentities/TileEntitySculkCatalyst.java')
DOC=text('docs/FIDELITY_PASS_38_SCULK_GAMEPLAY.md')

for label,src in [('Sculk runtime',RUN),('Catalyst',CAT)]:
    body=method_body(src,'validate')
    need(body is not None,label+' validate() missing')
    if body is not None:
        for forbidden in ('getBlock(', 'getBlockMetadata(', 'setBlockMetadataWithNotify(', 'getTileEntity(', 'getChunkFrom', 'register('):
            need(forbidden not in body,label+' validate() performs unsafe world/registration work: '+forbidden)
        need('super.validate()' in body,label+' validate() must still call super.validate()')
        need('runtimeInitPending' in body,label+' validate() does not defer runtime initialization')

need('ensureRuntimeReady();' in RUN and 'SculkVibrationDispatcher.register(this)' in RUN,
     'Sensor/Shrieker registration is not deferred to updateEntity')
need('normalizeImportedTransientState();' in RUN and RUN.find('ensureRuntimeReady();', RUN.find('updateEntity')) >= 0,
     'Sensor/Shrieker imported-state normalization is not deferred to first installed tick')
need('if (runtimeRegistered) SculkVibrationDispatcher.unregister(this);' in RUN,
     'Sensor/Shrieker unload does not guard unregister against never-registered TEs')

need('ensureRuntimeReady();' in CAT and 'SculkEventHandler.INSTANCE.registerCatalyst(this)' in CAT,
     'Catalyst registration is not deferred to updateEntity')
need('if(runtimeRegistered)SculkEventHandler.INSTANCE.unregisterCatalyst(this);' in CAT,
     'Catalyst unload does not guard unregister against never-registered TEs')
cat_ready=method_body(CAT,'ensureRuntimeReady') or ''
need('getBlockMetadata' in cat_ready and 'bloomEndsAt=now+8L' in cat_ready,
     'Catalyst imported bloom normalization was lost instead of deferred')

# Regression-sensitive Pass-38 mechanics must remain present.
need('clearPendingVibration();' in RUN and 'phaseEndsAt=now+10' in RUN,
     'Sensor stale-arrival/cooldown correction regressed')
need('block.sculk_shrieker.shriek' in RUN and 'block.sculk_catalyst.bloom' in CAT,
     'Pass-38b Sculk sound calls regressed')
need('Pass 38c chunk-load lifecycle correction' in DOC and 'StackOverflowError' in DOC,
     'Pass-38c lifecycle correction is not documented')

if ERR:
    print('Fidelity Pass 38c validation FAILED')
    for e in ERR: print(' - '+e)
    sys.exit(1)
print('Fidelity Pass 38c validation PASSED')
print(' - Sculk Catalyst validate() performs no chunk/world reads during TileEntity insertion')
print(' - Sensor/Calibrated/Shrieker validate() performs no world reads or early listener registration')
print(' - registration and imported transient-state normalization are deferred to the first installed server tick')
print(' - guarded unload/invalidate paths preserve listener/catalyst registry safety')
print(' - Pass-38b cooldown and sound corrections remain intact')
