package ganymedes01.etfuturum.mixins.early.extendedheight.client;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Re-renders the full 0..383 physical column after normal and bulk chunk packets. */
@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @ModifyConstant(method = {"handleChunkData", "handleMapChunkBulk"}, constant = @Constant(intValue = WorldHeightCompat.LEGACY_HEIGHT))
    private int etfu$extendReceivedChunkRenderRange(int original) {
        return WorldHeightCompat.EXTENDED_HEIGHT;
    }
}
