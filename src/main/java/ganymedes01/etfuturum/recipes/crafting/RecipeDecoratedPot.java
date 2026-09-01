package ganymedes01.etfuturum.recipes.crafting;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.ModernPotterySherds;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;

/** Four-way decorated-pot recipe: top/back, left, right, and bottom/front. */
public final class RecipeDecoratedPot implements IRecipe {
    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return decorations(inventory) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        String[] sherds = decorations(inventory);
        if (sherds == null || ModernMapParityBlocks.DECORATED_POT.get() == null) return null;
        ItemStack result = new ItemStack(Item.getItemFromBlock(ModernMapParityBlocks.DECORATED_POT.get()));
        NBTTagCompound blockEntityTag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (String sherd : sherds) list.appendTag(new NBTTagString(sherd));
        blockEntityTag.setTag("sherds", list);
        result.setTagInfo("BlockEntityTag", blockEntityTag);
        return result;
    }

    @Override
    public int getRecipeSize() {
        return 4;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ModernMapParityBlocks.DECORATED_POT.get() == null ? null
                : new ItemStack(Item.getItemFromBlock(ModernMapParityBlocks.DECORATED_POT.get()));
    }

    private static String[] decorations(InventoryCrafting inventory) {
        if (inventory == null || inventory.getSizeInventory() != 9) return null;
        int[] used = {1, 3, 5, 7};
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            boolean expected = false;
            for (int target : used) if (slot == target) expected = true;
            ItemStack stack = inventory.getStackInSlot(slot);
            if (expected != (stack != null)) return null;
        }
        // Modern NBT order is back, left, right, front.
        String[] result = {decoration(inventory.getStackInSlot(1)), decoration(inventory.getStackInSlot(3)),
                decoration(inventory.getStackInSlot(5)), decoration(inventory.getStackInSlot(7))};
        for (String decoration : result) if (decoration == null) return null;
        return result;
    }

    private static String decoration(ItemStack stack) {
        if (stack == null) return null;
        if (stack.getItem() == net.minecraft.init.Items.brick) return "minecraft:brick";
        ModernPotterySherds sherd = ModernPotterySherds.fromItem(stack.getItem());
        return sherd == null ? null : "minecraft:" + sherd.getRegistryName();
    }
}
