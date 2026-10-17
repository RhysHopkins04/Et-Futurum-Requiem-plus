package ganymedes01.etfuturum.inventory;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.recipes.CartographyRecipes;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Map/addition inputs; atomic server-side copy, scale and lock operations. */
public final class ContainerCartographyTable extends Container {
    private final EntityPlayer owner;
    private final World world;
    private final int x,y,z;
    public final InventoryBasic input=new InventoryBasic("Cartography",false,2) {
        @Override public void markDirty() { super.markDirty();ContainerCartographyTable.this.onCraftMatrixChanged(this); }
    };
    private final InventoryCraftResult result=new InventoryCraftResult();
    public ContainerCartographyTable(InventoryPlayer inventory,World world,int x,int y,int z) {
        this.owner=inventory.player;this.world=world;this.x=x;this.y=y;this.z=z;
        addSlotToContainer(new Slot(input,0,15,15) {
            @Override public boolean isItemValid(ItemStack stack) { return stack!=null && stack.getItem()==net.minecraft.init.Items.filled_map; }
        });
        addSlotToContainer(new Slot(input,1,15,52) {
            @Override public boolean isItemValid(ItemStack stack) { return CartographyRecipes.addition(stack); }
        });
        addSlotToContainer(new Slot(result,0,145,39) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
            @Override public boolean canTakeStack(EntityPlayer player) {
                ItemStack expected=preview();
                return canInteractWith(player) && (world.isRemote?getHasStack():expected!=null && ItemStack.areItemStacksEqual(getStack(),expected));
            }
            @Override public void onPickupFromSlot(EntityPlayer player,ItemStack crafted) {
                completeCraft(player,crafted);super.onPickupFromSlot(player,crafted);
            }
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlotToContainer(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlotToContainer(new Slot(inventory,col,8+col*18,142));
        refresh();
    }
    @Override public boolean canInteractWith(EntityPlayer player) {
        return player==owner && !ganymedes01.etfuturum.api.spectator.SpectatorUtils.isSpectator(player) && CartographyRecipes.enabled() && world.getBlock(x,y,z)==ModBlocks.CARTOGRAPHY_TABLE.get()
                && player.getDistanceSq(x+.5D,y+.5D,z+.5D)<=64D;
    }
    private ItemStack preview() { return CartographyRecipes.preview(world,input.getStackInSlot(0),input.getStackInSlot(1)); }
    private void refresh() { if(!world.isRemote)result.setInventorySlotContents(0,preview()); }
    @Override public void detectAndSendChanges() { refresh();super.detectAndSendChanges(); }
    @Override public void onCraftMatrixChanged(IInventory inventory) { refresh();super.onCraftMatrixChanged(inventory); }
    private void completeCraft(EntityPlayer player,ItemStack crafted) {
        if(!world.isRemote) {
            ItemStack finished=CartographyRecipes.finish(world,input.getStackInSlot(0),input.getStackInSlot(1));
            if(finished==null)throw new IllegalStateException("Cartography result changed during take");
            crafted.setItemDamage(finished.getItemDamage());crafted.setTagCompound(finished.hasTagCompound()?(net.minecraft.nbt.NBTTagCompound)finished.getTagCompound().copy():null);
        }
        input.decrStackSize(0,1);input.decrStackSize(1,1);refresh();
        if(!world.isRemote)world.playSoundEffect(x+.5D,y+.5D,z+.5D,Tags.MC_ASSET_VER+":ui.cartography_table.take_result",1F,1F);
    }
    private boolean canFit(ItemStack stack,boolean newIdentity) {
        int remaining=stack.stackSize;
        for(int i=0;i<36;i++) {
            ItemStack slot=owner.inventory.getStackInSlot(i);
            if(slot==null)remaining-=Math.min(stack.getMaxStackSize(),owner.inventory.getInventoryStackLimit());
            else if(!newIdentity && slot.getItem()==stack.getItem() && slot.getItemDamage()==stack.getItemDamage() && ItemStack.areItemStackTagsEqual(slot,stack))
                remaining-=Math.max(0,Math.min(slot.getMaxStackSize(),owner.inventory.getInventoryStackLimit())-slot.stackSize);
            if(remaining<=0)return true;
        }
        return false;
    }
    @Override public ItemStack slotClick(int slot,int button,int mode,EntityPlayer player) {
        if (!canInteractWith(player) || slot>=inventorySlots.size()) return null;
        // Vanilla swaps a COPY into the hotbar before onPickup; a later new-map ID would
        // then update only the discarded preview. Finalize before installing the output.
        if(slot==2 && mode==2 && button>=0 && button<9) {
            Slot output=getSlot(2);if(!output.getHasStack() || !output.canTakeStack(player))return null;
            ItemStack previous=player.inventory.getStackInSlot(button);int empty=player.inventory.getFirstEmptyStack();
            if(previous!=null && empty<0)return null;
            ItemStack crafted=output.getStack().copy();result.setInventorySlotContents(0,null);completeCraft(player,crafted);
            if(previous!=null)player.inventory.setInventorySlotContents(empty,previous);
            player.inventory.setInventorySlotContents(button,crafted);return null;
        }
        if(slot==2 && mode==3)return null; // Never export a pending-operation preview through creative clone.
        return super.slotClick(slot,button,mode,player);
    }
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int index) {
        if(!canInteractWith(player) || index<0 || index>=inventorySlots.size())return null;
        Slot slot=(Slot)inventorySlots.get(index);if(!slot.getHasStack() || !slot.canTakeStack(player))return null;
        ItemStack current=slot.getStack(),original=current.copy();
        if(index==2) {
            boolean newIdentity=!"copy".equals(CartographyRecipes.operation(world,input.getStackInSlot(0),input.getStackInSlot(1)));
            if(!canFit(original,newIdentity))return null;
            ItemStack moved=original.copy();result.setInventorySlotContents(0,null);completeCraft(player,moved);
            if(!mergeItemStack(moved,3,39,true) || moved.stackSize!=0)throw new IllegalStateException("Validated cartography capacity changed");
            refresh();return original;
        }
        boolean moved;
        if(index<2)moved=mergeItemStack(current,3,39,true);
        else {
            moved=current.getItem()==net.minecraft.init.Items.filled_map?mergeItemStack(current,0,1,false)
                :CartographyRecipes.addition(current) && mergeItemStack(current,1,2,false);
            if(!moved)moved=index<30?mergeItemStack(current,30,39,false):mergeItemStack(current,3,30,false);
        }
        if(!moved)return null;
        if(current.stackSize<=0)slot.putStack(null);else slot.onSlotChanged();
        slot.onPickupFromSlot(player,current);refresh();return original;
    }
    @Override public boolean func_94530_a(ItemStack stack,Slot slot) { return slot.inventory!=result && super.func_94530_a(stack,slot); }
    @Override public boolean canDragIntoSlot(Slot slot) { return slot.inventory!=result && super.canDragIntoSlot(slot); }
    @Override public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        result.setInventorySlotContents(0,null);
        for(int i=0;i<2;i++) {
            ItemStack remaining=input.getStackInSlotOnClosing(i);
            if(!world.isRemote && remaining!=null) {
                if(!player.isEntityAlive() || !player.inventory.addItemStackToInventory(remaining))player.dropPlayerItemWithRandomChoice(remaining,false);
            }
        }
    }
}
