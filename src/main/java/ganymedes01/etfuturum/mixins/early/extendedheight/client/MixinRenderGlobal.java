package ganymedes01.etfuturum.mixins.early.extendedheight.client;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Allocates/render-tracks 24 vertical 16-block render chunks instead of 16. */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = WorldHeightCompat.LEGACY_SECTION_COUNT, ordinal = 0))
    private int etfu$extendRendererAllocationHeight(int original) {
        return WorldHeightCompat.EXTENDED_SECTION_COUNT;
    }

    @ModifyConstant(method = "loadRenderers", constant = @Constant(intValue = WorldHeightCompat.LEGACY_SECTION_COUNT, ordinal = 0))
    private int etfu$extendRendererGridHeight(int original) {
        return WorldHeightCompat.EXTENDED_SECTION_COUNT;
    }
}
