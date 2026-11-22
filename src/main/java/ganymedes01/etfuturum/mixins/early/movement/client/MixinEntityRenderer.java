package ganymedes01.etfuturum.mixins.early.movement.client;

import ganymedes01.etfuturum.movement.MovementPose;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Redirect(method = "orientCamera", at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/EntityLivingBase;yOffset:F"), require = 1)
    private float etfu$cameraAtPoseEye(EntityLivingBase view) {
        MovementPose pose = PlayerMovement.pose(view);
        if (pose == MovementPose.VANILLA || PlayerMovement.vanillaPoseRequired((EntityPlayer) view)) {
            return view.yOffset;
        }
        // The view origin and third-person obstruction rays use this same offset.
        return 1.62F - view.getEyeHeight();
    }
}
