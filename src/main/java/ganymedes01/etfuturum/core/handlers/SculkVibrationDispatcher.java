package ganymedes01.etfuturum.core.handlers;

import ganymedes01.etfuturum.tileentities.TileEntitySculkRuntime;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Pass 38 bounded 1.7.10 bridge for the subset of modern GameEvent/VibrationSystem used by Sculk.
 * Listeners register by chunk; dispatch never scans every loaded block and never loads chunks.
 */
public final class SculkVibrationDispatcher {
    public enum Event {
        STEP(1), SWIM(1), FLAP(1),
        PROJECTILE_LAND(2), HIT_GROUND(2), SPLASH(2),
        ITEM_INTERACT_FINISH(3), PROJECTILE_SHOOT(3), INSTRUMENT_PLAY(3),
        ENTITY_ACTION(4), ELYTRA_GLIDE(4), UNEQUIP(4),
        ENTITY_DISMOUNT(5), EQUIP(5),
        ENTITY_MOUNT(6), ENTITY_INTERACT(6), SHEAR(6),
        ENTITY_DAMAGE(7),
        DRINK(8), EAT(8),
        CONTAINER_CLOSE(9), BLOCK_CLOSE(9), BLOCK_DEACTIVATE(9), BLOCK_DETACH(9),
        CONTAINER_OPEN(10), BLOCK_OPEN(10), BLOCK_ACTIVATE(10), BLOCK_ATTACH(10), PRIME_FUSE(10), NOTE_BLOCK_PLAY(10),
        BLOCK_CHANGE(11),
        BLOCK_DESTROY(12), FLUID_PICKUP(12),
        BLOCK_PLACE(13), FLUID_PLACE(13),
        ENTITY_PLACE(14), LIGHTNING_STRIKE(14), TELEPORT(14),
        ENTITY_DIE(15), EXPLODE(15),
        // Modern shriekers listen to the sensor's tendrils-clicking game event, not raw player noise.
        SCULK_SENSOR_TENDRILS_CLICKING(0);

        public final int frequency;
        Event(int frequency) { this.frequency = frequency; }
    }

    public static final class Vibration {
        public final Event event;
        public final double x, y, z;
        public final WeakReference<net.minecraft.entity.Entity> source;
        public final UUID responsiblePlayer;
        public Vibration(Event event, double x, double y, double z, net.minecraft.entity.Entity source, UUID responsiblePlayer) {
            this.event = event; this.x=x; this.y=y; this.z=z;
            this.source = new WeakReference<net.minecraft.entity.Entity>(source);
            this.responsiblePlayer = responsiblePlayer;
        }
    }

    private static final Map<World, Map<Long, Set<TileEntitySculkRuntime>>> LISTENERS =
            Collections.synchronizedMap(new WeakHashMap<World, Map<Long, Set<TileEntitySculkRuntime>>>());

    private SculkVibrationDispatcher() {}

    private static long key(int cx, int cz) { return (((long)cx) << 32) ^ (cz & 0xffffffffL); }

    public static void register(TileEntitySculkRuntime te) {
        if (te == null || te.getWorldObj() == null || te.getWorldObj().isRemote) return;
        World world = te.getWorldObj();
        synchronized (LISTENERS) {
            Map<Long, Set<TileEntitySculkRuntime>> byChunk = LISTENERS.get(world);
            if (byChunk == null) {
                byChunk = new HashMap<Long, Set<TileEntitySculkRuntime>>();
                LISTENERS.put(world, byChunk);
            }
            long k = key(te.xCoord >> 4, te.zCoord >> 4);
            Set<TileEntitySculkRuntime> set = byChunk.get(k);
            if (set == null) { set = Collections.newSetFromMap(new WeakHashMap<TileEntitySculkRuntime, Boolean>()); byChunk.put(k, set); }
            set.add(te);
        }
    }

    public static void unregister(TileEntitySculkRuntime te) {
        if (te == null || te.getWorldObj() == null) return;
        synchronized (LISTENERS) {
            Map<Long, Set<TileEntitySculkRuntime>> byChunk = LISTENERS.get(te.getWorldObj());
            if (byChunk == null) return;
            Set<TileEntitySculkRuntime> set = byChunk.get(key(te.xCoord >> 4, te.zCoord >> 4));
            if (set != null) set.remove(te);
        }
    }

    public static void clearWorld(World world) { synchronized (LISTENERS) { LISTENERS.remove(world); } }

    public static void post(World world, double x, double y, double z, Event event, net.minecraft.entity.Entity source) {
        post(world, x, y, z, event, source, source instanceof net.minecraft.entity.player.EntityPlayer ? source.getUniqueID() : null);
    }

    public static void post(World world, double x, double y, double z, Event event, net.minecraft.entity.Entity source, UUID responsiblePlayer) {
        if (world == null || world.isRemote || event == null) return;
        Vibration vibration = new Vibration(event, x, y, z, source, responsiblePlayer);
        // Calibrated sensors are the widest vanilla Sculk listener at 16 blocks.
        int minCx = ((int)Math.floor(x) - 16) >> 4, maxCx = ((int)Math.floor(x) + 16) >> 4;
        int minCz = ((int)Math.floor(z) - 16) >> 4, maxCz = ((int)Math.floor(z) + 16) >> 4;
        List<TileEntitySculkRuntime> targets = new ArrayList<TileEntitySculkRuntime>();
        synchronized (LISTENERS) {
            Map<Long, Set<TileEntitySculkRuntime>> byChunk = LISTENERS.get(world);
            if (byChunk == null) return;
            for (int cx=minCx; cx<=maxCx; cx++) for (int cz=minCz; cz<=maxCz; cz++) {
                Set<TileEntitySculkRuntime> set = byChunk.get(key(cx,cz));
                if (set != null) targets.addAll(set);
            }
        }
        for (TileEntitySculkRuntime te : targets) {
            if (te != null && !te.isInvalid() && te.getWorldObj() == world) te.offer(vibration);
        }
    }

    /** Modern VibrationSystem occlusion: all six tiny-offset rays must be blocked by damping blocks. */
    public static boolean isOccludedByDampening(World world, double sx, double sy, double sz, double tx, double ty, double tz) {
        if (world == null) return false;
        final double eps = 0.00001D;
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            double ox = sx + d.offsetX * eps, oy = sy + d.offsetY * eps, oz = sz + d.offsetZ * eps;
            if (!rayHitsDampening(world, ox, oy, oz, tx, ty, tz)) return false;
        }
        return true;
    }

    private static boolean rayHitsDampening(World world, double sx, double sy, double sz, double tx, double ty, double tz) {
        double dx=tx-sx, dy=ty-sy, dz=tz-sz;
        int steps=Math.max(1,(int)Math.ceil(Math.sqrt(dx*dx+dy*dy+dz*dz)*8.0D));
        for(int i=1;i<steps;i++) {
            double f=i/(double)steps;
            int x=(int)Math.floor(sx+dx*f), y=(int)Math.floor(sy+dy*f), z=(int)Math.floor(sz+dz*f);
            if (world.getChunkProvider() == null || !world.getChunkProvider().chunkExists(x >> 4, z >> 4)) continue; // never force a neighbour chunk
            Block b=world.getBlock(x,y,z);
            if (isVibrationDampeningBlock(b)) return true;
        }
        return false;
    }

    public static boolean isVibrationDampeningBlock(Block block) {
        if (block == null) return false;
        // 1.21 vibration-dampening includes wool blocks and wool carpets. In 1.7 both
        // are single blocks with colour metadata, so metadata is intentionally irrelevant here.
        if (block == Blocks.wool || block == Blocks.carpet) return true;
        String n = Block.blockRegistry.getNameForObject(block);
        return n != null && (n.endsWith(":wool") || n.contains("wool_") || n.endsWith(":carpet") || n.contains("wool_carpet"));
    }

    public static int redstoneStrength(double distance, int radius) {
        return Math.max(1, 15 - (int)Math.floor(15.0D * distance / (double)radius));
    }
}
