package ganymedes01.etfuturum.mixins.early.endflashes.client;

import ganymedes01.etfuturum.api.client.EndFlashAPI;
import ganymedes01.etfuturum.client.renderer.EndFlashRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
	@Shadow private float bossColorModifier;
	@Unique private float etfu$lightmapFlash;

	@Inject(method = "updateLightmap", at = @At("HEAD"))
	private void etfu$sampleEndFlash(float partialTicks, CallbackInfo ci) {
		float intensity = EndFlashAPI.getIntensity(partialTicks);
		etfu$lightmapFlash = intensity > 0.0F && !EndFlashRenderer.usesShaderFlash()
				? (bossColorModifier > 0.0F ? intensity / 3.0F : intensity)
				: 0.0F;
	}

	@ModifyConstant(method = "updateLightmap", constant = @Constant(floatValue = 0.22F))
	private float etfu$endFlashRed(float original) {
		return original + etfu$lightmapFlash * (172.0F / 255.0F);
	}

	@ModifyConstant(method = "updateLightmap", constant = @Constant(floatValue = 0.28F))
	private float etfu$endFlashGreen(float original) {
		return original + etfu$lightmapFlash * (96.0F / 255.0F);
	}

	@ModifyConstant(method = "updateLightmap", constant = @Constant(floatValue = 0.25F))
	private float etfu$endFlashBlue(float original) {
		return original + etfu$lightmapFlash * (205.0F / 255.0F);
	}
}
