package ganymedes01.etfuturum.mixins.late.containedwater.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuilderMeshingTask", remap = false)
public abstract class MixinAngelicaWaterloggedMesher {
    @WrapOperation(method = {"execute", "lambda$processDeferredBlocks$0"}, at = @At(value = "INVOKE", target =
            "Lcom/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuilderMeshingTask;renderBlock(Lnet/minecraft/block/Block;IIIIILnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/client/renderer/RenderBlocks;Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildBuffers;Lcom/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuildContext;Lcom/gtnewhorizons/angelica/rendering/celeritas/iris/BlockRenderContext;IIILorg/embeddedt/embeddium/impl/render/chunk/terrain/material/Material;ZLnet/coderbot/iris/block_rendering/NbtConditionalIdMap;J)V"))
    private void etfu$renderWaterloggedSource(@Coerce Object task, Block host, int meta, int x, int y, int z, int pass,
            Tessellator tessellator, RenderBlocks renderer, @Coerce Object buffers, @Coerce Object buildContext,
            @Coerce Object blockContext, int originX, int originY, int originZ, @Coerce Object material,
            boolean shaderOverride, @Coerce Object teMap, long tick, Operation<Void> original,
            @Share("renderedWater") LocalRef<int[]> renderedWater) {
        original.call(task, host, meta, x, y, z, pass, tessellator, renderer, buffers, buildContext,
                blockContext, originX, originY, originZ, material, shaderOverride, teMap, tick);
        if (host instanceof BlockLeaves || !NativeWaterlogging.canHold(host)
                || !NativeWaterlogging.contains(renderer.blockAccess, x, y, z)) return;

        int[] last = renderedWater.get();
        if (last == null) {
            last = new int[3];
            renderedWater.set(last);
        } else if (x == last[0] && y == last[1] && z == last[2]) return;
        last[0] = x;
        last[1] = y;
        last[2] = z;
        original.call(task, Blocks.water, 0, x, y, z, 1, tessellator, renderer, buffers, buildContext,
                blockContext, originX, originY, originZ, null, false, teMap, tick);
    }
}
