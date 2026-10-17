package ganymedes01.etfuturum.blocks.itemblocks;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import java.util.List;

public class ItemBlockDecorationWorkbench extends ItemBlock {

	public ItemBlockDecorationWorkbench(Block p_i45328_1_) {
		super(p_i45328_1_);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> lore, boolean f3h) {
        if (field_150939_a instanceof ganymedes01.etfuturum.blocks.BlockLoom
                && ganymedes01.etfuturum.recipes.LoomRecipes.enabled()) return;
        if (field_150939_a instanceof ganymedes01.etfuturum.blocks.BlockStonecutter
                && ganymedes01.etfuturum.recipes.StonecutterRecipes.enabled()) return;
		if(field_150939_a instanceof ganymedes01.etfuturum.blocks.BlockCartographyTable
                && ganymedes01.etfuturum.recipes.CartographyRecipes.enabled())return;
        lore.add("\u00a7o" + StatCollector.translateToLocal("efr.decoration.only"));
	}

}
