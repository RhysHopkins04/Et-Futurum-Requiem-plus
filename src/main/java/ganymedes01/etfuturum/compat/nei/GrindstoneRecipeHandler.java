package ganymedes01.etfuturum.compat.nei;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.recipes.GrindstoneRecipes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import java.util.*;

/** Representative repair/disenchant recipes; usage queries calculate from the actual item NBT. */
public final class GrindstoneRecipeHandler extends SimpleProcessRecipeHandler {
    @Override public String getOverlayIdentifier() { return "etfuturum.grinding"; }
    @Override public String getRecipeName() { return StatCollector.translateToLocal("container.grindstone_title"); }
    @Override protected String instruction() { return StatCollector.translateToLocal("efr.nei.grindstone.instruction"); }
    private void add(List<ProcessRecipe> rows,ItemStack first,ItemStack second) {
        ItemStack output=GrindstoneRecipes.result(first,second);
        if(output!=null)rows.add(new ProcessRecipe(first,second,output,new ItemStack(ModernMapParityBlocks.GRINDSTONE.get())));
    }
    @Override protected List<ProcessRecipe> recipes() {
        List<ProcessRecipe> rows=new ArrayList<>();if(!GrindstoneRecipes.enabled())return rows;
        // Do not manufacture empty-NBT stacks for every third-party item: some require initialized data.
        for(Item item:new Item[]{Items.wooden_sword,Items.stone_pickaxe,Items.iron_axe,Items.diamond_sword,Items.bow,Items.shears,Items.iron_helmet,Items.fishing_rod}) {
            ItemStack tool=new ItemStack(item);tool.setItemDamage(tool.getMaxDamage()/2);add(rows,tool,tool.copy());
        }
        ItemStack book=new ItemStack(Items.enchanted_book);Items.enchanted_book.addEnchantment(book,new EnchantmentData(Enchantment.unbreaking,1));add(rows,book,null);
        return rows;
    }
    @Override public void loadCraftingRecipes(ItemStack result) {
        if(result==null || !GrindstoneRecipes.enabled())return;
        if(!result.isItemStackDamageable()) { super.loadCraftingRecipes(result);return; }
        ItemStack expected=result.copy();expected.stackSize=1;
        int max=expected.getMaxDamage(),damage=expected.getItemDamage(),bonus=max*5/100;
        if(damage<0 || damage>max-2-bonus)return;
        ItemStack first=expected.copy(),second=expected.copy();first.setItemDamage(max-1);second.setItemDamage(bonus+damage+1);
        ItemStack actual=GrindstoneRecipes.result(first,second);
        if(ItemStack.areItemStacksEqual(actual,expected))arecipes.add(new ProcessRecipe(first,second,actual,new ItemStack(ModernMapParityBlocks.GRINDSTONE.get())));
    }
    @Override public void loadUsageRecipes(ItemStack input) {
        if(input==null || !GrindstoneRecipes.enabled())return;
        if(input.getItem()==Item.getItemFromBlock(ModernMapParityBlocks.GRINDSTONE.get())) { arecipes.addAll(recipes());return; }
        ItemStack single=input.copy();single.stackSize=1;List<ProcessRecipe> rows=new ArrayList<>();
        add(rows,single,null);if(GrindstoneRecipes.accepts(single))add(rows,single,single.copy());arecipes.addAll(rows);
    }
}
