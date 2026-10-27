package ganymedes01.etfuturum.core.transformers;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** Patches only the exact Bukkit bridge branch; Minecraft argument names may be obfuscated or mapped. */
public final class CrucibleInteractionHeightTransformer implements IClassTransformer {
    private static final String TARGET = "org.bukkit.craftbukkit.v1_7_R4.event.CraftEventFactory";
    private static final String HELPER = "ganymedes01/etfuturum/core/utils/CrucibleHeightCompat";

    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(TARGET.equals(name) || TARGET.equals(transformedName))) return bytes;
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(node, 0);
        MethodNode selected = null; AbstractInsnNode bound = null; int candidates = 0;
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) == 0) continue;
            Type[] args = Type.getArgumentTypes(method.desc);
            if (args.length != 7 || args[0].getSort() != Type.OBJECT || args[6].getSort() != Type.OBJECT
                    || !args[1].getDescriptor().equals("Lorg/bukkit/event/block/Action;")
                    || !Type.getReturnType(method.desc).getDescriptor().equals("Lorg/bukkit/event/player/PlayerInteractEvent;")) continue;
            boolean ints = true;
            for (int i = 2; i <= 5; i++) ints &= args[i].getSort() == Type.INT;
            if (!ints) continue;
            // Idempotence if another loader asks to transform the already-patched class.
            for (AbstractInsnNode insn : method.instructions.toArray())
                if (insn instanceof MethodInsnNode && HELPER.equals(((MethodInsnNode)insn).owner)) return bytes;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (!(insn instanceof IntInsnNode) || ((IntInsnNode)insn).operand != 255) continue;
                AbstractInsnNode previous = executable(insn, false), next = executable(insn, true);
                if (previous instanceof VarInsnNode && previous.getOpcode() == Opcodes.ILOAD
                        && ((VarInsnNode)previous).var == 3 && next != null && next.getOpcode() == Opcodes.IF_ICMPLE) {
                    selected = method; bound = insn; candidates++;
                }
            }
        }
        if (candidates != 1) {
            LogManager.getLogger("EFR-CrucibleHeight").warn("Unrecognized Crucible interaction height branch ({} matches); leaving class unchanged", candidates);
            return bytes;
        }
        InsnList replacement = new InsnList();
        replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
        replacement.add(new VarInsnNode(Opcodes.ALOAD, 1));
        replacement.add(new IntInsnNode(Opcodes.SIPUSH, 255));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "maximumInteractionY", "(Ljava/lang/Object;Ljava/lang/Object;I)I", false));
        selected.instructions.insertBefore(bound, replacement); selected.instructions.remove(bound);
        // Net stack shape at every existing frame is unchanged; only the temporary maximum grows.
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer);
        LogManager.getLogger("EFR-CrucibleHeight").info("Patched exact Crucible block-interaction height branch; plugin event dispatch remains intact");
        return writer.toByteArray();
    }
    private static AbstractInsnNode executable(AbstractInsnNode at, boolean forward) {
        AbstractInsnNode next = forward ? at.getNext() : at.getPrevious();
        while (next != null && next.getOpcode() < 0) next = forward ? next.getNext() : next.getPrevious();
        return next;
    }
}
