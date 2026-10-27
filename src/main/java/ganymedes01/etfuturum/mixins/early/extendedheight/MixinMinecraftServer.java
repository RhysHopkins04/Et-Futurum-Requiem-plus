package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import ganymedes01.etfuturum.network.profile.ProfilePacketFormat;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Existing server.properties commonly still specifies 256; EFR owns the extended ceiling. */
@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Inject(method = "getBuildLimit", at = @At("HEAD"), cancellable = true)
    private void etfu$buildCeiling(CallbackInfoReturnable<Integer> cir) {
        if (ProfilePacketFormat.serverExtended()) cir.setReturnValue(WorldHeightCompat.configuredHeight());
    }
}
