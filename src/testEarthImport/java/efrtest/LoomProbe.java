package efrtest;

import com.mojang.authlib.GameProfile;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.inventory.ContainerLoom;
import ganymedes01.etfuturum.recipes.LoomRecipes;
import ganymedes01.etfuturum.tileentities.TileEntityBanner;
import ganymedes01.etfuturum.tileentities.TileEntityBanner.EnumBannerPattern;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import java.util.*;
import static efrtest.EarthImportContracts.check;

public final class LoomProbe {
    private static final int X=-148,Y=128,Z=112;
    private static void clear(FakePlayer p) { for(int i=0;i<p.inventory.getSizeInventory();i++)p.inventory.setInventorySlotContents(i,null);p.inventory.setItemStack(null); }
    private static ContainerLoom table(FakePlayer p,WorldServer w) { return new ContainerLoom(p.inventory,w,X,Y,Z); }
    private static ItemStack banner() { ItemStack s=new ItemStack(ModBlocks.BANNER.getItem(),1,15);s.setStackDisplayName("Independent banner");s.getTagCompound().setString("Custom","retained");return s; }
    private static void input(ContainerLoom c,int count,int color,int template) {
        ItemStack b=banner();b.stackSize=count;c.input.setInventorySlotContents(0,b);
        c.input.setInventorySlotContents(1,new ItemStack(Items.dye,count,color));
        c.input.setInventorySlotContents(2,template<0?null:new ItemStack(ModItems.BANNER_PATTERN.get(),1,template));
    }
    private static void layer(ItemStack s,String id,int color) {
        check(s!=null && s.stackSize==1,"missing loom output");
        NBTTagList layers=s.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns",10);
        check(layers.tagCount()==1 && layers.getCompoundTagAt(0).getString("Pattern").equals(id) && layers.getCompoundTagAt(0).getInteger("Color")==color,"incorrect banner pattern/color");
        TileEntityBanner tile=new TileEntityBanner();tile.setItemValues(s);
        check(tile.getPatternList().get(1).getPatternID().equals(id) && tile.getColorList().get(1).ordinal()==15-color,"banner renderer color/pattern contract");
        NBTTagCompound persisted=new NBTTagCompound();tile.writeToNBT(persisted);TileEntityBanner loaded=new TileEntityBanner();loaded.readFromNBT(persisted);
        check(loaded.getPatternList().get(1).getPatternID().equals(id) && loaded.getColorList().get(1).ordinal()==15-color,"banner NBT reload");
        check(s.getDisplayName().equals("Independent banner") && s.getTagCompound().getString("Custom").equals("retained"),"banner NBT lost");
    }
    private static final class Sync implements ICrafting {
        final ContainerLoom client;Sync(ContainerLoom c) { client=c; }
        public void sendContainerAndContentsToPlayer(Container s,List stacks) { for(int i=0;i<stacks.size();i++)sendSlotContents(s,i,(ItemStack)stacks.get(i)); }
        public void sendSlotContents(Container s,int slot,ItemStack stack) { client.getSlot(slot).putStack(stack==null?null:stack.copy()); }
        public void sendProgressBarUpdate(Container s,int id,int value) { client.updateProgressBar(id,value); }
    }
    public static void run(WorldServer world,boolean legacy) {
        if(legacy) { check(!LoomRecipes.enabled(),"loom bypasses legacy gate");System.out.println("LOOM_LEGACY PASSED");return; }
        FakePlayer p=FakePlayerFactory.get(world,new GameProfile(UUID.fromString("ba5917d8-1c33-4a62-8731-250cb403e225"),"loom_probe"));
        p.setPosition(X+.5,Y,Z+.5);world.setBlock(X,Y,Z,ModBlocks.LOOM.get());int cases=0;
        for(int color=0;color<16;color++)for(int template=-1;template<10;template++) {
            ContainerLoom seed=table(p,world);input(seed,1,color,template);
            check(seed.getRecipes().size()==(template<0?32:1),"loom choice catalog mismatch");
            for(int i=0;i<seed.getRecipes().size();i++) {
                clear(p);ContainerLoom c=table(p,world);input(c,2,color,template);
                check(c.enchantItem(p,i),"selection rejected");String id=c.getRecipes().get(i).getPatternID();
                ItemStack source=c.input.getStackInSlot(0).copy();layer(c.getSlot(3).getStack(),id,15-color);
                c.slotClick(3,cases%2,0,p);layer(p.inventory.getItemStack(),id,15-color);
                check(c.input.getStackInSlot(0).stackSize==1 && c.input.getStackInSlot(1).stackSize==1,"loom must consume one banner and dye");
                check(ItemStack.areItemStackTagsEqual(source,c.input.getStackInSlot(0)) && TileEntityBanner.getPatterns(source)==0,"loom mutated source");
                if(template>=0)check(c.input.getStackInSlot(2).stackSize==1,"template consumed");
                c.onContainerClosed(p);clear(p);cases++;
            }
        }
        ContainerLoom c=table(p,world);input(c,16,1,-1);c.enchantItem(p,0);c.slotClick(3,0,1,p);
        int total=0;for(ItemStack s:p.inventory.mainInventory)if(s!=null && s.getItem()==ModBlocks.BANNER.getItem())total+=s.stackSize;
        check(total==16 && c.input.getStackInSlot(0)==null && c.input.getStackInSlot(1)==null,"bulk loom consumption");clear(p);
        c=table(p,world);input(c,2,4,1);c.enchantItem(p,0);
        for(int i=0;i<36;i++)p.inventory.setInventorySlotContents(i,new ItemStack(Blocks.cobblestone,64));
        c.slotClick(3,0,1,p);check(c.input.getStackInSlot(0).stackSize==2 && c.input.getStackInSlot(1).stackSize==2,"full inventory consumed input");clear(p);
        check(!c.enchantItem(p,-1) && !c.enchantItem(p,127),"invalid selection accepted");
        c.input.setInventorySlotContents(2,new ItemStack(Items.paper));check(c.getSlot(3).getStack()==null && c.getRecipes().isEmpty(),"invalid template accepted");
        c.input.setInventorySlotContents(2,null);c.input.setInventorySlotContents(1,new ItemStack(Items.stick));check(c.getRecipes().isEmpty(),"non-dye accepted");
        input(c,1,3,-1);ItemStack b=banner();
        for(int i=0;i<6;i++)b=LoomRecipes.apply(b,0,EnumBannerPattern.CROSS);
        c.input.setInventorySlotContents(0,b);check(c.getRecipes().isEmpty() && c.getSlot(3).getStack()==null && LoomRecipes.apply(b,0,EnumBannerPattern.CROSS)==null,"six layer limit");
        input(c,2,2,0);ContainerLoom client=table(p,world);c.addCraftingToCrafters(new Sync(client));c.detectAndSendChanges();
        check(client.getSelected()==0 && ItemStack.areItemStacksEqual(client.getSlot(3).getStack(),c.getSlot(3).getStack()),"loom slot/selection sync");
        c.onContainerClosed(p);int returned=0;for(ItemStack s:p.inventory.mainInventory)if(s!=null)returned+=s.stackSize;
        c.onContainerClosed(p);int again=0;for(ItemStack s:p.inventory.mainInventory)if(s!=null)again+=s.stackSize;
        check(returned==5 && again==returned && c.getSlot(3).getStack()==null,"loom close did not return only inputs");clear(p);
        c=table(p,world);input(c,1,1,-1);p.setPosition(X+20,Y,Z);check(!c.enchantItem(p,0),"distant player used loom");p.setPosition(X+.5,Y,Z+.5);
        world.setBlock(X,Y,Z,Blocks.air);check(!c.enchantItem(p,0) && c.slotClick(3,0,0,p)==null,"removed loom usable");c.onContainerClosed(p);clear(p);
        System.out.println("LOOM PASSED: "+cases+" pattern/color extractions, templates retained, NBT, six-layer limit, bulk/full inventory, invalid inputs, sync, close and range");
    }
}
