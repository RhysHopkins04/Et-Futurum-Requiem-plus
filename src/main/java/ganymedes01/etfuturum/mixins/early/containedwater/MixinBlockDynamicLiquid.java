package ganymedes01.etfuturum.mixins.early.containedwater;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.core.utils.IContainedWaterFlow;
import ganymedes01.etfuturum.core.utils.DripleafWater;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Reuse vanilla's bounded flow-cost search and placement without replacing the occupied source. */
@Mixin(BlockDynamicLiquid.class)
public abstract class MixinBlockDynamicLiquid implements IContainedWaterFlow {
    @Shadow private boolean func_149809_q(World w,int x,int y,int z) { throw new AssertionError(); }
    @Shadow private boolean[] func_149808_o(World w,int x,int y,int z) { throw new AssertionError(); }
    @Shadow private void func_149813_h(World w,int x,int y,int z,int decay) { throw new AssertionError(); }
    /**
     * Vanilla 1.7.10 treats waterlogged plants as ordinary Material.plants and
     * consequently regards them as flowing-water replacement targets.  A
     * contained source has already occupied this cell: no incoming water flow
     * may replace the plant (or cause it to drop an item).
     */
    @Inject(method = "func_149809_q", at = @At("HEAD"), cancellable = true)
    private void etfu$doNotDisplaceContainedSource(World world, int x, int y, int z,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (((Block) (Object) this).getMaterial() == Material.water
                && ModernMapParityBlocks.containsWater(world, x, y, z)) {
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
    private void etfu$campfireAllowsIncomingWater(World world,int x,int y,int z,CallbackInfoReturnable<Boolean> cir) {
        if (((Block)(Object)this).getMaterial() != Material.water || world.provider.isHellWorld) return;
        ModernMapParityBlocks entry=ModernMapParityBlocks.fromBlock(world.getBlock(x,y,z));
        if (entry==ModernMapParityBlocks.CAMPFIRE || entry==ModernMapParityBlocks.SOUL_CAMPFIRE) cir.setReturnValue(false);
    }

    public void etfu$spreadSource(World w,int x,int y,int z) {
        if (w.isRemote || !ModernMapParityBlocks.containsWater(w,x,y,z)) return;
        if (func_149809_q(w,x,y-1,z)) { func_149813_h(w,x,y-1,z,8); return; }
        boolean[] flow=func_149808_o(w,x,y,z);
        if (flow[0]) func_149813_h(w,x-1,y,z,1);
        if (flow[1]) func_149813_h(w,x+1,y,z,1);
        if (flow[2]) func_149813_h(w,x,y,z-1,1);
        if (flow[3]) func_149813_h(w,x,y,z+1,1);
    }
}
