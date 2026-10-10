package ganymedes01.etfuturum.blocks.itemblocks;

import ganymedes01.etfuturum.blocks.BlockSmallDripleaf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Places both small-dripleaf halves atomically with the existing water bits.
 * A standard ItemBlock publishes the lower half to water physics before the
 * upper half exists; this can trigger a support update and remove both halves.
 */
public class ItemBlockSmallDripleaf extends ItemBlock {
    public ItemBlockSmallDripleaf(Block block) { super(block); }

    private static boolean isSourceWater(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return (block == Blocks.water || block == Blocks.flowing_water)
                && (world.getBlockMetadata(x, y, z) & 15) == 0;
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world,
                                int x, int y, int z, int side,
                                float hitX, float hitY, float hitZ, int metadata) {
        BlockSmallDripleaf leaf = (BlockSmallDripleaf) field_150939_a;
        if (!leaf.canPlaceBlockAt(world, x, y, z)) return false;
        boolean wetLower = isSourceWater(world, x, y, z);
        boolean wetUpper = isSourceWater(world, x, y + 1, z);
        int yaw = MathHelper.floor_double((player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        int facing = (yaw + 2) & 3;
        int lowerMeta = BlockSmallDripleaf.makeMeta(facing, false)
                | (wetLower ? BlockSmallDripleaf.WATERLOGGED_BIT : 0);
        int upperMeta = BlockSmallDripleaf.makeMeta(facing, true)
                | (wetUpper ? BlockSmallDripleaf.WATERLOGGED_BIT : 0);

        // Flag 2 renders/syncs without neighboring block notifications. Only
        // notify after BOTH halves are present, preventing premature collapse.
        if (!world.setBlock(x, y, z, leaf, lowerMeta, 2)) return false;
        if (!world.setBlock(x, y + 1, z, leaf, upperMeta, 2)) {
            // Restore the source or air if a mod unexpectedly rejected the upper half.
            world.setBlock(x, y, z, wetLower ? Blocks.water : Blocks.air, 0, 2);
            world.markBlockForUpdate(x, y, z);
            return false;
        }
        world.markBlockForUpdate(x, y, z);
        world.markBlockForUpdate(x, y + 1, z);
        if (!world.isRemote) {
            world.notifyBlocksOfNeighborChange(x, y, z, leaf);
            world.notifyBlocksOfNeighborChange(x, y + 1, z, leaf);
        }
        return true;
    }
}
