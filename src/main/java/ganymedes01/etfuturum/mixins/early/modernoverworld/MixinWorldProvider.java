package ganymedes01.etfuturum.mixins.early.modernoverworld;

import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import ganymedes01.etfuturum.world.generate.terrain.ModernOverworldWorldChunkManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.WorldChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Establishes the vertical reference frame used by the Plus modern Overworld path.
 *
 * The 1.7.10 engine remains positive-Y: logical modern -64..319 maps to physical 0..383.
 * This mixin does not replace terrain density generation yet; it centralises the reference heights
 * that the subsequent terrain/cave patches must target.
 */
@Mixin(WorldProvider.class)
public abstract class MixinWorldProvider {

    @Shadow public int dimensionId;
    @Shadow public World worldObj;
    @Shadow public WorldChunkManager worldChunkMgr;

    private boolean etfu$isModernOverworld() {
        return this.dimensionId == 0 && ConfigWorld.modernOverworldGeneration;
    }


    /** Replace only dimension 0's legacy GenLayer biome map with the terrain-aligned modern source. */
    @Inject(method = "registerWorldChunkManager", at = @At("RETURN"))
    private void etfu$modernBiomeSource(CallbackInfo ci) {
        if (etfu$isModernOverworld() && this.worldObj != null) {
            this.worldChunkMgr = new ModernOverworldWorldChunkManager(this.worldObj);
        }
    }

    /** Forge-added literal method; cloud Y 192 modern -> physical Y 256. */
    @Inject(method = "getCloudHeight", at = @At("HEAD"), cancellable = true, remap = false)
    private void etfu$modernCloudHeight(CallbackInfoReturnable<Float> cir) {
        if (etfu$isModernOverworld()) {
            cir.setReturnValue((float) WorldHeightCompat.PHYSICAL_CLOUD_HEIGHT);
        }
    }

    /** Vanilla/MCP method: logical Y64 spawn reference -> physical Y128. */
    @Inject(method = "getAverageGroundLevel", at = @At("HEAD"), cancellable = true)
    private void etfu$modernAverageGroundLevel(CallbackInfoReturnable<Integer> cir) {
        if (etfu$isModernOverworld()) {
            cir.setReturnValue(WorldHeightCompat.PHYSICAL_AVERAGE_GROUND_LEVEL);
        }
    }

    /** Forge-added literal method: logical sea/horizon Y63 -> physical Y127. */
    @Inject(method = "getHorizon", at = @At("HEAD"), cancellable = true, remap = false)
    private void etfu$modernHorizon(CallbackInfoReturnable<Double> cir) {
        if (etfu$isModernOverworld()) {
            cir.setReturnValue((double) WorldHeightCompat.PHYSICAL_SEA_LEVEL);
        }
    }
}
