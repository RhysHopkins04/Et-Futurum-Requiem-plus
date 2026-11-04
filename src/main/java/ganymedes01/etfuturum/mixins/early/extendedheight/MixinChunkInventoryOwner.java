package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.CrucibleInventoryHeightContext;
import net.minecraft.block.Block;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only the exact owner's wrapped reads are redirected; corrected calls cannot recurse again. */
@Mixin(Chunk.class)
public abstract class MixinChunkInventoryOwner {
    @Inject(method="getBlock(III)Lnet/minecraft/block/Block;",at=@At("HEAD"),cancellable=true)
    private void etfu$ownerBlock(int x,int y,int z,CallbackInfoReturnable<Block> ci) {
        Chunk chunk=(Chunk)(Object)this;int fixed=CrucibleInventoryHeightContext.blockY(chunk,x,y,z);
        if(fixed!=y)ci.setReturnValue(chunk.getBlock(x,fixed,z));
    }
    @Inject(method="getBlockMetadata",at=@At("HEAD"),cancellable=true)
    private void etfu$ownerData(int x,int y,int z,CallbackInfoReturnable<Integer> ci) {
        Chunk chunk=(Chunk)(Object)this;int fixed=CrucibleInventoryHeightContext.blockY(chunk,x,y,z);
        if(fixed!=y)ci.setReturnValue(chunk.getBlockMetadata(x,fixed,z));
    }
    @Inject(method="getSavedLightValue",at=@At("HEAD"),cancellable=true)
    private void etfu$ownerLight(EnumSkyBlock sky,int x,int y,int z,CallbackInfoReturnable<Integer> ci) {
        Chunk chunk=(Chunk)(Object)this;int fixed=CrucibleInventoryHeightContext.blockY(chunk,x,y,z);
        if(fixed!=y)ci.setReturnValue(chunk.getSavedLightValue(sky,x,fixed,z));
    }
}
