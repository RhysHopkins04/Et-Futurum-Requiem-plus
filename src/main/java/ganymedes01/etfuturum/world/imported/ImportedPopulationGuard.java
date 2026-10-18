package ganymedes01.etfuturum.world.imported;

import ganymedes01.etfuturum.api.imported.ImportedBiomeData;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import net.minecraft.world.World;

/** Confine normal target generation to chunks which were not imported. */
public final class ImportedPopulationGuard {
    private static final ThreadLocal<World> POPULATING = new ThreadLocal<>();
    private ImportedPopulationGuard() {}
    public static void run(World world, Runnable generation) {
        if (!ConfigMapCompatibility.importedEarthMode || world.isRemote || world.provider.dimensionId!=0) {
            generation.run();return;
        }
        World previous=POPULATING.get();POPULATING.set(world);
        try { generation.run(); }
        finally { if(previous==null)POPULATING.remove();else POPULATING.set(previous); }
    }
    public static boolean blocks(World world,int x,int y,int z) {
        // Do not change ordinary gameplay writes, ticks, machines or other dimensions.
        if (POPULATING.get()!=world || y<0 || y>=world.getHeight() || x < -30000000 || x>=30000000 || z < -30000000 || z>=30000000) return false;
        // This lookup may load an existing imported chunk, exactly as the attempted
        // World write would. A loaded-only check would miss the boundary on disk.
        return ImportedBiomeData.isImportedChunk(world.getChunkFromChunkCoords(x>>4,z>>4));
    }
}
