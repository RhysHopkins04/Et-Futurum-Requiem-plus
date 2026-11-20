package ganymedes01.etfuturum.mixins.early.endflashes.client;

import ganymedes01.etfuturum.client.renderer.EndFlashRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {
	@Inject(method = "renderSky", at = @At("TAIL"))
	private void etfu$renderEndFlash(float partialTicks, CallbackInfo ci) {
		EndFlashRenderer.render(partialTicks);
	}
}
