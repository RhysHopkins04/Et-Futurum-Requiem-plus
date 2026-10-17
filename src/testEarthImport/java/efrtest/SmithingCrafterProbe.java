package efrtest;

import com.mojang.authlib.GameProfile;
import ganymedes01.etfuturum.*;
import ganymedes01.etfuturum.inventory.*;
import ganymedes01.etfuturum.recipes.*;
import ganymedes01.etfuturum.tileentities.TileEntityCrafter;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.*;
import java.io.File;
import java.util.*;
import static efrtest.EarthImportContracts.check;

public final class SmithingCrafterProbe {
    private static final int X=-288,Y=128,Z=144;
    private static void clear(FakePlayer p) { for(int i=0;i<p.inventory.getSizeInventory();i++)p.inventory.setInventorySlotContents(i,null);p.inventory.setItemStack(null);p.capabilities.isCreativeMode=false;p.capabilities.allowEdit=true; }
    private static void smithing(WorldServer world,FakePlayer p) {
        p.setPosition(X+.5,Y,Z+.5);world.setBlock(X,Y,Z,ModBlocks.SMITHING_TABLE.get());
        clear(p);ContainerSmithingTable error=new ContainerSmithingTable(p.inventory,world,X,Y,Z);
        check(!error.unable(),"empty smithing screen shows error");
        error.inputMatrix.setInventorySlotContents(0,ModItems.SMITHING_TEMPLATE.newItemStack());
        check(!error.unable(),"template alone shows error");
        error.inputMatrix.setInventorySlotContents(1,new ItemStack(Items.diamond_pickaxe));
        check(!error.unable(),"partial smithing recipe shows error");
        error.inputMatrix.setInventorySlotContents(2,new ItemStack(Items.stick));
        check(error.unable(),"complete invalid smithing recipe hides error");
        error.onContainerClosed(p);
        Item[] bases={Items.diamond_pickaxe,Items.diamond_axe,Items.diamond_shovel,Items.diamond_sword,Items.diamond_hoe,Items.diamond_helmet,Items.diamond_chestplate,Items.diamond_leggings,Items.diamond_boots};
        for(Item base:bases)for(int button=0;button<2;button++) {
            clear(p);ContainerSmithingTable c=new ContainerSmithingTable(p.inventory,world,X,Y,Z);ItemStack input=new ItemStack(base,1,73);input.setStackDisplayName("Keep name");input.getTagCompound().setString("Custom","preserved");input.addEnchantment(net.minecraft.enchantment.Enchantment.unbreaking,2);
            ItemStack before=input.copy();c.inputMatrix.setInventorySlotContents(0,ModItems.SMITHING_TEMPLATE.newItemStack(3,0));c.inputMatrix.setInventorySlotContents(1,input);c.inputMatrix.setInventorySlotContents(2,ModItems.NETHERITE_INGOT.newItemStack(4));
            check(c.getSlot(3).getHasStack() && !c.unable(),"modern netherite preview missing or false error");c.slotClick(3,button,0,p);ItemStack output=p.inventory.getItemStack();
            check(output!=null && output.getItemDamage()==73 && ItemStack.areItemStackTagsEqual(output,before),"modern upgrade lost NBT/damage");check(c.inputMatrix.getStackInSlot(0).stackSize==2 && c.inputMatrix.getStackInSlot(2).stackSize==3 && c.inputMatrix.getStackInSlot(1)==null,"template/material costs wrong");
            output.getTagCompound().setString("Custom","changed");check(input.getTagCompound().getString("Custom").equals("preserved"),"upgrade aliases input NBT");
        }
        List<Item> armor=new ArrayList<>();for(Object item:Item.itemRegistry)if(item instanceof ItemArmor && ModernSmithingRecipes.trimmable(new ItemStack((Item)item)))armor.add((Item)item);
        int cases=0;for(Item item:armor)for(int pattern=1;pattern<19;pattern++)for(String material:ModernSmithingRecipes.MATERIALS) {
            ItemStack base=new ItemStack(item,1,29);base.setStackDisplayName("Trim keeps name");base.getTagCompound().setInteger("Custom",42);base.addEnchantment(net.minecraft.enchantment.Enchantment.unbreaking,1);
            ItemStack template=ModItems.SMITHING_TEMPLATE.newItemStack(1,pattern),addition=ModernSmithingRecipes.materialExample(material),output=ModernSmithingRecipes.result(template,base,addition,world);
            if(addition==null)continue;check(output!=null && output.getItem()==item && output.getItemDamage()==29 && output.getTagCompound().getInteger("Custom")==42 && output.isItemEnchanted(),"trim result changed armor");
            check(ModernSmithingRecipes.trim(output)!=null && ModernSmithingRecipes.trim(output).getString("material").equals("minecraft:"+material) && ModernSmithingRecipes.trim(output).getString("pattern").equals("minecraft:"+ModernSmithingRecipes.pattern(template)) && !base.getTagCompound().hasKey("Trim"),"trim pattern/material NBT wrong or aliased");check(ModernSmithingRecipes.result(template,output,addition,world)==null,"identical trim consumed more material");
            cases++;
        }
        int availableMaterials=0;for(String material:ModernSmithingRecipes.MATERIALS)if(ModernSmithingRecipes.materialExample(material)!=null)availableMaterials++;
        check(availableMaterials>=10 && cases==armor.size()*18*availableMaterials,"trim combinations omitted");
        boolean allowMaterials=ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowModdedTrimMaterials;
        String[] metals={"iron","gold","copper","netherite"};
        try {
            for(int i=0;i<metals.length;i++) {
                String material=metals[i],ore="ingot"+material.substring(0,1).toUpperCase(java.util.Locale.ROOT)+material.substring(1);
                ItemStack equivalent=new ItemStack(Items.stick,1,16+i);
                net.minecraftforge.oredict.OreDictionary.registerOre(ore,equivalent);
                ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowModdedTrimMaterials=false;
                check(ModernSmithingRecipes.material(equivalent).isEmpty() && ModernSmithingRecipes.result(ModItems.SMITHING_TEMPLATE.newItemStack(1,1),new ItemStack(Items.iron_chestplate),equivalent,world)==null,"mod material bypassed strict trim whitelist");
                ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowModdedTrimMaterials=true;
                check(ModernSmithingRecipes.material(equivalent).equals(material),"opt-in metal mapped to wrong trim color");
                ItemStack trimmed=ModernSmithingRecipes.result(ModItems.SMITHING_TEMPLATE.newItemStack(1,1),new ItemStack(Items.iron_chestplate),equivalent,world);
                check(trimmed!=null && ModernSmithingRecipes.trim(trimmed).getString("material").equals("minecraft:"+material),"opt-in metal trim NBT wrong");
            }
        }finally {
            ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowModdedTrimMaterials=allowMaterials;
            for(String material:metals)net.minecraftforge.oredict.OreDictionary.getOres("ingot"+material.substring(0,1).toUpperCase(java.util.Locale.ROOT)+material.substring(1)).removeIf(stack->stack.getItem()==Items.stick && stack.getItemDamage()>=16 && stack.getItemDamage()<20);
        }
        for(String material:ModernSmithingRecipes.MATERIALS) {
            ItemStack sample=ModernSmithingRecipes.materialExample(material);
            if(sample!=null)check(ModernSmithingRecipes.material(sample).equals(material),"canonical trim example mapped to wrong material: "+material);
        }
        check(ModernSmithingRecipes.material(new ItemStack(Items.dye,1,1)).isEmpty() && ModernSmithingRecipes.material(new ItemStack(Items.gold_nugget)).isEmpty(),"non-trim resource admitted");
        ItemStack modded=new ItemStack(new ItemArmor(ItemArmor.ArmorMaterial.IRON,0,1){});
        boolean allow=ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowModdedArmorTrims;
        try {
            ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowModdedArmorTrims=false;
            check(!ModernSmithingRecipes.trimmable(modded) && ModernSmithingRecipes.result(ModItems.SMITHING_TEMPLATE.newItemStack(1,1),modded,new ItemStack(Items.diamond),world)==null,"default whitelist admitted mod armor");
            ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowModdedArmorTrims=true;
            check(ModernSmithingRecipes.trimmable(modded) && ModernSmithingRecipes.result(ModItems.SMITHING_TEMPLATE.newItemStack(1,1),modded,new ItemStack(Items.diamond),world)!=null,"mod armor opt-in ignored");
        }finally { ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowModdedArmorTrims=allow; }

        clear(p);ContainerSmithingTable c=new ContainerSmithingTable(p.inventory,world,X,Y,Z);c.inputMatrix.setInventorySlotContents(1,new ItemStack(Items.diamond_sword));c.inputMatrix.setInventorySlotContents(2,ModItems.NETHERITE_INGOT.newItemStack());check(!c.getSlot(3).getHasStack(),"missing template upgraded in strict mode");
        boolean strict=ganymedes01.etfuturum.configuration.configs.ConfigFunctions.requireNetheriteUpgradeTemplate;
        try { ganymedes01.etfuturum.configuration.configs.ConfigFunctions.requireNetheriteUpgradeTemplate=false;c.onCraftMatrixChanged(c.inputMatrix);check(c.getSlot(3).getHasStack(),"legacy progression config ignored"); }finally { ganymedes01.etfuturum.configuration.configs.ConfigFunctions.requireNetheriteUpgradeTemplate=strict; }
        c.inputMatrix.setInventorySlotContents(0,ModItems.SMITHING_TEMPLATE.newItemStack());for(int i=0;i<36;i++)p.inventory.setInventorySlotContents(i,new ItemStack(Items.stick,64));check(c.transferStackInSlot(p,3)==null && c.inputMatrix.getStackInSlot(0)!=null,"full smithing inventory consumed template");
        p.inventory.setInventorySlotContents(0,null);check(c.transferStackInSlot(p,3)!=null && p.inventory.getStackInSlot(0).getItem()==ModItems.NETHERITE_SWORD.get(),"smithing shift output failed");
        clear(p);c=new ContainerSmithingTable(p.inventory,world,X,Y,Z);c.inputMatrix.setInventorySlotContents(0,ModItems.SMITHING_TEMPLATE.newItemStack(2,1));c.inputMatrix.setInventorySlotContents(1,new ItemStack(Items.iron_chestplate));c.inputMatrix.setInventorySlotContents(2,new ItemStack(Items.diamond,3));c.slotClick(3,0,2,p);check(ModernSmithingRecipes.trim(p.inventory.getStackInSlot(0))!=null,"hotbar trim result lost");
        c.onContainerClosed(p);check(c.inputMatrix.getStackInSlot(0)==null && c.getSlot(3).getStack()==null,"smithing close retained preview");
        c=new ContainerSmithingTable(p.inventory,world,X,Y,Z);p.setPosition(X+20,Y,Z);check(!c.canInteractWith(p),"distant smithing allowed");p.setPosition(X+.5,Y,Z+.5);world.setBlockToAir(X,Y,Z);check(!c.canInteractWith(p),"removed smithing table allowed");
        SmithingTableRecipes.SmithingTableRecipe custom=new SmithingTableRecipes.SmithingTableRecipe(Items.stick,Items.flint,new ItemStack(Items.arrow,2),false);SmithingTableRecipes.getInstance().addRecipe(custom);
        try { check(ModernSmithingRecipes.result(null,new ItemStack(Items.stick),new ItemStack(Items.flint),world).stackSize==2,"custom two-input API broken"); }finally { SmithingTableRecipes.getInstance().getRecipes().remove(custom); }
        System.out.println("MODERN_SMITHING PASSED: 18 upgrade takes, "+cases+" trim combinations, template costs, names/damage/enchantments/custom NBT, strict/legacy configs, hotbar/shift/full inventory and custom API");
    }
    private static TileEntityCrafter place(WorldServer world,int x,int meta) {
        world.setBlockToAir(x,Y,Z);world.setBlock(x,Y,Z,ModernMapParityBlocks.CRAFTER.get(),meta,3);return (TileEntityCrafter)world.getTileEntity(x,Y,Z);
    }
    private static int dropped(WorldServer world,int x,Item item) { int count=0;for(Object entity:world.loadedEntityList)if(entity instanceof EntityItem) { EntityItem drop=(EntityItem)entity;if(!drop.isDead && Math.abs(drop.posX-x)<3 && Math.abs(drop.posZ-Z)<3 && drop.getEntityItem().getItem()==item)count+=drop.getEntityItem().stackSize; }return count; }
    private static void crafter(WorldServer world,FakePlayer p) throws Exception {
        int x=X+32;File fixture=new File("crafter-fixture-created");
        if(fixture.exists()) { TileEntityCrafter saved=(TileEntityCrafter)world.getTileEntity(x,Y,Z);check(saved!=null && saved.getStackInSlot(0)!=null && saved.getStackInSlot(0).stackSize==13 && saved.isDisabled(8),"crafter inventory/disabled mask lost on restart");System.out.println("CRAFTER_RESTART PASSED"); }
        TileEntityCrafter tile=place(world,x,5);p.setPosition(x+.5,Y,Z+.5);for(int i=0;i<8;i++)check(tile.setDisabled(i,true),"cannot disable empty crafter slot");check(tile.comparator()==8,"disabled slots missing from comparator");check(tile.setDisabled(0,false),"cannot enable crafter slot");
        tile.setInventorySlotContents(0,new ItemStack(Blocks.log,2));check(!tile.setDisabled(0,true),"occupied crafter slot disabled");int before=dropped(world,x,Item.getItemFromBlock(Blocks.planks));check(tile.craft() && tile.getStackInSlot(0).stackSize==1 && dropped(world,x,Item.getItemFromBlock(Blocks.planks))-before==4,"crafter craft/drop quantity wrong");
        check(tile.isCrafting(),"crafting visual absent");for(int i=0;i<6;i++)tile.updateEntity();check(!tile.isCrafting(),"crafting visual not cleared");
        tile=place(world,x,5);for(int i=0;i<18;i++)check(TileEntityHopper.func_145889_a(tile,new ItemStack(Items.iron_ingot),1)==null,"hopper insertion failed");for(int i=0;i<9;i++)check(tile.getStackInSlot(i).stackSize==2,"hopper failed balanced insertion");
        tile=place(world,x,5);world.setBlock(x,Y,Z-1,Blocks.chest);IInventory chest=(IInventory)world.getTileEntity(x,Y,Z-1);for(int i=0;i<chest.getSizeInventory();i++)chest.setInventorySlotContents(i,null);
        tile.setInventorySlotContents(0,new ItemStack(Blocks.log));check(tile.craft() && chest.getStackInSlot(0).stackSize==4,"crafter failed front inventory output");
        // Cake returns its three buckets through the same output path.
        ItemStack[] cake={new ItemStack(Items.milk_bucket),new ItemStack(Items.milk_bucket),new ItemStack(Items.milk_bucket),new ItemStack(Items.sugar),new ItemStack(Items.egg),new ItemStack(Items.sugar),new ItemStack(Items.wheat),new ItemStack(Items.wheat),new ItemStack(Items.wheat)};
        for(int i=0;i<9;i++)tile.setInventorySlotContents(i,cake[i]);check(tile.craft(),"crafter cake recipe missing");int buckets=0,cakes=0;for(int i=0;i<chest.getSizeInventory();i++) { ItemStack s=chest.getStackInSlot(i);if(s!=null && s.getItem()==Items.bucket)buckets+=s.stackSize;if(s!=null && s.getItem()==Items.cake)cakes+=s.stackSize; }check(buckets==3 && cakes==1,"crafter lost container remainders");
        world.setBlockToAir(x,Y,Z-1);tile=place(world,x,5);tile.setInventorySlotContents(0,new ItemStack(Blocks.log,3));
        int prior=dropped(world,x,Item.getItemFromBlock(Blocks.planks));world.setBlock(x+1,Y,Z,Blocks.redstone_block);long tick=world.getTotalWorldTime();world.getWorldInfo().incrementTotalWorldTime(tick+3);world.tickUpdates(false);check(tile.getStackInSlot(0).stackSize==3,"crafter fired before four ticks");world.getWorldInfo().incrementTotalWorldTime(tick+4);world.tickUpdates(false);check(tile.getStackInSlot(0).stackSize==2 && dropped(world,x,Item.getItemFromBlock(Blocks.planks))-prior==4,"rising edge did not craft once");world.getWorldInfo().incrementTotalWorldTime(tick+30);world.tickUpdates(false);check(tile.getStackInSlot(0).stackSize==2,"held power repeatedly crafted");
        world.setBlockToAir(x+1,Y,Z);world.setBlock(x+1,Y,Z,Blocks.redstone_block);world.getWorldInfo().incrementTotalWorldTime(tick+34);world.tickUpdates(false);check(tile.getStackInSlot(0).stackSize==1,"second rising edge ignored");world.setBlockToAir(x+1,Y,Z);
        ContainerCrafter c=new ContainerCrafter(p.inventory,tile);clear(p);check(c.slotClick(9,0,0,p)==null && p.inventory.getItemStack()==null,"crafter preview was extractable");check(c.enchantItem(p,8) && !c.enchantItem(p,0) && !c.enchantItem(p,99),"crafter disabled-slot validation wrong");c.onContainerClosed(p);check(tile.getStackInSlot(0)!=null,"closing crafter removed persistent inventory");
        for(ForgeDirection direction:ForgeDirection.VALID_DIRECTIONS)world.setBlockToAir(x+direction.offsetX,Y+direction.offsetY,Z+direction.offsetZ);
        for(int meta=0;meta<12;meta++) {
            tile=place(world,x,meta);tile.setInventorySlotContents(0,new ItemStack(Blocks.log));java.util.Set<Object> entities=new java.util.HashSet<>(world.loadedEntityList);check(tile.craft(),"oriented crafter failed");
            ForgeDirection front=TileEntityCrafter.front(meta);int found=0;
            for(Object entity:world.loadedEntityList)if(!entities.contains(entity) && entity instanceof EntityItem) {
                EntityItem drop=(EntityItem)entity;check(drop.getEntityItem().getItem()==Item.getItemFromBlock(Blocks.planks) && drop.getEntityItem().stackSize==4,"oriented output wrong");
                double dot=(drop.posX-x-.5)*front.offsetX+(drop.posY-Y-.5)*front.offsetY+(drop.posZ-Z-.5)*front.offsetZ;check(dot>.6,"output did not follow FrontAndTop direction");found++;
            }check(found==1,"oriented output missing/duplicated");
        }
        tile=place(world,x,5);world.setBlock(x,Y,Z-1,ModernMapParityBlocks.CRAFTER.get(),5,3);TileEntityCrafter receiver=(TileEntityCrafter)world.getTileEntity(x,Y,Z-1);tile.setInventorySlotContents(0,new ItemStack(Blocks.log));check(tile.craft(),"chained crafter failed");for(int i=0;i<9;i++)check(receiver.getStackInSlot(i)==null?i>=4:receiver.getStackInSlot(i).stackSize==1,"crafter-to-crafter insertion unbalanced");world.setBlockToAir(x,Y,Z-1);
        tile=place(world,x,5);tile.setInventorySlotContents(0,new ItemStack(Items.iron_ingot,7));int droppedBefore=dropped(world,x,Items.iron_ingot);world.setBlockToAir(x,Y,Z);check(dropped(world,x,Items.iron_ingot)-droppedBefore==7,"crafter break lost or duplicated stored items");

        tile=place(world,x,5);tile.setInventorySlotContents(0,new ItemStack(Items.iron_ingot,13));tile.setDisabled(8,true);NBTTagCompound nbt=new NBTTagCompound();tile.writeToNBT(nbt);ModernMapParityBlocks.ParityCrafterStateTileEntity copy=new ModernMapParityBlocks.ParityCrafterStateTileEntity();copy.readFromNBT(nbt);check(copy.isDisabled(8) && copy.getStackInSlot(0).stackSize==13,"crafter NBT failed");check(nbt.getString("id").equals("etfuturum:modern_parity_crafter_state"),"registered crafter TE identity changed");
        if(!fixture.exists())check(fixture.createNewFile(),"fixture creation failed");
        System.out.println("CRAFTER PASSED: actual recipes, counts, 9-slot balance, container remainders, front inventory/ejection, four-tick rising edges, held power, disabled slots, comparator, preview protection and persistent TE identity");
    }
    public static void run(WorldServer world,boolean legacy) throws Exception {
        if(legacy) { check(!ModernSmithingRecipes.enabled() && !TileEntityCrafter.enabled(),"legacy workstation suppression failed");System.out.println("SMITHING_CRAFTER_LEGACY PASSED");return; }
        FakePlayer p=FakePlayerFactory.get(world,new GameProfile(UUID.fromString("11000000-0000-0000-0000-000000000024"),"SmithCrafterProbe"));clear(p);smithing(world,p);clear(p);crafter(world,p);
    }
}
