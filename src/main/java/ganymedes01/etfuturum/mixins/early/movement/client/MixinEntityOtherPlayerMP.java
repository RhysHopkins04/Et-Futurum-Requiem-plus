package ganymedes01.etfuturum.mixins.early.movement.client;

import com.mojang.authlib.GameProfile;
import ganymedes01.etfuturum.movement.MovementPose;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityOtherPlayerMP.class)
public abstract class MixinEntityOtherPlayerMP extends AbstractClientPlayer {
    @Unique private MovementPose etfu$appliedPose = MovementPose.VANILLA;

    protected MixinEntityOtherPlayerMP(World world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "onUpdate", at = @At("HEAD"), require = 1)
    private void etfu$sizeFromRecordedPose(CallbackInfo ci) {
        if (!isEntityAlive() || isPlayerSleeping()) return;
        if (PlayerMovement.gliding(this)) {
            etfu$appliedPose = MovementPose.VANILLA;
            return;
        }
        MovementPose pose = PlayerMovement.pose(this);
        if (pose != MovementPose.VANILLA || etfu$appliedPose != MovementPose.VANILLA) {
            setSize(this.width, pose.height);
        }
        etfu$appliedPose = pose;
    }
}
