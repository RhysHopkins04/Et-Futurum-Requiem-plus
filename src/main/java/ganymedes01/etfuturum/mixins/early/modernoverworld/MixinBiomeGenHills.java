package ganymedes01.etfuturum.mixins.early.modernoverworld;

import ganymedes01.etfuturum.world.generate.terrain.ModernOverworldOreGenerator;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenHills;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Suppress the hard-coded 1.7 mountain emerald band when the modern ore distributor owns emeralds. */
@Mixin(BiomeGenHills.class)
public abstract class MixinBiomeGenHills {

    @Redirect(method = "decorate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;II)Z"))
    private boolean etfu$suppressLegacyEmerald(World world, int x, int y, int z, Block block, int meta, int flags) {
        if (ModernOverworldOreGenerator.isModernOreWorld(world)) {
            return false;
        }
        return world.setBlock(x, y, z, block, meta, flags);
    }
}
