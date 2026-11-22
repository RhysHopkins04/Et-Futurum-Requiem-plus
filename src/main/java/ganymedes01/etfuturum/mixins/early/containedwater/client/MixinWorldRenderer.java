package ganymedes01.etfuturum.mixins.early.containedwater.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.world.ChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @WrapOperation(method = "updateRenderer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/ChunkCache;getBlock(III)Lnet/minecraft/block/Block;"))
    private Block etfu$readHost(ChunkCache cache, int x, int y, int z, Operation<Block> original,
                                @Share("drawWater") LocalBooleanRef drawWater) {
        Block host = original.call(cache, x, y, z);
        drawWater.set(NativeWaterlogging.canHold(host, cache, x, y, z)
                && NativeWaterlogging.contains(cache, x, y, z));
        return host;
    }

    @WrapOperation(method = "updateRenderer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/block/Block;getRenderBlockPass()I"))
    private int etfu$requestTranslucent(Block block, Operation<Integer> original,
                                        @Share("drawWater") LocalBooleanRef drawWater) {
        int pass = original.call(block);
        return drawWater.get() ? Math.max(1, pass) : pass;
    }

    @WrapOperation(method = "updateRenderer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/block/Block;canRenderInPass(I)Z", remap = false))
    private boolean etfu$allowTranslucent(Block block, int pass, Operation<Boolean> original,
                                           @Share("drawWater") LocalBooleanRef drawWater,
                                           @Share("drawHost") LocalBooleanRef drawHost,
                                           @Share("drawFluidInPass") LocalBooleanRef drawFluid) {
        drawHost.set(original.call(block, pass));
        drawFluid.set(pass == 1 && drawWater.get());
        return drawHost.get() || drawFluid.get();
    }

    @WrapOperation(method = "updateRenderer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/RenderBlocks;renderBlockByRenderType(Lnet/minecraft/block/Block;III)Z", ordinal = 0))
    private boolean etfu$emitWater(RenderBlocks renderer, Block block, int x, int y, int z, Operation<Boolean> original,
                                    @Share("drawHost") LocalBooleanRef drawHost,
                                    @Share("drawFluidInPass") LocalBooleanRef drawFluid) {
        boolean rendered = drawHost.get() && original.call(renderer, block, x, y, z);
        return drawFluid.get() ? original.call(renderer, Blocks.water, x, y, z) || rendered : rendered;
    }
}
