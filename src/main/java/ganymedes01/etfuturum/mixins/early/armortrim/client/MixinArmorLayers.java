package ganymedes01.etfuturum.mixins.early.armortrim.client;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import ganymedes01.etfuturum.client.renderer.trim.ArmorTrimRenderer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RendererLivingEntity.class)
public abstract class MixinArmorLayers {
    @Unique private int etfuturum$armorPass,etfuturum$armorFlags;
    @WrapOperation(method="doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/RendererLivingEntity;shouldRenderPass(Lnet/minecraft/entity/EntityLivingBase;IF)I"))
    private int etfuturum$pass(RendererLivingEntity renderer,EntityLivingBase entity,int pass,float ticks,Operation<Integer> original) { etfuturum$armorPass=pass;return etfuturum$armorFlags=original.call(renderer,entity,pass,ticks); }
    @WrapOperation(method="doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V",ordinal=0))
    private void etfuturum$base(ModelBase model,Entity entity,float limb,float amount,float age,float yaw,float pitch,float scale,Operation<Void> original) {
        original.call(model,entity,limb,amount,age,yaw,pitch,scale);
        if((etfuturum$armorFlags&240)!=16)ArmorTrimRenderer.renderArmor((EntityLivingBase)entity,etfuturum$armorPass,model,limb,amount,age,yaw,pitch,scale);
    }
    @WrapOperation(method="doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V",ordinal=1))
    private void etfuturum$leatherOverlay(ModelBase model,Entity entity,float limb,float amount,float age,float yaw,float pitch,float scale,Operation<Void> original) {
        original.call(model,entity,limb,amount,age,yaw,pitch,scale);ArmorTrimRenderer.renderArmor((EntityLivingBase)entity,etfuturum$armorPass,model,limb,amount,age,yaw,pitch,scale);
    }
}
