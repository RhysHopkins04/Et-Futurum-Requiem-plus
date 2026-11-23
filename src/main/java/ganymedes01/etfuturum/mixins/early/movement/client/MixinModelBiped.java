package ganymedes01.etfuturum.mixins.early.movement.client;

import ganymedes01.etfuturum.movement.client.CrawlModelRender;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
public abstract class MixinModelBiped {
    @Inject(method = "setRotationAngles", at = @At("TAIL"), require = 1)
    private void etfu$crawlLimbs(float stride, float strideAmount, float age, float headYaw, float headPitch,
                                 float scale, Entity player, CallbackInfo ci) {
        if (!CrawlModelRender.appliesTo(player)) return;
        ModelBiped model = (ModelBiped) (Object) this;
        model.bipedRightLeg.rotateAngleX *= 0.25F;
        model.bipedLeftLeg.rotateAngleX *= 0.25F;
        if (model.aimedBow) {
            // Keep aimed items pointing forward when the crawl transform pitches the body down.
            float pitchOffset = (float) Math.PI / 2.0F;
            model.bipedRightArm.rotateAngleX -= pitchOffset;
            model.bipedLeftArm.rotateAngleX -= pitchOffset;
            return;
        }

        // In the horizontal body transform, arms pointing above the head reach forward.
        float reach = -(float) Math.PI + 0.13F;
        float stroke = MathHelper.sin(stride * 0.6662F) * strideAmount * 0.12F;
        model.bipedLeftArm.rotateAngleX = reach + stroke;
        if (model.onGround <= 0.0F) {
            model.bipedRightArm.rotateAngleX = reach - stroke;
        }
    }
}
