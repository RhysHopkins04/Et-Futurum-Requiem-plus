package ganymedes01.etfuturum.mixins.early.containedwater;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Explosion.class)
public abstract class MixinExplosion {
    @WrapOperation(method = "doExplosionA", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/block/Block;getExplosionResistance(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;IIIDDD)F",
            remap = false))
    private float etfu$includeWaterResistance(Block block, Entity exploder, World world, int x, int y, int z,
            double explosionX, double explosionY, double explosionZ, Operation<Float> original) {
        float resistance = original.call(block, exploder, world, x, y, z, explosionX, explosionY, explosionZ);
        if (ModernMapParityBlocks.containsWater(world, x, y, z))
            resistance = Math.max(resistance, Blocks.water.getExplosionResistance(exploder));
        return resistance;
    }

    @WrapOperation(method = "doExplosionA", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/entity/Entity;func_145772_a(Lnet/minecraft/world/Explosion;Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;)F"))
    private float etfu$includeWaterResistanceForEntity(Entity exploder, Explosion explosion, World world,
            int x, int y, int z, Block block, Operation<Float> original) {
        float resistance = original.call(exploder, explosion, world, x, y, z, block);
        if (ModernMapParityBlocks.containsWater(world, x, y, z))
            resistance = Math.max(resistance, original.call(exploder, explosion, world, x, y, z, Blocks.water));
        return resistance;
    }
}
