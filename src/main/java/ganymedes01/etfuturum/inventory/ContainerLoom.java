package ganymedes01.etfuturum.inventory;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.recipes.LoomRecipes;
import ganymedes01.etfuturum.tileentities.TileEntityBanner.EnumBannerPattern;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import java.util.Collections;
import java.util.List;

/** Player-owned banner, dye and reusable template slots; server-validated pattern selection. */
public final class ContainerLoom extends Container {
    private final EntityPlayer owner;
    private final World world;
    private final int x,y,z;
    public final InventoryBasic input=new InventoryBasic("Loom",false,3) {
        @Override public void markDirty() { super.markDirty();ContainerLoom.this.onCraftMatrixChanged(this); }
    };
    private final InventoryCraftResult result=new InventoryCraftResult();
    private List<EnumBannerPattern> choices=Collections.emptyList();
    private ItemStack lastInput,lastDye,lastTemplate;
    private int selected=-1,lastSent=-2;
    public ContainerLoom(InventoryPlayer inventory,World world,int x,int y,int z) {
        this.owner=inventory.player;this.world=world;this.x=x;this.y=y;this.z=z;
        addSlotToContainer(new Slot(input,0,13,26) {
            @Override public boolean isItemValid(ItemStack s) { return s.getItem()==ModBlocks.BANNER.getItem(); }
        });
        addSlotToContainer(new Slot(input,1,33,26) {
            @Override public boolean isItemValid(ItemStack s) { return LoomRecipes.dye(s)>=0; }
        });
        addSlotToContainer(new Slot(input,2,23,45) {
            @Override public boolean isItemValid(ItemStack s) { return LoomRecipes.template(s); }
        });
        addSlotToContainer(new Slot(result,0,143,58) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
            @Override public boolean canTakeStack(EntityPlayer player) {
                EnumBannerPattern recipe=selectedRecipe();
                return canInteractWith(player) && recipe!=null && choices.contains(recipe)
                        && ItemStack.areItemStacksEqual(getStack(),output(recipe));
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
        return player==owner && LoomRecipes.enabled() && world.getBlock(x,y,z)==ModBlocks.LOOM.get()
                && player.getDistanceSq(x+.5D,y+.5D,z+.5D)<=64D;
    }
    private static ItemStack key(ItemStack stack) { if(stack==null || stack.stackSize<=0)return null;ItemStack key=stack.copy();key.stackSize=1;return key; }
    private void refresh() {
        ItemStack current=key(input.getStackInSlot(0));
        ItemStack dye=key(input.getStackInSlot(1)),template=key(input.getStackInSlot(2));
        if(!ItemStack.areItemStacksEqual(lastInput,current) || !ItemStack.areItemStacksEqual(lastDye,dye)
                || !ItemStack.areItemStacksEqual(lastTemplate,template)) {
            lastInput=current;lastDye=dye;lastTemplate=template;
            choices=LoomRecipes.choices(current,dye,template);selected=template!=null && choices.size()==1?0:-1;
        }
        EnumBannerPattern recipe=selectedRecipe();
        result.setInventorySlotContents(0,recipe==null?null:output(recipe));
    }
    private ItemStack output(EnumBannerPattern pattern) { return LoomRecipes.apply(input.getStackInSlot(0),LoomRecipes.dye(input.getStackInSlot(1)),pattern); }
    private EnumBannerPattern selectedRecipe() { return selected>=0 && selected<choices.size()?choices.get(selected):null; }
    public List<EnumBannerPattern> getRecipes() { return choices; }
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
        EnumBannerPattern recipe=selectedRecipe();
        if(recipe==null || !canInteractWith(player) || !choices.contains(recipe)) { refresh();return; }
        input.decrStackSize(0,1);input.decrStackSize(1,1);input.markDirty();
        if(!world.isRemote) {
            crafted.onCrafting(world,player,crafted.stackSize);
            world.playSoundEffect(x+.5D,y+.5D,z+.5D,Tags.MC_ASSET_VER+":ui.loom.take_result",1F,1F);
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
        if(index==3) {
            if(!canFit(original))return null;
            ItemStack moved=original.copy();if(!mergeItemStack(moved,4,40,true) || moved.stackSize!=0)return null;
            result.setInventorySlotContents(0,null);completeCraft(player,original);return original;
        }
        boolean moved;
        if(index<3)moved=mergeItemStack(current,4,40,true);
        else {
            int target=current.getItem()==ModBlocks.BANNER.getItem()?0:LoomRecipes.dye(current)>=0?1:LoomRecipes.template(current)?2:-1;
            moved=target>=0 && mergeItemStack(current,target,target+1,false);
            if(!moved)moved=index<31?mergeItemStack(current,31,40,false):mergeItemStack(current,4,31,false);
        }
        if(!moved)return null;
        if(current.stackSize<=0)slot.putStack(null);else slot.onSlotChanged();
        slot.onPickupFromSlot(player,current);refresh();return original;
    }
    @Override public boolean func_94530_a(ItemStack stack,Slot slot) { return slot.inventory!=result && super.func_94530_a(stack,slot); }
    @Override public boolean canDragIntoSlot(Slot slot) { return slot.inventory!=result && super.canDragIntoSlot(slot); }
    @Override public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        result.setInventorySlotContents(0,null);selected=-1;choices=Collections.emptyList();lastInput=null;lastDye=null;lastTemplate=null;
        for(int i=0;i<3;i++) {
            ItemStack remaining=input.getStackInSlotOnClosing(i);
            if(!world.isRemote && remaining!=null) {
                if(!player.isEntityAlive() || !player.inventory.addItemStackToInventory(remaining))player.dropPlayerItemWithRandomChoice(remaining,false);
            }
        }
    }
}
