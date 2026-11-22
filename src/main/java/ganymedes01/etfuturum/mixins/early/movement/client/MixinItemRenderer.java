package ganymedes01.etfuturum.mixins.early.movement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
    @Shadow private Minecraft mc;

    @ModifyExpressionValue(method = "renderItemInFirstPerson", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;posY:D"), require = 1)
    private double etfu$sampleLightAtHorizontalEye(double legacyY) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (PlayerMovement.pose(player).horizontal() && !PlayerMovement.vanillaPoseRequired(player)) {
            // Keep the existing lightmap lookup so dynamic lights can use the corrected position too.
            return player.posY + player.getEyeHeight();
        }
        return legacyY;
    }
}
