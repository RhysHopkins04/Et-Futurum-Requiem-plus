package ganymedes01.etfuturum.mixins.early.movement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {
    // Forge subtracts the default eye offset when constructing local pick rays.
    @ModifyExpressionValue(method = "getPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;getDefaultEyeHeight()F", remap = false),
            require = 2)
    private float etfu$pickFromPoseEye(float legacy) {
        return PlayerMovement.legacyClientEyeOffset(legacy, (EntityPlayer) (Object) this);
    }
}
