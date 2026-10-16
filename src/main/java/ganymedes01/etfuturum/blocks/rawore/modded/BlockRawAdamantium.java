package ganymedes01.etfuturum.blocks.rawore.modded;

import ganymedes01.etfuturum.blocks.rawore.BaseRawOreBlock;
import ganymedes01.etfuturum.compat.ExternalContent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockRawAdamantium extends BaseRawOreBlock {
	public BlockRawAdamantium() {
		super(Material.rock);
		setNames("raw_adamantium_block");
		setCreativeTab(null);
	}

	@Override public void getSubBlocks(net.minecraft.item.Item item, net.minecraft.creativetab.CreativeTabs tab, java.util.List<net.minecraft.item.ItemStack> list) {}
	@Override public void onLoadAction() {}

	@Override
	public String getTextureSubfolder() {
		return "simpleores";
	}

	@Override
	protected Block getBase() {
		return ExternalContent.Blocks.SIMPLEORES_ADAMANTIUM_BLOCK.get();
	}
}
