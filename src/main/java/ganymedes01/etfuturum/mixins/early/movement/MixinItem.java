package ganymedes01.etfuturum.mixins.early.movement;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Item.class)
public abstract class MixinItem {
    @ModifyExpressionValue(method = "getMovingObjectPositionFromPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;getDefaultEyeHeight()F", remap = false),
            require = 1)
    private float etfu$itemRayFromPoseEye(float legacy, World world, EntityPlayer player) {
        return PlayerMovement.legacyClientEyeOffset(legacy, player);
    }
}
