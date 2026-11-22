package ganymedes01.etfuturum.mixins.early.movement;

import ganymedes01.etfuturum.movement.MovementPose;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {
    @Shadow public EntityPlayerMP playerEntity;

    @Inject(method = "processEntityAction", at = @At("RETURN"))
    private void etfu$applyMovementIntent(C0BPacketEntityAction packet, CallbackInfo ci) {
        PlayerMovement.resolve(this.playerEntity);
    }

    @Inject(method = "processPlayer", at = @At("HEAD"))
    private void etfu$applyPoseBeforeCollision(C03PacketPlayer packet, CallbackInfo ci) {
        PlayerMovement.resolve(this.playerEntity);
    }

    @ModifyConstant(method = "processPlayerDigging", constant = @Constant(doubleValue = 1.5D), require = 1)
    private double etfu$digFromPoseEye(double legacy) {
        return PlayerMovement.pose(this.playerEntity) == MovementPose.VANILLA ? legacy : this.playerEntity.getEyeHeight();
    }
}
