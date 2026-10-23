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
        subprocess.run([str(JDK/'javac'), '-d', str(api), *fixtures], check=True)
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
  if(args.length>0) {
   try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(new Object())) { }
   System.out.println("PASS without Angelica"); return;
  }
  Tess tess=new Tess(); mapping(32000);
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
  try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(new Object())) { }
  try(ContainedWaterShaderCompat.Scope s=ContainedWaterShaderCompat.water(null)) { }
  System.out.println("PASS scoped IDs, shader reload, exception cleanup, missing maps, flowing-water fallback, optional API");
 }
}''')
        cp = f'{common}:{api}'
        subprocess.run([str(JDK/'javac'), '-cp', cp, '-d', str(test), harness], check=True)
        subprocess.run([str(JDK/'java'), '-cp', f'{test}:{cp}', 'Harness'], check=True)
        subprocess.run([str(JDK/'java'), '-cp', f'{test}:{common}', 'Harness', 'absent'], check=True)

if __name__ == '__main__': main()
