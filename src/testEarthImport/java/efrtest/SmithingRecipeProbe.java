package efrtest;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.recipes.SmithingTableRecipes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import static efrtest.EarthImportContracts.check;

/** Exercise the recipes populated by real mod startup; never initialize them in the test. */
public final class SmithingRecipeProbe {
    private SmithingRecipeProbe() {}

    public static void run(World world, boolean legacy) {
        SmithingTableRecipes recipes = SmithingTableRecipes.getInstance();
        if (legacy || !ConfigBlocksItems.enableNetherite) {
            check(recipes.getRecipes().isEmpty(), "smithing recipes bypass profile/feature suppression");
            System.out.println("EARTH_IMPORT_SMITHING PASSED: suppressed profile/feature has no built-in upgrades");
            return;
        }
        InventoryCrafting grid = new InventoryCrafting(new Container() {
            @Override public boolean canInteractWith(EntityPlayer player) { return true; }
        }, 2, 1);
        Item[] diamond = {Items.diamond_pickaxe, Items.diamond_axe, Items.diamond_shovel,
                Items.diamond_sword, Items.diamond_hoe, Items.diamond_helmet,
                Items.diamond_chestplate, Items.diamond_leggings, Items.diamond_boots};
        ModItems[] netherite = {ModItems.NETHERITE_PICKAXE, ModItems.NETHERITE_AXE, ModItems.NETHERITE_SPADE,
                ModItems.NETHERITE_SWORD, ModItems.NETHERITE_HOE, ModItems.NETHERITE_HELMET,
                ModItems.NETHERITE_CHESTPLATE, ModItems.NETHERITE_LEGGINGS, ModItems.NETHERITE_BOOTS};
        for (int i = 0; i < diamond.length; i++) {
            ItemStack input = new ItemStack(diamond[i], 1, 17);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("SmithingProbe", "preserve-custom-data");
            input.setTagCompound(tag);
            ItemStack before = input.copy();
            grid.setInventorySlotContents(0, input);
            grid.setInventorySlotContents(1, ModItems.NETHERITE_INGOT.newItemStack());
            ItemStack output = recipes.findMatchingRecipe(grid, world);
            check(output != null && output.getItem() == netherite[i].get() && output.stackSize == 1,
                    "missing/incorrect netherite smithing upgrade " + diamond[i].getUnlocalizedName());
            check(output.getItemDamage() == 17 && tag.equals(output.getTagCompound()),
                    "smithing lost damage/custom data");
            check(ItemStack.areItemStacksEqual(before, input), "recipe query changed smithing input");
            grid.setInventorySlotContents(1, new ItemStack(Items.gold_ingot));
            check(recipes.findMatchingRecipe(grid, world) == null, "smithing accepted wrong upgrade material");
            grid.setInventorySlotContents(0, ModItems.NETHERITE_INGOT.newItemStack());
            grid.setInventorySlotContents(1, input);
            check(recipes.findMatchingRecipe(grid, world) == null, "smithing accepted reversed slots");
        }
        System.out.println("EARTH_IMPORT_SMITHING PASSED: all 9 upgrades, quantities, damage/NBT, unchanged inputs and invalid ingredients");
    }
}
