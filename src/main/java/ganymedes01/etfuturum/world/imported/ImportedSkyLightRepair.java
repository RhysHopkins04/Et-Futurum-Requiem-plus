package ganymedes01.etfuturum.world.imported;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.api.imported.ImportedBiomeData;
import ganymedes01.etfuturum.api.imported.ImportedBiomeVolume;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** One-shot direct-sky seed repair for imported chunks, before their first client packet.
 * The converter's conservative seed treats every non-air cell as a roof. Vanilla's
 * queued relight visits air, so non-air plants can retain isolated zero-light cells.
 * Use real pack opacity to raise only cells reached by direct sky. Preserve lateral
 * skylight, block light, storage topology and ordinary worlds; vanilla still owns
 * lateral reconciliation. No adjacent chunk is requested by this traversal.
 */
public final class ImportedSkyLightRepair {
    public static final String KEY="etfuturum:imported_skylight_version";
    public static final ImportedSkyLightRepair EVENTS=new ImportedSkyLightRepair();
    private static final Map<Chunk,Integer> VERSIONS=Collections.synchronizedMap(new WeakHashMap<Chunk,Integer>());
    private ImportedSkyLightRepair() {}

    @SubscribeEvent public void load(ChunkDataEvent.Load event) {
        if(event.world.isRemote || !event.getData().hasKey(ImportedBiomeVolume.NBT_KEY,10))return;
        VERSIONS.put(event.getChunk(),event.getData().getInteger(KEY));
    }
    @SubscribeEvent public void save(ChunkDataEvent.Save event) {
        Integer version=VERSIONS.get(event.getChunk());
        if(!event.world.isRemote && version!=null && version>0)event.getData().setInteger(KEY,version);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void activate(ChunkEvent.Load event) {
        repair(event.getChunk());
    }
    public static int repair(Chunk chunk) {
        if(chunk.worldObj.isRemote || chunk.worldObj.provider.dimensionId!=0
                || chunk.worldObj.provider.hasNoSky || !ConfigMapCompatibility.importedEarthMode
                || !ImportedBiomeData.isImportedChunk(chunk))return 0;
        Integer version=VERSIONS.get(chunk);
        if(version!=null && version>=1)return 0;
        ExtendedBlockStorage[] sections=chunk.getBlockStorageArray();
        int minimumHeight=Integer.MAX_VALUE;
        int top=Math.min(chunk.worldObj.getHeight()-1,chunk.getTopFilledSegment()+15),changed=0;
        for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
            int sky=15,height=0;
            for(int y=top;y>=0 && sky>0;y--) {
                int opacity=chunk.getBlock(x,y,z).getLightOpacity(chunk.worldObj,(chunk.xPosition<<4)+x,y,(chunk.zPosition<<4)+z);
                if(opacity>0 && height==0)height=y+1;
                if(opacity==0 && sky<15)opacity=1; // Vanilla direct-sky attenuation below leaves/water.
                sky=Math.max(0,sky-Math.max(0,opacity));
                ExtendedBlockStorage storage=sections[y>>4];
                if(storage!=null && sky>storage.getExtSkylightValue(x,y&15,z)) {
                    storage.setExtSkylightValue(x,y&15,z,sky);changed++;
                }
            }
            int column=(z<<4)|x;
            chunk.heightMap[column]=height;
            chunk.precipitationHeightMap[column]=-999;
            minimumHeight=Math.min(minimumHeight,height);
        }
        // Opacity, rather than the uppermost plant, determines the legacy sky boundary.
        chunk.heightMapMinimum=minimumHeight;
        if(changed>0) { chunk.isLightPopulated=false;chunk.resetRelightChecks(); }
        VERSIONS.put(chunk,1);chunk.setChunkModified();return changed;
    }
}
