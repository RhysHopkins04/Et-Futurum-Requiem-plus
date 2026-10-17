package ganymedes01.etfuturum.compat.nei;

import codechicken.nei.NEIServerUtils;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import ganymedes01.etfuturum.core.utils.ItemStackMap;

/** Query the live workstation registry for every lookup: never reuse another item's results. */
public abstract class FilteredFurnaceRecipeHandler extends FurnaceRecipeHandler {
    protected abstract ItemStack lookup(ItemStack input);
    protected abstract ItemStack machine();
    protected abstract ItemStackMap<ItemStack> explicitRecipes();

    private void load(ItemStack output, ItemStack input) {
        ItemStackMap<ItemStack> candidates = new ItemStackMap<>();
        candidates.putAll(FurnaceRecipes.smelting().getSmeltingList());
        candidates.putAll(explicitRecipes());
        for (ItemStack source : candidates.keySet()) {
            ItemStack result = lookup(source);
            if (result != null && (output == null || NEIServerUtils.areStacksSameType(result, output))
                    && (input == null || NEIServerUtils.areStacksSameTypeCrafting(source, input))) {
                arecipes.add(new SmeltingPair(source.copy(), result.copy()));
            }
        }
    }
    @Override public void loadCraftingRecipes(String id, Object... results) {
        if (getOverlayIdentifier().equals(id)) load(null, null);
        else if ("item".equals(id) && results.length>0 && results[0] instanceof ItemStack) loadCraftingRecipes((ItemStack)results[0]);
        // The furnace's generic smelting/fuel categories must not populate this workstation.
    }
    @Override public void loadCraftingRecipes(ItemStack result) { if (result != null) load(result, null); }
    @Override public void loadUsageRecipes(ItemStack input) {
        if (input == null) return;
        if (NEIServerUtils.areStacksSameTypeCrafting(machine(), input)) load(null, null);
        else load(null, input);
    }
    public void clearCache() { /* No result-dependent display caches. */ }
}
