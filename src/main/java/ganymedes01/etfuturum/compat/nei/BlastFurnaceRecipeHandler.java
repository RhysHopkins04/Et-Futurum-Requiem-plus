package ganymedes01.etfuturum.compat.nei;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.gui.inventory.GuiBlastFurnace;
import ganymedes01.etfuturum.core.utils.ItemStackMap;
import ganymedes01.etfuturum.recipes.BlastFurnaceRecipes;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import java.awt.Rectangle;

public class BlastFurnaceRecipeHandler extends FilteredFurnaceRecipeHandler {
    @Override protected ItemStack lookup(ItemStack input) { return BlastFurnaceRecipes.smelting().getSmeltingResult(input); }
    @Override protected ItemStack machine() { return ModBlocks.BLAST_FURNACE.newItemStack(); }
    @Override protected ItemStackMap<ItemStack> explicitRecipes() { return BlastFurnaceRecipes.smelting().smeltingList; }
    @Override public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(74,23,24,18),getOverlayIdentifier()));
        transferRects.add(new RecipeTransferRect(new Rectangle(50,23,18,18),"fuel"));
    }
    @Override public String getRecipeName() { return StatCollector.translateToLocal("efr.nei.blast_furnace"); }
    @Override public String getOverlayIdentifier() { return "etfuturum.blastfurnace"; }
    @Override public Class<? extends GuiContainer> getGuiClass() { return GuiBlastFurnace.class; }
}
