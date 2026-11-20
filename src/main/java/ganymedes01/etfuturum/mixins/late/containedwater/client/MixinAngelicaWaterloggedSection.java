package ganymedes01.etfuturum.mixins.late.containedwater.client;

import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.client.renderer.block.WaterloggedSectionSnapshot;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import ganymedes01.etfuturum.core.utils.WaterloggedAccess;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.world.cloned.ClonedChunkSection", remap = false)
public abstract class MixinAngelicaWaterloggedSection implements WaterloggedSectionSnapshot {
    @Unique private WaterloggedAccess etfu$water;

    @Inject(method = "init", at = @At("RETURN"))
    private void etfu$captureWater(@Coerce Object position, CallbackInfo ci, @Local Chunk chunk) {
        etfu$water = NativeWaterlogging.snapshot(chunk);
    }

    @Override public WaterloggedAccess etfu$waterloggingSnapshot() {
        return etfu$water;
    }
}
