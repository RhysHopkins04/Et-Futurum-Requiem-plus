package ganymedes01.etfuturum.compat.nei;

import codechicken.nei.*;
import codechicken.nei.recipe.TemplateRecipeHandler;
import codechicken.lib.gui.GuiDraw;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.recipes.*;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import java.util.*;

/** Three input modern recipes and explicitly empty-template legacy custom recipes. */
public class SmithingRecipeHandler extends TemplateRecipeHandler {
    private final class Row extends CachedRecipe {
        private final List<PositionedStack> inputs=new ArrayList<>();
        private final PositionedStack output;
        Row(ItemStack template,Object base,Object addition,ItemStack result) {
            if(template!=null)inputs.add(new PositionedStack(template,8,18));
            inputs.add(new PositionedStack(base,44,18));inputs.add(new PositionedStack(addition,80,18));output=new PositionedStack(result,134,18);
        }
        @Override public List<PositionedStack> getIngredients() { return getCycledIngredients(cycleticks/20,inputs); }
        @Override public PositionedStack getResult() { return output; }
        boolean uses(ItemStack stack) { for(PositionedStack ingredient:inputs)for(ItemStack candidate:ingredient.items)if(NEIServerUtils.areStacksSameTypeCrafting(candidate,stack))return true;return false; }
    }
    @Override public String getOverlayIdentifier() { return "etfuturum.smithing"; }
    @Override public String getRecipeName() { return StatCollector.translateToLocal("efr.nei.smithing"); }
    @Override public String getGuiTexture() { return "textures/gui/container/furnace.png"; }
    private List<Row> rows(ItemStack base) {
        List<Row> rows=new ArrayList<>();if(!ModernSmithingRecipes.enabled())return rows;
        for(SmithingTableRecipes.SmithingTableRecipe recipe:SmithingTableRecipes.getInstance().getRecipes())rows.add(new Row(recipe.isBuiltinUpgrade()?ModItems.SMITHING_TEMPLATE.newItemStack():null,recipe.getInputItem(),recipe.getMaterial(),recipe.getRecipeOutput().copy()));
        ItemStack armor=base==null?new ItemStack(Items.diamond_chestplate):base.copy();armor.stackSize=1;
        if(ModernSmithingRecipes.trimmable(armor))for(int pattern=1;pattern<19;pattern++)for(String material:ModernSmithingRecipes.MATERIALS) {
            ItemStack template=ModItems.SMITHING_TEMPLATE.newItemStack(1,pattern),addition=ModernSmithingRecipes.materialExample(material);
            ItemStack output=ModernSmithingRecipes.result(template,armor,addition,null);if(output!=null)rows.add(new Row(template,armor,addition,output));
        }
        return rows;
    }
    @Override public void loadCraftingRecipes(String id,Object... results) {
        if(getOverlayIdentifier().equals(id))arecipes.addAll(rows(null));else if("item".equals(id) && results.length>0 && results[0] instanceof ItemStack)loadCraftingRecipes((ItemStack)results[0]);
    }
    @Override public void loadCraftingRecipes(ItemStack result) {
        if(result==null)return;
        for(Row row:rows(result))if(NEIServerUtils.areStacksSameType(row.output.item,result))arecipes.add(row);
        // A requested trimmed armor output can be reconstructed from its own untrimmed base.
        net.minecraft.nbt.NBTTagCompound trim=ModernSmithingRecipes.trim(result);
        if(trim!=null) { ItemStack base=result.copy();base.getTagCompound().removeTag("Trim");for(Row row:rows(base))if(ItemStack.areItemStackTagsEqual(row.output.item,result) && row.output.item.isItemEqual(result))arecipes.add(row); }
    }
    @Override public void loadUsageRecipes(ItemStack input) { if(input!=null)for(Row row:rows(input))if(row.uses(input))arecipes.add(row); }
    @Override public void drawBackground(int recipe) {
        GuiDraw.changeTexture(getGuiTexture());for(int x:new int[]{7,43,79})GuiDraw.drawTexturedModalRect(x,17,55,16,18,18);GuiDraw.drawTexturedModalRect(129,13,111,30,26,26);
    }
    @Override public void drawExtras(int recipe) { GuiDraw.drawString("->",110,22,0x404040,false);GuiDraw.drawString(StatCollector.translateToLocal("efr.nei.smithing.modern"),4,53,0x404040,false); }
    @Override public int recipiesPerPage() { return 2; }
}
