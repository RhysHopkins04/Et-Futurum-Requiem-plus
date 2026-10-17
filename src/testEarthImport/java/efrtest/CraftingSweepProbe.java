package efrtest;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import static efrtest.EarthImportContracts.check;

/** Real startup recipe queries and NBT/input ownership checks, never replacement recipes. */
public final class CraftingSweepProbe {
    private CraftingSweepProbe() {}
    private static InventoryCrafting grid() {
        return new InventoryCrafting(new Container() {
            @Override public boolean canInteractWith(EntityPlayer p) { return true; }
        },3,3);
    }
    private static ItemStack result(InventoryCrafting inv, World world) {
        return CraftingManager.getInstance().findMatchingRecipe(inv,world);
    }
    private static void equals(ItemStack actual,ItemStack expected,String name) {
        check(ItemStack.areItemStacksEqual(actual,expected),"crafting sweep "+name+": "+actual+" expected "+expected);
    }
    public static void run(World world, boolean legacy) {
        if (legacy) {
            InventoryCrafting inv=grid();for(int slot:new int[]{1,3,5,7}) inv.setInventorySlotContents(slot,new ItemStack(Items.brick));
            check(result(inv,world)==null,"decorated pot bypasses legacy recipe suppression");
            System.out.println("CRAFTING_SWEEP_LEGACY PASSED: decorated-pot suppression");return;
        }
        java.util.List<Integer> tiers=new java.util.ArrayList<>();tiers.add(0);
        if(ganymedes01.etfuturum.configuration.configs.ConfigModCompat.shulkerBoxesIronChest)
            for(ganymedes01.etfuturum.tileentities.TileEntityShulkerBox.ShulkerBoxType t:ganymedes01.etfuturum.tileentities.TileEntityShulkerBox.ShulkerBoxType.VALUES)
                if(t.ordinal()>0 && ganymedes01.etfuturum.compat.CompatIronChests.tierExists(t.name()))tiers.add(t.ordinal());
        for(int tier:tiers) for(int color=0;color<16;color++) for(int previous=0;previous<=16;previous++) {
            InventoryCrafting inv=grid();ItemStack box=ModBlocks.SHULKER_BOX.newItemStack();
            NBTTagCompound tag=new NBTTagCompound();tag.setString("AuditPayload","retain-name-and-inventory");
            NBTTagList contents=new NBTTagList();NBTTagCompound stored=new NBTTagCompound();
            new ItemStack(Items.diamond,7).writeToNBT(stored);stored.setByte("Slot",(byte)2);contents.appendTag(stored);
            tag.setTag("Items",contents);if(tier>0)tag.setByte("Type",(byte)tier);if(previous>0)tag.setByte("Color",(byte)previous);box.setTagCompound(tag);
            ItemStack before=box.copy();
            ItemStack dye=new ItemStack(Items.dye,1,15-color);
            if(color==0)dye=ModItems.DYE.newItemStack(1,0);
            if(color==11)dye=ModItems.DYE.newItemStack(1,1);
            if(color==12)dye=ModItems.DYE.newItemStack(1,2);
            if(color==15)dye=ModItems.DYE.newItemStack(1,3);
            inv.setInventorySlotContents(0,box);inv.setInventorySlotContents(8,dye);
            ItemStack output=result(inv,world);ItemStack expected=before.copy();expected.getTagCompound().setByte("Color",(byte)(color+1));
            equals(output,expected,"shulker colour "+color+" from "+previous);
            equals(box,before,"shulker input unchanged");output.getTagCompound().setString("AuditPayload","mutated-output");
            equals(box,before,"shulker output NBT isolated");
            inv.setInventorySlotContents(4,new ItemStack(Items.diamond));check(result(inv,world)==null,"shulker consumed unrelated extra ingredient");
            inv.setInventorySlotContents(4,box.copy());check(result(inv,world)==null,"shulker accepted two boxes");
            inv.setInventorySlotContents(4,dye.copy());check(result(inv,world)==null,"shulker accepted two dyes");
            inv.setInventorySlotContents(4,null);inv.setInventorySlotContents(8,null);
            check(result(inv,world)==null,"shulker recoloured without dye");
        }
        InventoryCrafting inv=grid();inv.setInventorySlotContents(2,ModBlocks.STONE.newItemStack(1,3));inv.setInventorySlotContents(7,new ItemStack(Items.quartz));
        equals(result(inv,world),ModBlocks.STONE.newItemStack(1,1),"granite quantity");
        inv=grid();for(int slot:new int[]{0,1,3,4})inv.setInventorySlotContents(slot,ModItems.COPPER_INGOT.newItemStack());
        equals(result(inv,world),ModBlocks.COPPER_TRAPDOOR.newItemStack(),"copper trapdoor quantity");
        inv=grid();inv.setInventorySlotContents(0,new ItemStack(Items.blaze_rod));inv.setInventorySlotContents(3,ModItems.CHORUS_FRUIT_POPPED.newItemStack());
        equals(result(inv,world),ModBlocks.END_ROD.newItemStack(4),"end rod quantity");
        inv=grid();for(int i=0;i<3;i++)inv.setInventorySlotContents(i,ModBlocks.BAMBOO_MOSAIC.newItemStack());
        equals(result(inv,world),ModBlocks.BAMBOO_MOSAIC_SLAB.newItemStack(6,0),"canonical bamboo slab metadata");
        inv=grid();for(int slot:new int[]{0,1,3,4})inv.setInventorySlotContents(slot,new ItemStack(ModernMapParityBlocks.PALE_OAK_PLANKS.get()));
        equals(result(inv,world),new ItemStack(Blocks.crafting_table),"pale oak generic plank recipe");
        System.out.println("CRAFTING_SWEEP PASSED: "+(tiers.size()*272)+" shulker colour/tier/NBT cases, dye requirement, extra-input rejection, quantities, slab metadata and pale-oak generic crafting");
    }
}
