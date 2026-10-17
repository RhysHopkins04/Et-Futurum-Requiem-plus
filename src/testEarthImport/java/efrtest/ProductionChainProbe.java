package efrtest;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.api.CompostingRegistry;
import ganymedes01.etfuturum.recipes.BlastFurnaceRecipes;
import ganymedes01.etfuturum.recipes.SmokerRecipes;
import ganymedes01.etfuturum.tileentities.TileEntitySmoker;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import static efrtest.EarthImportContracts.check;

/** Actual food consumption, fuel cycles, cooking progress/reload and production recipe queries. */
public final class ProductionChainProbe {
    private static final int X=-112,Y=128,Z=96;
    private static InventoryCrafting grid() { return new InventoryCrafting(new Container() {
        @Override public boolean canInteractWith(EntityPlayer p) { return true; }
    },3,3); }
    private static ItemStack stack(ModernMapParityBlocks entry,int count) { return new ItemStack(entry.get(),count); }
    private static void recipe(WorldServer world,ItemStack output,String[] shape,ItemStack input) {
        InventoryCrafting grid=grid();
        for(int y=0;y<shape.length;y++)for(int x=0;x<shape[y].length();x++)
            if(shape[y].charAt(x)!=' ')grid.setInventorySlotContents(y*3+x,input.copy());
        ItemStack result=CraftingManager.getInstance().findMatchingRecipe(grid,world);
        check(ItemStack.areItemStacksEqual(result,output),"production recipe "+output+" got "+result);
    }
    private static int dropped(WorldServer world,Item item) {
        int count=0;for(Object value:world.loadedEntityList)if(value instanceof EntityItem) {
            EntityItem entity=(EntityItem)value;
            if(!entity.isDead && Math.abs(entity.posX-X-.5)<2 && Math.abs(entity.posZ-Z-.5)<2 && entity.getEntityItem().getItem()==item)
                count+=entity.getEntityItem().stackSize;
        }return count;
    }
    private static void cooking(WorldServer world,Block block,ItemStack input,ItemStack expected,int ticks) {
        world.setBlockToAir(X,Y,Z);world.setBlock(X,Y,Z,block,0,3);
        TileEntityFurnace oven=(TileEntityFurnace)world.getTileEntity(X,Y,Z);
        oven.setInventorySlotContents(0,input.copy());oven.setInventorySlotContents(1,new ItemStack(Items.coal));
        for(int i=0;i<ticks-1;i++)oven.updateEntity();
        check(oven.getStackInSlot(2)==null,"production cooking completed too early");
        NBTTagCompound saved=new NBTTagCompound();oven.writeToNBT(saved);
        TileEntityFurnace restored=oven instanceof TileEntitySmoker?new TileEntitySmoker():new TileEntityFurnace();
        restored.readFromNBT(saved);world.setTileEntity(X,Y,Z,restored);restored.updateEntity();
        check(ItemStack.areItemStacksEqual(restored.getStackInSlot(2),expected),"production cook output/reload "+expected);
        check(restored.getStackInSlot(0)==null && restored.getStackInSlot(1)==null,"production cooking did not consume input/fuel");
        world.setBlockToAir(X,Y,Z);
    }
    public static void run(WorldServer world) {
        ItemStack kelp=stack(ModernMapParityBlocks.KELP,1),dried=ModItems.DRIED_KELP.newItemStack();
        ItemStack clump=stack(ModernMapParityBlocks.RESIN_CLUMP,1),brick=ModItems.RESIN_BRICK.newItemStack();
        check(ItemStack.areItemStacksEqual(FurnaceRecipes.smelting().getSmeltingResult(kelp),dried),"kelp furnace registration");
        check(ItemStack.areItemStacksEqual(SmokerRecipes.smelting().getSmeltingResult(kelp),dried),"kelp smoker registration");
        check(BlastFurnaceRecipes.smelting().getSmeltingResult(kelp)==null,"blast furnace cooks kelp");
        check(SmokerRecipes.smelting().getSmeltingResult(clump)==null && BlastFurnaceRecipes.smelting().getSmeltingResult(clump)==null,"resin brick leaked into smoker/blast furnace");
        check(Math.abs(FurnaceRecipes.smelting().func_151398_b(brick)-.1F)<.00001F,"resin XP");
        check(Math.abs(FurnaceRecipes.smelting().func_151398_b(dried)-.1F)<.00001F,"kelp XP");
        recipe(world,stack(ModernMapParityBlocks.DRIED_KELP_BLOCK,1),new String[]{"KKK","KKK","KKK"},dried);
        recipe(world,ModItems.DRIED_KELP.newItemStack(9),new String[]{"K"},stack(ModernMapParityBlocks.DRIED_KELP_BLOCK,1));
        recipe(world,stack(ModernMapParityBlocks.RESIN_BLOCK,1),new String[]{"RRR","RRR","RRR"},clump);
        recipe(world,stack(ModernMapParityBlocks.RESIN_CLUMP,9),new String[]{"R"},stack(ModernMapParityBlocks.RESIN_BLOCK,1));
        recipe(world,stack(ModernMapParityBlocks.RESIN_BRICKS,1),new String[]{"RR","RR"},brick);
        recipe(world,stack(ModernMapParityBlocks.RESIN_BRICK_SLAB,6),new String[]{"RRR"},stack(ModernMapParityBlocks.RESIN_BRICKS,1));
        recipe(world,stack(ModernMapParityBlocks.RESIN_BRICK_STAIRS,4),new String[]{"R  ","RR ","RRR"},stack(ModernMapParityBlocks.RESIN_BRICKS,1));
        recipe(world,stack(ModernMapParityBlocks.RESIN_BRICK_WALL,6),new String[]{"RRR","RRR"},stack(ModernMapParityBlocks.RESIN_BRICKS,1));
        recipe(world,stack(ModernMapParityBlocks.CHISELED_RESIN_BRICKS,1),new String[]{"R","R"},stack(ModernMapParityBlocks.RESIN_BRICK_SLAB,1));
        FakePlayer player=FakePlayerFactory.getMinecraft(world);player.capabilities.isCreativeMode=false;player.inventory.currentItem=0;
        NBTTagCompound hunger=new NBTTagCompound();player.getFoodStats().writeNBT(hunger);hunger.setInteger("foodLevel",10);hunger.setFloat("foodSaturationLevel",1);
        player.getFoodStats().readNBT(hunger);ItemStack eating=ModItems.DRIED_KELP.newItemStack(2);
        check(eating.getItem().getMaxItemUseDuration(eating)==16,"dried kelp eating duration");
        eating.getItem().onEaten(eating,world,player);
        check(eating.stackSize==1 && player.getFoodStats().getFoodLevel()==11 && Math.abs(player.getFoodStats().getSaturationLevel()-1.6F)<.00001F,"dried kelp hunger/saturation/consumption");
        check(CompostingRegistry.getCompostChance(dried)==30 && CompostingRegistry.getCompostChance(kelp)==30
                && CompostingRegistry.getCompostChance(stack(ModernMapParityBlocks.DRIED_KELP_BLOCK,1))==50,"kelp composting chances");
        world.setBlock(X,Y-1,Z,Blocks.stone);
        cooking(world,Blocks.furnace,kelp,dried,200);cooking(world,ModBlocks.SMOKER.get(),kelp,dried,100);cooking(world,Blocks.furnace,clump,brick,200);
        for(ModernMapParityBlocks fire:new ModernMapParityBlocks[]{ModernMapParityBlocks.CAMPFIRE,ModernMapParityBlocks.SOUL_CAMPFIRE}) {
            world.setBlock(X,Y,Z,fire.get(),0,3);player.inventory.setInventorySlotContents(0,stack(ModernMapParityBlocks.KELP,4));
            for(int i=0;i<4;i++)check(fire.get().onBlockActivated(world,X,Y,Z,player,1,.5F,.5F,.5F),"kelp cannot be placed on unlit campfire");
            check(player.getHeldItem().stackSize==0,"kelp campfire insertion consumption");
            ModernMapParityBlocks.ParityCampfireTileEntity tile=(ModernMapParityBlocks.ParityCampfireTileEntity)world.getTileEntity(X,Y,Z);
            int before=dropped(world,dried.getItem());for(int i=0;i<600;i++)tile.updateEntity();
            check(dropped(world,dried.getItem())==before,"unlit campfire dried kelp");world.setBlockMetadataWithNotify(X,Y,Z,4,3);
            for(int i=0;i<599;i++)tile.updateEntity();check(dropped(world,dried.getItem())==before,"campfire dried kelp early");
            NBTTagCompound saved=new NBTTagCompound();tile.writeToNBT(saved);ModernMapParityBlocks.ParityCampfireTileEntity restored=new ModernMapParityBlocks.ParityCampfireTileEntity();
            restored.readFromNBT(saved);world.setTileEntity(X,Y,Z,restored);restored.updateEntity();
            check(dropped(world,dried.getItem())==before+4,"campfire kelp NBT/output");world.setBlockToAir(X,Y,Z);
        }
        ItemStack fuel=stack(ModernMapParityBlocks.DRIED_KELP_BLOCK,1);
        check(TileEntityFurnace.getItemBurnTime(fuel)==4000 && TileEntitySmoker.getItemBurnTime(fuel)==2000
                && ganymedes01.etfuturum.tileentities.TileEntityBlastFurnace.getItemBurnTime(fuel)==2000,"kelp fuel duration in three ovens");
        world.setBlock(X,Y,Z,Blocks.furnace,0,3);TileEntityFurnace oven=(TileEntityFurnace)world.getTileEntity(X,Y,Z);
        oven.setInventorySlotContents(0,stack(ModernMapParityBlocks.KELP,21));oven.setInventorySlotContents(1,fuel);
        for(int i=0;i<4200;i++)oven.updateEntity();
        check(oven.getStackInSlot(2)!=null && oven.getStackInSlot(2).stackSize==20 && oven.getStackInSlot(0).stackSize==1,"kelp block must cook exactly 20 items");
        world.setBlockToAir(X,Y,Z);player.inventory.setInventorySlotContents(0,null);
        System.out.println("PRODUCTION_CHAINS PASSED: nine crafting routes, 200/100/600 tick cooking with NBT, fast food/saturation, composting and exact 20-item kelp fuel cycle");
    }
}
