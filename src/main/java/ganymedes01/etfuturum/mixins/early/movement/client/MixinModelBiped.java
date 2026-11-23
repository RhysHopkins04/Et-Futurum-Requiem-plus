package ganymedes01.etfuturum.mixins.early.movement.client;

import ganymedes01.etfuturum.movement.MovementPose;
import ganymedes01.etfuturum.movement.client.HorizontalPlayerRender;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
public abstract class MixinModelBiped {
    @Unique private boolean etfu$rotatedHorizontalHead;
    @Unique private float etfu$headRollBeforePose;
    @Unique private float etfu$headwearRollBeforePose;

    @Inject(method = "setRotationAngles", at = @At("HEAD"), require = 1)
    private void etfu$resetHeadRoll(float stride, float strideAmount, float age, float headYaw, float headPitch,
                                   float scale, Entity player, CallbackInfo ci) {
        if (!etfu$rotatedHorizontalHead) return;
        ModelBiped model = (ModelBiped) (Object) this;
        model.bipedHead.rotateAngleZ = etfu$headRollBeforePose;
        model.bipedHeadwear.rotateAngleZ = etfu$headwearRollBeforePose;
        etfu$rotatedHorizontalHead = false;
    }

    @Inject(method = "setRotationAngles", at = @At("TAIL"), require = 1)
    private void etfu$horizontalPose(float stride, float strideAmount, float age, float headYaw, float headPitch,
                                 float scale, Entity player, CallbackInfo ci) {
        MovementPose pose = HorizontalPlayerRender.pose(player);
        if (!pose.horizontal()) return;
        ModelBiped model = (ModelBiped) (Object) this;
        etfu$headRollBeforePose = model.bipedHead.rotateAngleZ;
        etfu$headwearRollBeforePose = model.bipedHeadwear.rotateAngleZ;
        etfu$turnHead(model.bipedHead, model.bipedHeadwear, HorizontalPlayerRender.bodyPitch(pose, headPitch));
        etfu$rotatedHorizontalHead = true;
        if (pose != MovementPose.CRAWL) return;

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

    @Unique
    private static void etfu$turnHead(ModelRenderer head, ModelRenderer outer, float bodyPitch) {
        // Express the pitch correction in model space, then decompose X-Y-X into ModelRenderer's Z-Y-X order.
        double body = Math.toRadians(bodyPitch);
        double sinBody = Math.sin(body), cosBody = Math.cos(body);
        double sinYaw = Math.sin(head.rotateAngleY), cosYaw = Math.cos(head.rotateAngleY);
        double sinPitch = Math.sin(head.rotateAngleX), cosPitch = Math.cos(head.rotateAngleX);
        double leftX = cosYaw, leftY = sinBody * sinYaw;
        double horizontal = Math.hypot(leftX, leftY);
        float yaw = (float) Math.atan2(cosBody * sinYaw, horizontal);
        float roll = horizontal < 1e-6 ? 0.0F : (float) Math.atan2(leftY, leftX);
        float pitch = horizontal < 1e-6
                ? (float) Math.atan2(cosBody * sinPitch + sinBody * cosYaw * cosPitch,
                        cosBody * cosPitch - sinBody * cosYaw * sinPitch)
                : (float) Math.atan2(sinBody * cosPitch + cosBody * cosYaw * sinPitch,
                        -sinBody * sinPitch + cosBody * cosYaw * cosPitch);
        head.rotateAngleX = outer.rotateAngleX = pitch;
        head.rotateAngleY = outer.rotateAngleY = yaw;
        head.rotateAngleZ = outer.rotateAngleZ = roll;
    }
}
