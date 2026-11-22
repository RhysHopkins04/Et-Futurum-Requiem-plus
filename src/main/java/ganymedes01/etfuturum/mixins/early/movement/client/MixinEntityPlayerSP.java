package ganymedes01.etfuturum.mixins.early.movement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.movement.MovementMode;
import ganymedes01.etfuturum.movement.MovementPose;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {
    @Shadow protected Minecraft mc;
    @Unique private boolean etfu$swimmingWithSprintKey;

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V", shift = At.Shift.AFTER), require = 1)
    private void etfu$predictAfterInput(CallbackInfo ci) {
        EntityPlayerSP player = (EntityPlayerSP) (Object) this;
        if (etfu$swimmingWithSprintKey && !mc.gameSettings.keyBindSprint.getIsKeyPressed()) {
            etfu$swimmingWithSprintKey = false;
            if (PlayerMovement.pose(player) == MovementPose.SWIM) player.setSprinting(false);
        }
        // Resolve an existing swim after vanilla has processed sprint input for this tick.
        if (PlayerMovement.pose(player) != MovementPose.SWIM) PlayerMovement.resolve(player);
        MovementPose pose = PlayerMovement.pose(player);
        if (!player.movementInput.sneak && (pose == MovementPose.CROUCH || pose == MovementPose.CRAWL && !player.isInWater())) {
            player.movementInput.moveForward *= 0.3F;
            player.movementInput.moveStrafe *= 0.3F;
        }
    }

    @ModifyExpressionValue(method = "onLivingUpdate", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/entity/EntityPlayerSP;isCollidedHorizontally:Z"), require = 1)
    private boolean etfu$keepSwimmingAtWall(boolean collided) {
        EntityPlayerSP player = (EntityPlayerSP) (Object) this;
        // A solid block still stops movement. Contact alone should not cancel aquatic sprint.
        return collided && (PlayerMovement.pose(player) != MovementPose.SWIM || !player.isInWater());
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
        if (PlayerMovement.mode(player) == MovementMode.DISABLED) return;
        PlayerMovement.resolve(player);
        etfu$swimmingWithSprintKey = PlayerMovement.pose(player) == MovementPose.SWIM
                && mc.gameSettings.keyBindSprint.getIsKeyPressed();
    }
}
