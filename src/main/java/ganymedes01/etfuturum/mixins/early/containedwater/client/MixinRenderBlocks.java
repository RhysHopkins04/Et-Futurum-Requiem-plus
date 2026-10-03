package ganymedes01.etfuturum.mixins.early.containedwater.client;

import ganymedes01.etfuturum.client.renderer.block.ContainedWaterAccess;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {
    @Shadow public IBlockAccess blockAccess;
    @Unique private IBlockAccess etfu$fluidRenderSource;
    @Inject(method = "renderBlockLiquid", at = @At("HEAD"))
    private void etfu$fluidView(Block block,int x,int y,int z,CallbackInfoReturnable<Boolean> cir) {
        etfu$fluidRenderSource = blockAccess;
        if (!(blockAccess instanceof ContainedWaterAccess)) blockAccess = new ContainedWaterAccess(blockAccess);
    }
    @Inject(method = "renderBlockLiquid", at = @At("RETURN"))
    private void etfu$restoreView(Block block,int x,int y,int z,CallbackInfoReturnable<Boolean> cir) {
        blockAccess = etfu$fluidRenderSource;
        etfu$fluidRenderSource = null;
    }
}
