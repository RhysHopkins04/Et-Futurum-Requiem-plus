package ganymedes01.etfuturum.mixins.early.containedwater;

import ganymedes01.etfuturum.core.utils.ContainedWater;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ganymedes01.etfuturum.core.utils.PowderSnowPhysics;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Unique private boolean etfu$stuckInSnow;
    @Inject(method = "moveEntity", at = @At("HEAD"))
    private void etfu$snowMovement(double x,double y,double z,CallbackInfo ci) {
        Entity e=(Entity)(Object)this;
        etfu$stuckInSnow=!e.noClip && PowderSnowPhysics.stuck(e);
    }
    @ModifyVariable(method = "moveEntity", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double etfu$snowX(double value) { return !((Entity)(Object)this).noClip && PowderSnowPhysics.stuck((Entity)(Object)this) ? value*0.9D : value; }
    @ModifyVariable(method = "moveEntity", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double etfu$snowY(double value) { return !((Entity)(Object)this).noClip && PowderSnowPhysics.stuck((Entity)(Object)this) ? value*1.5D : value; }
    @ModifyVariable(method = "moveEntity", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double etfu$snowZ(double value) { return !((Entity)(Object)this).noClip && PowderSnowPhysics.stuck((Entity)(Object)this) ? value*0.9D : value; }
    @Inject(method = "moveEntity", at = @At("RETURN"))
    private void etfu$snowDrag(double x,double y,double z,CallbackInfo ci) {
        if (etfu$stuckInSnow) { Entity e=(Entity)(Object)this; e.motionX=0; e.motionY=0; e.motionZ=0; }
    }

    @Inject(method = "isInsideOfMaterial", at = @At("RETURN"), cancellable = true)
    private void etfu$eyeWater(Material material, CallbackInfoReturnable<Boolean> cir) {
        Entity e = (Entity)(Object)this;
        if (material == Material.water && !cir.getReturnValue() && ContainedWater.at(e.worldObj,e.posX,e.posY+e.getEyeHeight(),e.posZ))
            cir.setReturnValue(true);
    }
}
