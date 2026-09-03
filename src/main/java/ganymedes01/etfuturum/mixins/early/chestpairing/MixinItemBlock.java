package ganymedes01.etfuturum.mixins.early.chestpairing;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.core.utils.ModernChestPairing;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures the original click target before ItemBlock offsets the placement position. 1.7's
 * Block.onBlockPlacedBy hook does not receive the clicked face, but modern chest secondary-use
 * placement needs that distinction to tell "sneak on chest side" from "sneak on floor".
 */
@Mixin(ItemBlock.class)
public abstract class MixinItemBlock {

    @Inject(method = "onItemUse", at = @At("HEAD"))
    private void etfu$captureChestClick(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ,
            CallbackInfoReturnable<Boolean> cir) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks) return;
        Block placedBlock = Block.getBlockFromItem((Item) (Object) this);
        if (ModernChestPairing.isManagedChestBlock(placedBlock)) {
            ModernChestPairing.capturePlacementClick(world, placedBlock, x, y, z, side);
        }
    }

    @Inject(method = "onItemUse", at = @At("RETURN"))
    private void etfu$clearChestClick(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ,
            CallbackInfoReturnable<Boolean> cir) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks) return;
        Block placedBlock = Block.getBlockFromItem((Item) (Object) this);
        if (ModernChestPairing.isManagedChestBlock(placedBlock)) {
            ModernChestPairing.clearPlacementClick(world, placedBlock);
        }
    }
}
