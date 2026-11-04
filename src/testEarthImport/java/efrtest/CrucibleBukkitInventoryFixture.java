package efrtest;

import ganymedes01.etfuturum.core.utils.CrucibleInventoryHeightContext;
import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

/** Executes unchanged dev-280 Bukkit lookup/read/chest methods, remapping only dependencies. */
public final class CrucibleBukkitInventoryFixture {
    static final String PREFIX = "efrtest/CrucibleBukkitInventoryFixture$";
    static final String CRAFT = "org/bukkit/craftbukkit/v1_7_R4/";
    public interface State { }
    public interface Holder { Inventory getInventory(); }
    public interface Inventory { }
    public interface Block { State getState(); }
    public interface Chunk { Block getBlock(int x, int y, int z); }
    public static class PlainState implements State { }
    public static class NativeInventory { }
    public static class NativeChest extends NativeInventory { }
    public static class CraftInventory implements Inventory {
        public final NativeInventory nativeInventory;
        public CraftInventory(NativeInventory nativeInventory) { this.nativeInventory = nativeInventory; }
    }
    public static class DoubleInventory extends CraftInventory {
        public DoubleInventory(CraftInventory first, CraftInventory second) { super(first.nativeInventory); }
    }
    public enum Material {
        CHEST(54), TRAPPED_CHEST(146);
        final int id;
        Material(int id) { this.id = id; }
        public int getId() { return id; }
    }
    public static class NativeBlock {
        final int id;
        NativeBlock(int id) { this.id = id; }
    }
    public static class Magic {
        public static int getId(NativeBlock block) { return block.id; }
    }
    public static class NativeHandle {
        final BukkitWorld world;
        NativeHandle(BukkitWorld world) { this.world = world; }
        public int getTypeId(int x, int y, int z) {
            if (x != 9795 || z != -2181) return 1;
            return y == world.ownerY ? world.type : 1;
        }
        public NativeChest o(int x, int y, int z) {
            return getTypeId(x, y, z) == world.type ? world.chest : null;
        }
    }
    public static class ChunkHandle {
        final BukkitWorld world;
        ChunkHandle(BukkitWorld world) { this.world = world; }
        public NativeBlock a(int x, int y, int z) {
            int fullY = CrucibleInventoryHeightContext.blockY(world.nativeChunk, x, y, z);
            return new NativeBlock(world.world.getTypeId(9792 + x, fullY, -2192 + z));
        }
    }
    public static class BukkitWorld {
        public NativeHandle world;
        public net.minecraft.world.chunk.Chunk nativeChunk;
        public CraftChunk chunk;
        public NativeChest chest = new NativeChest();
        public int ownerY, type;
        public boolean fail;
        public Block getBlockAt(int x, int y, int z) { throw new AssertionError("real method not installed"); }
        public int getBlockTypeIdAt(int x, int y, int z) { throw new AssertionError("real method not installed"); }
        public Chunk getChunkAt(int x, int z) {
            if (fail) throw new IllegalArgumentException("original failure");
            if (x != 612 || z != -137) throw new AssertionError("incorrect chunk coordinates");
            return chunk;
        }
        public NativeHandle getHandle() { return world; }
    }
    public static class CraftChunk implements Chunk {
        public BukkitWorld world;
        public int getX() { return 612; }
        public int getZ() { return -137; }
        public ChunkHandle getHandle() { return new ChunkHandle(world); }
        public Block getBlock(int x, int y, int z) { throw new AssertionError("real method not installed"); }
    }
    public static class CraftBlock implements Block {
        public final CraftChunk chunk;
        public final int x, y, z;
        public CraftBlock(CraftChunk chunk, int x, int y, int z) {
            this.chunk = chunk; this.x = x; this.y = y; this.z = z;
        }
        public int getTypeId() { throw new AssertionError("real method not installed"); }
        public State getState() {
            // Block-state selection is a fixture; the coordinates and all height masks above it
            // execute original Bukkit methods. Chest inventory validation below is also original.
            int id = getTypeId();
            if (id == 54 || id == 146) {
                try { return (State) chestClass.getConstructor(CraftBlock.class).newInstance(this); }
                catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            }
            if (id == 154) return new HopperState(this);
            return new PlainState();
        }
    }
    public static class HopperState implements State, Holder {
        final CraftBlock block;
        HopperState(CraftBlock block) { this.block = block; }
        public Inventory getInventory() {
            if (block.y != block.chunk.world.ownerY) throw new IllegalStateException("hopper owner captured wrapped Y=" + block.y);
            return new CraftInventory(new NativeInventory());
        }
    }
    public static class ChestState implements State, Holder {
        public final BukkitWorld world;
        public final NativeChest chest;
        final CraftBlock block;
        public ChestState(CraftBlock block) {
            this.block = block; this.world = block.chunk.world;
            this.chest = world.world.o(block.x, block.y, block.z);
        }
        public int getX() { return block.x; }
        public int getY() { return block.y; }
        public int getZ() { return block.z; }
        public Inventory getInventory() { throw new AssertionError("real method not installed"); }
    }
    static final class Definitions extends ClassLoader {
        Class<?> define(byte[] bytes) { return defineClass(null, bytes, 0, bytes.length); }
    }
    static Class<?> worldClass, chunkClass, chestClass;
    static final String GENERATED = "efrtest/GeneratedCrucible";
    static Map<String, String> mappings() {
        Map<String, String> map = new HashMap<>();
        map.put(CRAFT + "CraftWorld", PREFIX + "BukkitWorld");
        map.put(CRAFT + "CraftChunk", PREFIX + "CraftChunk");
        map.put(CRAFT + "block/CraftBlock", GENERATED + "Block");
        map.put(CRAFT + "block/CraftChest", PREFIX + "ChestState");
        map.put(CRAFT + "inventory/CraftInventory", PREFIX + "CraftInventory");
        map.put(CRAFT + "inventory/CraftInventoryDoubleChest", PREFIX + "DoubleInventory");
        map.put(CRAFT + "util/CraftMagicNumbers", PREFIX + "Magic");
        for (String name : new String[]{"Block", "BlockState"}) map.put("org/bukkit/block/" + name, PREFIX + (name.equals("Block") ? "Block" : "State"));
        map.put("org/bukkit/Chunk", PREFIX + "Chunk");
        map.put("org/bukkit/Material", PREFIX + "Material");
        map.put("org/bukkit/inventory/Inventory", PREFIX + "Inventory");
        map.put("org/bukkit/inventory/InventoryHolder", PREFIX + "Holder");
        map.put("mt", PREFIX + "NativeHandle"); map.put("aow", PREFIX + "NativeChest");
        map.put("rb", PREFIX + "NativeInventory"); map.put("apx", PREFIX + "ChunkHandle");
        map.put("aji", PREFIX + "NativeBlock");
        return map;
    }
    static void initialize(String jar) throws Exception {
        Definitions definitions = new Definitions();
        try (ZipFile zip = new ZipFile(jar)) {
            ClassNode block = fixture(zip, CRAFT + "block/CraftBlock", GENERATED + "Block", PREFIX + "CraftBlock", "getTypeId");
            constructor(block, "(L" + PREFIX + "CraftChunk;III)V", 4);
            definitions.define(write(block));
            ClassNode chunk = fixture(zip, CRAFT + "CraftChunk", GENERATED + "Chunk", PREFIX + "CraftChunk", "getBlock");
            constructor(chunk, "()V", 0); chunkClass = definitions.define(write(chunk));
            ClassNode world = fixture(zip, CRAFT + "CraftWorld", GENERATED + "World", PREFIX + "BukkitWorld", "getBlockAt", "getBlockTypeIdAt");
            constructor(world, "()V", 0); worldClass = definitions.define(write(world));
            ClassNode chest = fixture(zip, CRAFT + "block/CraftChest", GENERATED + "Chest", PREFIX + "ChestState", "getInventory");
            constructor(chest, "(L" + PREFIX + "CraftBlock;)V", 1); chestClass = definitions.define(write(chest));
        }
    }
    static ClassNode fixture(ZipFile zip, String source, String name, String parent, String... methods) throws IOException {
        ClassNode original = new ClassNode(Opcodes.ASM5);
        try (InputStream in = zip.getInputStream(zip.getEntry(source + ".class"))) { new ClassReader(in).accept(original, ClassReader.EXPAND_FRAMES); }
        ClassNode generated = new ClassNode(Opcodes.ASM5);
        generated.version = original.version; generated.access = Opcodes.ACC_PUBLIC;
        generated.name = name; generated.superName = parent;
        for (MethodNode method : original.methods) {
            if (!Arrays.asList(methods).contains(method.name)) continue;
            if ((method.name.equals("getBlockAt") || method.name.equals("getBlockTypeIdAt")) && !method.desc.startsWith("(III)")) continue;
            MethodNode copied = new MethodNode(method.access, method.name, new SimpleRemapper(mappings()).mapMethodDesc(method.desc), null, null);
            method.accept(new RemappingMethodAdapter(method.access, method.desc, copied, new SimpleRemapper(mappings())));
            generated.methods.add(copied);
        }
        return generated;
    }
    static void constructor(ClassNode node, String desc, int arguments) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", desc, null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        for (int i = 1; i <= arguments; i++) method.instructions.add(new VarInsnNode(i == 1 ? Opcodes.ALOAD : Opcodes.ILOAD, i));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, "<init>", desc, false));
        method.instructions.add(new InsnNode(Opcodes.RETURN)); node.methods.add(method);
    }
    static byte[] write(ClassNode node) { ClassWriter out = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(out); return out.toByteArray(); }
    static BukkitWorld world(net.minecraft.world.chunk.Chunk nativeChunk, int y, int type) throws Exception {
        BukkitWorld world = (BukkitWorld) worldClass.newInstance();
        world.nativeChunk = nativeChunk; world.ownerY = y; world.type = type;
        world.world = new NativeHandle(world); world.chunk = (CraftChunk) chunkClass.newInstance(); world.chunk.world = world;
        return world;
    }
}
