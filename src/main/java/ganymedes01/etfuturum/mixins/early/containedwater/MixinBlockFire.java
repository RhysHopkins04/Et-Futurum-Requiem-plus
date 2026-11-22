package ganymedes01.etfuturum.mixins.early.containedwater;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(BlockFire.class)
public abstract class MixinBlockFire {
    @Inject(method = "tryCatchFire(Lnet/minecraft/world/World;IIIILjava/util/Random;ILnet/minecraftforge/common/util/ForgeDirection;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void etfu$wetBlockDoesNotBurn(World world, int x, int y, int z, int chance, Random random, int age,
            ForgeDirection face, CallbackInfo ci) {
        if (ModernMapParityBlocks.containsWater(world, x, y, z)) ci.cancel();
    }

    @Inject(method = "getChanceToEncourageFire", at = @At("HEAD"), cancellable = true, remap = false)
    private void etfu$wetBlockDoesNotSpreadFire(IBlockAccess world, int x, int y, int z,
            int oldChance, ForgeDirection face, CallbackInfoReturnable<Integer> cir) {
        if (ModernMapParityBlocks.containsWater(world, x, y, z)) cir.setReturnValue(oldChance);
    }

    @WrapOperation(method = "canCatchFire", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/block/Block;isFlammable(Lnet/minecraft/world/IBlockAccess;IIILnet/minecraftforge/common/util/ForgeDirection;)Z",
            remap = false), remap = false)
    private boolean etfu$wetBlockDoesNotCatchFire(Block block, IBlockAccess world, int x, int y, int z,
            ForgeDirection face, Operation<Boolean> original) {
        return !ModernMapParityBlocks.containsWater(world, x, y, z) && original.call(block, world, x, y, z, face);
    }
}
