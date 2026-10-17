package efrtest;

public final class TrimMixinContracts {
    private static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
    private static boolean injected(Class<?> type, String name) {
        for (java.lang.reflect.Method method : type.getDeclaredMethods()) if (method.getName().contains(name)) return true;
        return false;
    }
    public static void main(String[] args) throws Exception {
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Class<?> item=Class.forName("net.minecraft.client.renderer.entity.RenderItem");
        Class<?> living=Class.forName("net.minecraft.client.renderer.entity.RendererLivingEntity");
        check(injected(item,"etfuturum$trim"),"inventory trim hook did not apply");
        check(injected(living,"etfuturum$base") && injected(living,"etfuturum$leatherOverlay") && injected(living,"etfuturum$pass"),"worn/stand trim hooks did not apply");
        org.objectweb.asm.tree.ClassNode transformed=new org.objectweb.asm.tree.ClassNode();
        byte[] bytes=java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(".mixin.out/class/net/minecraft/client/renderer/entity/RenderItem.class"));
        new org.objectweb.asm.ClassReader(bytes).accept(transformed,0);
        int forgeHooks=0,wrapperHooks=0;
        for(Object entry:transformed.methods) {
            org.objectweb.asm.tree.MethodNode method=(org.objectweb.asm.tree.MethodNode)entry;
            if(!method.name.equals("renderItemIntoGUI"))continue;
            for(org.objectweb.asm.tree.AbstractInsnNode insn=method.instructions.getFirst();insn!=null;insn=insn.getNext())if(insn instanceof org.objectweb.asm.tree.MethodInsnNode) {
                org.objectweb.asm.tree.MethodInsnNode call=(org.objectweb.asm.tree.MethodInsnNode)insn;
                if(call.name.contains("etfuturum$trim")) { if(method.desc.endsWith("IIZ)V"))forgeHooks++;else wrapperHooks++; }
            }
        }
        check(forgeHooks==1 && wrapperHooks==0,"trim must hook Forge's boolean overload exactly once, not just the five-argument wrapper");
        System.out.println("TRIM_MIXINS PASSED: production icon and worn/stand hooks applied to real Forge renderer classes; no window or GL calls");
    }
}
