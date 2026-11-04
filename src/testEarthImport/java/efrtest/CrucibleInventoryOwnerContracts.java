package efrtest;

import efrtest.CrucibleBukkitInventoryFixture.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Native owner lookup plus unchanged Bukkit World/Chunk/Block readers and chest getInventory. */
public final class CrucibleInventoryOwnerContracts {
    static final String NATIVE="net.minecraft.tileentity.TileEntity", TYPE="ganymedes01.etfuturum.core.transformers.CrucibleInventoryOwnerTransformer";
    static final String WORLD="org/bukkit/craftbukkit/v1_7_R4/CraftWorld";
    static final String BLOCK_DESC="(III)Lorg/bukkit/block/Block;";
    public static class NativeWorld extends net.minecraft.world.World {
        public BukkitWorld world;
        NativeWorld(){super(null,"unused",(net.minecraft.world.WorldProvider)null,null,null);}
        protected net.minecraft.world.chunk.IChunkProvider createChunkProvider(){throw new AssertionError("Chunk load");}
        protected int func_152379_p(){return 0;}
        public net.minecraft.entity.Entity getEntityByID(int id){return null;}
        public BukkitWorld getWorld(){return world;}
    }
    public static void main(String[] args)throws Exception {
        File home=new File(System.getProperty("java.io.tmpdir"),"efr-inventory-owner-fixture");home.mkdirs();net.minecraft.launchwrapper.Launch.minecraftHome=home;
        java.lang.reflect.Field homeField=cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");homeField.setAccessible(true);homeField.set(null,home);
        ganymedes01.etfuturum.configuration.configs.ConfigWorld.extendedWorldHeight=true;
        ganymedes01.etfuturum.configuration.configs.ConfigWorld.extendedWorldHeightBlocks=1024;
        CrucibleBukkitInventoryFixture.initialize(args[2]);
        byte[] original=Files.readAllBytes(Paths.get(args[0]));byte[] release;
        try(ZipFile z=new ZipFile(args[1]);InputStream in=z.getInputStream(z.getEntry(TYPE.replace('.','/')+".class"))){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);release=out.toByteArray();}
        ClassLoader loader=new ClassLoader(CrucibleInventoryOwnerContracts.class.getClassLoader()) {
            protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.equals(TYPE))return super.loadClass(name,resolve);
                Class<?> c=findLoadedClass(name);if(c==null)c=defineClass(name,release,0,release.length);if(resolve)resolveClass(c);return c;
            }
        };
        IClassTransformer transformer=(IClassTransformer)Class.forName(TYPE,true,loader).newInstance();
        byte[] patched=transformer.transform("aor",NATIVE,original);
        check(!Arrays.equals(original,patched),"native target not patched");
        check(Arrays.equals(patched,transformer.transform("aor",NATIVE,patched)),"not idempotent");
        check(transformer.transform("other.Type","other.Type",original)==original,"unrelated class altered");
        ClassNode before=parse(original),after=parse(patched);
        check(after.methods.size()==before.methods.size()+2,"unexpected methods");
        MethodNode owner=null,wrapped=null;
        for(MethodNode m:before.methods)if(m.name.equals("getOwner"))owner=m;
        for(MethodNode m:after.methods) {
            if(m.name.equals("etfu$originalInventoryOwner"))wrapped=m;
            if(m.name.equals("getOwner")||m.name.equals("etfu$inventoryBlockAt"))new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,m);
        }
        check(owner!=null&&wrapped!=null,"missing owner path");
        restoreLookup(wrapped); // The single bridge replacement must be the only original-body change.
        org.objectweb.asm.util.Textifier a=new org.objectweb.asm.util.Textifier(),b=new org.objectweb.asm.util.Textifier();
        owner.accept(new org.objectweb.asm.util.TraceMethodVisitor(a));wrapped.accept(new org.objectweb.asm.util.TraceMethodVisitor(b));
        check(a.text.toString().equals(b.text.toString()),"unrelated original owner instructions changed");
        ClassWriter oldScope=new ClassWriter(0);after.accept(oldScope);
        execute(original,385,54,0);
        execute(oldScope.toByteArray(),385,54,1); // X7: holder created at wrapped Y, then real CraftChest throws.
        execute(patched,384,154,2);
        execute(patched,385,54,2);
        execute(patched,385,146,2);
        for(int y:new int[]{128,255,256,511,769,1023})execute(patched,y,54,2);
        before.methods.remove(owner);ClassWriter plain=new ClassWriter(0);before.accept(plain);byte[] forge=plain.toByteArray();
        check(transformer.transform("aor",NATIVE,forge)==forge,"plain Forge requires Bukkit");
        System.out.println("PASS dev-280 Bukkit path: reproduces X7 CraftChest failure at Y129; full-coordinate hopper/chest/trapped-chest holders and getInventory succeed after scope cleanup. Exceptions, Forge absence and idempotence pass.");
    }
    static void restoreLookup(MethodNode method) {
        for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode) {
            MethodInsnNode call=(MethodInsnNode)instruction;
            if(call.name.equals("etfu$inventoryBlockAt"))method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKEVIRTUAL,WORLD,"getBlockAt",BLOCK_DESC,false));
        }
    }
    static void execute(byte[] bytes,int y,int blockType,int mode)throws Exception {
        ClassNode node=parse(bytes);node.interfaces.clear();node.superName="net/minecraft/tileentity/TileEntityHopper";
        node.fields.removeIf(f->!Arrays.asList("b","c","d","e").contains(f.name));
        node.methods.removeIf(m->!m.name.equals("getOwner")&&!m.name.startsWith("etfu$"));
        CrucibleBukkitInventoryFixture.constructor(node,"()V",0);
        Map<String,String> map=CrucibleBukkitInventoryFixture.mappings();
        map.put(node.name,"efrtest/GeneratedNativeOwner");map.put("ahb","efrtest/CrucibleInventoryOwnerContracts$NativeWorld");
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(new RemappingClassAdapter(writer,new SimpleRemapper(map)));
        // Use fixture loader as parent because the bridge constructs its generated real-reader block.
        ClassLoader generatedBlock=CrucibleBukkitInventoryFixture.worldClass.getClassLoader();
        ClassLoader loader=new ClassLoader(generatedBlock){Class<?> define(){return defineClass(null,writer.toByteArray(),0,writer.toByteArray().length);}};
        java.lang.reflect.Method define=loader.getClass().getDeclaredMethod("define");define.setAccessible(true);
        Class<?> type=(Class<?>)define.invoke(loader);Object tile=type.newInstance();
        java.lang.reflect.Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);sun.misc.Unsafe unsafe=(sun.misc.Unsafe)uf.get(null);
        NativeWorld world=(NativeWorld)unsafe.allocateInstance(NativeWorld.class);
        java.lang.reflect.Field provider=net.minecraft.world.World.class.getDeclaredField("provider");provider.setAccessible(true);provider.set(world,new net.minecraft.world.WorldProviderSurface());
        net.minecraft.world.chunk.Chunk chunk=(net.minecraft.world.chunk.Chunk)unsafe.allocateInstance(net.minecraft.world.chunk.Chunk.class);
        for(String name:new String[]{"worldObj","xPosition","zPosition"}){java.lang.reflect.Field f=net.minecraft.world.chunk.Chunk.class.getDeclaredField(name);f.setAccessible(true);f.set(chunk,name.equals("worldObj")?world:name.equals("xPosition")?612:-137);}
        world.world=CrucibleBukkitInventoryFixture.world(chunk,y,blockType);
        net.minecraft.tileentity.TileEntity nativeTile=(net.minecraft.tileentity.TileEntity)tile;
        nativeTile.setWorldObj(world);nativeTile.xCoord=9795;nativeTile.yCoord=y;nativeTile.zCoord=-2181;
        type.getField("b").set(tile,world);type.getField("c").setInt(tile,9795);type.getField("d").setInt(tile,y);type.getField("e").setInt(tile,-2181);
        Holder holder=(Holder)type.getMethod("getOwner").invoke(tile);
        check((holder!=null)==(mode>0),"holder lookup result mismatch");
        check(!ganymedes01.etfuturum.core.utils.CrucibleInventoryHeightContext.isOwnerPosition(9795,y,-2181),"normal path leaked context");
        if(mode==1) {
            try{holder.getInventory();throw new AssertionError("X7 failure not reproduced");}
            catch(IllegalStateException expected){System.out.println("REPRODUCED X7: "+expected.getMessage());}
        }
        if(mode==2) {
            check(holder.getInventory()!=null,"inventory access failed after owner scope ended");
            if(holder instanceof ChestState)check(((ChestState)holder).getY()==y,"state captured wrapped Y");
            world.world.fail=true;
            try{type.getMethod("getOwner").invoke(tile);throw new AssertionError("failure swallowed");}
            catch(java.lang.reflect.InvocationTargetException expected){check(expected.getCause() instanceof IllegalArgumentException,"original failure changed");}
            check(!ganymedes01.etfuturum.core.utils.CrucibleInventoryHeightContext.isOwnerPosition(9795,y,-2181),"exception leaked context");
        }
    }
    static ClassNode parse(byte[] bytes){ClassNode node=new ClassNode(Opcodes.ASM5);new ClassReader(bytes).accept(node,ClassReader.EXPAND_FRAMES);return node;}
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
}
