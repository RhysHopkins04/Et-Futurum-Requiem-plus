package ganymedes01.etfuturum.mixins.early.containedwater;

import ganymedes01.etfuturum.core.utils.ContainedWater;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class MixinWorld {
    @Inject(method = "handleMaterialAcceleration", at = @At("RETURN"), cancellable = true)
    private void etfu$bodyWater(AxisAlignedBB box, Material material, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (material == Material.water && !cir.getReturnValue() && ContainedWater.intersects((World)(Object)this,box))
            cir.setReturnValue(true); // Source fluid has no current. Vanilla swimming consumes this result.
    }
    @Inject(method = {"isMaterialInBB", "isAABBInMaterial"}, at = @At("RETURN"), cancellable = true)
    private void etfu$waterVolume(AxisAlignedBB box, Material material, CallbackInfoReturnable<Boolean> cir) {
        if (material == Material.water && !cir.getReturnValue() && ContainedWater.intersects((World)(Object)this,box))
            cir.setReturnValue(true);
    }
}
