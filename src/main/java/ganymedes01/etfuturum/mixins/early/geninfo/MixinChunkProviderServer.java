package ganymedes01.etfuturum.mixins.early.geninfo;

import ganymedes01.etfuturum.api.world.IGeneratingCheck;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicInteger;

/** Tracks nested vanilla chunk-population calls without relying on HogUtils. */
@Mixin(ChunkProviderServer.class)
public class MixinChunkProviderServer implements IGeneratingCheck {
    private final ThreadLocal<AtomicInteger> efr$chunksGenerating = ThreadLocal.withInitial(() -> new AtomicInteger(0));

    @Inject(method = "populate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;func_150809_p()V"))
    private void efr$pushGeneratingCheck(IChunkProvider provider, int chunkX, int chunkZ, CallbackInfo ci) {
        efr$chunksGenerating.get().incrementAndGet();
    }

    @Inject(method = "populate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;setChunkModified()V", shift = At.Shift.AFTER))
    private void efr$popGeneratingCheck(IChunkProvider provider, int chunkX, int chunkZ, CallbackInfo ci) {
        AtomicInteger counter = efr$chunksGenerating.get();
        if (counter.get() > 0) counter.decrementAndGet();
    }

    @Override
    public boolean efr$isGenerating() {
        return efr$chunksGenerating.get().get() > 0;
    }
}
