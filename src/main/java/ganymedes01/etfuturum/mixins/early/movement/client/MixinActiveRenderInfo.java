package ganymedes01.etfuturum.mixins.early.movement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ActiveRenderInfo.class)
public abstract class MixinActiveRenderInfo {
    @Inject(method = "projectViewFromEntity", at = @At("HEAD"), cancellable = true)
    private static void etfu$sampleFirstPersonPoseEye(EntityLivingBase view, double partial,
                                                      CallbackInfoReturnable<Vec3> cir) {
        Minecraft mc = Minecraft.getMinecraft();
        if (view != mc.renderViewEntity || mc.gameSettings.thirdPersonView != 0 || mc.gameSettings.debugCamEnable
                || !PlayerMovement.usesPoseEye(view)) return;
        // The first-person camera sits at this eye. The unprojected near plane belongs to the prior pass
        // during FOV setup and is unsuitable for deciding which side of a fluid surface the eye is on.
        cir.setReturnValue(Vec3.createVectorHelper(
                view.prevPosX + (view.posX - view.prevPosX) * partial,
                view.prevPosY + (view.posY - view.prevPosY) * partial + view.getEyeHeight(),
                view.prevPosZ + (view.posZ - view.prevPosZ) * partial));
    }

    @ModifyExpressionValue(method = "projectViewFromEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;getEyeHeight()F"), require = 1)
    private static float etfu$projectEyeOnce(float legacy, EntityLivingBase view, double partial) {
        // The third-person unprojection already contains the pose eye translation from orientCamera.
        return PlayerMovement.usesPoseEye(view) ? 0.0F : legacy;
    }
}
