package ganymedes01.etfuturum.mixins.early.containedwater;

import ganymedes01.etfuturum.core.utils.PowderSnowPhysics;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {
    @Shadow protected boolean isJumping;
    @Inject(method = "moveEntityWithHeading", at = @At("HEAD"))
    private void etfu$leatherClimb(float strafe,float forward,CallbackInfo ci) {
        EntityLivingBase e=(EntityLivingBase)(Object)this;
        if (isJumping && PowderSnowPhysics.canWalk(e) && PowderSnowPhysics.atFeet(e)) e.motionY=0.2D;
    }
}
