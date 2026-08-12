package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends only dimension 0. Nether, End and modded dimensions retain their provider-defined height. */
@Mixin(WorldProvider.class)
public class MixinWorldProvider {

    @Shadow public int dimensionId;

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true, remap = false)
    private void etfu$extendedHeight(CallbackInfoReturnable<Integer> cir) {
        if (this.dimensionId == 0) {
            cir.setReturnValue(WorldHeightCompat.EXTENDED_HEIGHT);
        }
    }

    @Inject(method = "getActualHeight", at = @At("HEAD"), cancellable = true, remap = false)
    private void etfu$extendedActualHeight(CallbackInfoReturnable<Integer> cir) {
        if (this.dimensionId == 0) {
            cir.setReturnValue(WorldHeightCompat.EXTENDED_HEIGHT);
        }
    }
}
