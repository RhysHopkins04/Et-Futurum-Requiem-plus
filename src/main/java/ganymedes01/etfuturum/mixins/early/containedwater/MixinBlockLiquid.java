package ganymedes01.etfuturum.mixins.early.containedwater;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid {
    @Shadow protected abstract void func_149799_m(World world,int x,int y,int z);
    @Inject(method = "func_149804_e", at = @At("HEAD"), cancellable = true)
    private void etfu$sourceDecay(World world,int x,int y,int z,CallbackInfoReturnable<Integer> cir) {
        if (((Block)(Object)this).getMaterial()==Material.water && ModernMapParityBlocks.containsWater(world,x,y,z)) cir.setReturnValue(0);
    }
    @Inject(method = "func_149805_n", at = @At("HEAD"))
    private void etfu$containedWaterCoolsLava(World world,int x,int y,int z,CallbackInfo ci) {
        if (((Block)(Object)this).getMaterial()!=Material.lava || world.getBlock(x,y,z)!=(Object)this) return;
        if (ModernMapParityBlocks.containsWater(world,x-1,y,z) || ModernMapParityBlocks.containsWater(world,x+1,y,z)
                || ModernMapParityBlocks.containsWater(world,x,y,z-1) || ModernMapParityBlocks.containsWater(world,x,y,z+1)
                || ModernMapParityBlocks.containsWater(world,x,y+1,z)) {
            int meta=world.getBlockMetadata(x,y,z);
            if (meta==0) world.setBlock(x,y,z,Blocks.obsidian);
            else if (meta<=4) world.setBlock(x,y,z,Blocks.cobblestone);
            func_149799_m(world,x,y,z);
        }
    }
}
