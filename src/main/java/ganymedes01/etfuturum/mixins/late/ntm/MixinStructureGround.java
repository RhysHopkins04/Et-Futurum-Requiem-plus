package ganymedes01.etfuturum.mixins.late.ntm;

import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** NTM legacy components must not treat imported spawn Y128 as minimum foundation height. */
@Pseudo
@Mixin(targets="com.hbm.world.gen.component.Component",remap=false)
public abstract class MixinStructureGround {
    @Redirect(method={"setAverageHeight","getAverageHeight"},remap=false,
        at=@At(value="INVOKE",target="Lnet/minecraft/world/WorldProvider;getAverageGroundLevel()I",remap=true),require=1)
    private static int etfu$actualFoundationHeight(WorldProvider provider) {
        return ConfigMapCompatibility.importedEarthMode && provider.dimensionId==0
            ? 0 : provider.getAverageGroundLevel();
    }
}
