package ganymedes01.etfuturum.mixins.early.movement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.movement.MovementPose;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {
    @ModifyExpressionValue(method = "onLivingUpdate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/MathHelper;sqrt_double(D)F"), require = 1)
    private float etfu$decayWalkingBobWhileSwimming(float movement) {
        // Keep vanilla's 0.4 damping. Like modern Java, swimming sets the next bob target to zero.
        return PlayerMovement.pose((EntityPlayer) (Object) this) == MovementPose.SWIM ? 0.0F : movement;
    }

    @ModifyExpressionValue(method = "onLivingUpdate", at = @At(value = "INVOKE",
            target = "Ljava/lang/Math;atan(D)D", remap = false), require = 1)
    private double etfu$decayLegacyPitchBobWhileSwimming(double pitch) {
        return PlayerMovement.pose((EntityPlayer) (Object) this) == MovementPose.SWIM ? 0.0D : pitch;
    }

    // Forge subtracts the default eye offset when constructing local pick rays.
    @ModifyExpressionValue(method = "getPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;getDefaultEyeHeight()F", remap = false),
            require = 2)
    private float etfu$pickFromPoseEye(float legacy) {
        return PlayerMovement.legacyClientEyeOffset(legacy, (EntityPlayer) (Object) this);
    }
}
