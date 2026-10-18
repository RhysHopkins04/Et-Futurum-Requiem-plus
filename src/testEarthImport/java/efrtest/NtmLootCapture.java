package efrtest;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.WeightedRandomChestContent;

/** Read-only pool provenance for memory-world generation, outside release code. */
public final class NtmLootCapture {
    private static final Map<IInventory,NBTTagCompound> ACTIVE=new IdentityHashMap<>();
    public static void before(Random rng,WeightedRandomChestContent[] content,IInventory inventory,int count){
        if(!(inventory instanceof TileEntity)||!(((TileEntity)inventory).getWorldObj() instanceof NtmRecordingWorld))return;
        try{
            TileEntity tile=(TileEntity)inventory;
            NtmRecordingWorld world=(NtmRecordingWorld)tile.getWorldObj();
            if(world.loot.size()>=256||count<1||count>128||ACTIVE.containsKey(inventory))throw new AssertionError("Native inventory loot capture bounds/recursion");
            String pool=null;
            Class<?> pools=Class.forName("com.hbm.itempool.ItemPool");
            for(Map.Entry<?,?> entry:((Map<?,?>)pools.getField("pools").get(null)).entrySet())
                if(pools.getField("pool").get(entry.getValue())==content){
                    if(pool!=null)throw new AssertionError("Ambiguous native loot pool identity");pool=entry.getKey().toString();
                }
            // Library loot is independently adapted using Forge's live dungeon tables.
            if(pool==null)return;
            Field seed=Random.class.getDeclaredField("seed");seed.setAccessible(true);
            long initial=((AtomicLong)seed.get(rng)).get()^0x5DEECE66DL;
            NBTTagCompound record=new NBTTagCompound();record.setInteger("x",tile.xCoord);record.setInteger("y",tile.yCoord);record.setInteger("z",tile.zCoord);
            record.setString("pool",pool);record.setInteger("count",count);record.setLong("seed",initial);
            record.setTag("before",snapshot(inventory));
            ACTIVE.put(inventory,record);world.loot.add(record);
            if(((AtomicLong)seed.get(rng)).get()!=(initial^0x5DEECE66DL))throw new AssertionError("Loot observation changed native RNG state");
        }catch(ReflectiveOperationException ex){throw new AssertionError("Cannot record native inventory loot provenance",ex);}
    }
    public static void after(IInventory inventory){
        NBTTagCompound record=ACTIVE.remove(inventory);
        if(record!=null)record.setTag("rolled",snapshot(inventory));
    }
    private static NBTTagList snapshot(IInventory inventory){
        NBTTagList items=new NBTTagList();
        for(int slot=0;slot<inventory.getSizeInventory();slot++)if(inventory.getStackInSlot(slot)!=null){
            NBTTagCompound item=new NBTTagCompound();inventory.getStackInSlot(slot).writeToNBT(item);item.setInteger("slot",slot);items.appendTag(item);
        }
        return items;
    }
}
