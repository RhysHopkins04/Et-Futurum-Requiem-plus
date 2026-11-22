package ganymedes01.etfuturum.mixins.early.movement;

import ganymedes01.etfuturum.movement.MovementMode;
import ganymedes01.etfuturum.movement.MovementPlayer;
import ganymedes01.etfuturum.movement.MovementPose;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase implements MovementPlayer {
    // Player metadata travels in vanilla spawn and metadata packets, including the self update recorded by ReplayMod.
    @Unique private static final int ETFU_MOVEMENT_POSE = 29;
    @Unique private MovementPose etfu$predictedPose = MovementPose.VANILLA;

    protected MixinEntityPlayer(World world) {
        super(world);
    }

    @Inject(method = "entityInit", at = @At("RETURN"))
    private void etfu$watchMovementPose(CallbackInfo ci) {
        this.dataWatcher.addObject(ETFU_MOVEMENT_POSE, MovementPose.VANILLA.wireId);
    }

    @Override
    public MovementPose etfu$movementPose() {
        if (this.worldObj.isRemote && this.isClientWorld()) {
            return this.etfu$predictedPose == null ? MovementPose.VANILLA : this.etfu$predictedPose;
        }
        return MovementPose.fromWire(this.dataWatcher.getWatchableObjectByte(ETFU_MOVEMENT_POSE));
    }

    @Override
    public void etfu$resolveMovementPose() {
        EntityPlayer player = (EntityPlayer) (Object) this;
        if (player instanceof FakePlayer) return;
        MovementMode mode = PlayerMovement.mode(player);
        MovementPose before = etfu$movementPose();
        if (mode == MovementMode.DISABLED && before == MovementPose.VANILLA) return;
        MovementPose next = PlayerMovement.choose(player, mode, before);
        if (this.worldObj.isRemote) this.etfu$predictedPose = next;
        else this.dataWatcher.updateObject(ETFU_MOVEMENT_POSE, next.wireId);
        etfu$applyPoseSize(player, before, next);
    }

    @Unique
    private void etfu$applyPoseSize(EntityPlayer player, MovementPose before, MovementPose next) {
        if (!player.isEntityAlive() || player.isPlayerSleeping() || PlayerMovement.gliding(player)) return;
        if (next != MovementPose.VANILLA || before != MovementPose.VANILLA) {
            this.setSize(this.width, next.height);
        }
    }

    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    private void etfu$resolveBeforeLivingUpdate(CallbackInfo ci) {
        if ((Object) this instanceof EntityPlayerMP) etfu$resolveMovementPose();
    }

    @Inject(method = "getEyeHeight", at = @At("RETURN"), cancellable = true)
    private void etfu$poseEyeHeight(CallbackInfoReturnable<Float> cir) {
        MovementPose pose = etfu$movementPose();
        if (pose != MovementPose.VANILLA && !PlayerMovement.vanillaPoseRequired((EntityPlayer) (Object) this)) {
            cir.setReturnValue((float) (this.boundingBox.minY + pose.eye - this.posY));
        }
    }
}
