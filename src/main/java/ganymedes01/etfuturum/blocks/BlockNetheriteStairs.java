package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.ModBlocks;

public class BlockNetheriteStairs extends BaseStairs {

	public BlockNetheriteStairs() {
		super(ModBlocks.NETHERITE_BLOCK.get(), 0);
		setUnlocalizedNameWithPrefix("netherite");
		setCreativeTab(null); // obsolete compatibility-only registry block; Minecraft 1.21.11 has no Netherite Stairs
	}
}
