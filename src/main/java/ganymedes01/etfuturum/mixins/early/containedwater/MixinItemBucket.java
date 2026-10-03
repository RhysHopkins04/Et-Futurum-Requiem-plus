package ganymedes01.etfuturum.mixins.early.containedwater;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBucket;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBucket.class)
public abstract class MixinItemBucket {
    @Shadow private Block isFull;
    @Inject(method = "tryPlaceContainedLiquid", at = @At("HEAD"), cancellable = true)
    private void etfu$fillAquatic(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (isFull == Blocks.flowing_water && ModernMapParityBlocks.fillContainedWater(world,x,y,z)) cir.setReturnValue(true);
    }
}
