package ganymedes01.etfuturum.core.utils;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.network.WaterloggedChunkMessage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;

import java.util.BitSet;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Water beside the ordinary block state, owned by the loaded chunk. */
public final class NativeWaterlogging {
    public static final String NBT_KEY = "etfuturum_waterlogging_v1";
    public static final int MAX_Y = 512;
    public static final int MAX_BYTES = MAX_Y * 32;
    public static final NativeWaterlogging EVENTS = new NativeWaterlogging();

    // Published bitsets are immutable. Readers include asynchronous chunk meshers and IO threads.
    private static final Map<Chunk, BitSet> WATER = Collections.synchronizedMap(new WeakHashMap<>());
    private NativeWaterlogging() {}

    public static boolean canHold(Block block) {
        return block instanceof BlockStairs || block instanceof BlockLeaves
                || block instanceof BlockSlab && !block.isOpaqueCube();
    }

    public static boolean contains(IBlockAccess world, int x, int y, int z) {
        if (y < 0 || y >= MAX_Y) return false;
        if (world instanceof World) {
            Chunk chunk = loadedChunk((World) world, x >> 4, z >> 4);
            return chunk != null && contains(chunk, x, y, z) && canHold(chunk.getBlock(x & 15, y, z & 15));
        }
        return world instanceof WaterloggedAccess && ((WaterloggedAccess) world).etfu$isWaterlogged(x, y, z)
                && canHold(world.getBlock(x, y, z));
    }

    public static boolean contains(Chunk chunk, int x, int y, int z) {
        BitSet water = WATER.get(chunk);
        return water != null && y >= 0 && y < MAX_Y && water.get(index(x, y, z));
    }

    public static WaterloggedAccess snapshot(Chunk chunk) {
        BitSet water = WATER.get(chunk);
        return water == null ? null : (x, y, z) -> y >= 0 && y < MAX_Y && water.get(index(x, y, z));
    }

    public static boolean fill(World world, int x, int y, int z) {
        if (world.provider.isHellWorld || !canHold(world.getBlock(x, y, z))) return false;
        if (!world.isRemote && set(world, x, y, z, true)) {
            Block host = world.getBlock(x, y, z);
            world.scheduleBlockUpdate(x, y, z, host, Blocks.flowing_water.tickRate(world));
            world.notifyBlocksOfNeighborChange(x, y, z, Blocks.water);
        }
        return true;
    }

    public static boolean drain(World world, int x, int y, int z) {
        if (!contains(world, x, y, z)) return false;
        if (!world.isRemote && set(world, x, y, z, false))
            world.notifyBlocksOfNeighborChange(x, y, z, Blocks.air);
        return true;
    }

    public static void onReplacement(World world, int x, int y, int z) {
        if (!canHold(world.getBlock(x, y, z))) set(world, x, y, z, false);
    }

    public static boolean faceCovered(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
        Block host = world.getBlock(x, y, z);
        if (host instanceof BlockLeaves) return true;
        if (host instanceof BlockSlab) {
            boolean upper = (world.getBlockMetadata(x, y, z) & 8) != 0;
            return face == (upper ? ForgeDirection.UP : ForgeDirection.DOWN);
        }
        return world.isSideSolid(x, y, z, face, false);
    }

    private static boolean set(World world, int x, int y, int z, boolean wet) {
        if (world.isRemote || y < 0 || y >= MAX_Y) return false;
        Chunk chunk = loadedChunk(world, x >> 4, z >> 4);
        if (chunk == null) return false;
        BitSet current = WATER.get(chunk);
        int cell = index(x, y, z);
        if (wet == (current != null && current.get(cell))) return false;
        BitSet updated = current == null ? new BitSet() : (BitSet) current.clone();
        updated.set(cell, wet);
        attach(chunk, updated);
        chunk.setChunkModified();

        if (world instanceof WorldServer && EtFuturum.networkWrapper != null) {
            for (Object entry : world.playerEntities) {
                if (entry instanceof EntityPlayerMP) {
                    EntityPlayerMP player = (EntityPlayerMP) entry;
                    if (((WorldServer) world).getPlayerManager().isPlayerWatchingChunk(player, chunk.xPosition, chunk.zPosition))
                        EtFuturum.networkWrapper.sendTo(WaterloggedChunkMessage.cell(chunk, cell, wet), player);
                }
            }
        }
        return true;
    }

    private static int index(int x, int y, int z) {
        return (y << 8) | ((z & 15) << 4) | (x & 15);
    }

    private static Chunk loadedChunk(World world, int cx, int cz) {
        if (!world.getChunkProvider().chunkExists(cx, cz)) return null;
        Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
        return chunk instanceof EmptyChunk ? null : chunk;
    }

    private static void attach(Chunk chunk, BitSet water) {
        if (water.isEmpty()) WATER.remove(chunk);
        else WATER.put(chunk, water);
    }

    public static void receive(World world, int cx, int cz, int cell, boolean wet, byte[] bytes) {
        Chunk chunk = loadedChunk(world, cx, cz);
        if (chunk == null) return;
        BitSet previous = WATER.get(chunk);
        if (cell >= 0) {
            BitSet updated = previous == null ? new BitSet() : (BitSet) previous.clone();
            updated.set(cell, wet);
            attach(chunk, updated);
            int x = (cx << 4) | (cell & 15), y = cell >> 8, z = (cz << 4) | ((cell >> 4) & 15);
            world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
        } else {
            BitSet updated = BitSet.valueOf(bytes);
            attach(chunk, updated);
            BitSet changed = (BitSet) updated.clone();
            if (previous != null) changed.xor(previous);
            for (int start = changed.nextSetBit(0); start >= 0; start = changed.nextSetBit(((start >> 12) + 1) << 12)) {
                int y = (start >> 12) << 4;
                world.markBlockRangeForRenderUpdate(cx << 4, y, cz << 4, (cx << 4) + 15, y + 15, (cz << 4) + 15);
            }
        }
    }

    @SubscribeEvent
    public void load(ChunkDataEvent.Load event) {
        byte[] bytes = event.getData().getCompoundTag("Level").getByteArray(NBT_KEY);
        if (bytes.length <= MAX_BYTES) attach(event.getChunk(), BitSet.valueOf(bytes));
    }

    @SubscribeEvent
    public void save(ChunkDataEvent.Save event) {
        BitSet water = WATER.get(event.getChunk());
        if (water != null)
            event.getData().getCompoundTag("Level").setByteArray(NBT_KEY, water.toByteArray());
    }

    @SubscribeEvent
    public void watch(ChunkWatchEvent.Watch event) {
        ChunkCoordIntPair pos = event.chunk;
        Chunk chunk = loadedChunk(event.player.worldObj, pos.chunkXPos, pos.chunkZPos);
        if (chunk == null) return;
        BitSet water = WATER.get(chunk);
        if (water != null)
            EtFuturum.networkWrapper.sendTo(WaterloggedChunkMessage.snapshot(chunk, water.toByteArray()), event.player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void place(BlockEvent.PlaceEvent event) {
        if (event.world.isRemote) return;
        if (event instanceof BlockEvent.MultiPlaceEvent) {
            for (BlockSnapshot snapshot : ((BlockEvent.MultiPlaceEvent) event).getReplacedBlockSnapshots())
                placed(event.world, snapshot);
        } else {
            placed(event.world, event.blockSnapshot);
        }
    }

    private static void placed(World world, BlockSnapshot snapshot) {
        Block old = snapshot.getReplacedBlock();
        if ((old == Blocks.water || old == Blocks.flowing_water) && snapshot.meta == 0)
            fill(world, snapshot.x, snapshot.y, snapshot.z);
        else
            onReplacement(world, snapshot.x, snapshot.y, snapshot.z);
    }
}
