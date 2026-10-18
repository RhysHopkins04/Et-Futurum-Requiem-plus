package ganymedes01.etfuturum.mixins.early.imported.client;

import ganymedes01.etfuturum.world.imported.ImportedWaterProfiles;
import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLiquid.class)
public class MixinBlockLiquid {
    @Inject(method="colorMultiplier",at=@At("HEAD"),cancellable=true,require=1)
    private void etfu$modernWaterTint(IBlockAccess world,int x,int y,int z,CallbackInfoReturnable<Integer> cir) {
        if(ImportedWaterProfiles.enabled() && ((Object)this==Blocks.water || (Object)this==Blocks.flowing_water))
            cir.setReturnValue(ImportedWaterProfiles.blendedColor(world,x,z));
    }
}
