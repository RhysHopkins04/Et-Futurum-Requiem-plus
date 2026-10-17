package ganymedes01.etfuturum.recipes;

import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

/** Shared admission rule for campfire gameplay and its fuel-free NEI display. */
public final class CampfireRecipes {
    private CampfireRecipes() {}
    public static final int COOK_TIME = 600;
    public static ItemStack getCookingResult(ItemStack input) {
        if (input == null) return null;
        boolean kelp=ganymedes01.etfuturum.ModernMapParityBlocks.KELP.get()!=null
                && input.getItem()==net.minecraft.item.Item.getItemFromBlock(ganymedes01.etfuturum.ModernMapParityBlocks.KELP.get());
        if (!kelp && !(input.getItem() instanceof ItemFood)) return null;
        ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(input);
        return result != null && result.getItem() instanceof ItemFood
                && ((ItemFood) result.getItem()).func_150905_g(result) > 0 ? result : null;
    }
}
