package ganymedes01.etfuturum.recipes.crafting;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeDyedShulkerBox extends ShapelessOreRecipe {

	public RecipeDyedShulkerBox(ItemStack result, Object[] recipe) {
		super(result, recipe);
		List<ItemStack> stacks = new ArrayList<ItemStack>();
        int tier=result.hasTagCompound()?result.getTagCompound().getByte("Type"):0;
        for (int i=0;i<=16;i++) {
            ItemStack shulker=new ItemStack(ModBlocks.SHULKER_BOX.get());
            if(i>0 || tier>0) {
                NBTTagCompound tag=new NBTTagCompound();
                if(i>0)tag.setByte("Color",(byte)i);
                if(tier>0)tag.setByte("Type",(byte)tier);
                shulker.setTagCompound(tag);
            }
            stacks.add(shulker);
        }
		getInput().set(0, stacks);
	}

	@Override
	public boolean matches(InventoryCrafting inv, World world) {
		// Match this recipe's specific colour and reject every extra occupied slot.
        if (!super.matches(inv, world)) return false;
        int type=getRecipeOutput().hasTagCompound()?getRecipeOutput().getTagCompound().getByte("Type"):0;
        for(int i=0;i<inv.getSizeInventory();i++) {
            ItemStack stack=inv.getStackInSlot(i);
            if(stack!=null && stack.getItem()==Item.getItemFromBlock(ModBlocks.SHULKER_BOX.get()))
                return (stack.hasTagCompound()?stack.getTagCompound().getByte("Type"):0)==type;
        }
        return false;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		if (!matches(inv, null)) return null;
		byte color = getRecipeOutput().getTagCompound().getByte("Color");
		ItemStack box = null;

		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack slot = inv.getStackInSlot(i);

			if (slot != null && slot.getItem() == Item.getItemFromBlock(ModBlocks.SHULKER_BOX.get())) {
				box = slot;
				break;
			}
		}

		if (box == null) return null;

		ItemStack newBox = box.copy();
		newBox.stackSize = 1;

		if (!newBox.hasTagCompound()) {
			newBox.setTagCompound(new NBTTagCompound());
		}
		newBox.getTagCompound().setByte("Color", color);
		return newBox;
	}

	@Override
	public int getRecipeSize() {
		return 2;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return super.getRecipeOutput();
	}

}
