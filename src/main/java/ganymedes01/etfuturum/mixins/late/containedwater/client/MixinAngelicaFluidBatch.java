package ganymedes01.etfuturum.mixins.late.containedwater.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.client.renderer.block.ContainedWaterShaderCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/** Material overrides alone do not select Angelica's fluid lighting/vertex context. */
@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuilderMeshingTask", remap = false)
public abstract class MixinAngelicaFluidBatch {
    @WrapOperation(method = "renderBlock", remap = false, require = 0, at = @At(value = "INVOKE", remap = false,
        target = "Lcom/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuildContext;copyRawBuffer([II[I[ILorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildBuffers;Lorg/embeddedt/embeddium/impl/render/chunk/terrain/material/Material;ZZ)V"))
    private void etfu$encodeWaterAsFluid(@Coerce Object builder, int[] vertices, int vertexCount,
            int[] states, int[] shaderOverrides, @Coerce Object buffers, @Coerce Object material,
            boolean shaderPackOverride, boolean vanillaAo, Operation<Void> original) {
        try (ContainedWaterShaderCompat.FluidScope scope = ContainedWaterShaderCompat.fluidBatch(builder, shaderOverrides, vertexCount)) {
            original.call(builder, vertices, vertexCount, states, shaderOverrides, buffers, material, shaderPackOverride, vanillaAo);
        }
    }
}
