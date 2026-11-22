package ganymedes01.etfuturum.mixins.early.movement;

import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @ModifyConstant(method = "handleWaterMovement", constant = @Constant(doubleValue = -0.4000000059604645D), require = 1)
    private double etfu$waterProbeInset(double legacy) {
        // The legacy probe inverts a 0.6-block-high box.
        return (Object) this instanceof EntityPlayer
                && PlayerMovement.pose((Entity) (Object) this).horizontal() ? -0.05D : legacy;
    }
}
