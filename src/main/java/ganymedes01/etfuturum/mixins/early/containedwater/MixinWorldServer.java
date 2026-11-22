package ganymedes01.etfuturum.mixins.early.containedwater;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.core.utils.IContainedWaterFlow;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Random;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer {
    @WrapOperation(method = "tickUpdates", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/Block;updateTick(Lnet/minecraft/world/World;IIILjava/util/Random;)V"))
    private void etfu$spreadWaterloggedSource(Block host, World world, int x, int y, int z, Random random,
                                              Operation<Void> original) {
        original.call(host, world, x, y, z, random);
        if (NativeWaterlogging.contains(world, x, y, z))
            ((IContainedWaterFlow) Blocks.flowing_water).etfu$spreadSource(world, x, y, z);
        else if (NativeWaterlogging.canFillFromSources(world, world.getBlock(x, y, z), x, y, z))
            NativeWaterlogging.fill(world, x, y, z);
    }
}
