package ganymedes01.etfuturum.mixins.early.imported;

import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Imported spawn Y128 is a reference, not a minimum foundation height for native terrain. */
@Mixin(targets = "net.minecraft.world.gen.structure.StructureVillagePieces$Village")
public abstract class MixinVillageGround {
    @Redirect(method = "getAverageGroundLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/WorldProvider;getAverageGroundLevel()I"), require = 1)
    private int etfu$useActualSurface(WorldProvider provider) {
        // Math.max still uses the queried solid surface. Other dimensions retain native policy.
        return provider.dimensionId == 0 ? 0 : provider.getAverageGroundLevel();
    }
}
