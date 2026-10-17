package ganymedes01.etfuturum.inventory;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.recipes.StonecutterRecipes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import java.util.Collections;
import java.util.List;

/** One player-owned input and an atomic recipe result, selected through the vanilla window protocol. */
public final class ContainerStonecutter extends Container {
    private final EntityPlayer owner;
    private final World world;
    private final int x,y,z;
    public final InventoryBasic input=new InventoryBasic("Stonecutter",false,1) {
        @Override public void markDirty() { super.markDirty();ContainerStonecutter.this.onCraftMatrixChanged(this); }
    };
    private final InventoryCraftResult result=new InventoryCraftResult();
    private List<StonecutterRecipes.Recipe> choices=Collections.emptyList();
    private ItemStack lastInput;
    private int selected=-1,lastSent=-2;
    public ContainerStonecutter(InventoryPlayer inventory,World world,int x,int y,int z) {
        this.owner=inventory.player;this.world=world;this.x=x;this.y=y;this.z=z;
        addSlotToContainer(new Slot(input,0,20,33));
        addSlotToContainer(new Slot(result,0,143,33) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
            @Override public boolean canTakeStack(EntityPlayer player) {
                StonecutterRecipes.Recipe recipe=selectedRecipe();
                return canInteractWith(player) && recipe!=null && recipe.matches(input.getStackInSlot(0))
                        && ItemStack.areItemStacksEqual(getStack(),recipe.getOutput());
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
        return player==owner && StonecutterRecipes.enabled() && world.getBlock(x,y,z)==ModBlocks.STONECUTTER.get()
                && player.getDistanceSq(x+.5D,y+.5D,z+.5D)<=64D;
    }
    private static ItemStack key(ItemStack stack) { if(stack==null || stack.stackSize<=0)return null;ItemStack key=stack.copy();key.stackSize=1;return key; }
    private void refresh() {
        ItemStack current=key(input.getStackInSlot(0));
        if(!ItemStack.areItemStacksEqual(lastInput,current)) {
            lastInput=current;choices=StonecutterRecipes.find(current);selected=-1;
        }
        StonecutterRecipes.Recipe recipe=selectedRecipe();
        result.setInventorySlotContents(0,recipe!=null && recipe.matches(input.getStackInSlot(0))?recipe.getOutput():null);
    }
    private StonecutterRecipes.Recipe selectedRecipe() { return selected>=0 && selected<choices.size()?choices.get(selected):null; }
    public List<StonecutterRecipes.Recipe> getRecipes() { return choices; }
    public int getSelected() { return selected; }
    @Override public void onCraftMatrixChanged(IInventory inventory) { refresh();super.onCraftMatrixChanged(inventory); }
    @Override public boolean enchantItem(EntityPlayer player,int button) {
        refresh();if(!canInteractWith(player) || button<0 || button>=choices.size())return false;
        selected=button;refresh();detectAndSendChanges();return true;
    }
    @Override public void addCraftingToCrafters(ICrafting crafter) { super.addCraftingToCrafters(crafter);crafter.sendProgressBarUpdate(this,0,selected); }
    @Override public void detectAndSendChanges() {
        refresh();super.detectAndSendChanges();
        if(selected!=lastSent) {
            for(Object crafter:crafters)((ICrafting)crafter).sendProgressBarUpdate(this,0,selected);
            lastSent=selected;
        }
    }
    @Override public void updateProgressBar(int id,int value) {
        if(id==0) { refresh();selected=value>=0 && value<choices.size()?value:-1;refresh(); }
    }
    private void completeCraft(EntityPlayer player,ItemStack crafted) {
        StonecutterRecipes.Recipe recipe=selectedRecipe();
        if(recipe==null || !canInteractWith(player) || !recipe.matches(input.getStackInSlot(0))) { refresh();return; }
        input.decrStackSize(0,1);input.markDirty();
        if(!world.isRemote) {
            crafted.onCrafting(world,player,crafted.stackSize);
            world.playSoundEffect(x+.5D,y+.5D,z+.5D,Tags.MC_ASSET_VER+":ui.stonecutter.take_result",1F,1F);
        }
    }
    private boolean canFit(ItemStack stack) {
        int remaining=stack.stackSize;
        for(int i=0;i<36;i++) {
            ItemStack slot=owner.inventory.getStackInSlot(i);
            if(slot==null)remaining-=Math.min(stack.getMaxStackSize(),owner.inventory.getInventoryStackLimit());
            else if(slot.getItem()==stack.getItem() && slot.getItemDamage()==stack.getItemDamage() && ItemStack.areItemStackTagsEqual(slot,stack))
                remaining-=Math.max(0,Math.min(slot.getMaxStackSize(),owner.inventory.getInventoryStackLimit())-slot.stackSize);
            if(remaining<=0)return true;
        }
        return false;
    }
    @Override public ItemStack slotClick(int slot,int button,int mode,EntityPlayer player) {
        if (!canInteractWith(player) || slot>=inventorySlots.size()) return null;
        return super.slotClick(slot,button,mode,player);
    }
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int index) {
        if(!canInteractWith(player) || index<0 || index>=inventorySlots.size())return null;
        Slot slot=(Slot)inventorySlots.get(index);if(!slot.getHasStack() || !slot.canTakeStack(player))return null;
        ItemStack current=slot.getStack(),original=current.copy();
        if(index==1) {
            if(!canFit(original))return null; // Never consume a block for only part of a two-slab result.
            ItemStack moved=original.copy();if(!mergeItemStack(moved,2,38,true) || moved.stackSize!=0)return null;
            result.setInventorySlotContents(0,null);completeCraft(player,original);return original;
        }
        boolean moved;
        if(index==0)moved=mergeItemStack(current,2,38,true);
        else {
            moved=!StonecutterRecipes.find(current).isEmpty() && mergeItemStack(current,0,1,false);
            if(!moved)moved=index<29?mergeItemStack(current,29,38,false):mergeItemStack(current,2,29,false);
        }
        if(!moved)return null;
        if(current.stackSize<=0)slot.putStack(null);else slot.onSlotChanged();
        slot.onPickupFromSlot(player,current);refresh();return original;
    }
    @Override public boolean func_94530_a(ItemStack stack,Slot slot) { return slot.inventory!=result && super.func_94530_a(stack,slot); }
    @Override public boolean canDragIntoSlot(Slot slot) { return slot.inventory!=result && super.canDragIntoSlot(slot); }
    @Override public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);ItemStack remaining=input.getStackInSlotOnClosing(0);
        result.setInventorySlotContents(0,null);selected=-1;choices=Collections.emptyList();lastInput=null;
        if(!world.isRemote && remaining!=null) {
            if(!player.isEntityAlive() || !player.inventory.addItemStackToInventory(remaining))player.dropPlayerItemWithRandomChoice(remaining,false);
        }
    }
}
