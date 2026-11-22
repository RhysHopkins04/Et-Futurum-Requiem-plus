package ganymedes01.etfuturum.mixins.early.containedwater;

import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import ganymedes01.etfuturum.core.utils.WaterloggedPistonMove;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityPiston.class)
public abstract class MixinTileEntityPiston extends TileEntity implements WaterloggedPistonMove {
    @Shadow public abstract Block getStoredBlockID();

    @Unique private static final String WATER_MOVE_TAG = "etfuturumWaterMove";
    @Unique private static final byte MOVED_WET = 1;
    @Unique private static final byte RETAIN_WATER = 2;
    @Unique private byte etfu$waterMove;

    @Override
    public void etfu$recordMovedWater() {
        etfu$waterMove = MOVED_WET;
    }

    @Override
    public void etfu$retainWaterOnCompletion() {
        if (etfu$waterMove == MOVED_WET) {
            etfu$waterMove = RETAIN_WATER;
            markDirty();
        }
    }

    @Inject(method = {"clearPistonTileEntity", "updateEntity"}, at = @At("RETURN"))
    private void etfu$finishShortPulse(CallbackInfo ci) {
        if (etfu$waterMove == RETAIN_WATER && worldObj != null && !worldObj.isRemote
                && worldObj.getBlock(xCoord, yCoord, zCoord) == getStoredBlockID()) {
            etfu$waterMove = 0;
            NativeWaterlogging.fill(worldObj, xCoord, yCoord, zCoord);
        }
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void etfu$readWaterMove(NBTTagCompound tag, CallbackInfo ci) {
        byte stored = tag.getByte(WATER_MOVE_TAG);
        etfu$waterMove = stored == MOVED_WET || stored == RETAIN_WATER ? stored : 0;
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void etfu$writeWaterMove(NBTTagCompound tag, CallbackInfo ci) {
        if (etfu$waterMove != 0) tag.setByte(WATER_MOVE_TAG, etfu$waterMove);
    }
}
