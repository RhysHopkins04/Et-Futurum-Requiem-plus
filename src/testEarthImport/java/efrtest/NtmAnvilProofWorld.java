package efrtest;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.*;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.*;

/** Native codec context containing only supplied converted chunks; never generates. */
final class NtmAnvilProofWorld extends World {
    final Map<Long,Chunk> supplied=new HashMap<>();
    final List<NextTickListEntry> ticks=new ArrayList<>();
    NtmAnvilProofWorld(){
        super(new NtmRecordingWorld.MemorySaveHandler(),"converted-native-proof",
            new WorldSettings(12345L,WorldSettings.GameType.SURVIVAL,true,false,WorldType.DEFAULT),WorldProvider.getProviderForDimension(0),new Profiler());
        provider.worldChunkMgr=new WorldChunkManagerHell(BiomeGenBase.plains,0.8F);
    }
    static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    void supply(Chunk chunk){supplied.put(key(chunk.xPosition,chunk.zPosition),chunk);}
    @Override protected int func_152379_p(){return 0;}
    // Comparator notifications are outside a codec/inventory fixture and may
    // otherwise request chunks beyond its deliberately finite source patches.
    @Override public void func_147453_f(int x,int y,int z,Block block){}
    @Override public Entity getEntityByID(int id){return null;}
    @Override public boolean spawnEntityInWorld(Entity e){throw new AssertionError("Codec proof must not spawn entities");}
    @Override public void scheduleBlockUpdate(int x,int y,int z,Block block,int delay){scheduleBlockUpdateWithPriority(x,y,z,block,delay,0);}
    @Override public void scheduleBlockUpdateWithPriority(int x,int y,int z,Block block,int delay,int priority){
        NextTickListEntry tick=new NextTickListEntry(x,y,z,block).setScheduledTime(getTotalWorldTime()+delay);tick.setPriority(priority);ticks.add(tick);
    }
    @Override public void func_147446_b(int x,int y,int z,Block block,int delay,int priority){scheduleBlockUpdateWithPriority(x,y,z,block,delay,priority);}
    @Override public List getPendingBlockUpdates(Chunk chunk,boolean remove){
        List<NextTickListEntry> found=new ArrayList<>();
        for(NextTickListEntry tick:ticks)if((tick.xCoord>>4)==chunk.xPosition&&(tick.zCoord>>4)==chunk.zPosition)found.add(tick);
        if(remove)ticks.removeAll(found);return found;
    }
    @Override protected IChunkProvider createChunkProvider(){return new IChunkProvider(){
        public boolean chunkExists(int x,int z){return supplied!=null&&supplied.containsKey(key(x,z));}
        public Chunk provideChunk(int x,int z){Chunk chunk=supplied.get(key(x,z));if(chunk==null)throw new AssertionError("Native proof attempted absent chunk access: "+x+","+z);return chunk;}
        public Chunk loadChunk(int x,int z){return provideChunk(x,z);}
        public void populate(IChunkProvider p,int x,int z){throw new AssertionError("Native proof must not run generation");}
        public boolean saveChunks(boolean all,IProgressUpdate progress){return true;}
        public boolean unloadQueuedChunks(){return false;}
        public boolean canSave(){return true;}
        public String makeString(){return "supplied-native-codec-chunks";}
        public List getPossibleCreatures(net.minecraft.entity.EnumCreatureType t,int x,int y,int z){return Collections.emptyList();}
        public ChunkPosition func_147416_a(World world,String name,int x,int y,int z){return null;}
        public int getLoadedChunkCount(){return supplied.size();}
        public void recreateStructures(int x,int z){throw new AssertionError("Native proof must not replay structures");}
        public void saveExtraData(){}
    };}
}
