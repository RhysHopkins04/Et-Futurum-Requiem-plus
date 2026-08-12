package ganymedes01.etfuturum.mixins.early.extendedheight.client;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Marks the complete 384-block chunk column dirty when client chunks are created/removed. */
@Mixin(WorldClient.class)
public class MixinWorldClient {

    @ModifyConstant(method = "doPreChunk", constant = @Constant(intValue = WorldHeightCompat.LEGACY_HEIGHT))
    private int etfu$extendChunkRenderInvalidation(int original) {
        return WorldHeightCompat.EXTENDED_HEIGHT;
    }
}
