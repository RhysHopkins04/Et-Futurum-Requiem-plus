package ganymedes01.etfuturum.compat.nei;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.recipes.CampfireRecipes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.StatCollector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CampfireRecipeHandler extends SimpleProcessRecipeHandler {
    @Override public String getOverlayIdentifier() { return "etfuturum.campfire"; }
    @Override public String getRecipeName() { return StatCollector.translateToLocal("efr.nei.campfire"); }
    @Override protected String instruction() { return StatCollector.translateToLocal("efr.nei.campfire.instruction"); }
    @Override protected List<ProcessRecipe> recipes() {
        List<ProcessRecipe> recipes=new ArrayList<>();
        if(ModernMapParityBlocks.CAMPFIRE.get()==null || ModernMapParityBlocks.SOUL_CAMPFIRE.get()==null)return recipes;
        for(Object key:FurnaceRecipes.smelting().getSmeltingList().keySet()) {
            ItemStack input=(ItemStack)key, output=CampfireRecipes.getCookingResult(input);
            if(output!=null)recipes.add(new ProcessRecipe(input,null,output,Arrays.asList(
                    new ItemStack(ModernMapParityBlocks.CAMPFIRE.get()),new ItemStack(ModernMapParityBlocks.SOUL_CAMPFIRE.get()))));
        }
        return recipes;
    }
}
