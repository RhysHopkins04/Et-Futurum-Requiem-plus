package ganymedes01.etfuturum.mixins.early.imported;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Imported height references without changing the selected world provider or chunk manager. */
@Mixin(WorldProvider.class)
public class MixinWorldProvider {
    @Shadow public int dimensionId;
    @Inject(method="getAverageGroundLevel", at=@At("HEAD"), cancellable=true, require=1)
    private void etfu$importedGroundReference(CallbackInfoReturnable<Integer> cir) {
        if (dimensionId==0) cir.setReturnValue(WorldHeightCompat.PHYSICAL_AVERAGE_GROUND_LEVEL);
    }
}
