package ganymedes01.etfuturum.world.imported;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import java.util.*;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.DungeonHooks;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;

/** Narrow one-shot imported dungeon actions; no worldgen or absent-chunk loading. */
public final class ImportedStructureActions {
    public static final String KEY="etfuturum:imported_structure_actions";
    public static final ImportedStructureActions EVENTS=new ImportedStructureActions();
    private static final Map<Chunk,NBTTagCompound> PENDING=Collections.synchronizedMap(new WeakHashMap<>());
    private ImportedStructureActions() {}

    public static void attach(Chunk chunk,NBTTagCompound data) {
        if(chunk.worldObj.isRemote||chunk.worldObj.provider.dimensionId!=0||!ConfigMapCompatibility.importedEarthMode)
            throw new IllegalStateException("Imported structure actions require the server imported Earth profile: enable importedEarthMode=true and mapCompatibilityMode=false in config/etfuturum/mapcompat.cfg, then fully restart the server");
        if(!data.hasKey("Version",3)||(data.getInteger("Version")!=1&&data.getInteger("Version")!=2)||data.func_150296_c().size()!=2||!data.hasKey("Entries",9))
            throw new IllegalArgumentException("Unsupported imported structure action contract");
        NBTTagList entries=data.getTagList("Entries",10);
        if(entries.tagCount()==0||entries.tagCount()>128)throw new IllegalArgumentException("Invalid imported structure action count");
        Set<String> positions=new HashSet<>();
        for(int i=0;i<entries.tagCount();i++) {
            NBTTagCompound entry=entries.getCompoundTagAt(i);
            int x=entry.getInteger("x"),y=entry.getInteger("y"),z=entry.getInteger("z");
            String kind=entry.getString("Kind");
            boolean nativePool=kind.equals("ntm-pool")&&data.getInteger("Version")==2;
            if((!nativePool&&entry.func_150296_c().size()!=5)||!entry.hasKey("x",3)||!entry.hasKey("y",3)||!entry.hasKey("z",3)
                ||!entry.hasKey("Seed",4)||!entry.hasKey("Kind",8)||y<1||y>=chunk.worldObj.getHeight()
                ||(x>>4)!=chunk.xPosition||(z>>4)!=chunk.zPosition||!positions.add(x+","+y+","+z)
                ||!(nativePool||kind.equals("dungeon-chest")||kind.equals("dungeon-spawner")))
                throw new IllegalArgumentException("Malformed imported structure action");
            if(nativePool)NtmInventoryActions.validate(entry);
        }
        PENDING.put(chunk,(NBTTagCompound)data.copy());
    }
    @SubscribeEvent public void load(ChunkDataEvent.Load event) {
        if(event.world.isRemote||!event.getData().hasKey(KEY))return;
        if(!event.getData().hasKey(KEY,10))throw new IllegalArgumentException("Imported structure actions must be a compound");
        attach(event.getChunk(),event.getData().getCompoundTag(KEY));
    }
    @SubscribeEvent public void save(ChunkDataEvent.Save event) {
        NBTTagCompound pending=PENDING.get(event.getChunk());
        if(event.world.isRemote)return;
        if(pending!=null)event.getData().setTag(KEY,pending.copy());
        else event.getData().removeTag(KEY);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void activate(ChunkEvent.Load event) {
        if(!event.world.isRemote)resolve(event.getChunk());
    }
    public static int resolve(Chunk chunk) {
        NBTTagCompound pending=PENDING.get(chunk);if(pending==null)return 0;
        NBTTagList entries=pending.getTagList("Entries",10);
        List<TileEntity> targets=new ArrayList<>();List<Object> results=new ArrayList<>();
        // Validate every target and prepare all results before touching a live inventory.
        for(int i=0;i<entries.tagCount();i++) {
            NBTTagCompound entry=entries.getCompoundTagAt(i);int x=entry.getInteger("x"),y=entry.getInteger("y"),z=entry.getInteger("z");
            TileEntity tile=chunk.func_150806_e(x&15,y,z&15);Random rng=new Random(entry.getLong("Seed"));
            if(entry.getString("Kind").equals("dungeon-chest")) {
                if(chunk.getBlock(x&15,y,z&15)!=Blocks.chest||tile==null||tile.getClass()!=TileEntityChest.class)
                    throw new IllegalStateException("Imported dungeon chest target changed; refusing to replace content");
                IInventory inventory=(IInventory)tile;
                for(int slot=0;slot<inventory.getSizeInventory();slot++)if(inventory.getStackInSlot(slot)!=null)
                    throw new IllegalStateException("Imported dungeon chest is not empty; refusing to overwrite loot");
                TileEntityChest scratch=new TileEntityChest();
                WeightedRandomChestContent.generateChestContents(rng,ChestGenHooks.getItems(ChestGenHooks.DUNGEON_CHEST,rng),scratch,
                    ChestGenHooks.getCount(ChestGenHooks.DUNGEON_CHEST,rng));
                results.add(scratch);
            }else if(entry.getString("Kind").equals("ntm-pool")){
                results.add(NtmInventoryActions.prepare(tile,chunk.getBlock(x&15,y,z&15),entry,rng));
            }else {
                if(chunk.getBlock(x&15,y,z&15)!=Blocks.mob_spawner||!(tile instanceof TileEntityMobSpawner))
                    throw new IllegalStateException("Imported dungeon spawner target changed");
                String mob=DungeonHooks.getRandomDungeonMob(rng);
                if(mob==null||mob.isEmpty())throw new IllegalStateException("Live dungeon mob table is empty");
                results.add(mob);
            }
            targets.add(tile);
        }
        for(int i=0;i<targets.size();i++) {
            TileEntity tile=targets.get(i);Object result=results.get(i);
            if(result instanceof IInventory) {
                IInventory source=(IInventory)result,target=(IInventory)tile;
                for(int slot=0;slot<target.getSizeInventory();slot++)target.setInventorySlotContents(slot,source.getStackInSlot(slot));
            }else ((TileEntityMobSpawner)tile).func_145881_a().setEntityName((String)result);
            tile.markDirty();
        }
        PENDING.remove(chunk);chunk.setChunkModified();return targets.size();
    }
}
