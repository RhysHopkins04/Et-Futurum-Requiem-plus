package ganymedes01.etfuturum.compat.nei;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Interaction display, deliberately without a furnace fuel slot or crafting-grid transfer. */
public abstract class SimpleProcessRecipeHandler extends TemplateRecipeHandler {
    protected abstract List<ProcessRecipe> recipes();
    protected abstract String instruction();
    protected class ProcessRecipe extends CachedRecipe {
        final List<PositionedStack> inputs = new ArrayList<>();
        final PositionedStack output;
        final List<PositionedStack> other = new ArrayList<>();
        public ProcessRecipe(Object first, Object second, ItemStack result, Object catalyst) {
            inputs.add(new PositionedStack(first,18,18));
            if (second != null) inputs.add(new PositionedStack(second,54,18));
            output = new PositionedStack(result,116,18);
            if (catalyst != null) other.add(new PositionedStack(catalyst,82,42));
        }
        @Override public List<PositionedStack> getIngredients() { return getCycledIngredients(cycleticks/20,inputs); }
        @Override public PositionedStack getResult() { return output; }
        @Override public List<PositionedStack> getOtherStacks() { return other; }
        boolean uses(ItemStack stack) {
            for(PositionedStack p:inputs) for(ItemStack candidate:p.items)
                if(NEIServerUtils.areStacksSameTypeCrafting(candidate,stack)) return true;
            for(PositionedStack p:other) for(ItemStack candidate:p.items)
                if(NEIServerUtils.areStacksSameTypeCrafting(candidate,stack)) return true;
            return false;
        }
    }
    @Override public String getGuiTexture() { return "textures/gui/container/furnace.png"; }
    @Override public void loadCraftingRecipes(String id,Object... results) {
        if(getOverlayIdentifier().equals(id)) arecipes.addAll(recipes());
        else if("item".equals(id) && results.length>0 && results[0] instanceof ItemStack)loadCraftingRecipes((ItemStack)results[0]);
    }
    @Override public void loadCraftingRecipes(ItemStack result) {
        if(result==null)return;
        for(ProcessRecipe recipe:recipes()) if(NEIServerUtils.areStacksSameType(recipe.output.item,result)) arecipes.add(recipe);
    }
    @Override public void loadUsageRecipes(ItemStack input) {
        if(input==null)return;
        for(ProcessRecipe recipe:recipes()) if(recipe.uses(input)) arecipes.add(recipe);
    }
    @Override public void drawBackground(int recipe) {
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(17,17,55,16,18,18);
        if(arecipes.get(recipe).getIngredients().size()>1) GuiDraw.drawTexturedModalRect(53,17,55,16,18,18);
        GuiDraw.drawTexturedModalRect(111,13,111,30,26,26);
    }
    @Override public void drawExtras(int recipe) {
        GuiDraw.drawString("->",87,22,0x404040,false);
        GuiDraw.drawString(instruction(),4,64,0x404040,false);
    }
    @Override public int recipiesPerPage() { return 2; }
}
