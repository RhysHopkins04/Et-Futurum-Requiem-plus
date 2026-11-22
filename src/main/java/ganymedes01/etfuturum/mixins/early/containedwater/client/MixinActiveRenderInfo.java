package ganymedes01.etfuturum.mixins.early.containedwater.client;

import ganymedes01.etfuturum.core.utils.ContainedWater;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ActiveRenderInfo.class)
public abstract class MixinActiveRenderInfo {
    @Inject(method = "getBlockAtEntityViewpoint", at = @At("RETURN"), cancellable = true)
    private static void etfu$waterFog(World world, EntityLivingBase entity, float partial, CallbackInfoReturnable<Block> cir) {
        Vec3 camera = ActiveRenderInfo.projectViewFromEntity(entity, partial);
        if (PlayerMovement.usesPoseEye(entity)) {
            boolean submerged = PlayerMovement.waterAt(world, camera.xCoord, camera.yCoord, camera.zCoord);
            boolean foundWater = cir.getReturnValue().getMaterial() == Material.water;
            if (submerged != foundWater) cir.setReturnValue(submerged ? Blocks.water : Blocks.air);
        } else if (ContainedWater.at(world, camera.xCoord, camera.yCoord, camera.zCoord)) {
            cir.setReturnValue(Blocks.water);
        }
    }
}
