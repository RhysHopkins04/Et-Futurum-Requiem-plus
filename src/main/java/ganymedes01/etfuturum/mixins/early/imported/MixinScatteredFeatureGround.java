package ganymedes01.etfuturum.mixins.early.imported;

import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Imported spawn Y128 is a reference, not a minimum foundation height for native terrain. */
@Mixin(targets = "net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces$Feature")
public abstract class MixinScatteredFeatureGround {
    @Redirect(method = "func_74935_a", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/WorldProvider;getAverageGroundLevel()I"), require = 1)
    private int etfu$useActualSurface(WorldProvider provider) {
        // Math.max still uses the queried solid surface. Other dimensions retain native policy.
        return ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility.importedEarthMode && provider.dimensionId == 0 ? 0 : provider.getAverageGroundLevel();
    }
}
