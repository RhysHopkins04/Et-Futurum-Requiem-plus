package ganymedes01.etfuturum.world.imported;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.WeightedRandomChestContent;

/** Native inventory pools evaluated once against the running pack, without worldgen. */
final class NtmInventoryActions {
    private NtmInventoryActions() {}
    static void validate(NBTTagCompound entry) {
        int size=entry.func_150296_c().size();
        if((size!=9&&size!=10)||!entry.hasKey("Pool",8)||entry.getString("Pool").length()>128
            ||!entry.hasKey("Count",3)||entry.getInteger("Count")<1||entry.getInteger("Count")>128
            ||!entry.hasKey("Block",8)||!entry.hasKey("TileId",8)
            ||!(entry.getString("Block").startsWith("hbm:")||entry.getString("Block").equals("minecraft:chest")))
            throw new IllegalArgumentException("Malformed native inventory pool action");
        if(size==10&&!entry.hasKey("Overrides",9))throw new IllegalArgumentException("Unknown native pool action field");
        NBTTagList overrides=entry.getTagList("Overrides",10);
        if(overrides.tagCount()>256)throw new IllegalArgumentException("Too many native inventory overrides");
        Set<Integer> slots=new HashSet<>();
        for(int i=0;i<overrides.tagCount();i++){
            NBTTagCompound override=overrides.getCompoundTagAt(i);
            if(override.func_150296_c().size()!=2||!override.hasKey("Slot",3)||!override.hasKey("Stack",10)
                ||override.getInteger("Slot")<0||override.getInteger("Slot")>255||!slots.add(override.getInteger("Slot")))
                throw new IllegalArgumentException("Invalid native inventory override");
            NBTTagCompound stack=override.getCompoundTag("Stack");
            if(!stack.hasNoTags()){
                ItemStack item=ItemStack.loadItemStackFromNBT(stack);
                if(item==null||item.stackSize<1||item.stackSize>item.getMaxStackSize())throw new IllegalArgumentException("Invalid native fixed item");
            }
        }
    }
    static IInventory prepare(TileEntity tile,Block block,NBTTagCompound entry,Random rng){
        if(!(tile instanceof IInventory)||!Block.blockRegistry.getNameForObject(block).equals(entry.getString("Block")))
            throw new IllegalStateException("Native inventory target block changed");
        NBTTagCompound state=new NBTTagCompound();tile.writeToNBT(state);
        if(!state.getString("id").equals(entry.getString("TileId")))throw new IllegalStateException("Native inventory target tile changed");
        IInventory target=(IInventory)tile;
        if(target.getSizeInventory()<1||target.getSizeInventory()>256||target.getInventoryStackLimit()!=64)
            throw new IllegalStateException("Unsupported native inventory dimensions");
        for(int i=0;i<target.getSizeInventory();i++)if(target.getStackInSlot(i)!=null)
            throw new IllegalStateException("Native inventory is not empty; refusing to overwrite loot");
        try{
            Class<?> type=Class.forName("com.hbm.itempool.ItemPool");
            Map<?,?> pools=(Map<?,?>)type.getField("pools").get(null);
            Object pool=pools.get(entry.getString("Pool"));
            if(pool==null)throw new IllegalStateException("Requested live native loot pool is unavailable");
            WeightedRandomChestContent[] content=(WeightedRandomChestContent[])type.getField("pool").get(pool);
            IInventory scratch=new InventoryBasic("imported-native-loot",false,target.getSizeInventory());
            WeightedRandomChestContent.generateChestContents(rng,content,scratch,entry.getInteger("Count"));
            NBTTagList overrides=entry.getTagList("Overrides",10);
            for(int i=0;i<overrides.tagCount();i++){
                NBTTagCompound override=overrides.getCompoundTagAt(i);int slot=override.getInteger("Slot");
                if(slot>=scratch.getSizeInventory())throw new IllegalStateException("Fixed item slot exceeds native inventory");
                NBTTagCompound stack=override.getCompoundTag("Stack");
                scratch.setInventorySlotContents(slot,stack.hasNoTags()?null:ItemStack.loadItemStackFromNBT(stack));
            }
            return scratch;
        }catch(ReflectiveOperationException ex){throw new IllegalStateException("Native inventory loot API unavailable",ex);}
    }
}
