#!/usr/bin/env python3
"""Execute Angelica's real selectMaterial bytecode against controlled sprite states."""
import argparse
from pathlib import Path
import subprocess
import tempfile
import zipfile
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('angelica_jar',type=Path)
p.add_argument('--asm-jar',required=True,type=Path,action='append',help='ASM classpath jars; repeat for separate asm and asm-tree jars (ASM 9+ for Java 17 entries)')
a=p.parse_args()
asm_classpath=':'.join(str(jar) for jar in a.asm_jar)
jdk=Path('/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home/bin')
fixtures={
'org/embeddedt/embeddium/impl/render/chunk/terrain/material/Material.java': 'package org.embeddedt.embeddium.impl.render.chunk.terrain.material;public class Material {}',
'org/embeddedt/embeddium/impl/render/chunk/sprite/SpriteTransparencyLevel.java': 'package org.embeddedt.embeddium.impl.render.chunk.sprite;public enum SpriteTransparencyLevel {OPAQUE,CUTOUT,TRANSLUCENT}',
'com/gtnewhorizons/angelica/rendering/celeritas/SpriteExtension.java': '''package com.gtnewhorizons.angelica.rendering.celeritas;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
public interface SpriteExtension {SpriteTransparencyLevel celeritas$getTransparencyLevel();}''',
'net/minecraft/client/renderer/texture/TextureAtlasSprite.java': '''package net.minecraft.client.renderer.texture;
import com.gtnewhorizons.angelica.rendering.celeritas.SpriteExtension;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
public class TextureAtlasSprite implements SpriteExtension {
 public boolean animated;public SpriteTransparencyLevel alpha;
 public boolean func_130098_m(){return animated;}
 public SpriteTransparencyLevel celeritas$getTransparencyLevel(){return alpha;}
}''',
'com/gtnewhorizons/angelica/rendering/celeritas/AngelicaRenderPassConfiguration.java': '''package com.gtnewhorizons.angelica.rendering.celeritas;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
public class AngelicaRenderPassConfiguration {
 public static final Material SOLID_MATERIAL=new Material(),CUTOUT_MIPPED_MATERIAL=new Material(),TRANSLUCENT_MATERIAL=new Material();
}''',
'Harness.java': '''import java.util.zip.*;import java.lang.reflect.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import static com.gtnewhorizons.angelica.rendering.celeritas.AngelicaRenderPassConfiguration.*;
public class Harness {
 static void check(boolean v){if(!v)throw new AssertionError();}
 static class Definer extends ClassLoader {Class<?> define(byte[] b){return defineClass(null,b,0,b.length);}}
 public static void main(String[] args)throws Exception{
  ClassNode source=new ClassNode();try(ZipFile zip=new ZipFile(args[0])){
   new ClassReader(zip.getInputStream(zip.getEntry(args[1]))).accept(source,ClassReader.SKIP_FRAMES);
  }
  MethodNode method=null;for(Object o:source.methods)if(((MethodNode)o).name.equals("selectMaterial"))method=(MethodNode)o;
  check(method!=null);method.access=Opcodes.ACC_PUBLIC;
  ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
  cw.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,"NativeSelector",null,"java/lang/Object",null);
  MethodVisitor ctor=cw.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);
  ctor.visitCode();ctor.visitVarInsn(Opcodes.ALOAD,0);ctor.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);ctor.visitInsn(Opcodes.RETURN);ctor.visitMaxs(0,0);ctor.visitEnd();
  method.accept(cw);cw.visitEnd();
  Class<?> type=new Definer().define(cw.toByteArray());Object selector=type.newInstance();
  Method select=type.getMethod("selectMaterial",Material.class,TextureAtlasSprite.class,boolean.class,boolean.class);
  int cases=0;
  for(SpriteTransparencyLevel alpha:SpriteTransparencyLevel.values())for(boolean animated:new boolean[]{false,true})for(boolean optimize:new boolean[]{false,true}){
   TextureAtlasSprite sprite=new TextureAtlasSprite();sprite.alpha=alpha;sprite.animated=animated;
   Object normal=select.invoke(selector,TRANSLUCENT_MATERIAL,sprite,false,optimize);
   check(normal==((optimize&&!animated&&alpha!=SpriteTransparencyLevel.TRANSLUCENT)?CUTOUT_MIPPED_MATERIAL:TRANSLUCENT_MATERIAL));cases++;
   check(select.invoke(selector,TRANSLUCENT_MATERIAL,sprite,true,optimize)==TRANSLUCENT_MATERIAL);cases++;
  }
  TextureAtlasSprite opaque=new TextureAtlasSprite();opaque.alpha=SpriteTransparencyLevel.OPAQUE;
  check(select.invoke(selector,CUTOUT_MIPPED_MATERIAL,opaque,false,true)==SOLID_MATERIAL);cases++;
  System.out.println("PASS "+cases+" real Angelica material-selection cases in "+args[1]+": preserve water translucency; retain unrelated pass optimization");
 }
}'''
}
with tempfile.TemporaryDirectory(prefix='efr-native-water-pass-') as tmp:
 root=Path(tmp);sources=[]
 for n,s in fixtures.items():
  f=root/n;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(s);sources.append(str(f))
 subprocess.run([str(jdk/'javac'),'-cp',asm_classpath,'-d',str(root),*sources],check=True)
 with zipfile.ZipFile(a.angelica_jar) as jar:
  suffix='com/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuildContext.class'
  entries=[n for n in jar.namelist() if n==suffix or (n.startswith('META-INF/versions/') and n.endswith('/'+suffix))]
  if not entries: raise AssertionError('No native material-selection class found')
  for entry in entries:
   subprocess.run([str(jdk/'java'),'-cp',str(root)+':'+asm_classpath,'Harness',str(a.angelica_jar),entry],check=True)
