package ganymedes01.etfuturum.mixins.early.movement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ganymedes01.etfuturum.movement.MovementPose;
import ganymedes01.etfuturum.movement.PlayerMovement;
import ganymedes01.etfuturum.movement.PlayerRenderPose;
import ganymedes01.etfuturum.movement.client.HorizontalPlayerRender;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer {
    @WrapMethod(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V")
    private void etfu$horizontalModelScope(AbstractClientPlayer player, double x, double y, double z, float yaw,
                                      float partial, Operation<Void> original) {
        MovementPose pose = PlayerRenderPose.pose(player);
        if (pose.horizontal() && !PlayerMovement.vanillaPoseRequired(player)) {
            HorizontalPlayerRender.render(player, pose, () -> original.call(player, x, y, z, yaw, partial));
        } else {
            original.call(player, x, y, z, yaw, partial);
        }
    }

    @ModifyExpressionValue(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;isSneaking()Z", ordinal = 0), require = 1)
    private boolean etfu$modelCrouch(boolean legacy, AbstractClientPlayer player) {
        MovementPose pose = PlayerRenderPose.pose(player);
        return pose == MovementPose.CROUCH || legacy && !pose.horizontal();
    }

    @ModifyExpressionValue(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;isSneaking()Z", ordinal = 1), require = 1)
    private boolean etfu$modelSneakOffset(boolean legacy, AbstractClientPlayer player) {
        return legacy && !PlayerRenderPose.pose(player).horizontal();
    }

    @Inject(method = "rotateCorpse(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V",
            at = @At("TAIL"), require = 1)
    private void etfu$rotateBodyAndEquipment(AbstractClientPlayer player, float age, float yaw, float partial, CallbackInfo ci) {
        MovementPose pose = PlayerRenderPose.pose(player);
        if (!pose.horizontal() || PlayerMovement.vanillaPoseRequired(player)) return;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partial;
        GL11.glTranslatef(0.0F, pose == MovementPose.CRAWL ? 0.25F : 0.32F, 0.0F);
        GL11.glRotatef(HorizontalPlayerRender.bodyPitch(pose, pitch), 1.0F, 0.0F, 0.0F);
        GL11.glTranslatef(0.0F, -0.85F, 0.0F);
    }
}
