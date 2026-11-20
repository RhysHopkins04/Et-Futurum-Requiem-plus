package ganymedes01.etfuturum.mixins.early.containedwater;

import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import ganymedes01.etfuturum.core.utils.WaterloggedAccess;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkCache.class)
public abstract class MixinChunkCache implements WaterloggedAccess {
    @Shadow private int chunkX;
    @Shadow private int chunkZ;
    @Shadow private Chunk[][] chunkArray;

    @Override public boolean etfu$isWaterlogged(int x, int y, int z) {
        int cx = (x >> 4) - chunkX, cz = (z >> 4) - chunkZ;
        if (cx < 0 || cx >= chunkArray.length || cz < 0 || cz >= chunkArray[cx].length) return false;
        Chunk chunk = chunkArray[cx][cz];
        return chunk != null && NativeWaterlogging.contains(chunk, x, y, z);
    }
}
