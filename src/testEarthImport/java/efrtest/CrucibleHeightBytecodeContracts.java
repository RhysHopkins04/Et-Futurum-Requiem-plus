package efrtest;

import net.minecraft.launchwrapper.IClassTransformer;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.util.*;

/** Applies the production transformer to the exact supplied server class, never a stand-in target. */
public final class CrucibleHeightBytecodeContracts {
    private static final String TARGET = "org.bukkit.craftbukkit.v1_7_R4.event.CraftEventFactory";
    private static final String HELPER = "ganymedes01/etfuturum/core/utils/CrucibleHeightCompat";
    public static void main(String[] args) throws Exception {
        byte[] original;
        try (ZipFile jar = new ZipFile(args[0]); InputStream in = jar.getInputStream(jar.getEntry(TARGET.replace('.','/')+".class"))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer,0,count);
            original = out.toByteArray();
        }
        // Load the built, reobfuscated transformer rather than reusing development classes.
        final String transformerName = "ganymedes01.etfuturum.core.transformers.CrucibleInteractionHeightTransformer";
        final byte[] releaseClass;
        try (ZipFile release = new ZipFile(args[1]); InputStream in = release.getInputStream(release.getEntry(transformerName.replace('.','/')+".class"))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer,0,count);
            releaseClass = out.toByteArray();
        }
        ClassLoader releaseLoader = new ClassLoader(CrucibleHeightBytecodeContracts.class.getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(transformerName)) return super.loadClass(name,resolve);
                Class<?> type = findLoadedClass(name);
                if (type == null) type = defineClass(name,releaseClass,0,releaseClass.length);
                if (resolve) resolveClass(type);
                return type;
            }
        };
        IClassTransformer transformer = (IClassTransformer)Class.forName(transformerName,true,releaseLoader).newInstance();
        byte[] patched = transformer.transform(TARGET,TARGET,original);
        check(!Arrays.equals(original,patched),"actual server target was not patched");
        check(Arrays.equals(patched,transformer.transform(TARGET,TARGET,patched)),"not idempotent");
        check(transformer.transform("other.Class","other.Class",original)==original,"unrelated class changed");
        ClassNode before = parse(original), after = parse(patched);
        check(before.methods.size()==after.methods.size(),"method set changed");
        int modified = 0;
        for (int i=0;i<before.methods.size();i++) {
            MethodNode a=before.methods.get(i),b=after.methods.get(i);
            check(a.name.equals(b.name)&&a.desc.equals(b.desc),"method identity changed");
            if (dump(a).equals(dump(b))) continue;
            modified++;
            check(b.name.equals("callPlayerInteractEvent")&&Type.getArgumentTypes(b.desc).length==7,"wrong overload patched");
            int calls=0;
            for(AbstractInsnNode insn:b.instructions.toArray()) if(insn instanceof MethodInsnNode && HELPER.equals(((MethodInsnNode)insn).owner)) {
                calls++;
                check(((MethodInsnNode)insn).desc.equals("(Ljava/lang/Object;Ljava/lang/Object;I)I"),"helper descriptor");
                AbstractInsnNode bound=insn.getPrevious(),action=bound.getPrevious(),player=action.getPrevious();
                check(bound instanceof IntInsnNode&&((IntInsnNode)bound).operand==255,"original bound lost");
                check(action instanceof VarInsnNode&&action.getOpcode()==Opcodes.ALOAD&&((VarInsnNode)action).var==1,"action slot");
                check(player instanceof VarInsnNode&&player.getOpcode()==Opcodes.ALOAD&&((VarInsnNode)player).var==0,"player slot");
                new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,b);
                b.instructions.remove(insn);b.instructions.remove(action);b.instructions.remove(player);
            }
            check(calls==1,"wrong helper count");
            check(dump(a).equals(dump(b)),"event dispatch/protection or other instructions changed");
        }
        check(modified==1,"unexpected changed-method count");
        Map<String,String> names = new HashMap<>(); names.put("yz","net/minecraft/entity/player/EntityPlayer");
        names.put("add","net/minecraft/item/ItemStack");
        ClassWriter mappedWriter = new ClassWriter(0);
        new ClassReader(original).accept(new org.objectweb.asm.commons.RemappingClassAdapter(mappedWriter,
                new org.objectweb.asm.commons.SimpleRemapper(names)),ClassReader.EXPAND_FRAMES);
        byte[] mapped = mappedWriter.toByteArray();
        check(!Arrays.equals(mapped,transformer.transform(TARGET,TARGET,mapped)),"mapped MC argument names not supported");
        // Unknown target shapes must leave the class intact, never prevent Bukkit from loading.
        for(MethodNode m:before.methods) for(AbstractInsnNode in:m.instructions.toArray())
            if(in instanceof IntInsnNode && ((IntInsnNode)in).operand==255)((IntInsnNode)in).operand=254;
        ClassWriter w=new ClassWriter(0);before.accept(w);byte[] unknown=w.toByteArray();
        check(transformer.transform(TARGET,TARGET,unknown)==unknown,"unknown shape not preserved");
        System.out.println("CRUCIBLE_HEIGHT_BYTECODE_CONTRACTS PASSED: release transformer + actual jar, raw/mapped signatures, exact overload, preserved dispatch/protection, stack verification, idempotence, unknown-target fallback");
    }
    private static ClassNode parse(byte[] bytes){ClassNode n=new ClassNode(Opcodes.ASM5);new ClassReader(bytes).accept(n,0);return n;}
    private static String dump(MethodNode m){Textifier p=new Textifier();m.accept(new TraceMethodVisitor(p));StringWriter s=new StringWriter();p.print(new PrintWriter(s));return s.toString().replaceAll("(?m)^.*MAXSTACK.*\\n?","");}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
