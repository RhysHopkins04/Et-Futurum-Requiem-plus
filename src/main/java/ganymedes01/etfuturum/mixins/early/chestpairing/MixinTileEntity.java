package ganymedes01.etfuturum.mixins.early.chestpairing;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.core.utils.IChestPairingState;
import ganymedes01.etfuturum.core.utils.ModernChestPairing;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies the missing TileEntityChest description packet used to sync explicit pair state. */
@Mixin(TileEntity.class)
public abstract class MixinTileEntity {

    @Inject(method = "getDescriptionPacket", at = @At("HEAD"), cancellable = true)
    private void etfu$chestPairDescription(CallbackInfoReturnable<Packet> cir) {
        Object self = this;
        if (!ConfigBlocksItems.enableModernMapParityBlocks || !(self instanceof TileEntityChest)
                || !(self instanceof IChestPairingState)) return;
        TileEntity tile = (TileEntity) self;
        if (!ModernChestPairing.isManagedChestBlock(tile.getBlockType())) return;
        IChestPairingState pairing = (IChestPairingState) self;
        pairing.etfu$resolvePairing();
        byte direction = pairing.etfu$getPairDirection();
        if (direction == IChestPairingState.UNKNOWN) direction = IChestPairingState.NONE;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte(ModernChestPairing.NBT_PAIR_DIRECTION, direction);
        cir.setReturnValue(new S35PacketUpdateTileEntity(tile.xCoord, tile.yCoord, tile.zCoord,
                ModernChestPairing.PAIR_BLOCK_EVENT, tag));
    }

    // Forge adds onDataPacket after MCP/SRG mappings are generated, so its literal name must not be remapped.
    @Inject(method = "onDataPacket", at = @At("HEAD"), cancellable = true, remap = false)
    private void etfu$chestPairData(NetworkManager net, S35PacketUpdateTileEntity packet, CallbackInfo ci) {
        Object self = this;
        if (!ConfigBlocksItems.enableModernMapParityBlocks || !(self instanceof TileEntityChest)
                || !(self instanceof IChestPairingState)) return;
        TileEntity tile = (TileEntity) self;
        if (!ModernChestPairing.isManagedChestBlock(tile.getBlockType())) return;
        NBTTagCompound tag = packet.func_148857_g();
        if (tag != null && tag.hasKey(ModernChestPairing.NBT_PAIR_DIRECTION, 1)) {
            ((IChestPairingState) self).etfu$setPairDirection(tag.getByte(ModernChestPairing.NBT_PAIR_DIRECTION));
            ci.cancel();
        }
    }
}
