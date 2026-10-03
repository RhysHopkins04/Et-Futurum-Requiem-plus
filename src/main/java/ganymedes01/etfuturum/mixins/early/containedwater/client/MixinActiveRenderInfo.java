package ganymedes01.etfuturum.mixins.early.containedwater.client;

import ganymedes01.etfuturum.core.utils.ContainedWater;
import net.minecraft.block.Block;
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
    private static void etfu$waterFog(World world,EntityLivingBase entity,float partial,CallbackInfoReturnable<Block> cir) {
        Vec3 v = ActiveRenderInfo.projectViewFromEntity(entity,partial);
        if (ContainedWater.at(world,v.xCoord,v.yCoord,v.zCoord)) cir.setReturnValue(Blocks.water);
    }
}
