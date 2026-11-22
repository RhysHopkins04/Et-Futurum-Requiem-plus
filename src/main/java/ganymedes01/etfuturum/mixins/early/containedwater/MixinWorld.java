package ganymedes01.etfuturum.mixins.early.containedwater;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ganymedes01.etfuturum.core.utils.ContainedWater;
import ganymedes01.etfuturum.core.utils.DripleafWater;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class MixinWorld {
    @WrapMethod(method = "setBlock(IIILnet/minecraft/block/Block;II)Z")
    private boolean etfu$replaceWaterlogged(int x, int y, int z, Block block, int meta, int flags,
                                            Operation<Boolean> original) {
        World world = (World) (Object) this;
        if (world.isRemote || world.captureBlockSnapshots || world.restoringBlockSnapshots)
            return original.call(x, y, z, block, meta, flags);
        if (!NativeWaterlogging.contains(world, x, y, z)) {
            // Dripleaf has its own persistent encoding, including both halves of small dripleaf.
            if (block == Blocks.air && DripleafWater.contains(world, x, y, z))
                return original.call(x, y, z, Blocks.flowing_water, 0, flags);
            return original.call(x, y, z, block, meta, flags);
        }
        boolean changed = block == Blocks.air
                ? original.call(x, y, z, Blocks.flowing_water, 0, flags)
                : original.call(x, y, z, block, meta, flags);
        if (changed) NativeWaterlogging.onReplacement(world, x, y, z);
        return changed;
    }

    @Inject(method = "notifyBlockOfNeighborChange", at = @At("RETURN"))
    private void etfu$scheduleWaterlogged(int x, int y, int z, Block changed, CallbackInfo ci) {
        World world = (World) (Object) this;
        if (world.isRemote || !world.blockExists(x, y, z)) return;
        Block host = world.getBlock(x, y, z);
        if (NativeWaterlogging.canHold(host, world, x, y, z)
                && (NativeWaterlogging.contains(world, x, y, z) || NativeWaterlogging.canFillFromSources(world, host, x, y, z)))
            world.scheduleBlockUpdate(x, y, z, host, Blocks.flowing_water.tickRate(world));
    }

    @Inject(method = "setBlockMetadataWithNotify", at = @At("RETURN"))
    private void etfu$refreshTrapdoorFlow(int x, int y, int z, int meta, int flags, CallbackInfoReturnable<Boolean> cir) {
        World world = (World) (Object) this;
        if (world.isRemote || !cir.getReturnValue()) return;
        Block host = world.getBlock(x, y, z);
        if (host instanceof BlockTrapDoor && NativeWaterlogging.contains(world, x, y, z)) {
            world.scheduleBlockUpdate(x, y, z, host, Blocks.flowing_water.tickRate(world));
            world.notifyBlocksOfNeighborChange(x, y, z, Blocks.water);
        }
    }
    @Inject(method = "handleMaterialAcceleration", at = @At("RETURN"), cancellable = true)
    private void etfu$bodyWater(AxisAlignedBB box, Material material, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (material == Material.water && !cir.getReturnValue() && ContainedWater.intersects((World)(Object)this,box))
            cir.setReturnValue(true); // Source fluid has no current. Vanilla swimming consumes this result.
    }
    @Inject(method = {"isMaterialInBB", "isAABBInMaterial"}, at = @At("RETURN"), cancellable = true)
    private void etfu$waterVolume(AxisAlignedBB box, Material material, CallbackInfoReturnable<Boolean> cir) {
        if (material == Material.water && !cir.getReturnValue() && ContainedWater.intersects((World)(Object)this,box))
            cir.setReturnValue(true);
    }
}
