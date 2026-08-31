package ganymedes01.etfuturum.core.utils;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.Tags;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.Iterator;
import java.util.function.Predicate;

/** Self-contained recipe helpers formerly consumed from HogUtils. */
public final class RecipeHelper {
    private RecipeHelper() {}

    public static void init() {
        RecipeSorter.register(Tags.MOD_ID + ":shapedHiPriority", HighPriorityShapedRecipe.class,
            RecipeSorter.Category.SHAPED, "before:minecraft:shaped");
        RecipeSorter.register(Tags.MOD_ID + ":shapelessHiPriority", HighPriorityShapelessRecipe.class,
            RecipeSorter.Category.SHAPELESS, "before:minecraft:shapeless");
        RecipeSorter.register(Tags.MOD_ID + ":shapedLoPriority", LowPriorityShapedRecipe.class,
            RecipeSorter.Category.SHAPED, "after:minecraft:shaped");
        RecipeSorter.register(Tags.MOD_ID + ":shapelessLoPriority", LowPriorityShapelessRecipe.class,
            RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");
    }

    public static void registerOre(String name, ItemStack ore) { if (validateItems(ore)) OreDictionary.registerOre(name, ore); }
    public static void registerOre(String name, Item ore) { if (validateItems(ore)) OreDictionary.registerOre(name, ore); }
    public static void registerOre(String name, Block ore) { if (validateItems(ore)) OreDictionary.registerOre(name, ore); }

    public static void addSmelting(Item input, ItemStack output, float exp) { if (validateItems(input, output)) GameRegistry.addSmelting(input, output, exp); }
    public static void addSmelting(Block input, ItemStack output, float exp) { if (validateItems(input, output)) GameRegistry.addSmelting(input, output, exp); }
    public static void addSmelting(ItemStack input, ItemStack output, float exp) { if (validateItems(input, output)) GameRegistry.addSmelting(input, output, exp); }

    public static void addShapelessRecipe(Item output, Object... objects) { addShapelessRecipe(Priority.NORMAL, new ItemStack(output), objects); }
    public static void addShapelessRecipe(Block output, Object... objects) { addShapelessRecipe(Priority.NORMAL, new ItemStack(output), objects); }
    public static void addShapelessRecipe(ItemStack output, Object... objects) { addShapelessRecipe(Priority.NORMAL, output, objects); }
    public static void addShapelessRecipe(Priority priority, Item output, Object... objects) { addShapelessRecipe(priority, new ItemStack(output), objects); }
    public static void addShapelessRecipe(Priority priority, Block output, Object... objects) { addShapelessRecipe(priority, new ItemStack(output), objects); }
    public static void addShapelessRecipe(Priority priority, ItemStack output, Object... objects) { priority.addShapelessRecipe(output, objects); }

    public static void addShapedRecipe(Item output, Object... objects) { addShapedRecipe(Priority.NORMAL, new ItemStack(output), objects); }
    public static void addShapedRecipe(Block output, Object... objects) { addShapedRecipe(Priority.NORMAL, new ItemStack(output), objects); }
    public static void addShapedRecipe(ItemStack output, Object... objects) { addShapedRecipe(Priority.NORMAL, output, objects); }
    public static void addShapedRecipe(Priority priority, Item output, Object... objects) { addShapedRecipe(priority, new ItemStack(output), objects); }
    public static void addShapedRecipe(Priority priority, Block output, Object... objects) { addShapedRecipe(priority, new ItemStack(output), objects); }
    public static void addShapedRecipe(Priority priority, ItemStack output, Object... objects) { priority.addShapedRecipe(output, objects); }

    public static void removeAllRecipesWithOutput(ItemStack find) { removeAllRecipesWithOutput(find, true); }
    public static void removeAllRecipesWithOutput(ItemStack find, boolean wildcards) { removeAllRecipesWithOutput(find.getItem(), find.getItemDamage(), wildcards); }
    public static void removeAllRecipesWithOutput(Block block, int meta, boolean wildcards) { removeAllRecipesWithOutput(Item.getItemFromBlock(block), meta, wildcards); }
    public static void removeAllRecipesWithOutput(Item item, int meta, boolean wildcards) {
        removeAllMatchingRecipes(recipe -> matchesOutput(recipe, item, meta, wildcards));
    }

    @SuppressWarnings("unchecked")
    public static void removeAllMatchingRecipes(Predicate<IRecipe> condition) {
        CraftingManager.getInstance().getRecipeList().removeIf(condition);
    }

    public static void removeFirstRecipeWithOutput(ItemStack find) { removeFirstRecipeWithOutput(find, true); }
    public static void removeFirstRecipeWithOutput(ItemStack find, boolean wildcards) { removeFirstRecipeWithOutput(find.getItem(), find.getItemDamage(), wildcards); }
    public static void removeFirstRecipeWithOutput(Block block, int meta, boolean wildcards) { removeFirstRecipeWithOutput(Item.getItemFromBlock(block), meta, wildcards); }
    public static void removeFirstRecipeWithOutput(Item item, int meta, boolean wildcards) {
        removeFirstMatchingRecipe(recipe -> matchesOutput(recipe, item, meta, wildcards));
    }

    @SuppressWarnings("unchecked")
    public static void removeFirstMatchingRecipe(Predicate<IRecipe> condition) {
        Iterator<IRecipe> iterator = CraftingManager.getInstance().getRecipeList().iterator();
        while (iterator.hasNext()) {
            if (condition.test(iterator.next())) { iterator.remove(); return; }
        }
    }

    private static boolean matchesOutput(IRecipe recipe, Item item, int meta, boolean wildcards) {
        ItemStack stack = recipe.getRecipeOutput();
        return stack != null && stack.getItem() == item
            && ((wildcards && stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) || meta == stack.getItemDamage());
    }

    /** Return false when any recipe object is absent/unregistered. */
    public static boolean validateItems(Object... objects) {
        for (Object object : objects) {
            if (object == null || object == Blocks.air) return false;
            if (object instanceof String || object instanceof Character) continue;
            if (object instanceof Object[] nested) {
                if (!validateItems(nested)) return false;
                continue;
            }
            if (object instanceof ItemStack stack) {
                Item item = stack.getItem();
                if (item == null || item.delegate.name() == null) return false;
                continue;
            }
            if (object instanceof Item item && item.delegate.name() == null) return false;
            if (object instanceof Block block && block.delegate.name() == null) return false;
        }
        return true;
    }

    public static class Templates {
        public void addOreBlockRecipe(Priority priority, Block oreBlock, int oreBlockMeta, Item material, int materialMeta, boolean small) {
            if (small) add2by2Recipe(priority, new ItemStack(oreBlock, 1, oreBlockMeta), new ItemStack(material, 1, materialMeta));
            else add3by3Recipe(priority, new ItemStack(oreBlock, 1, oreBlockMeta), new ItemStack(material, 1, materialMeta));
            addShapedRecipe(priority, new ItemStack(material, small ? 4 : 9, materialMeta), "x", 'x', new ItemStack(oreBlock, 1, oreBlockMeta));
        }
        public void add2by2Recipe(Priority priority, ItemStack output, Object input) { addShapedRecipe(priority, output, "xx", "xx", 'x', input); }
        public void add3by3Recipe(Priority priority, ItemStack output, Object input) { addShapedRecipe(priority, output, "xxx", "xxx", "xxx", 'x', input); }
        public void addSlabRecipe(Priority priority, Block output, int outputMeta, Object input) { addShapedRecipe(priority, new ItemStack(output, 6, outputMeta), "xxx", 'x', input); }
        public void addStairRecipe(Priority priority, Block output, int outputMeta, Object input) { addShapedRecipe(priority, new ItemStack(output, 4, outputMeta), "x  ", "xx ", "xxx", 'x', input); }
    }

    public static class LowPriorityShapedRecipe extends ShapedOreRecipe { public LowPriorityShapedRecipe(ItemStack result, Object... recipe) { super(result, recipe); } }
    public static class HighPriorityShapedRecipe extends ShapedOreRecipe { public HighPriorityShapedRecipe(ItemStack result, Object... recipe) { super(result, recipe); } }
    public static class LowPriorityShapelessRecipe extends ShapelessOreRecipe { public LowPriorityShapelessRecipe(ItemStack result, Object... recipe) { super(result, recipe); } }
    public static class HighPriorityShapelessRecipe extends ShapelessOreRecipe { public HighPriorityShapelessRecipe(ItemStack result, Object... recipe) { super(result, recipe); } }

    public enum Priority {
        HIGH, NORMAL, LOW;
        private void addShapelessRecipe(ItemStack output, Object... objects) {
            if (!validateItems(output) || !validateItems(objects)) return;
            switch (this) {
                case LOW -> GameRegistry.addRecipe(new LowPriorityShapelessRecipe(output, objects));
                case NORMAL -> GameRegistry.addShapelessRecipe(output, objects);
                case HIGH -> GameRegistry.addRecipe(new HighPriorityShapelessRecipe(output, objects));
            }
        }
        private void addShapedRecipe(ItemStack output, Object... objects) {
            if (!validateItems(output) || !validateItems(objects)) return;
            switch (this) {
                case LOW -> GameRegistry.addRecipe(new LowPriorityShapedRecipe(output, objects));
                case NORMAL -> GameRegistry.addShapedRecipe(output, objects);
                case HIGH -> GameRegistry.addRecipe(new HighPriorityShapedRecipe(output, objects));
            }
        }
    }
}
