package ganymedes01.etfuturum.mixins.early.containedwater;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.core.utils.IContainedWaterFlow;
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
