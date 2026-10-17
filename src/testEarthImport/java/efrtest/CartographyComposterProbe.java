package efrtest;

import com.mojang.authlib.GameProfile;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.api.LockedMapData;
import ganymedes01.etfuturum.blocks.BlockComposter;
import ganymedes01.etfuturum.inventory.ContainerCartographyTable;
import ganymedes01.etfuturum.recipes.CartographyRecipes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import java.io.File;
import java.util.*;
import static efrtest.EarthImportContracts.check;

public final class CartographyComposterProbe {
    private static final int X=-224,Y=128,Z=144;
    private static ItemStack map(WorldServer world,int scale) {
        int id=world.getUniqueDataId("map");ItemStack stack=new ItemStack(Items.filled_map,1,id);stack.setStackDisplayName("Named map");stack.getTagCompound().setString("Custom","retained");
        MapData data=new MapData("map_"+id);data.scale=(byte)scale;data.dimension=0;data.xCenter=-192;data.zCenter=448;
        for(int i=0;i<data.colors.length;i++)data.colors[i]=(byte)(i%53+4);
        world.setItemData("map_"+id,data);data.markDirty();return stack;
    }
    private static void clear(FakePlayer p) { for(int i=0;i<p.inventory.getSizeInventory();i++)p.inventory.setInventorySlotContents(i,null);p.inventory.setItemStack(null);p.capabilities.isCreativeMode=false;p.capabilities.allowEdit=true; }
    private static void cartography(WorldServer world,FakePlayer p) throws Exception {
        world.setBlock(X,Y,Z,ModBlocks.CARTOGRAPHY_TABLE.get());p.setPosition(X+.5,Y,Z+.5);
        File fixture=new File("cartography-fixture.txt");
        if(fixture.exists()) {
            int id=Integer.parseInt(new String(java.nio.file.Files.readAllBytes(fixture.toPath()),java.nio.charset.StandardCharsets.UTF_8));MapData saved=(MapData)world.loadItemData(MapData.class,"map_"+id);
            check(saved!=null && ((LockedMapData)saved).etfuturum$isLocked() && saved.colors[789]==(byte)(789%53+4),"locked map lost pixels/state after server restart");
            System.out.println("CARTOGRAPHY_RESTART PASSED");
        }
        int routes=0;
        for(int scale=0;scale<=4;scale++)for(ItemStack extra:new ItemStack[]{new ItemStack(Items.map),new ItemStack(Items.paper),new ItemStack(Blocks.glass_pane)})for(int button:new int[]{0,1}) {
            clear(p);ItemStack source=map(world,scale),snapshot=source.copy();MapData original=CartographyRecipes.data(world,source);byte[] pixels=original.colors.clone();
            ContainerCartographyTable c=new ContainerCartographyTable(p.inventory,world,X,Y,Z);
            source.stackSize=3;extra=extra.copy();extra.stackSize=4;c.input.setInventorySlotContents(0,source);c.input.setInventorySlotContents(1,extra);
            ItemStack preview=c.getSlot(2).getStack();
            if(scale==4 && extra.getItem()==Items.paper) { check(preview==null,"max-scale map expanded");continue; }
            check(preview!=null,"cartography lacks preview");
            check(original.colors.length==16384 && Arrays.equals(pixels,original.colors),"preview mutates map");
            c.slotClick(2,button,0,p);ItemStack output=p.inventory.getItemStack();check(output!=null,"cartography take failed");
            check(c.input.getStackInSlot(0).stackSize==2 && c.input.getStackInSlot(1).stackSize==3,"cartography input consumption");
            check(output.getDisplayName().equals("Named map") && output.getTagCompound().getString("Custom").equals("retained"),"map custom NBT/name lost");
            MapData result=CartographyRecipes.data(world,output);check(result!=null,"result has no saved map");
            if(extra.getItem()==Items.map)check(output.stackSize==2 && output.getItemDamage()==snapshot.getItemDamage(),"copy lost shared identity/count");
            else {
                check(output.stackSize==1 && output.getItemDamage()!=snapshot.getItemDamage(),"scale/lock reused source map identity");
                if(extra.getItem()==Items.paper)check(result.scale==scale+1 && result.xCenter==CartographyRecipes.center(original.xCenter,scale+1) && !Arrays.equals(result.colors,pixels),"expanded map grid/pixels wrong");
                else {
                    check(result.scale==scale && result.xCenter==original.xCenter && Arrays.equals(result.colors,pixels) && result.colors!=original.colors && ((LockedMapData)result).etfuturum$isLocked(),"locked copy state wrong");
                    NBTTagCompound saved=new NBTTagCompound();result.writeToNBT(saved);MapData loaded=new MapData("lock_test");loaded.readFromNBT(saved);check(((LockedMapData)loaded).etfuturum$isLocked(),"lock NBT missing");
                    byte[] before=result.colors.clone();Items.filled_map.updateMapData(world,p,result);check(Arrays.equals(before,result.colors),"locked pixels changed while exploring");
                    check(CartographyRecipes.preview(world,output,new ItemStack(Items.paper))==null && CartographyRecipes.preview(world,output,new ItemStack(Blocks.glass_pane))==null,"locked map rescaled/relocked");
                    check(CartographyRecipes.preview(world,output,new ItemStack(Items.map)).stackSize==2,"locked map cannot clone");
                    if(!fixture.exists())java.nio.file.Files.write(fixture.toPath(),Integer.toString(output.getItemDamage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    InventoryCrafting grid=new InventoryCrafting(new Container(){public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player){return false;}},3,3);
                    for(int i=0;i<9;i++)grid.setInventorySlotContents(i,i==4?output.copy():new ItemStack(Items.paper));
                    check(!new net.minecraft.item.crafting.RecipesMapExtending().matches(grid,world),"crafting table bypassed locked map");
                }
            }
            check(Arrays.equals(original.colors,pixels) && !((LockedMapData)original).etfuturum$isLocked() && original.scale==scale,"taking map changed original/shared copies");
            routes++;
        }
        clear(p);ItemStack source=map(world,0);ContainerCartographyTable c=new ContainerCartographyTable(p.inventory,world,X,Y,Z);c.input.setInventorySlotContents(0,source);c.input.setInventorySlotContents(1,new ItemStack(Items.paper));
        for(int i=0;i<36;i++)p.inventory.setInventorySlotContents(i,new ItemStack(Items.stick,64));
        check(c.transferStackInSlot(p,2)==null && c.input.getStackInSlot(0)!=null && c.input.getStackInSlot(1)!=null,"full inventory consumed maps");
        p.inventory.setInventorySlotContents(0,null);check(c.transferStackInSlot(p,2)!=null && p.inventory.getStackInSlot(0).getItemDamage()!=source.getItemDamage(),"shift scale failed/new identity lost");
        clear(p);c=new ContainerCartographyTable(p.inventory,world,X,Y,Z);ItemStack maps=map(world,0);maps.stackSize=4;c.input.setInventorySlotContents(0,maps);c.input.setInventorySlotContents(1,new ItemStack(Items.map,4));
        for(int i=0;i<4;i++)check(c.transferStackInSlot(p,2)!=null,"repeat shift-copy failed");
        int count=0;for(ItemStack s:p.inventory.mainInventory)if(s!=null && s.getItem()==Items.filled_map)count+=s.stackSize;
        check(count==8 && c.input.getStackInSlot(0)==null && c.input.getStackInSlot(1)==null,"shift-copy count/input conservation");
        c.input.setInventorySlotContents(0,map(world,0));c.input.setInventorySlotContents(1,new ItemStack(Items.paper,2));c.onContainerClosed(p);check(c.input.getStackInSlot(0)==null && c.getSlot(2).getStack()==null,"close retained inputs/output");
        c=new ContainerCartographyTable(p.inventory,world,X,Y,Z);c.input.setInventorySlotContents(0,map(world,0));c.input.setInventorySlotContents(1,new ItemStack(Items.paper));world.setBlockToAir(X,Y,Z);check(c.slotClick(2,0,0,p)==null,"removed table permitted crafting");c.onContainerClosed(p);
        check(CartographyRecipes.preview(world,new ItemStack(Items.stick),new ItemStack(Items.paper))==null,"non-map cartography input");
        clear(p);world.setBlock(X,Y,Z,ModBlocks.CARTOGRAPHY_TABLE.get());c=new ContainerCartographyTable(p.inventory,world,X,Y,Z);
        ItemStack hotbarMap=map(world,0);c.input.setInventorySlotContents(0,hotbarMap);c.input.setInventorySlotContents(1,new ItemStack(Blocks.glass_pane));p.inventory.setInventorySlotContents(0,new ItemStack(Items.stick,17));
        c.slotClick(2,0,2,p);ItemStack hotbar=p.inventory.getStackInSlot(0);check(hotbar.getItem()==Items.filled_map && hotbar.getItemDamage()!=hotbarMap.getItemDamage() && ((LockedMapData)CartographyRecipes.data(world,hotbar)).etfuturum$isLocked(),"hotbar swap exported pending map ID");
        check(p.inventory.getStackInSlot(1).getItem()==Items.stick && p.inventory.getStackInSlot(1).stackSize==17,"hotbar swap lost displaced item");
        int beforeId=world.getUniqueDataId("map");for(int i=0;i<100;i++)CartographyRecipes.preview(world,hotbarMap,new ItemStack(Items.paper));int afterId=world.getUniqueDataId("map");check(afterId==beforeId+1,"preview allocated saved map IDs");
        check(CartographyRecipes.preview(world,new ItemStack(Items.filled_map,1,world.getUniqueDataId("map")),new ItemStack(Items.paper))==null,"missing map silently initialized by table");
        System.out.println("CARTOGRAPHY PASSED: "+routes+" left/right takes, clone/scale/lock, independent saved identities/pixels/NBT, lock enforcement, shifts, full inventory and close");
    }
    private static void composter(WorldServer world,FakePlayer p) throws Exception {
        int probabilities=0;
        try(java.io.InputStream stream=CartographyComposterProbe.class.getResourceAsStream("/assets/etfuturum/recipes/composting_1_21_11.json")) {
            for(com.google.gson.JsonElement element:new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(stream,"UTF-8")).getAsJsonObject().getAsJsonArray("entries")) {
                com.google.gson.JsonObject row=element.getAsJsonObject(),target=row.getAsJsonObject("target");String[] id=target.get("item").getAsString().split(":",2);net.minecraft.item.Item item=cpw.mods.fml.common.registry.GameRegistry.findItem(id[0],id[1]);
                if(item!=null) { check(ganymedes01.etfuturum.api.CompostingRegistry.getCompostChance(new ItemStack(item,1,target.get("meta").getAsInt()))==row.get("percent").getAsInt(),"wrong modern compost probability: "+row.get("modern"));probabilities++; }
            }
        }
        check(probabilities==115,"missing modern compost identities");
        BlockComposter block=(BlockComposter)ModBlocks.COMPOSTER.get();int x=X+16;world.setBlock(x,Y,Z,block,0,3);p.setPosition(x+.5,Y,Z+.5);
        net.minecraft.inventory.ISidedInventory input=(net.minecraft.inventory.ISidedInventory)block.getFakeTileEntity(world,x,Y,Z);
        ItemStack seed=new ItemStack(Items.wheat_seeds);
        for(int i=0;i<20;i++)check(input.canInsertItem(0,seed,1),"valid top input rejected");
        check(world.getBlockMetadata(x,Y,Z)==0,"eligibility probe composted items");check(!input.canInsertItem(0,seed,0),"bottom inserted compost");
        ItemStack left=TileEntityHopper.func_145889_a(input,seed.copy(),1);check(left==null || left.stackSize==0,"hopper insertion not consumed");check(world.getBlockMetadata(x,Y,Z)==1,"first compost not guaranteed");
        input.setInventorySlotContents(0,seed);check(world.getBlockMetadata(x,Y,Z)==1,"stale input proxy inserted twice");
        for(int meta=0;meta<=8;meta++) { world.setBlockMetadataWithNotify(x,Y,Z,meta,3);check(block.getComparatorInputOverride(world,x,Y,Z,1)==meta,"composter comparator wrong"); }
        world.setBlockMetadataWithNotify(x,Y,Z,8,3);ISidedInventory output=(ISidedInventory)block.getFakeTileEntity(world,x,Y,Z);
        check(output.getStackInSlot(0).getItem()==Items.dye && output.getStackInSlot(0).getItemDamage()==15,"compost output not bone meal");
        for(int i=0;i<20;i++)check(output.canExtractItem(0,output.getStackInSlot(0),0),"bottom extraction rejected");
        check(world.getBlockMetadata(x,Y,Z)==8 && !output.canExtractItem(0,output.getStackInSlot(0),1),"extract probe changed compost/allowed top");
        ItemStack item=output.decrStackSize(0,1);check(item!=null && world.getBlockMetadata(x,Y,Z)==8,"tentative extraction changed block before commit");
        output.setInventorySlotContents(0,item);check(world.getBlockMetadata(x,Y,Z)==8,"rolled-back extraction destroyed compost");
        item=output.decrStackSize(0,1);output.markDirty();check(item!=null && world.getBlockMetadata(x,Y,Z)==0 && !output.canExtractItem(0,item,0),"committed extraction did not empty/allow duplicate");
        world.setBlockMetadataWithNotify(x,Y,Z,8,3);world.setBlock(x,Y-1,Z,Blocks.hopper);
        TileEntityHopper hopper=(TileEntityHopper)world.getTileEntity(x,Y-1,Z);for(int i=0;i<5;i++)hopper.setInventorySlotContents(i,new ItemStack(Items.stick,63));
        check(!TileEntityHopper.func_145891_a(hopper) && world.getBlockMetadata(x,Y,Z)==8,"actual hopper failed transfer destroyed compost");
        hopper.setInventorySlotContents(0,null);check(TileEntityHopper.func_145891_a(hopper) && world.getBlockMetadata(x,Y,Z)==0 && hopper.getStackInSlot(0).getItemDamage()==15,"actual hopper did not commit bone meal extraction");
        world.setBlockToAir(x,Y-1,Z);
        world.setBlockMetadataWithNotify(x,Y,Z,0,3);for(int i=0;i<7;i++)BlockComposter.addToComposter(world,x,Y,Z,new ItemStack(Items.cake));
        check(world.getBlockMetadata(x,Y,Z)==7,"guaranteed compost did not fill");long time=world.getTotalWorldTime();world.getWorldInfo().incrementTotalWorldTime(time+19);world.tickUpdates(false);check(world.getBlockMetadata(x,Y,Z)==7,"compost ripened before 20 ticks");world.getWorldInfo().incrementTotalWorldTime(time+20);world.tickUpdates(false);check(world.getBlockMetadata(x,Y,Z)==8,"compost did not ripen at 20 ticks");
        for(int meta=0;meta<=8;meta++) { world.setBlockMetadataWithNotify(x,Y,Z,meta,3);java.util.List<AxisAlignedBB> boxes=new ArrayList<>();block.addCollisionBoxesToList(world,x,Y,Z,AxisAlignedBB.getBoundingBox(x,Y,Z,x+1,Y+1,Z+1),boxes,null);check(boxes.size()==5 && Math.abs(boxes.get(0).maxY-Y-Math.max(.125,(1+2*Math.min(meta,7))/16D))<.0001,"composter filled collision mismatch"); }
        System.out.println("COMPOSTER PASSED: first success, 20-tick ready, all comparator/fill collision levels, side restrictions, pure probes, real hopper insertion and extraction rollback/commit");
    }
    public static void run(WorldServer world,boolean legacy) throws Exception {
        if(legacy) { check(!CartographyRecipes.enabled(),"legacy cartography enabled");System.out.println("CARTOGRAPHY_LEGACY PASSED");return; }
        FakePlayer p=FakePlayerFactory.get(world,new GameProfile(UUID.fromString("11000000-0000-0000-0000-000000000023"),"CartographyProbe"));clear(p);cartography(world,p);clear(p);composter(world,p);
    }
}
