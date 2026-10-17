package ganymedes01.etfuturum.inventory;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.recipes.ModernSmithingRecipes;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ContainerSmithingTable extends Container {
    private final World world;
    private final EntityPlayer owner;
    private final int x,y,z;
    private boolean bound=true;
    public final InventoryCrafting inputMatrix=new InventoryCrafting(this,3,1);
    private final InventoryCraftResult resultInventory=new InventoryCraftResult();
    /** Retained for API callers embedding the container themselves. Block GUI uses the bound constructor. */
    public ContainerSmithingTable(InventoryPlayer inv,World world) { this(inv,world,0,0,0);bound=false; }
    public ContainerSmithingTable(InventoryPlayer inv,World world,int x,int y,int z) {
        this.owner=inv.player;this.world=world;this.x=x;this.y=y;this.z=z;
        for(int i=0;i<3;i++) {
            final int slot=i;addSlotToContainer(new Slot(inputMatrix,i,8+i*18,48) {
                @Override public boolean isItemValid(ItemStack stack) { return ModernSmithingRecipes.accepts(stack,slot); }
            });
        }
        addSlotToContainer(new Slot(resultInventory,0,98,48) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
            @Override public boolean canTakeStack(EntityPlayer player) { ItemStack expected=preview();return canInteractWith(player) && (world.isRemote?getHasStack():expected!=null && ItemStack.areItemStacksEqual(getStack(),expected)); }
            @Override public void onPickupFromSlot(EntityPlayer player,ItemStack stack) { consume(player,stack);super.onPickupFromSlot(player,stack); }
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlotToContainer(new Slot(inv,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlotToContainer(new Slot(inv,col,8+col*18,142));refresh();
    }
    @Override public boolean canInteractWith(EntityPlayer player) { return player==owner && ModernSmithingRecipes.enabled() && !ganymedes01.etfuturum.api.spectator.SpectatorUtils.isSpectator(player)
        && (!bound || world.getBlock(x,y,z)==ModBlocks.SMITHING_TABLE.get() && player.getDistanceSq(x+.5,y+.5,z+.5)<=64); }
    private ItemStack preview() { return ModernSmithingRecipes.result(inputMatrix.getStackInSlot(0),inputMatrix.getStackInSlot(1),inputMatrix.getStackInSlot(2),world); }
    private void refresh() { if(world!=null && !world.isRemote)resultInventory.setInventorySlotContents(0,preview()); }
    @Override public void onCraftMatrixChanged(IInventory inv) { refresh();super.onCraftMatrixChanged(inv); }
    @Override public void detectAndSendChanges() { refresh();super.detectAndSendChanges(); }
    private void consume(EntityPlayer player,ItemStack crafted) {
        for(int i=0;i<3;i++)if(inputMatrix.getStackInSlot(i)!=null)inputMatrix.decrStackSize(i,1);
        refresh();if(!world.isRemote) { crafted.onCrafting(world,player,crafted.stackSize);world.playSoundEffect(player.posX,player.posY,player.posZ,Tags.MC_ASSET_VER+":block.smithing_table.use",1F,1F); }
    }
    private boolean fits(ItemStack result) {
        int n=result.stackSize;for(int i=0;i<36;i++) { ItemStack old=owner.inventory.getStackInSlot(i);
            if(old==null)n-=Math.min(result.getMaxStackSize(),owner.inventory.getInventoryStackLimit());
            else if(old.isItemEqual(result) && ItemStack.areItemStackTagsEqual(old,result))n-=Math.max(0,Math.min(old.getMaxStackSize(),owner.inventory.getInventoryStackLimit())-old.stackSize);
            if(n<=0)return true;
        }return false;
    }
    @Override public ItemStack slotClick(int slot,int button,int mode,EntityPlayer player) { if(!canInteractWith(player) || slot>=inventorySlots.size())return null;return super.slotClick(slot,button,mode,player); }
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int index) {
        if(!canInteractWith(player) || index<0 || index>=40)return null;Slot slot=getSlot(index);if(!slot.getHasStack() || !slot.canTakeStack(player))return null;
        ItemStack current=slot.getStack(),old=current.copy();
        if(index==3) { if(!fits(old))return null;ItemStack moved=old.copy();if(!mergeItemStack(moved,4,40,true) || moved.stackSize!=0)return null;resultInventory.setInventorySlotContents(0,null);consume(player,old);return old; }
        boolean moved=false;
        if(index<3)moved=mergeItemStack(current,4,40,true);
        else {
            for(int i=0;i<3;i++)if(ModernSmithingRecipes.accepts(current,i) && mergeItemStack(current,i,i+1,false)) { moved=true;break; }
            if(!moved)moved=index<31?mergeItemStack(current,31,40,false):mergeItemStack(current,4,31,false);
        }
        if(!moved)return null;if(current.stackSize<=0)slot.putStack(null);else slot.onSlotChanged();refresh();return old;
    }
    @Override public boolean func_94530_a(ItemStack stack,Slot slot) { return slot.inventory!=resultInventory && super.func_94530_a(stack,slot); }
    @Override public boolean canDragIntoSlot(Slot slot) { return slot.inventory!=resultInventory && super.canDragIntoSlot(slot); }
    @Override public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);resultInventory.setInventorySlotContents(0,null);
        for(int i=0;i<3;i++) { ItemStack stack=inputMatrix.getStackInSlotOnClosing(i);if(!world.isRemote && stack!=null && (!player.isEntityAlive() || !player.inventory.addItemStackToInventory(stack)))player.dropPlayerItemWithRandomChoice(stack,false); }
    }
    public boolean unable() { return (inputMatrix.getStackInSlot(0)!=null && inputMatrix.getStackInSlot(1)!=null && inputMatrix.getStackInSlot(2)!=null) && !getSlot(3).getHasStack(); }
}
