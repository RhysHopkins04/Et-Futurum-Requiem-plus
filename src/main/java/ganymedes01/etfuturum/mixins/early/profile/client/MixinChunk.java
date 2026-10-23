package ganymedes01.etfuturum.mixins.early.profile.client;

import ganymedes01.etfuturum.network.profile.ClientWorldProfile;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Interpret the server's biome bytes per connection, without altering the global client registry. */
@Mixin(Chunk.class)
public abstract class MixinChunk {
    @Shadow public World worldObj;
    @Shadow public abstract byte[] getBiomeArray();
    @Inject(method = "getBiomeGenForWorldCoords", at = @At("HEAD"), cancellable = true, require = 1)
    private void etfu$serverBiome(int x, int z, WorldChunkManager manager, CallbackInfoReturnable<BiomeGenBase> cir) {
        if (!worldObj.isRemote || worldObj.provider.dimensionId != 0) return;
        BiomeGenBase biome = ClientWorldProfile.biome(getBiomeArray()[(z & 15) << 4 | (x & 15)] & 255);
        if (biome != null) cir.setReturnValue(biome);
    }
}
