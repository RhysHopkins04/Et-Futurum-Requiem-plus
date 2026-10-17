package ganymedes01.etfuturum.tileentities;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.*;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import java.util.*;

/** The existing crafter TE identity gains a real inventory; legacy visual NBT remains readable. */
public class TileEntityCrafter extends TileEntity implements ISidedInventory {
    private final ItemStack[] items=new ItemStack[9];
    private int disabled,craftingTicks;
    private boolean triggered,crafting;
    public static boolean enabled() { return ModernMapParityBlocks.CRAFTER.get()!=null && !ConfigMapCompatibility.mapCompatibilityMode; }
    public boolean isTriggered() { return triggered; }
    public boolean isCrafting() { return crafting; }
    public int getVisualFlags() { return (triggered?1:0)|(crafting?2:0); }
    public void setVisualState(boolean trigger,boolean craft) { triggered=trigger;crafting=craft;markDirty(); }
    public boolean isDisabled(int slot) { return slot>=0 && slot<9 && (disabled&(1<<slot))!=0; }
    public int disabledMask() { return disabled; }
    public boolean setDisabled(int slot,boolean value) {
        if(slot<0 || slot>=9 || items[slot]!=null)return false;
        disabled=value?disabled|(1<<slot):disabled&~(1<<slot);markDirty();return true;
    }
    public int comparator() { int n=0;for(int i=0;i<9;i++)if(isDisabled(i) || items[i]!=null)n++;return n; }
    public void powerChanged() {
        if(worldObj==null || worldObj.isRemote || !enabled())return;
        boolean powered=worldObj.isBlockIndirectlyGettingPowered(xCoord,yCoord,zCoord);
        if(powered && !triggered) { triggered=true;worldObj.scheduleBlockUpdate(xCoord,yCoord,zCoord,getBlockType(),4);markDirty(); }
        else if(!powered && triggered) { triggered=false;crafting=false;markDirty(); }
    }
    private InventoryCrafting grid() {
        InventoryCrafting grid=new InventoryCrafting(new Container() { @Override public boolean canInteractWith(EntityPlayer player) { return false; } },3,3);
        for(int i=0;i<9;i++)if(items[i]!=null)grid.setInventorySlotContents(i,items[i].copy());return grid;
    }
    public ItemStack preview() {
        if(worldObj==null || !enabled())return null;
        ItemStack result=CraftingManager.getInstance().findMatchingRecipe(grid(),worldObj);return result==null?null:result.copy();
    }
    public boolean craft() {
        if(worldObj==null || worldObj.isRemote || !enabled() || worldObj.getTileEntity(xCoord,yCoord,zCoord)!=this)return false;
        InventoryCrafting grid=grid();ItemStack result=CraftingManager.getInstance().findMatchingRecipe(grid,worldObj);
        if(result==null || result.stackSize<=0 || result.getItem()==null) { worldObj.playSoundEffect(xCoord+.5,yCoord+.5,zCoord+.5,Tags.MC_ASSET_VER+":block.crafter.fail",1F,1F);return false; }
        // Obtain recipe-dependent containers before consuming live inventory; queries use independent stacks.
        List<ItemStack> remaining=new ArrayList<>();
        for(int i=0;i<9;i++)if(items[i]!=null) {
            ItemStack one=items[i].copy();one.stackSize=1;
            if(one.getItem().hasContainerItem(one)) {
                ItemStack rest=one.getItem().getContainerItem(one);
                if(rest!=null && rest.stackSize>0 && (!rest.isItemStackDamageable() || rest.getItemDamage()<=rest.getMaxDamage()))remaining.add(rest.copy());
            }
        }
        for(int i=0;i<9;i++)if(items[i]!=null && --items[i].stackSize<=0)items[i]=null;
        dispense(result.copy());for(ItemStack rest:remaining)dispense(rest);
        crafting=true;craftingTicks=6;markDirty();worldObj.playSoundEffect(xCoord+.5,yCoord+.5,zCoord+.5,Tags.MC_ASSET_VER+":block.crafter.craft",1F,1F);return true;
    }
    /** Exact existing FrontAndTop metadata indices, independent of the top-face rotation. */
    public static ForgeDirection front(int meta) {
        int[] sides={0,0,0,0,5,2,3,1,1,1,1,4};return ForgeDirection.getOrientation(sides[meta>=0 && meta<12?meta:5]);
    }
    private void dispense(ItemStack stack) {
        ForgeDirection front=front(getBlockMetadata());int x=xCoord+front.offsetX,y=yCoord+front.offsetY,z=zCoord+front.offsetZ;
        IInventory target=TileEntityHopper.func_145893_b(worldObj,x,y,z);
        if(target instanceof TileEntityCrafter && target!=this) {
            TileEntityCrafter next=(TileEntityCrafter)target;
            while(stack.stackSize>0) {
                boolean inserted=false;
                for(int slot=0;slot<9;slot++)if(next.canInsertItem(slot,stack,front.getOpposite().ordinal())) {
                    ItemStack old=next.getStackInSlot(slot),one=stack.splitStack(1);
                    if(old!=null)one.stackSize=old.stackSize+1;next.setInventorySlotContents(slot,one);inserted=true;break;
                }
                if(!inserted)break;
            }
        } else if(target!=null && target!=this)stack=TileEntityHopper.func_145889_a(target,stack,front.getOpposite().ordinal());
        if(stack!=null && stack.stackSize>0) {
            EntityItem entity=new EntityItem(worldObj,xCoord+.5+front.offsetX*.7,yCoord+.5+front.offsetY*.7,zCoord+.5+front.offsetZ*.7,stack);
            entity.motionX=front.offsetX*.2;entity.motionY=front.offsetY*.2+.05;entity.motionZ=front.offsetZ*.2;worldObj.spawnEntityInWorld(entity);
        }
    }
    public void dropContents() { if(worldObj!=null && !worldObj.isRemote)for(int i=0;i<9;i++) { ItemStack stack=items[i];items[i]=null;if(stack!=null)worldObj.spawnEntityInWorld(new EntityItem(worldObj,xCoord+.5,yCoord+.5,zCoord+.5,stack)); } }
    @Override public void updateEntity() { if(worldObj!=null && !worldObj.isRemote && craftingTicks>0 && --craftingTicks==0) { crafting=false;markDirty(); } }
    @Override public int getSizeInventory() { return 9; }
    @Override public ItemStack getStackInSlot(int slot) { return slot>=0 && slot<9?items[slot]:null; }
    @Override public ItemStack decrStackSize(int slot,int count) {
        ItemStack stack=getStackInSlot(slot);if(stack==null || count<=0)return null;
        ItemStack taken=stack.splitStack(Math.min(count,stack.stackSize));if(stack.stackSize<=0)items[slot]=null;markDirty();return taken;
    }
    @Override public ItemStack getStackInSlotOnClosing(int slot) { return null; } // World-owned inventory persists when a reader closes.
    @Override public void setInventorySlotContents(int slot,ItemStack stack) {
        if(slot<0 || slot>=9)return;
        if(stack!=null) { disabled&=~(1<<slot);stack.stackSize=Math.min(stack.stackSize,Math.min(64,stack.getMaxStackSize())); }
        items[slot]=stack;markDirty();
    }
    @Override public String getInventoryName() { return "container.crafter"; }
    @Override public boolean hasCustomInventoryName() { return false; }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean isUseableByPlayer(EntityPlayer player) { return enabled() && player.isEntityAlive() && worldObj!=null && worldObj.getTileEntity(xCoord,yCoord,zCoord)==this && worldObj.getBlock(xCoord,yCoord,zCoord)==ModernMapParityBlocks.CRAFTER.get() && player.getDistanceSq(xCoord+.5,yCoord+.5,zCoord+.5)<=64; }
    @Override public void openInventory() {}
    @Override public void closeInventory() {}
    @Override public boolean isItemValidForSlot(int slot,ItemStack stack) { return canInsertItem(slot,stack,0); }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return new int[]{0,1,2,3,4,5,6,7,8}; }
    @Override public boolean canInsertItem(int slot,ItemStack stack,int side) {
        if(!enabled() || slot<0 || slot>=9 || isDisabled(slot) || stack==null || stack.stackSize<=0)return false;
        ItemStack current=items[slot];if(current==null)return true;
        if(!same(current,stack) || current.stackSize>=Math.min(64,current.getMaxStackSize()))return false;
        for(int i=slot+1;i<9;i++)if(!isDisabled(i) && (items[i]==null || same(current,items[i]) && items[i].stackSize<current.stackSize))return false;
        return true;
    }
    private static boolean same(ItemStack a,ItemStack b) { return a.getItem()==b.getItem() && a.getItemDamage()==b.getItemDamage() && ItemStack.areItemStackTagsEqual(a,b); }
    @Override public boolean canExtractItem(int slot,ItemStack stack,int side) { return true; }
    @Override public void markDirty() {
        super.markDirty();if(worldObj!=null) { worldObj.markBlockForUpdate(xCoord,yCoord,zCoord);if(!worldObj.isRemote)worldObj.func_147453_f(xCoord,yCoord,zCoord,getBlockType()); }
    }
    @Override public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);tag.setBoolean("Triggered",triggered);tag.setBoolean("Crafting",crafting);tag.setInteger("crafting_ticks_remaining",craftingTicks);
        int[] slots=new int[Integer.bitCount(disabled)];int n=0;for(int i=0;i<9;i++)if(isDisabled(i))slots[n++]=i;tag.setIntArray("disabled_slots",slots);
        NBTTagList list=new NBTTagList();for(int i=0;i<9;i++)if(items[i]!=null) { NBTTagCompound entry=items[i].writeToNBT(new NBTTagCompound());entry.setByte("Slot",(byte)i);list.appendTag(entry); }tag.setTag("Items",list);
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);Arrays.fill(items,null);disabled=0;for(int slot:tag.getIntArray("disabled_slots"))if(slot>=0 && slot<9)disabled|=1<<slot;
        NBTTagList list=tag.getTagList("Items",10);for(int i=0;i<list.tagCount();i++) { NBTTagCompound entry=list.getCompoundTagAt(i);int slot=entry.getByte("Slot")&255;if(slot<9) { items[slot]=ItemStack.loadItemStackFromNBT(entry);if(items[slot]!=null)disabled&=~(1<<slot); } }
        triggered=tag.getBoolean("Triggered");crafting=tag.getBoolean("Crafting");craftingTicks=Math.max(0,Math.min(6,tag.getInteger("crafting_ticks_remaining")));if(crafting && craftingTicks==0)craftingTicks=1;
    }
    @Override public Packet getDescriptionPacket() { NBTTagCompound tag=new NBTTagCompound();writeToNBT(tag);return new S35PacketUpdateTileEntity(xCoord,yCoord,zCoord,35,tag); }
    @Override public void onDataPacket(NetworkManager network,S35PacketUpdateTileEntity packet) { readFromNBT(packet.func_148857_g());if(worldObj!=null)worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); }
}
