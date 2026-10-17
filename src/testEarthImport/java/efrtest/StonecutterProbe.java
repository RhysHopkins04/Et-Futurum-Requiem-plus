package efrtest;

import com.mojang.authlib.GameProfile;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.inventory.ContainerStonecutter;
import ganymedes01.etfuturum.recipes.StonecutterRecipes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import java.util.List;
import java.util.UUID;
import static efrtest.EarthImportContracts.check;

/** Real window click/extraction paths, including atomic multi-output crafting and hostile selections. */
public final class StonecutterProbe {
    private static final int X=-144,Y=128,Z=112;
    private static void clear(FakePlayer p) { for(int i=0;i<p.inventory.getSizeInventory();i++)p.inventory.setInventorySlotContents(i,null);p.inventory.setItemStack(null); }
    private static void equals(ItemStack actual,ItemStack expected,String why) { check(ItemStack.areItemStacksEqual(actual,expected),why+": "+actual+" expected "+expected); }
    private static ContainerStonecutter table(FakePlayer p,WorldServer world) { return new ContainerStonecutter(p.inventory,world,X,Y,Z); }
    private static int select(ContainerStonecutter c,FakePlayer p,StonecutterRecipes.Recipe recipe) {
        for(int i=0;i<c.getRecipes().size();i++)if(c.getRecipes().get(i).getId().equals(recipe.getId())) {
            check(c.enchantItem(p,i),"stonecutter selection failed "+recipe.getId());return i;
        }
        throw new AssertionError("missing stonecutter choice "+recipe.getId());
    }
    private static int count(FakePlayer p,ItemStack target) {
        int n=0;for(ItemStack item:p.inventory.mainInventory)if(item!=null && item.getItem()==target.getItem() && item.getItemDamage()==target.getItemDamage())n+=item.stackSize;
        return n;
    }
    private static int drops(WorldServer world,Item item) {
        int count=0;for(Object e:world.loadedEntityList)if(e instanceof EntityItem) {
            EntityItem drop=(EntityItem)e;
            if(!drop.isDead && Math.abs(drop.posX-X-.5)<2 && Math.abs(drop.posZ-Z-.5)<2 && drop.getEntityItem().getItem()==item)count+=drop.getEntityItem().stackSize;
        }return count;
    }
    private static final class Sync implements ICrafting {
        final ContainerStonecutter client;
        Sync(ContainerStonecutter client) { this.client=client; }
        @Override public void sendContainerAndContentsToPlayer(Container server,List stacks) {
            for(int i=0;i<stacks.size();i++)sendSlotContents(server,i,(ItemStack)stacks.get(i));
        }
        @Override public void sendSlotContents(Container server,int slot,ItemStack stack) { client.getSlot(slot).putStack(stack==null?null:stack.copy()); }
        @Override public void sendProgressBarUpdate(Container server,int id,int value) { client.updateProgressBar(id,value); }
    }
    public static void run(WorldServer world,boolean legacy) {
        if(legacy) {
            check(!StonecutterRecipes.enabled() && StonecutterRecipes.getAll().isEmpty(),"stonecutter bypasses legacy recipe suppression");
            System.out.println("STONECUTTER_LEGACY PASSED");return;
        }
        check(StonecutterRecipes.enabled() && StonecutterRecipes.getAll().size()==254,"expected all 254 stonecutting recipes");
        FakePlayer player=FakePlayerFactory.getMinecraft(world);clear(player);player.capabilities.isCreativeMode=false;
        player.setPosition(X+.5D,Y+.5D,Z+.5D);world.setBlock(X,Y-1,Z,Blocks.stone);world.setBlock(X,Y,Z,ModBlocks.STONECUTTER.get(),0,3);
        check(!world.getBlock(X,Y,Z).hasTileEntity(0),"stonecutter must not introduce a saved block inventory");
        StonecutterRecipes.Recipe slabs=null;
        for(StonecutterRecipes.Recipe recipe:StonecutterRecipes.getAll()) {
            ContainerStonecutter c=table(player,world);ItemStack input=recipe.getInput();input.stackSize=2;c.input.setInventorySlotContents(0,input);
            check(c.getSlot(1).getStack()==null,"stonecutter auto-selected a recipe");select(c,player,recipe);
            c.slotClick(1,0,0,player);equals(player.inventory.getItemStack(),recipe.getOutput(),"left-click result "+recipe.getId());
            check(c.input.getStackInSlot(0)!=null && c.input.getStackInSlot(0).stackSize==1,"one input per stonecutting operation");
            player.inventory.setItemStack(null);c.slotClick(1,1,0,player);
            equals(player.inventory.getItemStack(),recipe.getOutput(),"right-click must take the full recipe output "+recipe.getId());
            check(c.input.getStackInSlot(0)==null && c.getSlot(1).getStack()==null,"last input/result not cleared");
            player.inventory.setItemStack(null);c.onContainerClosed(player);
            if(recipe.getInput().getItem()==Item.getItemFromBlock(Blocks.stone) && recipe.getOutput().stackSize==2)slabs=recipe;
        }
        check(slabs!=null,"missing two-slab stone recipe");
        ContainerStonecutter c=table(player,world);ItemStack stone=slabs.getInput();c.input.setInventorySlotContents(0,stone.copy());
        check(!c.enchantItem(player,-1) && !c.enchantItem(player,128) && !c.enchantItem(player,Integer.MAX_VALUE),"stonecutter accepted invalid packet button");
        select(c,player,slabs);c.input.setInventorySlotContents(0,new ItemStack(Blocks.sand,3));
        check(c.getSelected()==-1 && c.getSlot(1).getStack()==null,"input change retained stale selection/output");
        c.slotClick(1,0,0,player);check(player.inventory.getItemStack()==null && c.input.getStackInSlot(0).stackSize==3,"stale preview crafted");
        check(c.slotClick(9999,0,0,player)==null,"invalid slot index accepted");
        c.input.setInventorySlotContents(0,stone.copy());select(c,player,slabs);
        player.setPosition(X+100,Y,Z);check(!c.canInteractWith(player) && !c.enchantItem(player,0),"remote stonecutting permitted");
        c.slotClick(1,0,0,player);check(c.input.getStackInSlot(0)!=null && player.inventory.getItemStack()==null,"remote extraction consumed input");
        player.setPosition(X+.5D,Y+.5D,Z+.5D);world.setBlockToAir(X,Y,Z);check(!c.canInteractWith(player),"removed workstation still usable");
        world.setBlock(X,Y,Z,ModBlocks.STONECUTTER.get(),0,3);c.onContainerClosed(player);clear(player);
        c=table(player,world);c.input.setInventorySlotContents(0,stone.copy());select(c,player,slabs);
        for(int i=0;i<36;i++)player.inventory.setInventorySlotContents(i,new ItemStack(Items.diamond,64));
        c.slotClick(1,0,1,player);check(c.input.getStackInSlot(0)!=null,"full inventory consumed stonecutter input");
        ItemStack almostFull=slabs.getOutput();almostFull.stackSize=63;player.inventory.setInventorySlotContents(0,almostFull);
        c.slotClick(1,0,1,player);check(c.input.getStackInSlot(0)!=null && almostFull.stackSize==63,"partial space lost one slab");
        almostFull.stackSize=62;c.slotClick(1,0,1,player);check(c.input.getStackInSlot(0)==null && almostFull.stackSize==64,"two-slab atomic shift-click failed");clear(player);
        c=table(player,world);ItemStack batch=stone.copy();batch.stackSize=32;c.input.setInventorySlotContents(0,batch);select(c,player,slabs);
        c.slotClick(1,0,1,player);check(c.input.getStackInSlot(0)==null && count(player,slabs.getOutput())==64,"bulk shift-click count/consumption");clear(player);
        c=table(player,world);batch=stone.copy();batch.stackSize=7;player.inventory.setInventorySlotContents(9,batch);c.transferStackInSlot(player,2);
        check(c.input.getStackInSlot(0)!=null && c.input.getStackInSlot(0).stackSize==7 && player.inventory.getStackInSlot(9)==null,"shift-click input routing");
        c.onContainerClosed(player);check(count(player,stone)==7,"close did not return input");c.onContainerClosed(player);check(count(player,stone)==7,"repeated close duplicated input");clear(player);
        c=table(player,world);batch=stone.copy();batch.stackSize=3;c.input.setInventorySlotContents(0,batch);select(c,player,slabs);
        for(int i=0;i<36;i++)player.inventory.setInventorySlotContents(i,new ItemStack(Items.diamond,64));int before=drops(world,stone.getItem());c.onContainerClosed(player);
        check(drops(world,stone.getItem())==before+3,"close with full inventory lost input");check(count(player,slabs.getOutput())==0,"close dropped a free preview result");clear(player);
        c=table(player,world);batch=stone.copy();batch.stackSize=2;NBTTagCompound tag=new NBTTagCompound();tag.setString("StonecutterProbe","keep remaining input");batch.setTagCompound(tag);
        c.input.setInventorySlotContents(0,batch);select(c,player,slabs);c.slotClick(1,0,0,player);
        check(c.input.getStackInSlot(0).getTagCompound().equals(tag),"remaining input NBT changed");player.inventory.setItemStack(null);c.onContainerClosed(player);clear(player);
        ContainerStonecutter server=table(player,world),client=table(player,world);server.addCraftingToCrafters(new Sync(client));
        server.input.setInventorySlotContents(0,stone.copy());select(server,player,slabs);
        check(client.getSelected()==server.getSelected() && ItemStack.areItemStacksEqual(client.getSlot(1).getStack(),slabs.getOutput()),"selection/result sync failed");
        server.input.setInventorySlotContents(0,new ItemStack(Blocks.sand));server.detectAndSendChanges();
        check(client.getSelected()==-1 && client.getSlot(1).getStack()==null,"stale client selection survived input sync");server.onContainerClosed(player);clear(player);
        FakePlayer other=FakePlayerFactory.get(world,new GameProfile(UUID.fromString("82591946-23c6-4d59-82ba-a63e66a4c09e"),"StonecutterProbe"));
        clear(other);other.setPosition(X+.5D,Y+.5D,Z+.5D);ContainerStonecutter first=table(player,world),second=table(other,world);
        first.input.setInventorySlotContents(0,stone.copy());check(second.input.getStackInSlot(0)==null && !first.canInteractWith(other),"stonecutter inventories not player-isolated");
        first.onContainerClosed(player);second.onContainerClosed(other);clear(player);clear(other);world.setBlockToAir(X,Y,Z);
        System.out.println("STONECUTTER PASSED: all 254 routes left/right extraction; atomic slabs, bulk/full-inventory shift-click, packets, stale selection, range, block removal, NBT, close/drop, sync and player isolation");
    }
}
