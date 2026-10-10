package ganymedes01.etfuturum.mixins.early.extendedheight.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Angelica 2.2.14's WorldSlice already caches World.getHeight() on creation,
 * but two light queries still bound their reads to a literal 256. Respect
 * the cached world height only when it exceeds the vanilla upper bound.
 */
@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.world.WorldSlice", remap = false)
public abstract class MixinAngelicaWorldSlice {

    @Shadow @Final private int worldHeight;

    @ModifyConstant(method = "func_72802_i(IIII)I", constant = @Constant(intValue = 256),
            remap = false, require = 1)
    private int etfu$extendCombinedLightBounds(int original) {
        return Math.max(original, worldHeight);
    }

    @ModifyConstant(method = "getRawSkyLight", constant = @Constant(intValue = 256),
            remap = false, require = 1)
    private int etfu$extendRawSkyLightBounds(int original) {
        return Math.max(original, worldHeight);
    }
    /**
     * Angelica's WorldSlice.getLightLevel() calls CompatMathHelper.clampHeight,
     * whose non-CubicChunks branch caps all reads at Y=255. In a section above
     * Y=255 this moves the lookup BELOW the section-local cached arrays and
     * crashes with a negative local-section index. Preserve the original
     * 0..255 bounds for ordinary worlds; use the cached real height for EFR.
     *
     * Do not modify the world slice's 4x4x4 local lookup geometry: its data
     * is already populated using sections around the current render section.
     */
    @Redirect(method = "getLightLevel",
            at = @At(value = "INVOKE",
                    target = "Lcom/gtnewhorizons/angelica/compat/mojang/CompatMathHelper;clampHeight(I)I",
                    remap = false),
            remap = false, require = 1)
    private int etfu$clampLightLookupToWorldHeight(int y) {
        int maxY = Math.max(255, worldHeight - 1);
        return Math.max(0, Math.min(y, maxY));
    }

}
