package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Keeps client render/light block access valid through physical Y=383. */
@Mixin(ChunkCache.class)
public class MixinChunkCache {

    @Shadow private World worldObj;

    @ModifyConstant(
            method = {"getBlock", "getBlockMetadata", "getSkyBlockTypeBrightness", "getSpecialBlockBrightness", "getHeight"},
            constant = @Constant(intValue = WorldHeightCompat.LEGACY_HEIGHT)
    )
    private int etfu$extendCacheHeight(int original) {
        return this.worldObj.provider != null && this.worldObj.provider.dimensionId == 0 ? WorldHeightCompat.EXTENDED_HEIGHT : original;
    }

    @ModifyConstant(
            method = {"getSkyBlockTypeBrightness", "getSpecialBlockBrightness"},
            constant = @Constant(intValue = WorldHeightCompat.LEGACY_MAX_Y)
    )
    private int etfu$extendCacheMaxY(int original) {
        return this.worldObj.provider != null && this.worldObj.provider.dimensionId == 0 ? WorldHeightCompat.EXTENDED_MAX_Y : original;
    }
}
