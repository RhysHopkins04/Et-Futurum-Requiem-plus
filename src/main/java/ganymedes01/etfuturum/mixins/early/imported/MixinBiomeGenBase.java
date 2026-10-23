package ganymedes01.etfuturum.mixins.early.imported;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import ganymedes01.etfuturum.world.imported.ImportedSurfaceBiome;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Altitude climate for imported identities only. Vanilla and other mods' biomes remain untouched. */
@Mixin(BiomeGenBase.class)
public class MixinBiomeGenBase {
    @ModifyVariable(method = "getFloatTemperature", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 1)
    private int etfu$importedLogicalAltitude(int y) {
        return (Object) this instanceof ImportedSurfaceBiome && ((ImportedSurfaceBiome)(Object)this).importedAltitude ? WorldHeightCompat.physicalToModernY(y) : y;
    }
}
