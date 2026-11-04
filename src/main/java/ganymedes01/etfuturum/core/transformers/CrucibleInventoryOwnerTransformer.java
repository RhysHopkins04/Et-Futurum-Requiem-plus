package ganymedes01.etfuturum.core.transformers;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Minecraft classes use the FML chain even when Bukkit classes bypass it. No Bukkit linkage. */
public final class CrucibleInventoryOwnerTransformer implements IClassTransformer {
    private static final String HELPER="ganymedes01/etfuturum/core/utils/CrucibleInventoryHeightContext";
    private static final String WORLD="org/bukkit/craftbukkit/v1_7_R4/CraftWorld";
    private static final String CHUNK="org/bukkit/craftbukkit/v1_7_R4/CraftChunk";
    private static final String BLOCK="org/bukkit/craftbukkit/v1_7_R4/block/CraftBlock";
    private static final String BLOCK_DESC="(III)Lorg/bukkit/block/Block;";
    private static final String BRIDGE_DESC="(L"+WORLD+";III)Lorg/bukkit/block/Block;";
    public byte[] transform(String name,String transformedName,byte[] bytes) {
        if(bytes==null || !("net.minecraft.tileentity.TileEntity".equals(name)
                || "net.minecraft.tileentity.TileEntity".equals(transformedName)))return bytes;
        ClassNode node=new ClassNode(Opcodes.ASM5);new ClassReader(bytes).accept(node,0);
        MethodNode original=null;
        for(MethodNode m:node.methods) {
            if(m.name.equals("etfu$originalInventoryOwner"))return bytes;
            if(m.name.equals("getOwner")&&m.desc.equals("()Lorg/bukkit/inventory/InventoryHolder;"))original=m;
        }
        if(original==null)return bytes; // plain Forge has no Bukkit owner method
        MethodInsnNode lookup = null;
        for (AbstractInsnNode insn : original.instructions.toArray()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.getOpcode() == Opcodes.INVOKEVIRTUAL && call.owner.equals(WORLD)
                        && call.name.equals("getBlockAt") && call.desc.equals(BLOCK_DESC)) {
                    if (lookup != null) return bytes;
                    lookup = call;
                }
            }
        }
        if (lookup == null) return bytes; // unknown server layout: leave it untouched
        original.instructions.set(lookup, new MethodInsnNode(Opcodes.INVOKESTATIC, node.name,
                "etfu$inventoryBlockAt", BRIDGE_DESC, false));
        node.methods.add(blockLookup());
        int access=original.access;String desc=original.desc;
        original.name="etfu$originalInventoryOwner";original.access=Opcodes.ACC_PRIVATE;
        MethodNode wrapper=new MethodNode(access,"getOwner",desc,null,null);
        LabelNode start=new LabelNode(),end=new LabelNode(),error=new LabelNode();
        InsnList code=wrapper.instructions;
        code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HELPER,"enter","(Ljava/lang/Object;)Ljava/lang/Object;",false));code.add(new VarInsnNode(Opcodes.ASTORE,1));
        code.add(start);code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,node.name,original.name,desc,false));code.add(new VarInsnNode(Opcodes.ASTORE,2));code.add(end);
        code.add(new VarInsnNode(Opcodes.ALOAD,1));code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HELPER,"exit","(Ljava/lang/Object;)V",false));code.add(new VarInsnNode(Opcodes.ALOAD,2));code.add(new InsnNode(Opcodes.ARETURN));
        code.add(error);code.add(new FrameNode(Opcodes.F_FULL,2,new Object[]{node.name,"java/lang/Object"},1,new Object[]{"java/lang/Throwable"}));code.add(new VarInsnNode(Opcodes.ASTORE,2));
        code.add(new VarInsnNode(Opcodes.ALOAD,1));code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HELPER,"exit","(Ljava/lang/Object;)V",false));code.add(new VarInsnNode(Opcodes.ALOAD,2));code.add(new InsnNode(Opcodes.ATHROW));
        wrapper.tryCatchBlocks.add(new TryCatchBlockNode(start,end,error,null));wrapper.maxStack=2;wrapper.maxLocals=3;node.methods.add(wrapper);
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);
        LogManager.getLogger("EFR-CrucibleHeight").info("Installed full-coordinate inventory-owner lookup with exception-safe cleanup");
        return writer.toByteArray();
    }

    /** Construct the original CraftBlock at full Y before its state captures the coordinates. */
    private static MethodNode blockLookup() {
        MethodNode method = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "etfu$inventoryBlockAt", BRIDGE_DESC, null, null);
        InsnList code = method.instructions;
        LabelNode legacy = new LabelNode();
        code.add(new VarInsnNode(Opcodes.ILOAD, 1));
        code.add(new VarInsnNode(Opcodes.ILOAD, 2));
        code.add(new VarInsnNode(Opcodes.ILOAD, 3));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "isOwnerPosition", "(III)Z", false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, legacy));
        code.add(new TypeInsnNode(Opcodes.NEW, BLOCK));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ILOAD, 1));
        code.add(new InsnNode(Opcodes.ICONST_4));
        code.add(new InsnNode(Opcodes.ISHR));
        code.add(new VarInsnNode(Opcodes.ILOAD, 3));
        code.add(new InsnNode(Opcodes.ICONST_4));
        code.add(new InsnNode(Opcodes.ISHR));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, WORLD, "getChunkAt", "(II)Lorg/bukkit/Chunk;", false));
        code.add(new TypeInsnNode(Opcodes.CHECKCAST, CHUNK));
        code.add(new VarInsnNode(Opcodes.ILOAD, 1));
        code.add(new VarInsnNode(Opcodes.ILOAD, 2));
        code.add(new VarInsnNode(Opcodes.ILOAD, 3));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, BLOCK, "<init>", "(L"+CHUNK+";III)V", false));
        code.add(new InsnNode(Opcodes.ARETURN));
        code.add(legacy);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ILOAD, 1));
        code.add(new VarInsnNode(Opcodes.ILOAD, 2));
        code.add(new VarInsnNode(Opcodes.ILOAD, 3));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, WORLD, "getBlockAt", BLOCK_DESC, false));
        code.add(new InsnNode(Opcodes.ARETURN));
        return method;
    }
}
