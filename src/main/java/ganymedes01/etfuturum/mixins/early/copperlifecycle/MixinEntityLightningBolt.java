package ganymedes01.etfuturum.mixins.early.copperlifecycle;

import ganymedes01.etfuturum.blocks.ModernCopperLifecycle;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pass 36: attach modern copper cleaning to the existing 1.7 lightning entity once per strike. */
@Mixin(EntityLightningBolt.class)
public abstract class MixinEntityLightningBolt {

    @Unique private boolean etfu$pass36CopperCleaned;

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void etfu$cleanCopperOnInitialStrike(CallbackInfo ci) {
        if (etfu$pass36CopperCleaned || !ConfigBlocksItems.enableModernMapParityBlocks) return;
        EntityLightningBolt bolt = (EntityLightningBolt) (Object) this;
        if (bolt.worldObj == null || bolt.worldObj.isRemote) return;
        etfu$pass36CopperCleaned = true;
        int x = MathHelper.floor_double(bolt.posX);
        int y = MathHelper.floor_double(bolt.posY - 1.0E-6D);
        int z = MathHelper.floor_double(bolt.posZ);
        ModernCopperLifecycle.cleanCopperOnLightningStrike(bolt.worldObj, x, y, z, bolt.worldObj.rand);
    }
}
