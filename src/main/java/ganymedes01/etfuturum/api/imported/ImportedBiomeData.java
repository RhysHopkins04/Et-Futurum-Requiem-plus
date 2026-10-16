package ganymedes01.etfuturum.api.imported;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.world.imported.ImportedBiomes;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-side persistence and query API. Does not generate or load absent chunks; no client sync claimed. */
public final class ImportedBiomeData {
    public static final ImportedBiomeData EVENTS = new ImportedBiomeData();
    private static final Map<Chunk, ImportedBiomeVolume> DATA = Collections.synchronizedMap(new WeakHashMap<>());
    private ImportedBiomeData() {}

    @SubscribeEvent public void load(ChunkDataEvent.Load event) {
        if (!event.getData().hasKey(ImportedBiomeVolume.NBT_KEY)) return;
        if (!event.getData().hasKey(ImportedBiomeVolume.NBT_KEY, 10))
            throw new IllegalArgumentException("Imported biome data is not a compound");
        // Preserve valid existing data independently of config/dimension, including when a profile is disabled.
        DATA.put(event.getChunk(), ImportedBiomeVolume.fromNBT(event.getData().getCompoundTag(ImportedBiomeVolume.NBT_KEY)));
    }
    @SubscribeEvent public void save(ChunkDataEvent.Save event) {
        ImportedBiomeVolume data = DATA.get(event.getChunk());
        if (data != null) event.getData().setTag(ImportedBiomeVolume.NBT_KEY, data.toNBT());
    }
    public static void attach(Chunk chunk, ImportedBiomeVolume data) {
        if (chunk.worldObj.isRemote || chunk.worldObj.provider.dimensionId != 0)
            throw new IllegalArgumentException("Biome import writes require the server Overworld");
        if (data == null) throw new IllegalArgumentException("Biome volume is null");
        DATA.put(chunk, data); chunk.setChunkModified();
    }
    public static String sourceAt(World world, int x, int physicalY, int z) {
        if (world.isRemote || world.provider.dimensionId != 0 || !world.getChunkProvider().chunkExists(x >> 4, z >> 4)) return null;
        ImportedBiomeVolume data = DATA.get(world.getChunkFromChunkCoords(x >> 4, z >> 4));
        return data == null ? null : data.sourceAt(x & 15, physicalY, z & 15);
    }
    /** Legacy surface fallback is explicit; original unresolvable names remain available through sourceAt. */
    public static BiomeGenBase biomeAt(World world, int x, int physicalY, int z) {
        String name = sourceAt(world, x, physicalY, z);
        BiomeGenBase imported = name == null ? null : ImportedBiomes.resolve(name);
        return imported != null ? imported : world.getBiomeGenForCoords(x, z);
    }
}
