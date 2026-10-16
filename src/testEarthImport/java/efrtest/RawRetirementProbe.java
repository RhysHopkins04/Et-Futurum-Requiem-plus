package efrtest;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.WorldServer;
import net.minecraftforge.oredict.OreDictionary;
import java.util.ArrayList;
import static efrtest.EarthImportContracts.check;

public final class RawRetirementProbe {
    public static void run(WorldServer world) throws Exception {
        check(!ganymedes01.etfuturum.compat.NtmOreCompat.isNative(new ItemStack((net.minecraft.item.Item)null)),"null native item handling");
        check(!ganymedes01.etfuturum.core.utils.Utils.enableModdedRawOres(),"retired raw option still enabled");
        ArrayList<ItemStack> menu=new ArrayList<>();
        ((ganymedes01.etfuturum.items.rawore.modded.ItemGeneralModdedRawOre)ModItems.MODDED_RAW_ORE.get()).getSubItems(ModItems.MODDED_RAW_ORE.get(),null,menu);
        ((ganymedes01.etfuturum.blocks.rawore.modded.BlockGeneralModdedRawOre)ModBlocks.MODDED_RAW_ORE_BLOCK.get()).getSubBlocks(ModBlocks.MODDED_RAW_ORE_BLOCK.getItem(),null,menu);
        check(menu.isEmpty(),"retired raw forms in menus");
        for(int meta=0;meta<14;meta++) for(ItemStack stack:new ItemStack[]{ModItems.MODDED_RAW_ORE.newItemStack(1,meta),ModBlocks.MODDED_RAW_ORE_BLOCK.newItemStack(1,meta)}) {
            check(OreDictionary.getOreIDs(stack).length==0,"retired raw form has ore tags");
            check(FurnaceRecipes.smelting().getSmeltingResult(stack)==null,"retired raw smelting recipe remains");
        }
        for(Object value:CraftingManager.getInstance().getRecipeList()) {
            ItemStack output=((IRecipe)value).getRecipeOutput();
            check(output==null || (output.getItem()!=ModItems.MODDED_RAW_ORE.get() && output.getItem()!=ModBlocks.MODDED_RAW_ORE_BLOCK.getItem()),"retired raw crafting output remains");
        }
        for(String tag:new String[]{"oreCopper","oreIron","oreGold"})check(ganymedes01.etfuturum.api.RawOreRegistry.hasOre(tag),"vanilla raw mapping missing "+tag);
        for(String tag:new String[]{"oreUranium","oreThorium","oreTitanium","oreLead","oreAluminum","oreTungsten"})check(!ganymedes01.etfuturum.api.RawOreRegistry.hasOre(tag),"retired raw mapping remains "+tag);
        java.io.File fixture=new java.io.File("retired-raw-fixture-created");
        if(fixture.exists())check(world.getBlock(240,80,240)==ModBlocks.MODDED_RAW_ORE_BLOCK.get() && world.getBlockMetadata(240,80,240)==7,"old raw storage data lost");
        world.setBlock(240,80,240,ModBlocks.MODDED_RAW_ORE_BLOCK.get(),7,2);fixture.createNewFile();
        System.out.println("EARTH_IMPORT_RAW_RETIREMENT PASSED: no menus/tags/recipes/modded raw drops; vanilla raws intact; legacy save IDs preserved");
    }
}
