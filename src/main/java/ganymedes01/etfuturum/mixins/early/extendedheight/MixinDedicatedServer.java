package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Allows max-build-height to reach 384 on dedicated servers when extended height is enabled. */
@Mixin(DedicatedServer.class)
public class MixinDedicatedServer {

    @ModifyConstant(method = "startServer", constant = @Constant(intValue = WorldHeightCompat.LEGACY_HEIGHT))
    private int etfu$extendedDedicatedBuildLimit(int original) {
        return WorldHeightCompat.EXTENDED_HEIGHT;
    }
}
