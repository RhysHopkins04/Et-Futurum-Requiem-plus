package ganymedes01.etfuturum.mixins.early.modernoverworld;

import ganymedes01.etfuturum.configuration.configs.ConfigModCompat;
import ganymedes01.etfuturum.world.generate.terrain.ModernOverworldOreGenerator;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenMinable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Optional compatibility for ordinary WorldGenMinable-based ores owned by other mods.
 * Their ore identity/size/target remain theirs; Plus only translates legacy vertical coordinates,
 * and optionally substitutes clumpier geometry when explicitly requested by config.
 */
@Mixin(WorldGenMinable.class)
public abstract class MixinWorldGenMinable {

    @Shadow public Block field_150519_a;
    @Shadow public int numberOfBlocks;
    @Shadow public Block field_150518_c;
    // Forge adds this field in its WorldGenMinable source patch; it has no MCP/SRG mapping.
    @Shadow(remap = false) private int mineableBlockMeta;

    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void etfu$translateOrReshapeExternalOre(World world, Random rand, int x, int y, int z,
            CallbackInfoReturnable<Boolean> cir) {
        if (!ModernOverworldOreGenerator.isModernOreWorld(world)
                || !ModernOverworldOreGenerator.shouldTranslateExternalModOre(this.field_150519_a, this.mineableBlockMeta)) {
            return;
        }
        int translatedY = ModernOverworldOreGenerator.translateLegacyModOreY(world, y);
        boolean result;
        if (ConfigModCompat.modernOverworldReshapeModdedOreVeins) {
            result = ModernOverworldOreGenerator.generateExternalModOre(world, rand, x, translatedY, z,
                    this.field_150519_a, this.mineableBlockMeta, this.numberOfBlocks, this.field_150518_c);
        } else {
            result = ModernOverworldOreGenerator.generateExternalModOreLegacy(world, rand, x, translatedY, z,
                    this.field_150519_a, this.mineableBlockMeta, this.numberOfBlocks, this.field_150518_c);
        }
        cir.setReturnValue(result);
    }
}
