package ganymedes01.etfuturum.mixins.early.movement;

import ganymedes01.etfuturum.movement.MovementPose;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {
    @Inject(method = "moveEntityWithHeading", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;moveFlying(FFF)V", ordinal = 0), require = 1)
    private void etfu$swimTowardLook(float strafe, float forward, CallbackInfo ci) {
        EntityLivingBase entity = (EntityLivingBase) (Object) this;
        if (PlayerMovement.pose(entity) != MovementPose.SWIM) return;
        double ascent = -MathHelper.sin(entity.rotationPitch * (float) Math.PI / 180.0F);
        double rate = ascent < -0.2D ? 0.085D : 0.06D;
        entity.motionY += (ascent - entity.motionY) * rate;
    }

    @ModifyConstant(method = "moveEntityWithHeading", constant = @Constant(doubleValue = 0.02D, ordinal = 0), require = 1)
    private double etfu$swimGravity(double legacy) {
        return PlayerMovement.pose((EntityLivingBase) (Object) this) == MovementPose.SWIM ? 0.005D : legacy;
    }

    @ModifyConstant(method = "moveEntityWithHeading", constant = @Constant(doubleValue = 0.800000011920929D), require = 3)
    private double etfu$swimDrag(double legacy) {
        return PlayerMovement.pose((EntityLivingBase) (Object) this) == MovementPose.SWIM ? 0.9D : legacy;
    }
}
