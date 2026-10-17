package ganymedes01.etfuturum.inventory;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.recipes.GrindstoneRecipes;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import java.util.Collections;
import java.util.List;

/** Player-owned banner, dye and reusable template slots; server-validated pattern selection. */
public final class ContainerGrindstone extends Container {
    private final EntityPlayer owner;
    private final World world;
    private final int x,y,z;
    public final InventoryBasic input=new InventoryBasic("Grindstone",false,2) {
        @Override public void markDirty() { super.markDirty();ContainerGrindstone.this.onCraftMatrixChanged(this); }
    };
    private final InventoryCraftResult result=new InventoryCraftResult();
    public ContainerGrindstone(InventoryPlayer inventory,World world,int x,int y,int z) {
        this.owner=inventory.player;this.world=world;this.x=x;this.y=y;this.z=z;
        for(int i=0;i<2;i++)addSlotToContainer(new Slot(input,i,49,19+i*21) {
            @Override public boolean isItemValid(ItemStack stack) { return GrindstoneRecipes.accepts(stack); }
        });
        addSlotToContainer(new Slot(result,0,129,34) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
            @Override public boolean canTakeStack(EntityPlayer player) {
                ItemStack expected=preview();
                return canInteractWith(player) && expected!=null && ItemStack.areItemStacksEqual(getStack(),expected);
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
        return player==owner && !ganymedes01.etfuturum.api.spectator.SpectatorUtils.isSpectator(player) && GrindstoneRecipes.enabled() && world.getBlock(x,y,z)==ModernMapParityBlocks.GRINDSTONE.get()
                && player.getDistanceSq(x+.5D,y+.5D,z+.5D)<=64D;
    }
    private ItemStack preview() { return GrindstoneRecipes.result(input.getStackInSlot(0),input.getStackInSlot(1)); }
    private void refresh() { result.setInventorySlotContents(0,preview()); }
    @Override public void onCraftMatrixChanged(IInventory inventory) { refresh();super.onCraftMatrixChanged(inventory); }
    private void completeCraft(EntityPlayer player,ItemStack crafted) {
        int value=GrindstoneRecipes.experienceValue(input.getStackInSlot(0),input.getStackInSlot(1));
        input.setInventorySlotContents(0,null);input.setInventorySlotContents(1,null);refresh();
        if(!world.isRemote) {
            if(value>0) {
                int half=value/2+value%2,xp=half+world.rand.nextInt(half);
                while(xp>0) { int part=EntityXPOrb.getXPSplit(xp);xp-=part;world.spawnEntityInWorld(new EntityXPOrb(world,x+.5D,y+.5D,z+.5D,part)); }
            }
            crafted.onCrafting(world,player,crafted.stackSize);
            world.playSoundEffect(x+.5D,y+.5D,z+.5D,Tags.MC_ASSET_VER+":block.grindstone.use",1F,1F);
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
        if(index==2) {
            if(!canFit(original))return null;
            ItemStack moved=original.copy();if(!mergeItemStack(moved,3,39,true) || moved.stackSize!=0)return null;
            result.setInventorySlotContents(0,null);completeCraft(player,original);return original;
        }
        boolean moved;
        if(index<2)moved=mergeItemStack(current,3,39,true);
        else {
            moved=GrindstoneRecipes.accepts(current) && mergeItemStack(current,0,2,false);
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
