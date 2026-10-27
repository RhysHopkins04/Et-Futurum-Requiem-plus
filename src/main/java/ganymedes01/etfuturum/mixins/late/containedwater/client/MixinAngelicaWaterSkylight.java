package ganymedes01.etfuturum.mixins.late.containedwater.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.client.renderer.block.ContainedWaterSkylightCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuildContext", remap = false)
public abstract class MixinAngelicaWaterSkylight {
    @WrapOperation(method = "copyRawBuffer", remap = false, require = 0, at = @At(value = "INVOKE", remap = false,
            target = "Lcom/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuildContext;calculateQuadLight(IIIIIIIZZZ)V"))
    private void etfu$keepLiquidSkyLight(@Coerce Object builder, int normal, int localX, int localY, int localZ,
            int x, int y, int z, boolean fluid, boolean smooth, boolean shade, Operation<Void> original) {
        long sky = fluid ? ContainedWaterSkylightCompat.capture(builder) : 0;
        original.call(builder, normal, localX, localY, localZ, x, y, z, fluid, smooth, shade);
        ContainedWaterSkylightCompat.restore(builder, sky);
    }
}
