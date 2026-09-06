package ganymedes01.etfuturum.mixins.early.commandblockstate;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.core.utils.IModernCommandBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stores modern facing/conditional state without repurposing vanilla's powered metadata bit. */
@Mixin(TileEntityCommandBlock.class)
public abstract class MixinTileEntityCommandBlock extends TileEntity implements IModernCommandBlockState {
    @Unique private byte etfu$modernFacing = 2; // north
    @Unique private boolean etfu$conditional;

    @Override public int etfu$getModernFacing() { return etfu$modernFacing & 7; }
    @Override public boolean etfu$isConditional() { return etfu$conditional; }

    @Override
    public void etfu$setModernState(int facing, boolean conditional) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks) return;
        int clamped = facing < 0 ? 0 : facing > 5 ? 5 : facing;
        if ((etfu$modernFacing & 7) == clamped && etfu$conditional == conditional) return;
        etfu$modernFacing = (byte) clamped;
        etfu$conditional = conditional;
        markDirty();
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void etfu$writeModernState(NBTTagCompound tag, CallbackInfo ci) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks) return;
        tag.setByte("EFRModernFacing", etfu$modernFacing);
        tag.setBoolean("EFRConditional", etfu$conditional);
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void etfu$readModernState(NBTTagCompound tag, CallbackInfo ci) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks) return;
        if (tag.hasKey("EFRModernFacing")) {
            int facing = tag.getByte("EFRModernFacing");
            etfu$modernFacing = (byte) (facing < 0 ? 0 : facing > 5 ? 5 : facing);
        }
        etfu$conditional = tag.getBoolean("EFRConditional");
    }

    /** TileEntityCommandBlock inherits TileEntity's empty packet hook in 1.7.10. */
    @Override
    public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks) return;
        NBTTagCompound tag = packet.func_148857_g();
        if (tag.hasKey("EFRModernFacing")) {
            int facing = tag.getByte("EFRModernFacing");
            etfu$modernFacing = (byte) (facing < 0 ? 0 : facing > 5 ? 5 : facing);
        }
        etfu$conditional = tag.getBoolean("EFRConditional");
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
}
