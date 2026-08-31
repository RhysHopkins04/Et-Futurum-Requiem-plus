package ganymedes01.etfuturum.core.utils;

import ganymedes01.etfuturum.api.mappings.BlockMetaPair;
import ganymedes01.etfuturum.core.utils.helpers.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight fake world used by block simulation code.  This restores the
 * EFR-owned implementation that predated the HogUtils migration.
 */
public class DummyWorld extends World {
    public static class GT_IteratorRandom extends FastRandom {
        public int mIterationStep = Integer.MAX_VALUE;

        @Override
        public int nextInt(int parameter) {
            if (mIterationStep == 0 || mIterationStep > parameter) mIterationStep = parameter;
            return --mIterationStep;
        }
    }

    private static final ThreadLocal<DummyWorld> GLOBAL = ThreadLocal.withInitial(DummyWorld::new);
    public static DummyWorld getGlobalInstance() { return GLOBAL.get(); }

    public final GT_IteratorRandom mRandom = new GT_IteratorRandom();
    private final Map<BlockPos, BlockMetaPair> fakeWorldData = new HashMap<>();
    private static final BlockMetaPair AIR = BlockMetaPair.intern(Blocks.air, 0);

    DummyWorld(ISaveHandler saveHandler, String name, WorldProvider provider, WorldSettings settings, Profiler profiler) {
        super(saveHandler, name, settings, provider, profiler);
        rand = mRandom;
    }

    public DummyWorld() {
        this(new ISaveHandler() {
            @Override public void saveWorldInfoWithPlayer(WorldInfo info, NBTTagCompound player) {}
            @Override public void saveWorldInfo(WorldInfo info) {}
            @Override public WorldInfo loadWorldInfo() { return null; }
            @Override public IPlayerFileData getSaveHandler() { return null; }
            @Override public File getMapFileFromName(String name) { return null; }
            @Override public IChunkLoader getChunkLoader(WorldProvider provider) { return null; }
            @Override public void flush() {}
            @Override public void checkSessionLock() {}
            @Override public String getWorldDirectoryName() { return null; }
            @Override public File getWorldDirectory() { return null; }
        }, "DUMMY_DIMENSION", new WorldProvider() {
            @Override public String getDimensionName() { return "DUMMY_DIMENSION"; }
        }, new WorldSettings(new WorldInfo(new NBTTagCompound())), new Profiler());
    }

    @Override protected IChunkProvider createChunkProvider() { return null; }
    @Override public Entity getEntityByID(int id) { return null; }

    @Override
    public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flags) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockMetaPair block = fakeWorldData.get(pos);
        if (block == null) return false;
        setBlock(x, y, z, block.get(), meta, 0);
        return true;
    }

    @Override public boolean setBlockToAir(int x, int y, int z) { fakeWorldData.remove(new BlockPos(x, y, z)); return true; }
    @Override public boolean setBlock(int x, int y, int z, Block block) { return setBlock(x, y, z, block, 0, 0); }

    @Override
    public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
        BlockPos pos = new BlockPos(x, y, z);
        if (block == Blocks.air) fakeWorldData.remove(pos);
        else fakeWorldData.put(pos, BlockMetaPair.intern(block, meta));
        return true;
    }

    @Override public float getSunBrightnessFactor(float partialTicks) { return 1.0F; }
    @Override public BiomeGenBase getBiomeGenForCoords(int x, int z) { return BiomeGenBase.ocean; }
    @Override public int getFullBlockLightValue(int x, int y, int z) { return 10; }
    @Override public Block getBlock(int x, int y, int z) { return fakeWorldData.getOrDefault(new BlockPos(x, y, z), AIR).get(); }
    @Override public int getBlockMetadata(int x, int y, int z) { return fakeWorldData.getOrDefault(new BlockPos(x, y, z), AIR).getMeta(); }
    @Override public boolean canBlockSeeTheSky(int x, int y, int z) { return x >= 16 && z >= 16 && x < 32 && z < 32 ? y > 64 : true; }
    @Override protected int func_152379_p() { return 0; }

    public void clearFakeData() { fakeWorldData.clear(); }
}
