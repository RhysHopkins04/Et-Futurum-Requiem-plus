package ganymedes01.etfuturum.mixins.early.containedwater;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.core.utils.IContainedWaterFlow;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import ganymedes01.etfuturum.core.utils.DripleafWater;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Reuse vanilla's bounded flow-cost search and placement without replacing the occupied source. */
@Mixin(BlockDynamicLiquid.class)
public abstract class MixinBlockDynamicLiquid implements IContainedWaterFlow {
    @Shadow private boolean func_149809_q(World w,int x,int y,int z) { throw new AssertionError(); }
    @Shadow private boolean[] func_149808_o(World w,int x,int y,int z) { throw new AssertionError(); }
    @Shadow private void func_149813_h(World w,int x,int y,int z,int decay) { throw new AssertionError(); }
    /**
     * Flowing water cannot displace a waterloggable host or an existing
     * contained source. Source conversion is handled by the scheduled host update.
     */
    @Inject(method = "func_149809_q", at = @At("HEAD"), cancellable = true)
    private void etfu$doNotDisplaceContainedSource(World world, int x, int y, int z,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (((Block) (Object) this).getMaterial() == Material.water
                && (NativeWaterlogging.canHold(world.getBlock(x, y, z), world, x, y, z)
                        || ModernMapParityBlocks.containsWater(world, x, y, z))) {
            cir.setReturnValue(false);
        }
    }

    /**
     * When flowing water actually reaches a dry dripleaf, fill its bounded
     * waterlogged state instead of dropping it and converting it into water.
     * This must happen at the write site, NOT in the flow-cost query above.
     */
    @Inject(method = "func_149813_h", at = @At("HEAD"), cancellable = true)
    private void etfu$waterlogDripleafInsteadOfBreaking(World world, int x, int y, int z,
                                                          int decay, CallbackInfo ci) {
        if (((Block) (Object) this).getMaterial() != Material.water) return;
        if (DripleafWater.isDripleaf(world, x, y, z)) {
            if (!DripleafWater.contains(world, x, y, z)) {
                DripleafWater.fill(world, x, y, z);
            }
            ci.cancel();
        } else if ((ModernMapParityBlocks.fromBlock(world.getBlock(x,y,z)) == ModernMapParityBlocks.CAMPFIRE
                || ModernMapParityBlocks.fromBlock(world.getBlock(x,y,z)) == ModernMapParityBlocks.SOUL_CAMPFIRE)
                && ModernMapParityBlocks.fillContainedWater(world,x,y,z)) {
            ci.cancel();
        }
    }

    @Inject(method = "func_149807_p", at = @At("HEAD"), cancellable = true)
    private void etfu$containedWaterFlowCost(World world,int x,int y,int z,CallbackInfoReturnable<Boolean> cir) {
        if (((Block)(Object)this).getMaterial() != Material.water || world.provider.isHellWorld) return;
        Block block = world.getBlock(x,y,z);
        if (NativeWaterlogging.canHold(block, world, x, y, z)) {
            cir.setReturnValue(true);
            return;
        }
        ModernMapParityBlocks entry=ModernMapParityBlocks.fromBlock(block);
        if (entry==ModernMapParityBlocks.CAMPFIRE || entry==ModernMapParityBlocks.SOUL_CAMPFIRE) cir.setReturnValue(false);
    }

    public void etfu$spreadSource(World w,int x,int y,int z) {
        if (w.isRemote || !ModernMapParityBlocks.containsWater(w,x,y,z)) return;
        if (NativeWaterlogging.contains(w, x, y, z)) {
            // Full waterlogged cells hold water even when an entity can pass through the block.
            if (NativeWaterlogging.holdsWaterWithoutSpreading(w.getBlock(x, y, z), w, x, y, z)) return;
            for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                if (direction == ForgeDirection.UP || NativeWaterlogging.waterFaceHidden(w, x, y, z, direction)) continue;
                int nx = x + direction.offsetX, ny = y + direction.offsetY, nz = z + direction.offsetZ;
                if (func_149809_q(w, nx, ny, nz)) func_149813_h(w, nx, ny, nz, direction == ForgeDirection.DOWN ? 8 : 1);
            }
            return;
        }
        if (func_149809_q(w,x,y-1,z)) { func_149813_h(w,x,y-1,z,8); return; }
        boolean[] flow=func_149808_o(w,x,y,z);
        if (flow[0]) func_149813_h(w,x-1,y,z,1);
        if (flow[1]) func_149813_h(w,x+1,y,z,1);
        if (flow[2]) func_149813_h(w,x,y,z-1,1);
        if (flow[3]) func_149813_h(w,x,y,z+1,1);
    }
}
