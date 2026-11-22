package ganymedes01.etfuturum.mixins.early.movement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.ActiveRenderInfo;
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

    @ModifyExpressionValue(method = "renderOverlays", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;isInsideOfMaterial(Lnet/minecraft/block/material/Material;)Z"),
            require = 1)
    private boolean etfu$overlayAtCameraWaterline(boolean submerged, float partial) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player != mc.renderViewEntity || !PlayerMovement.usesPoseEye(player)) return submerged;
        return ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld, player, partial).getMaterial() == Material.water;
    }
}
