package ganymedes01.etfuturum.mixins.early.extendedheight.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Angelica 2.2.14 refuses to clone sections whose block Y is 256 or higher.
 * Use the extended Overworld height for its vertical out-of-bounds check.
 * Actual storage access continues to use Chunk.getBlockStorageArray().
 */
@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.world.cloned.ClonedChunkSection", remap = false)
public abstract class MixinAngelicaClonedChunkSection {

    @ModifyConstant(method = "isOutOfBuildLimitVertically", constant = @Constant(intValue = 256),
            remap = false, require = 1)
    private static int etfu$extendedSectionCloneHeight(int original,
            @Local(argsOnly = true) World world) {
        if (world != null && world.provider != null && world.provider.dimensionId == 0) {
            return world.getHeight();
        }
        return original;
    }
}
