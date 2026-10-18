package efrtest;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import ganymedes01.etfuturum.world.imported.ImportedStructureActions;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.*;

/** Actual converter protocol, native pool comparison, no rerolls and disk restart. */
final class NtmConvertedInventoryProbe {
    static void verify(NtmAnvilProofWorld world,Map<String,NBTTagCompound> inputs,Map<String,Chunk> chunks,File directory,Method encode,Method decode,Object loader)throws Exception{
        int count=0,refusals=0;Set<String> types=new HashSet<>();
        for(String filename:new TreeSet<>(inputs.keySet())){
            Chunk chunk=chunks.get(filename);NBTTagCompound input=inputs.get(filename);
            if(!input.hasKey(ImportedStructureActions.KEY))continue;
            NBTTagCompound protocol=input.getCompoundTag(ImportedStructureActions.KEY);
            NBTTagList entries=protocol.getTagList("Entries",10);Map<TileEntity,NBTTagCompound> expected=new HashMap<>();Map<TileEntity,String> spawners=new HashMap<>();
            for(int i=0;i<entries.tagCount();i++){
                NBTTagCompound entry=entries.getCompoundTagAt(i);
                TileEntity tile=world.getTileEntity(entry.getInteger("x"),entry.getInteger("y"),entry.getInteger("z"));
                Random reference=new Random(entry.getLong("Seed"));
                if(entry.getString("Kind").equals("dungeon-spawner")){
                    spawners.put(tile,net.minecraftforge.common.DungeonHooks.getRandomDungeonMob(reference));continue;
                }
                if(entry.getString("Kind").equals("dungeon-chest")){
                    IInventory scratch=new net.minecraft.tileentity.TileEntityChest();
                    WeightedRandomChestContent.generateChestContents(reference,net.minecraftforge.common.ChestGenHooks.getItems(net.minecraftforge.common.ChestGenHooks.DUNGEON_CHEST,reference),scratch,net.minecraftforge.common.ChestGenHooks.getCount(net.minecraftforge.common.ChestGenHooks.DUNGEON_CHEST,reference));
                    expected.put(tile,snapshot(scratch));types.add("dungeon-chest");count++;continue;
                }
                if(!(tile instanceof IInventory))throw new AssertionError("Pool target is not a native inventory");
                IInventory inventory=(IInventory)tile;for(int s=0;s<inventory.getSizeInventory();s++)if(inventory.getStackInSlot(s)!=null)throw new AssertionError("Converter froze rolled loot");
                Class<?> poolType=Class.forName("com.hbm.itempool.ItemPool");Object pool=((Map<?,?>)poolType.getField("pools").get(null)).get(entry.getString("Pool"));
                WeightedRandomChestContent[] contents=(WeightedRandomChestContent[])poolType.getField("pool").get(pool);
                IInventory scratch=new InventoryBasic("native-reference",false,inventory.getSizeInventory());
                WeightedRandomChestContent.generateChestContents(new Random(entry.getLong("Seed")),contents,scratch,entry.getInteger("Count"));
                NBTTagList overrides=entry.getTagList("Overrides",10);
                for(int j=0;j<overrides.tagCount();j++){
                    NBTTagCompound override=overrides.getCompoundTagAt(j),stack=override.getCompoundTag("Stack");
                    scratch.setInventorySlotContents(override.getInteger("Slot"),stack.hasNoTags()?null:ItemStack.loadItemStackFromNBT(stack));
                }
                expected.put(tile,snapshot(scratch));types.add(entry.getString("TileId"));count++;
            }
            MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(chunk,input));
            NBTTagCompound pending=new NBTTagCompound();MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save(chunk,pending));
            if(!pending.getCompoundTag(ImportedStructureActions.KEY).equals(protocol))throw new AssertionError("Pool request changed before first load");
            MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk));
            for(Map.Entry<TileEntity,NBTTagCompound> target:expected.entrySet())if(!snapshot((IInventory)target.getKey()).equals(target.getValue()))throw new AssertionError("Live native pool/fixed override mismatch");
            for(Map.Entry<TileEntity,String> spawner:spawners.entrySet()){
                NBTTagCompound state=new NBTTagCompound();spawner.getKey().writeToNBT(state);
                if(!spawner.getValue().equals(state.getString("EntityId")))throw new AssertionError("Live dungeon spawner mismatch");
            }
            if(ImportedStructureActions.resolve(chunk)!=0)throw new AssertionError("Pool request rerolled");
            boolean nonempty=false;
            for(TileEntity tile:expected.keySet())nonempty|=!snapshot((IInventory)tile).hasNoTags();
            if(nonempty){
                ImportedStructureActions.attach(chunk,protocol);
                try{ImportedStructureActions.resolve(chunk);throw new AssertionError("Native pool overwrote existing content");}
                catch(IllegalStateException refused){refusals++;}
                for(Map.Entry<TileEntity,NBTTagCompound> target:expected.entrySet())if(!snapshot((IInventory)target.getKey()).equals(target.getValue()))throw new AssertionError("Refused pool request partially changed content");
                for(int i=0;i<entries.tagCount();i++){
                    NBTTagCompound entry=entries.getCompoundTagAt(i);TileEntity tile=world.getTileEntity(entry.getInteger("x"),entry.getInteger("y"),entry.getInteger("z"));
                    if(tile instanceof IInventory){IInventory inventory=(IInventory)tile;for(int slot=0;slot<inventory.getSizeInventory();slot++)inventory.setInventorySlotContents(slot,null);}
                }
                ImportedStructureActions.resolve(chunk);
                for(Map.Entry<TileEntity,NBTTagCompound> target:expected.entrySet())if(!snapshot((IInventory)target.getKey()).equals(target.getValue()))throw new AssertionError("Native pool recovery differs from deterministic reference");
            }
            NBTTagCompound saved=new NBTTagCompound();MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save(chunk,saved));
            if(saved.hasKey(ImportedStructureActions.KEY))throw new AssertionError("Fulfilled pool request retained");
            NBTTagCompound state=new NBTTagCompound();encode.invoke(loader,chunk,world,state);
            File disk=new File(directory,"pool-roundtrip-"+filename);
            NBTTagCompound persisted=state;
            if(disk.exists()){
                try(InputStream in=new FileInputStream(disk)){persisted=CompressedStreamTools.readCompressed(in);}
                if(!persisted.getTagList("TileEntities",10).equals(state.getTagList("TileEntities",10)))throw new AssertionError("Converted native inventories changed across server restart");
            }else try(OutputStream out=new FileOutputStream(disk)){CompressedStreamTools.writeCompressed(state,out);}
            // Decode the native writer's populated disk payload, rather than
            // reconstructing loot from the original pending request alone.
            Chunk reloaded=(Chunk)decode.invoke(loader,world,persisted);world.supply(reloaded);
            ((net.minecraft.world.chunk.storage.AnvilChunkLoader)loader).loadEntities(world,persisted,reloaded);
            NBTTagCompound fulfilledRoot=new NBTTagCompound();fulfilledRoot.setTag("Level",persisted);
            MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(reloaded,fulfilledRoot));MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(reloaded));
            if(ImportedStructureActions.resolve(reloaded)!=0)throw new AssertionError("Disk reload reconstructed fulfilled inventory actions");
            NBTTagCompound after=new NBTTagCompound();encode.invoke(loader,reloaded,world,after);
            if(!after.getTagList("TileEntities",10).equals(persisted.getTagList("TileEntities",10)))throw new AssertionError("Native disk decoder changed populated tile inventories");
            NBTTagCompound finalRoot=new NBTTagCompound();MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save(reloaded,finalRoot));
            if(finalRoot.hasKey(ImportedStructureActions.KEY))throw new AssertionError("Disk reload restored completed requests");
        }
        if(count==0||refusals==0)throw new AssertionError("Converted native inventory proof contains no pool requests");
        System.out.println("NTM_CONVERTED_INVENTORIES PASSED: "+count+" live pool requests, "+types+", native RNG/reference, fixed overrides, events, refused overwrites, one-shot and disk restart");
    }
    private static NBTTagCompound snapshot(IInventory inventory){
        NBTTagCompound result=new NBTTagCompound();
        for(int i=0;i<inventory.getSizeInventory();i++)if(inventory.getStackInSlot(i)!=null){NBTTagCompound stack=new NBTTagCompound();inventory.getStackInSlot(i).writeToNBT(stack);result.setTag("slot"+i,stack);}
        return result;
    }
}
