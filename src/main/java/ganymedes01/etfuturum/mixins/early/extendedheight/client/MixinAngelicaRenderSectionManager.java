package ganymedes01.etfuturum.mixins.early.extendedheight.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Angelica 2.2.14 allocates section Y=0..15 when CubicChunks is absent,
 * independently of CeleritasWorldRenderer.getMaximumBuildHeight().
 * Expand its non-CubicChunks allocation for EFR's 24-section Overworld only.
 * The WorldClient argument is captured without linking against Angelica types.
 */
@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.AngelicaRenderSectionManager", remap = false)
public abstract class MixinAngelicaRenderSectionManager {

    @ModifyConstant(method = "create", constant = @Constant(intValue = 16), remap = false, require = 1)
    private static int etfu$extendedCeleritasSectionCount(int original,
            @Local(argsOnly = true) WorldClient world) {
        if (world != null && world.provider != null && world.provider.dimensionId == 0) {
            return world.getHeight() >> 4;
        }
        return original;
    }
}
