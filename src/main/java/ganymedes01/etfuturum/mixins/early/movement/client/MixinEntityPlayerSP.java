package ganymedes01.etfuturum.mixins.early.movement.client;

import ganymedes01.etfuturum.movement.MovementMode;
import ganymedes01.etfuturum.movement.MovementPose;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {
    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V", shift = At.Shift.AFTER), require = 1)
    private void etfu$predictAfterInput(CallbackInfo ci) {
        EntityPlayerSP player = (EntityPlayerSP) (Object) this;
        PlayerMovement.resolve(player);
        MovementPose pose = PlayerMovement.pose(player);
        if (!player.movementInput.sneak && (pose == MovementPose.CROUCH || pose == MovementPose.CRAWL && !player.isInWater())) {
            player.movementInput.moveForward *= 0.3F;
            player.movementInput.moveStrafe *= 0.3F;
        }
    }

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(floatValue = 0.2F, ordinal = 3), require = 1)
    private float etfu$disableLegacySneakDip(float legacy) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        return PlayerMovement.pose(player) == MovementPose.VANILLA ? legacy : 0.0F;
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onLivingUpdate()V"), require = 1)
    private void etfu$predictAfterSprint(CallbackInfo ci) {
        EntityPlayerSP player = (EntityPlayerSP) (Object) this;
        if (PlayerMovement.mode(player) != MovementMode.DISABLED) PlayerMovement.resolve(player);
    }
}
