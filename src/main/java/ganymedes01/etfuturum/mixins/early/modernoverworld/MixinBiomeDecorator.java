package ganymedes01.etfuturum.mixins.early.modernoverworld;

import ganymedes01.etfuturum.world.generate.terrain.ModernOverworldOreGenerator;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.gen.feature.WorldGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/** Replace only vanilla 1.7 ore helper calls on the Plus modern Overworld path. */
@Mixin(BiomeDecorator.class)
public abstract class MixinBiomeDecorator {

    @Shadow protected World currentWorld;
    @Shadow protected Random randomGenerator;
    @Shadow protected int chunk_X;
    @Shadow protected int chunk_Z;

    @Shadow public WorldGenerator coalGen;
    @Shadow public WorldGenerator ironGen;
    @Shadow public WorldGenerator goldGen;
    @Shadow public WorldGenerator redstoneGen;
    @Shadow public WorldGenerator diamondGen;
    @Shadow public WorldGenerator lapisGen;

    @Inject(method = "genStandardOre1", at = @At("HEAD"), cancellable = true)
    private void etfu$modernOreBands(int count, WorldGenerator generator, int minY, int maxY, CallbackInfo ci) {
        if (!ModernOverworldOreGenerator.isModernOreWorld(this.currentWorld)) {
            return;
        }
        if (generator == this.coalGen) {
            ModernOverworldOreGenerator.generateCoal(currentWorld, randomGenerator, chunk_X >> 4, chunk_Z >> 4);
        } else if (generator == this.ironGen) {
            ModernOverworldOreGenerator.generateIron(currentWorld, randomGenerator, chunk_X >> 4, chunk_Z >> 4);
        } else if (generator == this.goldGen) {
            ModernOverworldOreGenerator.generateGold(currentWorld, randomGenerator, chunk_X >> 4, chunk_Z >> 4);
        } else if (generator == this.redstoneGen) {
            ModernOverworldOreGenerator.generateRedstone(currentWorld, randomGenerator, chunk_X >> 4, chunk_Z >> 4);
        } else if (generator == this.diamondGen) {
            ModernOverworldOreGenerator.generateDiamond(currentWorld, randomGenerator, chunk_X >> 4, chunk_Z >> 4);
        } else {
            return; // Dirt/gravel and modded decorator helpers retain their original behaviour.
        }
        ci.cancel();
    }

    @Inject(method = "genStandardOre2", at = @At("HEAD"), cancellable = true)
    private void etfu$modernLapisBand(int count, WorldGenerator generator, int centerY, int spread,
            CallbackInfo ci) {
        if (ModernOverworldOreGenerator.isModernOreWorld(this.currentWorld) && generator == this.lapisGen) {
            ModernOverworldOreGenerator.generateLapis(currentWorld, randomGenerator, chunk_X >> 4, chunk_Z >> 4);
            ci.cancel();
        }
    }
}
