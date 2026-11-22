package ganymedes01.etfuturum.mixins.early.containedwater;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.core.utils.WaterloggedPistonMove;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.Facing;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockPistonBase.class)
public abstract class MixinBlockPistonBase {
    @Shadow @Final private boolean isSticky;

    @WrapOperation(method = "tryExtend", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/World;setTileEntity(IIILnet/minecraft/tileentity/TileEntity;)V", ordinal = 1))
    private void etfu$recordWaterBeforeExtension(World world, int x, int y, int z, TileEntity moving,
            Operation<Void> original) {
        if (isSticky && moving instanceof TileEntityPiston) {
            int facing = ((TileEntityPiston) moving).getPistonOrientation();
            WaterloggedPistonMove.record(world, x - Facing.offsetsXForSide[facing],
                    y - Facing.offsetsYForSide[facing], z - Facing.offsetsZForSide[facing], moving);
        }
        original.call(world, x, y, z, moving);
    }

    @WrapOperation(method = "onBlockEventReceived", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/tileentity/TileEntityPiston;clearPistonTileEntity()V", ordinal = 1))
    private void etfu$retainWaterWhenSpittingBlock(TileEntityPiston moving, Operation<Void> original) {
        WaterloggedPistonMove.retainOnShortPulse(moving);
        original.call(moving);
    }
}
