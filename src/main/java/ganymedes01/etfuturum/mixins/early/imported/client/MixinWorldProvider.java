package ganymedes01.etfuturum.mixins.early.imported.client;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla client sky reference; third-party sky renderers may use their own reference model. */
@Mixin(WorldProvider.class)
public class MixinWorldProvider {
    @Shadow public int dimensionId;
    @Shadow protected net.minecraft.world.World worldObj;
    @Inject(method="getCloudHeight", at=@At("HEAD"), cancellable=true, require=1)
    private void etfu$importedCloudReference(CallbackInfoReturnable<Float> cir) {
        if (WorldHeightCompat.imported(worldObj)) cir.setReturnValue((float)WorldHeightCompat.PHYSICAL_CLOUD_HEIGHT);
    }
    // Forge-added method: unlike vanilla getCloudHeight, this keeps its name in production.
    @Inject(method="getHorizon", at=@At("HEAD"), cancellable=true, remap=false, require=1)
    private void etfu$importedHorizonReference(CallbackInfoReturnable<Double> cir) {
        if (WorldHeightCompat.imported(worldObj)) cir.setReturnValue((double)WorldHeightCompat.PHYSICAL_SEA_LEVEL);
    }
}
