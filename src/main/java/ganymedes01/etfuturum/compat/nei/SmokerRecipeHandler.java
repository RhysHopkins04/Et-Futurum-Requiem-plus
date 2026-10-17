package ganymedes01.etfuturum.compat.nei;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.gui.inventory.GuiSmoker;
import ganymedes01.etfuturum.core.utils.ItemStackMap;
import ganymedes01.etfuturum.recipes.SmokerRecipes;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import java.awt.Rectangle;

public class SmokerRecipeHandler extends FilteredFurnaceRecipeHandler {
    @Override protected ItemStack lookup(ItemStack input) { return SmokerRecipes.smelting().getSmeltingResult(input); }
    @Override protected ItemStack machine() { return ModBlocks.SMOKER.newItemStack(); }
    @Override protected ItemStackMap<ItemStack> explicitRecipes() { return SmokerRecipes.smelting().smeltingList; }
    @Override public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(74,23,24,18),getOverlayIdentifier()));
        transferRects.add(new RecipeTransferRect(new Rectangle(50,23,18,18),"fuel"));
    }
    @Override public String getRecipeName() { return StatCollector.translateToLocal("efr.nei.smoker"); }
    @Override public String getOverlayIdentifier() { return "etfuturum.smoker"; }
    @Override public Class<? extends GuiContainer> getGuiClass() { return GuiSmoker.class; }
}
