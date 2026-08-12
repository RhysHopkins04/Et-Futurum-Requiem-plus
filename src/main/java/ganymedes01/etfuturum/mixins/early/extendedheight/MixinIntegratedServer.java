package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Makes the integrated server's build limit match the 384-block physical Overworld. */
@Mixin(IntegratedServer.class)
public class MixinIntegratedServer {

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = WorldHeightCompat.LEGACY_HEIGHT))
    private int etfu$extendedIntegratedBuildLimit(int original) {
        return WorldHeightCompat.EXTENDED_HEIGHT;
    }
}
