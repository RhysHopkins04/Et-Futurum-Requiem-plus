package ganymedes01.etfuturum.inventory;

import ganymedes01.etfuturum.tileentities.TileEntityCrafter;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;

public final class ContainerCrafter extends Container {
    public final TileEntityCrafter crafter;
    private final EntityPlayer owner;
    private final InventoryCraftResult preview=new InventoryCraftResult();
    public int disabledMask;
    public boolean powered;
    public ContainerCrafter(InventoryPlayer inventory,TileEntityCrafter crafter) {
        this.owner=inventory.player;this.crafter=crafter;
        for(int i=0;i<9;i++)addSlotToContainer(new Slot(crafter,i,26+(i%3)*18,17+(i/3)*18) {
            @Override public boolean isItemValid(ItemStack stack) { return !isDisabled(getSlotIndex()) && super.isItemValid(stack); }
        });
        addSlotToContainer(new Slot(preview,0,134,35) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
            @Override public boolean canTakeStack(EntityPlayer player) { return false; }
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlotToContainer(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlotToContainer(new Slot(inventory,col,8+col*18,142));
        disabledMask=crafter.disabledMask();powered=crafter.isTriggered();refresh();
    }
    public boolean isDisabled(int slot) { return (disabledMask&(1<<slot))!=0; }
    private void refresh() { if(crafter.getWorldObj()!=null && !crafter.getWorldObj().isRemote) { disabledMask=crafter.disabledMask();powered=crafter.isTriggered();preview.setInventorySlotContents(0,crafter.preview()); } }
    @Override public boolean canInteractWith(EntityPlayer player) { return player==owner && !ganymedes01.etfuturum.api.spectator.SpectatorUtils.isSpectator(player) && crafter.isUseableByPlayer(player); }
    @Override public void detectAndSendChanges() { refresh();super.detectAndSendChanges();for(Object listener:crafters) { ((ICrafting)listener).sendProgressBarUpdate(this,0,disabledMask);((ICrafting)listener).sendProgressBarUpdate(this,1,powered?1:0); } }
    @Override public void updateProgressBar(int id,int value) { if(id==0)disabledMask=value&511;else if(id==1)powered=value!=0; }
    @Override public boolean enchantItem(EntityPlayer player,int slot) {
        if(!canInteractWith(player) || slot<0 || slot>=9 || !player.capabilities.allowEdit)return false;
        if(crafter.getWorldObj().isRemote)return true;
        boolean changed=crafter.setDisabled(slot,!crafter.isDisabled(slot));detectAndSendChanges();return changed;
    }
    @Override public ItemStack slotClick(int slot,int button,int mode,EntityPlayer player) {
        if(!canInteractWith(player) || slot==9 || slot>=inventorySlots.size())return null;
        if(slot>=0 && slot<9 && isDisabled(slot) && player.inventory.getItemStack()!=null)return null;
        ItemStack result=super.slotClick(slot,button,mode,player);refresh();return result;
    }
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int index) {
        if(!canInteractWith(player) || index<0 || index>=46 || index==9)return null;
        Slot slot=getSlot(index);if(!slot.getHasStack())return null;ItemStack stack=slot.getStack(),old=stack.copy();
        boolean moved=index<9?mergeItemStack(stack,10,46,true):mergeItemStack(stack,0,9,false);
        if(!moved)return null;if(stack.stackSize<=0)slot.putStack(null);else slot.onSlotChanged();refresh();return old;
    }
    @Override public boolean canDragIntoSlot(Slot slot) { return slot.inventory!=preview && (slot.inventory!=crafter || !isDisabled(slot.getSlotIndex())); }
    @Override public boolean func_94530_a(ItemStack stack,Slot slot) { return slot.inventory!=preview && super.func_94530_a(stack,slot); }
}
