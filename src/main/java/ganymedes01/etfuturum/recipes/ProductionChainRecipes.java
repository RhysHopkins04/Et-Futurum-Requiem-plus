package ganymedes01.etfuturum.recipes;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.core.utils.RecipeHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** Companion ingredients for the existing aquatic and resin block registries. */
public final class ProductionChainRecipes {
    private ProductionChainRecipes() {}
    public static void init() {
        if (ConfigMapCompatibility.mapCompatibilityMode) return;
        LoomRecipes.init();
        if (ModItems.DRIED_KELP.isEnabled() && ModernMapParityBlocks.KELP.get()!=null) {
            RecipeHelper.addSmelting(new ItemStack(ModernMapParityBlocks.KELP.get(),1,OreDictionary.WILDCARD_VALUE),ModItems.DRIED_KELP.newItemStack(),0.1F);
            RecipeHelper.addShapedRecipe(RecipeHelper.Priority.HIGH,stack(ModernMapParityBlocks.DRIED_KELP_BLOCK,1),
                    "KKK","KKK","KKK",'K',ModItems.DRIED_KELP.get());
            RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.HIGH,ModItems.DRIED_KELP.newItemStack(9),ModernMapParityBlocks.DRIED_KELP_BLOCK.get());
        }
        if (ModItems.RESIN_BRICK.isEnabled() && ModernMapParityBlocks.RESIN_CLUMP.get()!=null) {
            RecipeHelper.addSmelting(new ItemStack(ModernMapParityBlocks.RESIN_CLUMP.get(),1,OreDictionary.WILDCARD_VALUE),ModItems.RESIN_BRICK.newItemStack(),0.1F);
            shaped(ModernMapParityBlocks.RESIN_BLOCK,1,new String[]{"RRR","RRR","RRR"},ModernMapParityBlocks.RESIN_CLUMP.get());
            RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.HIGH,stack(ModernMapParityBlocks.RESIN_CLUMP,9),ModernMapParityBlocks.RESIN_BLOCK.get());
            RecipeHelper.addShapedRecipe(RecipeHelper.Priority.HIGH,stack(ModernMapParityBlocks.RESIN_BRICKS,1),"RR","RR",'R',ModItems.RESIN_BRICK.get());
            shaped(ModernMapParityBlocks.RESIN_BRICK_SLAB,6,new String[]{"RRR"},ModernMapParityBlocks.RESIN_BRICKS.get());
            shaped(ModernMapParityBlocks.RESIN_BRICK_STAIRS,4,new String[]{"R  ","RR ","RRR"},ModernMapParityBlocks.RESIN_BRICKS.get());
            shaped(ModernMapParityBlocks.RESIN_BRICK_WALL,6,new String[]{"RRR","RRR"},ModernMapParityBlocks.RESIN_BRICKS.get());
            shaped(ModernMapParityBlocks.CHISELED_RESIN_BRICKS,1,new String[]{"R","R"},ModernMapParityBlocks.RESIN_BRICK_SLAB.get());
        }
    }
    private static ItemStack stack(ModernMapParityBlocks block,int count) {
        return block.get()==null?null:new ItemStack(block.get(),count);
    }
    private static void shaped(ModernMapParityBlocks output,int count,String[] rows,Object input) {
        java.util.List<Object> recipe=new java.util.ArrayList<>();
        java.util.Collections.addAll(recipe,rows);recipe.add('R');recipe.add(input);
        RecipeHelper.addShapedRecipe(RecipeHelper.Priority.HIGH,stack(output,count),recipe.toArray());
    }
}
