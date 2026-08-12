package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Replaces only vertical world-bound constants. Colour masks, biome IDs and other unrelated 255/256
 * constants are deliberately left untouched.
 */
@Mixin(World.class)
public class MixinWorld {

    @Shadow @Final public WorldProvider provider;

    @ModifyConstant(
            method = {
                    "getBlock", "blockExists", "checkChunksExist", "setBlock", "getBlockMetadata",
                    "setBlockMetadataWithNotify", "getFullBlockLightValue", "getBlockLightValue_do",
                    "getSkyBlockTypeBrightness", "getSavedLightValue", "setLightValue", "getTileEntity"
            },
            constant = @Constant(intValue = WorldHeightCompat.LEGACY_HEIGHT)
    )
    private int etfu$extendWorldHeightBound(int original) {
        return this.provider != null && this.provider.dimensionId == 0 ? WorldHeightCompat.EXTENDED_HEIGHT : original;
    }

    /**
     * Forge moves the vanilla freeze/snow bodies behind provider hooks and adds these literal helper
     * method names after MCP/SRG mappings are produced. They must therefore be targeted without
     * annotation remapping, just like Forge's getBlockLightOpacity helper below.
     */
    @ModifyConstant(
            method = {"canBlockFreezeBody", "canSnowAtBody"},
            constant = @Constant(intValue = WorldHeightCompat.LEGACY_HEIGHT),
            remap = false
    )
    private int etfu$extendForgeWeatherHeightBound(int original) {
        return this.provider != null && this.provider.dimensionId == 0 ? WorldHeightCompat.EXTENDED_HEIGHT : original;
    }

    /** Forge adds this helper after the MCP/SRG mappings are produced, so its literal name must not be remapped. */
    @ModifyConstant(
            method = "getBlockLightOpacity",
            constant = @Constant(intValue = WorldHeightCompat.LEGACY_HEIGHT),
            remap = false
    )
    private int etfu$extendForgeLightOpacityHeightBound(int original) {
        return this.provider != null && this.provider.dimensionId == 0 ? WorldHeightCompat.EXTENDED_HEIGHT : original;
    }

    @ModifyConstant(
            method = {"getFullBlockLightValue", "getBlockLightValue_do", "getSavedLightValue"},
            constant = @Constant(intValue = WorldHeightCompat.LEGACY_MAX_Y)
    )
    private int etfu$extendWorldMaxY(int original) {
        return this.provider != null && this.provider.dimensionId == 0 ? WorldHeightCompat.EXTENDED_MAX_Y : original;
    }
}
