package efrtest;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.*;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.storage.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.storage.IChunkLoader;

/** Bounded, memory-only native generator fixture. No Forge decorators or disk saves. */
final class NtmRecordingWorld extends World {
    static final int SURFACE = 64;
    final Set<Position> written = new LinkedHashSet<>();
    final List<String> entities = new ArrayList<>();
    final List<NBTTagCompound> loot = new ArrayList<>();
    final List<PendingTick> pendingTicks = new ArrayList<>();
    final Map<Long, Chunk> chunks = new HashMap<>();
    private final boolean ocean;
    final int referenceSurface;
    private final int captureBiomeId;
    private int writeCalls;

    static final class PendingTick {
        final int x,y,z,delay,priority;final String block;
        PendingTick(int x,int y,int z,Block block,int delay,int priority){
            this.x=x;this.y=y;this.z=z;this.block=Block.blockRegistry.getNameForObject(block);this.delay=delay;this.priority=priority;
        }
    }

    static final class MemorySaveHandler implements ISaveHandler {
        public WorldInfo loadWorldInfo(){return null;}
        public void checkSessionLock(){}
        public IChunkLoader getChunkLoader(WorldProvider provider){throw new IllegalStateException("No disk chunk loader in recording world");}
        public void saveWorldInfoWithPlayer(WorldInfo info,NBTTagCompound player){throw new IllegalStateException("No disk saves in recording world");}
        public void saveWorldInfo(WorldInfo info){throw new IllegalStateException("No disk saves in recording world");}
        public IPlayerFileData getSaveHandler(){throw new IllegalStateException("No players in recording world");}
        public void flush(){}
        public java.io.File getWorldDirectory(){return null;}
        public java.io.File getMapFileFromName(String name){return null;}
        public String getWorldDirectoryName(){return "memory-only";}
    }

    static final class Position {
        final int x,y,z;
        Position(int x,int y,int z) {this.x=x;this.y=y;this.z=z;}
        @Override public boolean equals(Object other) {if(!(other instanceof Position))return false;Position p=(Position)other;return x==p.x&&y==p.y&&z==p.z;}
        @Override public int hashCode(){return Objects.hash(x,y,z);}
    }

    NtmRecordingWorld(long seed, BiomeGenBase biome, boolean ocean) {this(seed,biome,ocean,64);}
    NtmRecordingWorld(long seed, BiomeGenBase biome, boolean ocean,int referenceSurface) {
        super(new MemorySaveHandler(), "native-structure-capture", new WorldSettings(seed,WorldSettings.GameType.SURVIVAL,true,false,WorldType.DEFAULT),
            WorldProvider.getProviderForDimension(0), new Profiler());
        this.ocean=ocean;this.referenceSurface=referenceSurface;this.captureBiomeId=biome.biomeID;
        provider.worldChunkMgr=new WorldChunkManagerHell(biome, biome.temperature);
        rand.setSeed(seed);
    }

    @Override protected int func_152379_p(){return 0;}
    @Override public Entity getEntityByID(int id){return null;}
    // Lighting is rebuilt by the converter. It must not expand capture reads.
    @Override public boolean func_147451_t(int x,int y,int z){return false;}
    @Override public boolean updateLightByType(EnumSkyBlock kind,int x,int y,int z){return false;}
    @Override public void markBlocksDirtyVertical(int x,int z,int low,int high){}
    @Override public boolean spawnEntityInWorld(Entity entity){
        entities.add(entity.getClass().getName()); return false;
    }
    @Override public void scheduleBlockUpdate(int x,int y,int z,Block block,int delay){
        scheduleBlockUpdateWithPriority(x,y,z,block,delay,0);
    }
    @Override public void scheduleBlockUpdateWithPriority(int x,int y,int z,Block block,int delay,int priority){
        if(pendingTicks.size()>=1000000)throw new IllegalStateException("Native structure exceeded tick budget");
        pendingTicks.add(new PendingTick(x,y,z,block,delay,priority));
    }
    @Override public boolean setBlock(int x,int y,int z,Block block,int meta,int flags){
        if(written==null)throw new IllegalStateException("Native generator wrote during world construction");
        if(Math.abs(x)>256||Math.abs(z)>256||y<0||y>=384||++writeCalls>1000000)
            throw new IllegalStateException("Native structure exceeded bounded recording volume/write budget");
        // Include explicit air writes even if the synthetic background was air.
        written.add(new Position(x,y,z));
        return super.setBlock(x,y,z,block,meta,flags);
    }
    @Override public boolean setBlockMetadataWithNotify(int x,int y,int z,int meta,int flags){
        if(Math.abs(x)>256||Math.abs(z)>256||y<0||y>=384||++writeCalls>1000000)
            throw new IllegalStateException("Native metadata write exceeded bounded recording volume/write budget");
        written.add(new Position(x,y,z)); return super.setBlockMetadataWithNotify(x,y,z,meta,flags);
    }

    @Override protected IChunkProvider createChunkProvider(){
        return new IChunkProvider(){
            private long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
            public boolean chunkExists(int x,int z){return Math.abs(x)<=17&&Math.abs(z)<=17;}
            public Chunk provideChunk(int x,int z){
                if(!chunkExists(x,z))throw new IllegalStateException("Native generator read outside bounded volume");
                long key=key(x,z);Chunk old=chunks.get(key);if(old!=null)return old;
                if(chunks.size()>=512)throw new IllegalStateException("Native structure exceeded chunk budget");
                Chunk chunk=new Chunk(NtmRecordingWorld.this,x,z);chunks.put(key,chunk);
                // Native removal ignores unloaded chunks; mark this memory chunk
                // loaded so overwritten blocks cannot retain stale tile entities.
                chunk.isChunkLoaded=true;
                Arrays.fill(chunk.getBiomeArray(),(byte)captureBiomeId);
                for(int sy=0;sy<referenceSurface/16;sy++){
                    ExtendedBlockStorage storage=new ExtendedBlockStorage(sy*16,true);
                    chunk.getBlockStorageArray()[sy]=storage;
                    for(int by=0;by<16;by++)for(int bz=0;bz<16;bz++)for(int bx=0;bx<16;bx++){
                        int y=sy*16+by;
                        Block block=y==0?Blocks.bedrock:ocean?(y<16?Blocks.stone:Blocks.water):y==referenceSurface-1?Blocks.grass:y>=referenceSurface-4?Blocks.dirt:Blocks.stone;
                        storage.func_150818_a(bx,by,bz,block);
                    }
                }
                chunk.generateSkylightMap();chunk.isTerrainPopulated=true;return chunk;
            }
            public Chunk loadChunk(int x,int z){return provideChunk(x,z);}
            public void populate(IChunkProvider provider,int x,int z){throw new IllegalStateException("Unexpected native decorator");}
            public boolean saveChunks(boolean all,IProgressUpdate progress){return true;}
            public boolean unloadQueuedChunks(){return false;}
            public boolean canSave(){return false;}
            public String makeString(){return "Bounded native structure fixture";}
            public List getPossibleCreatures(EnumCreatureType type,int x,int y,int z){return Collections.emptyList();}
            public ChunkPosition func_147416_a(World world,String name,int x,int y,int z){return null;}
            public int getLoadedChunkCount(){return chunks.size();}
            public void recreateStructures(int x,int z){throw new IllegalStateException("Unexpected native decorator");}
            public void saveExtraData(){}
        };
    }
}
