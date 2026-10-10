package ganymedes01.etfuturum.blocks.itemblocks;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Preserve the source water before the vanilla item placement overwrites it. */
public class ItemBlockBigDripleaf extends ItemBlock {
    public ItemBlockBigDripleaf(Block block) { super(block); }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world,
                                int x, int y, int z, int side,
                                float hitX, float hitY, float hitZ, int metadata) {
        Block old = world.getBlock(x, y, z);
        boolean wet = (old == Blocks.water || old == Blocks.flowing_water)
                && (world.getBlockMetadata(x, y, z) & 15) == 0;
        if (!wet) return super.placeBlockAt(stack, player, world, x, y, z, side,
                hitX, hitY, hitZ, metadata);
        Block replacement = ModBlocks.BIG_DRIPLEAF_WET.get();
        if (replacement == null || !world.setBlock(x, y, z, replacement, metadata, 3)) return false;
        if (world.getBlock(x, y, z) == replacement) {
            replacement.onBlockPlacedBy(world, x, y, z, player, stack);
            replacement.onPostBlockPlaced(world, x, y, z, metadata);
        }
        return true;
    }
}
