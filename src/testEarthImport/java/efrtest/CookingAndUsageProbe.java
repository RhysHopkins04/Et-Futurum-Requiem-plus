package efrtest;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.core.handlers.ServerEventHandler;
import ganymedes01.etfuturum.inventory.ContainerSmithingTable;
import ganymedes01.etfuturum.recipes.CampfireRecipes;
import ganymedes01.etfuturum.recipes.BlastFurnaceRecipes;
import ganymedes01.etfuturum.recipes.SmokerRecipes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import static efrtest.EarthImportContracts.check;

/** Real cooking ticks, survival interactions and smithing container movement. */
public final class CookingAndUsageProbe {
    private static final int X=-80,Y=128,Z=80;
    private static InventoryCrafting grid() { return new InventoryCrafting(new Container() {
        @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) { return true; }
    },3,3); }
    private static ItemStack craft(InventoryCrafting grid,WorldServer world) {
        return CraftingManager.getInstance().findMatchingRecipe(grid,world);
    }
    private static void output(ItemStack got,ItemStack expected,String message) {
        check(ItemStack.areItemStacksEqual(got,expected),message+": "+got+" expected "+expected);
    }
    private static int cookedItems(WorldServer world) {
        int count=0;
        for(Object e:world.loadedEntityList) if(e instanceof EntityItem) {
            EntityItem item=(EntityItem)e;
            if(!item.isDead && Math.abs(item.posX-X-.5)<2 && Math.abs(item.posZ-Z-.5)<2
                    && item.getEntityItem().getItem()==Items.cooked_beef)count+=item.getEntityItem().stackSize;
        }
        return count;
    }
    public static void run(WorldServer world) throws Exception {
        FakePlayer player=FakePlayerFactory.getMinecraft(world);
        player.capabilities.isCreativeMode=false;
        for(int i=0;i<player.inventory.getSizeInventory();i++)player.inventory.setInventorySlotContents(i,null);
        player.inventory.currentItem=0;
        for(ItemStack food:new ItemStack[]{new ItemStack(Items.beef),new ItemStack(Items.porkchop),new ItemStack(Items.chicken),
                new ItemStack(Items.potato),new ItemStack(Items.fish,1,0),new ItemStack(Items.fish,1,1),
                ModItems.MUTTON_RAW.newItemStack(),ModItems.RABBIT_RAW.newItemStack()}) {
            check(CampfireRecipes.getCookingResult(food)!=null,"campfire rejects cookable food "+food);
            check(SmokerRecipes.smelting().getSmeltingResult(food)!=null,"smoker rejects food "+food);
            check(BlastFurnaceRecipes.smelting().getSmeltingResult(food)==null,"blast furnace accepts food "+food);
        }
        for(ItemStack input:new ItemStack[]{new ItemStack(Blocks.iron_ore),new ItemStack(Blocks.gold_ore),new ItemStack(Blocks.sand),
                new ItemStack(Blocks.cobblestone),new ItemStack(Blocks.log),new ItemStack(Items.clay_ball)}) {
            check(CampfireRecipes.getCookingResult(input)==null,"campfire accepts non-food "+input);
            check(SmokerRecipes.smelting().getSmeltingResult(input)==null,"smoker accepts non-food "+input);
        }
        check(BlastFurnaceRecipes.smelting().getSmeltingResult(new ItemStack(Blocks.iron_ore))!=null,"blast furnace rejects iron ore");
        check(BlastFurnaceRecipes.smelting().getSmeltingResult(new ItemStack(Blocks.sand))==null,"blast furnace accepts glass recipe");
        net.minecraftforge.event.FuelBurnTimeEvent fuel=new net.minecraftforge.event.FuelBurnTimeEvent(ModItems.BAMBOO.newItemStack());
        ServerEventHandler.INSTANCE.fuelBurnTime(fuel);check(fuel.burnTime==50,"bamboo fuel remapping bypassed");
        fuel=new net.minecraftforge.event.FuelBurnTimeEvent(new ItemStack(ModBlocks.CRIMSON_STEM.get()));
        ServerEventHandler.INSTANCE.fuelBurnTime(fuel);check(fuel.burnTime==0,"nether stem incorrectly burns as furnace fuel");
        world.setBlock(X,Y-1,Z,Blocks.stone);
        for(ModernMapParityBlocks fire:new ModernMapParityBlocks[]{ModernMapParityBlocks.CAMPFIRE,ModernMapParityBlocks.SOUL_CAMPFIRE}) {
            world.setBlockToAir(X,Y,Z);world.setBlock(X,Y,Z,fire.get(),0,3);
            ModernMapParityBlocks.ParityCampfireTileEntity tile=(ModernMapParityBlocks.ParityCampfireTileEntity)world.getTileEntity(X,Y,Z);
            check(!tile.addCookingItem(new ItemStack(Blocks.iron_ore)),"campfire tile accepts ore");
            for(int i=0;i<4;i++)check(tile.addCookingItem(new ItemStack(Items.beef)),"campfire slot rejected food");
            check(!tile.addCookingItem(new ItemStack(Items.beef)),"campfire has more than four slots");
            int before=cookedItems(world);
            for(int i=0;i<600;i++)tile.updateEntity();
            check(cookedItems(world)==before && tile.getCookingItem(0)!=null,"unlit campfire cooked food");
            world.setBlockMetadataWithNotify(X,Y,Z,4,3);
            for(int i=0;i<599;i++)tile.updateEntity();
            check(cookedItems(world)==before && tile.getCookingItem(0)!=null,"campfire completed before 600 lit ticks");
            NBTTagCompound saved=new NBTTagCompound();tile.writeToNBT(saved);
            ModernMapParityBlocks.ParityCampfireTileEntity restored=new ModernMapParityBlocks.ParityCampfireTileEntity();
            restored.readFromNBT(saved);world.setTileEntity(X,Y,Z,restored);restored.updateEntity();
            check(cookedItems(world)==before+4,"campfire failed after NBT reload without fuel");
            for(int i=0;i<4;i++)check(restored.getCookingItem(i)==null,"campfire duplicated output");
        }
        ServerEventHandler handler=ServerEventHandler.INSTANCE;int colors=0;
        for(ModernMapParityBlocks candle:ModernMapParityBlocks.values()) {
            ModernMapParityBlocks cake=ModernMapParityBlocks.candleCakeFor(candle);if(cake==null)continue;
            colors++;world.setBlock(X,Y,Z,Blocks.cake,0,3);
            player.inventory.setInventorySlotContents(0,new ItemStack(candle.get(),2));
            PlayerInteractEvent event=new PlayerInteractEvent(player,PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,X,Y,Z,1,world);
            handler.onPlayerInteract(event);
            check(event.isCanceled() && world.getBlock(X,Y,Z)==cake.get(),"candle cake placement "+candle);
            check(player.getHeldItem().stackSize==1,"candle consumption "+candle);
            java.util.ArrayList<ItemStack> drops=cake.get().getDrops(world,X,Y,Z,0,0);
            check(drops.size()==1 && drops.get(0).getItem()==net.minecraft.item.Item.getItemFromBlock(candle.get()),"cake break must return only its candle");
            player.inventory.setInventorySlotContents(0,new ItemStack(Items.flint_and_steel));
            check(cake.get().onBlockActivated(world,X,Y,Z,player,1,.5F,.8F,.5F),"candle cake cannot be lit");
            check(world.getBlockMetadata(X,Y,Z)==1,"candle cake not lit");
            player.inventory.setInventorySlotContents(0,null);NBTTagCompound hunger=new NBTTagCompound();player.getFoodStats().writeNBT(hunger);hunger.setInteger("foodLevel",10);player.getFoodStats().readNBT(hunger);
            check(cake.get().onBlockActivated(world,X,Y,Z,player,2,.5F,.25F,.5F),"candle cake cannot be eaten");
            check(world.getBlock(X,Y,Z)==Blocks.cake && world.getBlockMetadata(X,Y,Z)==1 && player.getFoodStats().getFoodLevel()==12,"first cake bite state/food");
            player.inventory.setInventorySlotContents(0,new ItemStack(candle.get()));
            check(!ModernMapParityBlocks.addCandleToCake(world,X,Y,Z,player),"candle accepted on eaten cake");
        }
        check(colors==17,"missing candle cake colours");world.setBlockToAir(X,Y,Z);
        player.capabilities.isCreativeMode=true;world.setBlock(X,Y,Z,Blocks.cake,0,3);
        player.inventory.setInventorySlotContents(0,new ItemStack(ModernMapParityBlocks.CANDLE.get()));
        check(ModernMapParityBlocks.addCandleToCake(world,X,Y,Z,player) && player.getHeldItem().stackSize==1,"creative candle consumed");
        world.setBlockToAir(X,Y,Z);player.capabilities.isCreativeMode=false;
        for(int i=0;i<player.inventory.getSizeInventory();i++)player.inventory.setInventorySlotContents(i,null);
        ContainerSmithingTable table=new ContainerSmithingTable(player.inventory,world);
        player.inventory.setInventorySlotContents(9,ModItems.NETHERITE_INGOT.newItemStack());table.transferStackInSlot(player,4);
        check(table.inputMatrix.getStackInSlot(2)!=null && table.inputMatrix.getStackInSlot(1)==null,"shift-click ingot entered wrong smithing slot");
        player.inventory.setInventorySlotContents(10,new ItemStack(Items.diamond_pickaxe));table.transferStackInSlot(player,5);
        check(table.inputMatrix.getStackInSlot(1)!=null,"shift-click tool did not enter base slot");
        table.inputMatrix.setInventorySlotContents(0,ModItems.SMITHING_TEMPLATE.newItemStack(1,0));
        table.onCraftMatrixChanged(table.inputMatrix);
        check(((net.minecraft.inventory.Slot)table.inventorySlots.get(3)).getStack().getItem()==ModItems.NETHERITE_PICKAXE.get(),"smithing preview after shift-click");
        table.transferStackInSlot(player,3);
        check(table.inputMatrix.getStackInSlot(0)==null && table.inputMatrix.getStackInSlot(1)==null && table.inputMatrix.getStackInSlot(2)==null,"smithing output did not consume both inputs");
        boolean found=false;for(ItemStack stack:player.inventory.mainInventory)if(stack!=null && stack.getItem()==ModItems.NETHERITE_PICKAXE.get())found=true;
        check(found,"smithing result not moved to player inventory");
        InventoryCrafting grid=grid();grid.setInventorySlotContents(8,ModItems.COPPER_INGOT.newItemStack());
        output(craft(grid,world),ModItems.COPPER_NUGGET.newItemStack(9),"copper ingot to nuggets");
        grid=grid();for(int i=0;i<9;i++)grid.setInventorySlotContents(i,ModItems.COPPER_NUGGET.newItemStack());
        output(craft(grid,world),ModItems.COPPER_INGOT.newItemStack(),"copper nuggets to ingot");
        grid=grid();grid.setInventorySlotContents(2,ModItems.COPPER_NUGGET.newItemStack());grid.setInventorySlotContents(5,new ItemStack(Items.coal));grid.setInventorySlotContents(8,new ItemStack(Items.stick));
        output(craft(grid,world),new ItemStack(ModernMapParityBlocks.COPPER_TORCH.get(),4),"copper torches");
        grid=grid();grid.setInventorySlotContents(0,ModItems.COPPER_NUGGET.newItemStack());grid.setInventorySlotContents(3,ModItems.COPPER_INGOT.newItemStack());grid.setInventorySlotContents(6,ModItems.COPPER_NUGGET.newItemStack());
        output(craft(grid,world),new ItemStack(ModernMapParityBlocks.COPPER_CHAIN.get()),"copper chain");
        grid=grid();for(int i=0;i<9;i++)grid.setInventorySlotContents(i,i==4?new ItemStack(ModernMapParityBlocks.COPPER_TORCH.get()):ModItems.COPPER_NUGGET.newItemStack());
        output(craft(grid,world),new ItemStack(ModernMapParityBlocks.COPPER_LANTERN.get()),"copper lantern");
        for(ItemStack nugget:net.minecraftforge.oredict.OreDictionary.getOres("nuggetCopper")) {
            grid=grid();for(int i=0;i<9;i++)grid.setInventorySlotContents(i,nugget.copy());
            output(craft(grid,world),ModItems.COPPER_INGOT.newItemStack(),"shared copper nugget input");
        }
        for(ItemStack ingot:net.minecraftforge.oredict.OreDictionary.getOres("ingotCopper")) {
            grid=grid();grid.setInventorySlotContents(7,ingot.copy());
            output(craft(grid,world),ModItems.COPPER_NUGGET.newItemStack(9),"shared copper ingot input");
        }
        grid=grid();grid.setInventorySlotContents(0,ModItems.NETHERITE_SCRAP.newItemStack());grid.setInventorySlotContents(2,new ItemStack(Items.gold_ingot));
        grid.setInventorySlotContents(3,new ItemStack(Items.gold_ingot));grid.setInventorySlotContents(4,ModItems.NETHERITE_SCRAP.newItemStack());
        grid.setInventorySlotContents(5,ModItems.NETHERITE_SCRAP.newItemStack());grid.setInventorySlotContents(6,new ItemStack(Items.gold_ingot));
        grid.setInventorySlotContents(7,ModItems.NETHERITE_SCRAP.newItemStack());grid.setInventorySlotContents(8,new ItemStack(Items.gold_ingot));
        output(craft(grid,world),ModItems.NETHERITE_INGOT.newItemStack(),"netherite ingot is shapeless");
        System.out.println("COOKING_AND_USAGE PASSED: food admission, four-slot 600-tick fuel-free cooking and NBT, 17 cake colours/consumption/eating/drops, smithing shift-click, copper and shapeless netherite");
    }
}
