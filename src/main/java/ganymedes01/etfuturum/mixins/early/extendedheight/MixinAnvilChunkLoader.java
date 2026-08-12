package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Allows Anvil chunks to restore section tags Y=0..23 instead of truncating at section 15. */
@Mixin(AnvilChunkLoader.class)
public class MixinAnvilChunkLoader {

    @ModifyConstant(method = "readChunkFromNBT", constant = @Constant(intValue = WorldHeightCompat.LEGACY_SECTION_COUNT))
    private int etfu$allocateExtendedSectionArray(int original) {
        return WorldHeightCompat.EXTENDED_SECTION_COUNT;
    }
}
