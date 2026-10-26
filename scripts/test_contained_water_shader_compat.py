#!/usr/bin/env python3
"""Isolated Java 8 adapter checks; fixtures are never added to the mod jar."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
JDK = Path('/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home/bin')

def write(root, name, text):
    p = root / name
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text)
    return str(p)

def main():
    with tempfile.TemporaryDirectory(prefix='efr-water-shader-') as temp:
        root = Path(temp)
        common = root / 'common'; api = root / 'api'; test = root / 'test'
        for p in (common, api, test): p.mkdir()
        sources = [
            write(root, 'stubs/net/minecraft/init/Blocks.java', '''package net.minecraft.init;
public class Blocks { public static final Object water=new Object(),flowing_water=new Object(); }'''),
            write(root, 'stubs/ganymedes01/etfuturum/core/utils/Logger.java', '''package ganymedes01.etfuturum.core.utils;
public class Logger { public static void warn(Object value) { throw new AssertionError(value); } }'''),
            str(ROOT/'src/main/java/ganymedes01/etfuturum/client/renderer/block/ContainedWaterShaderCompat.java')]
        subprocess.run([str(JDK/'javac'), '-d', str(common), *sources], check=True)
        fixtures = [
            write(root, 'fixtures/com/gtnewhorizons/angelica/rendering/StateAwareTessellator.java', '''package com.gtnewhorizons.angelica.rendering;
public interface StateAwareTessellator { void angelica$setShaderOverrideBlockId(short id); }'''),
            write(root, 'fixtures/net/coderbot/iris/block_rendering/BlockRenderingSettings.java', '''package net.coderbot.iris.block_rendering;
import java.util.Map;
public class BlockRenderingSettings {
 public static final BlockRenderingSettings INSTANCE=new BlockRenderingSettings();
 public Map<Object,Map<Integer,Integer>> ids;
 public Map<Object,Map<Integer,Integer>> getBlockMetaMatches() { return ids; }
}''')]
        old_fixtures = list(fixtures)
        fixtures += [
            write(root, 'fixtures/com/gtnewhorizons/angelica/rendering/celeritas/iris/BlockRenderContext.java', '''package com.gtnewhorizons.angelica.rendering.celeritas.iris;
public class BlockRenderContext { public short renderType=42; }'''),
            write(root, 'fixtures/com/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuildContext.java', '''package com.gtnewhorizons.angelica.rendering.celeritas;
import com.gtnewhorizons.angelica.rendering.celeritas.iris.BlockRenderContext;
public class AngelicaChunkBuildContext {
 public final BlockRenderContext context=new BlockRenderContext();
 public BlockRenderContext getBlockRenderContext() { return context; }
}''')]
        subprocess.run([str(JDK/'javac'), '-d', str(api), *fixtures], check=True)
        old_api = root / 'old-api'; old_api.mkdir()
        subprocess.run([str(JDK/'javac'), '-d', str(old_api), *old_fixtures], check=True)
        harness = write(root, 'Harness.java', '''import java.util.*;
import net.minecraft.init.Blocks;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import com.gtnewhorizons.angelica.rendering.StateAwareTessellator;
import ganymedes01.etfuturum.client.renderer.block.ContainedWaterShaderCompat;
public class Harness {
 public static class Tess implements StateAwareTessellator {
  public short id=-1;
  public void angelica$setShaderOverrideBlockId(short id) { this.id=id; }
 }
 static void check(boolean ok) { if(!ok)throw new AssertionError(); }
 static void mapping(int id) {
  Map<Integer,Integer> values=new HashMap<>(); values.put(0,id);
  Map<Object,Map<Integer,Integer>> map=new HashMap<>(); map.put(Blocks.water,values);
  BlockRenderingSettings.INSTANCE.ids=map;
 }
 public static void main(String[] args) {
  if(args.length>0 && args[0].equals("absent")) {
   try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(new Object())) { }
   try(ContainedWaterShaderCompat.FluidScope f=ContainedWaterShaderCompat.fluidBatch(new Object(),new int[]{32000,32000,32000,32000},4)) { }
   System.out.println("PASS without Angelica"); return;
  }
  Tess tess=new Tess(); mapping(32000);
  if(args.length>0 && args[0].equals("old")) {
   try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(tess)) { check(tess.id==32000); }
   try(ContainedWaterShaderCompat.FluidScope f=ContainedWaterShaderCompat.fluidBatch(new Object(),new int[]{32000,32000,32000,32000},4)) { }
   System.out.println("PASS older Angelica without fluid context"); return;
  }
  com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuildContext batch=new com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuildContext();
  int[] ids=new int[]{32000,32000,32000,32000,32000,32000,32000,32000,-1};
  try(ContainedWaterShaderCompat.FluidScope f=ContainedWaterShaderCompat.fluidBatch(batch,ids,8)) {
   check(batch.context.renderType==1);
   try(ContainedWaterShaderCompat.FluidScope nested=ContainedWaterShaderCompat.fluidBatch(batch,ids,4)) { check(batch.context.renderType==1); }
   check(batch.context.renderType==1);
  }
  check(batch.context.renderType==42);
  try {
   try(ContainedWaterShaderCompat.FluidScope f=ContainedWaterShaderCompat.fluidBatch(batch,ids,8)) {
    check(batch.context.renderType==1);throw new IllegalStateException();
   }
  } catch(IllegalStateException expected) { }
  check(batch.context.renderType==42);
  for(int count:new int[]{0,3,12}) {
   try(ContainedWaterShaderCompat.FluidScope f=ContainedWaterShaderCompat.fluidBatch(batch,ids,count)) { check(batch.context.renderType==42); }
  }
  ids[4]=10017;
  try(ContainedWaterShaderCompat.FluidScope f=ContainedWaterShaderCompat.fluidBatch(batch,ids,8)) { check(batch.context.renderType==42); }
  try(ContainedWaterShaderCompat.FluidScope f=ContainedWaterShaderCompat.fluidBatch(batch,null,4)) { check(batch.context.renderType==42); }
  ids[4]=32000;
  mapping(33000);int[] signed=new int[]{(short)33000,(short)33000,(short)33000,(short)33000};
  try(ContainedWaterShaderCompat.FluidScope f=ContainedWaterShaderCompat.fluidBatch(batch,signed,4)) { check(batch.context.renderType==1); }
  check(batch.context.renderType==42);
  mapping(32000);
  try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(tess)) {
   check(tess.id==32000); mapping(1234);
   try(ContainedWaterShaderCompat.Scope nested=ContainedWaterShaderCompat.water(tess)) { check(tess.id==1234); }
   check(tess.id==32000);
  }
  check(tess.id==-1);
  try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(tess)) { check(tess.id==1234); }
  check(tess.id==-1);
  mapping(33000);
  try {
   try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(tess)) {
    check(tess.id==(short)33000); throw new IllegalStateException();
   }
  } catch(IllegalStateException expected) { }
  check(tess.id==-1);
  BlockRenderingSettings.INSTANCE.ids=null;
  try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(tess)) { check(tess.id==-1); }
  mapping(32000); BlockRenderingSettings.INSTANCE.ids.clear();
  try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(tess)) { check(tess.id==-1); }
  Map<Integer,Integer> fallback=new HashMap<>(); fallback.put(0,32123);
  BlockRenderingSettings.INSTANCE.ids.put(Blocks.flowing_water,fallback);
  try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(tess)) { check(tess.id==32123); }
  check(tess.id==-1);
  BlockRenderingSettings.INSTANCE.ids.put(Blocks.water,new HashMap<Integer,Integer>());
  try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(tess)) { check(tess.id==32123); }
  check(tess.id==-1);
  int[] flowingIds=new int[]{32123,32123,32123,32123};
  try(ContainedWaterShaderCompat.FluidScope f=ContainedWaterShaderCompat.fluidBatch(batch,flowingIds,4)) { check(batch.context.renderType==1); }
  check(batch.context.renderType==42);
  try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(new Object())) { }
  try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(null)) { }
  System.out.println("PASS scoped IDs, complete water batches, mixed-batch exclusion, nested/exception cleanup, unsigned IDs, shader reload, optional API");
 }
}''')
        cp = f'{common}:{api}'
        subprocess.run([str(JDK/'javac'), '-cp', cp, '-d', str(test), harness], check=True)
        subprocess.run([str(JDK/'java'), '-cp', f'{test}:{cp}', 'Harness'], check=True)
        subprocess.run([str(JDK/'java'), '-cp', f'{test}:{common}', 'Harness', 'absent'], check=True)
        subprocess.run([str(JDK/'java'), '-cp', f'{test}:{common}:{old_api}', 'Harness', 'old'], check=True)

if __name__ == '__main__': main()
