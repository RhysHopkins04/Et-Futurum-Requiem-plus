package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Ensures initial chunk-watch tile-entity sync includes physical Y=256..383. */
@Mixin(EntityPlayerMP.class)
public class MixinEntityPlayerMP {

    @ModifyConstant(method = "onUpdate", constant = @Constant(intValue = WorldHeightCompat.LEGACY_HEIGHT))
    private int etfu$extendInitialChunkTileEntitySync(int original) {
        EntityPlayerMP self = (EntityPlayerMP)(Object)this;
        return self.worldObj != null && self.worldObj.provider != null && self.worldObj.provider.dimensionId == 0
                ? WorldHeightCompat.EXTENDED_HEIGHT
                : original;
    }
}
