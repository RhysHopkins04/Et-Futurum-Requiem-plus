package ganymedes01.etfuturum.dispenser;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;

public final class DispenserBehaviourContainedWater implements IBehaviorDispenseItem {
    private final IBehaviorDispenseItem previous;
    private final BehaviorDefaultDispenseItem collect = new BehaviorDefaultDispenseItem() {
        @Override
        protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
            ItemStack filled = new ItemStack(Items.water_bucket);
            if (--stack.stackSize == 0) return filled;

            TileEntityDispenser dispenser = (TileEntityDispenser) source.getBlockTileEntity();
            if (dispenser.func_146019_a(filled) < 0)
                doDispense(source.getWorld(), filled, 6, BlockDispenser.func_149937_b(source.getBlockMetadata()),
                        BlockDispenser.func_149939_a(source));
            return stack;
        }
    };

    private DispenserBehaviourContainedWater(IBehaviorDispenseItem previous) {
        this.previous = previous;
    }

    public static void register() {
        IBehaviorDispenseItem previous = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry.getObject(Items.bucket);
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.bucket, new DispenserBehaviourContainedWater(previous));
    }

    @Override
    public ItemStack dispense(IBlockSource source, ItemStack stack) {
        EnumFacing facing = BlockDispenser.func_149937_b(source.getBlockMetadata());
        int x = source.getXInt() + facing.getFrontOffsetX();
        int y = source.getYInt() + facing.getFrontOffsetY();
        int z = source.getZInt() + facing.getFrontOffsetZ();
        if (!NativeWaterlogging.creativeBucketOnly(source.getWorld().getBlock(x, y, z))
                && ModernMapParityBlocks.drainContainedWater(source.getWorld(), x, y, z))
            return collect.dispense(source, stack);
        return previous.dispense(source, stack);
    }
}
