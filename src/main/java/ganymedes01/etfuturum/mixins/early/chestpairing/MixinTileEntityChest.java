package ganymedes01.etfuturum.mixins.early.chestpairing;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.core.utils.IChestPairingState;
import ganymedes01.etfuturum.core.utils.ModernChestPairing;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the modern explicit SINGLE/LEFT/RIGHT relationship to 1.7 TileEntityChest without changing
 * its registry id or inventory format. The stored value is the direction from this chest to its
 * one reciprocal partner; raw adjacency alone no longer makes a double chest.
 */
@Mixin(TileEntityChest.class)
public abstract class MixinTileEntityChest extends TileEntity implements IChestPairingState {

    @Shadow public boolean adjacentChestChecked;
    @Shadow public TileEntityChest adjacentChestZNeg;
    @Shadow public TileEntityChest adjacentChestXPos;
    @Shadow public TileEntityChest adjacentChestXNeg;
    @Shadow public TileEntityChest adjacentChestZPos;

    @Unique private byte etfu$pairDirection = IChestPairingState.UNKNOWN;

    @Override
    public byte etfu$getPairDirection() {
        return this.etfu$pairDirection;
    }

    @Override
    public void etfu$setPairDirection(byte direction) {
        byte normalized = ModernChestPairing.isStoredDirection(direction) ? direction : IChestPairingState.NONE;
        this.etfu$setPairDirectionInternal(normalized, true);
    }

    @Unique
    private void etfu$setPairDirectionInternal(byte direction, boolean sync) {
        if (this.etfu$pairDirection == direction && this.adjacentChestChecked == false) return;
        this.etfu$pairDirection = direction;
        this.adjacentChestChecked = false;
        this.adjacentChestZNeg = null;
        this.adjacentChestXPos = null;
        this.adjacentChestXNeg = null;
        this.adjacentChestZPos = null;
        this.markDirty();

        if (this.worldObj != null) {
            if (this.worldObj.isRemote) {
                this.worldObj.markBlockRangeForRenderUpdate(this.xCoord, this.yCoord, this.zCoord,
                        this.xCoord, this.yCoord, this.zCoord);
            } else if (sync) {
                this.worldObj.addBlockEvent(this.xCoord, this.yCoord, this.zCoord, this.getBlockType(),
                        ModernChestPairing.PAIR_BLOCK_EVENT, direction);
                this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
                this.worldObj.func_147453_f(this.xCoord, this.yCoord, this.zCoord, this.getBlockType());
            }
        }
    }

    @Override
    public void etfu$resolvePairing() {
        if (!ConfigBlocksItems.enableModernMapParityBlocks || this.worldObj == null
                || !ModernChestPairing.isManagedChestBlock(this.getBlockType())) return;

        if (this.etfu$pairDirection == IChestPairingState.UNKNOWN) {
            this.etfu$migrateLegacyPair();
        }

        if (!ModernChestPairing.isPair(this.etfu$pairDirection)) return;

        byte direction = this.etfu$pairDirection;
        TileEntityChest partner = this.etfu$partnerAt(direction);
        if (partner == null) {
            // Collapse locally as soon as the client sees the partner disappear. The server still
            // broadcasts the authoritative TE state, but Copper Chest rendering no longer waits for
            // that later packet before returning to the single model.
            this.etfu$setPairDirectionInternal(IChestPairingState.NONE, !this.worldObj.isRemote);
            return;
        }

        IChestPairingState other = (IChestPairingState) (Object) partner;
        byte expectedOther = ModernChestPairing.opposite(direction);
        byte otherDirection = other.etfu$getPairDirection();

        if (!this.worldObj.isRemote) {
            if (otherDirection == IChestPairingState.UNKNOWN || otherDirection == IChestPairingState.NONE) {
                other.etfu$setPairDirection(expectedOther);
                otherDirection = expectedOther;
            }
            if (otherDirection != expectedOther) {
                this.etfu$setPairDirectionInternal(IChestPairingState.NONE, true);
                return;
            }
        }

        this.etfu$assignAdjacent(direction, partner);
    }

    @Unique
    private void etfu$migrateLegacyPair() {
        int meta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
        byte foundDirection = IChestPairingState.NONE;
        TileEntityChest foundPartner = null;
        int found = 0;
        byte[] directions = new byte[] {
                IChestPairingState.WEST, IChestPairingState.EAST,
                IChestPairingState.NORTH, IChestPairingState.SOUTH
        };

        for (byte direction : directions) {
            if (!ModernChestPairing.isLateralForFacing(direction, meta)) continue;
            TileEntityChest partner = this.etfu$partnerAt(direction);
            if (partner == null) continue;
            IChestPairingState state = (IChestPairingState) (Object) partner;
            byte partnerDirection = state.etfu$getPairDirection();
            if (partnerDirection != IChestPairingState.UNKNOWN
                    && partnerDirection != ModernChestPairing.opposite(direction)) continue;
            foundDirection = direction;
            foundPartner = partner;
            found++;
        }

        if (found != 1 || foundPartner == null) {
            this.etfu$setPairDirectionInternal(IChestPairingState.NONE, !this.worldObj.isRemote);
            return;
        }

        this.etfu$setPairDirectionInternal(foundDirection, !this.worldObj.isRemote);
        if (!this.worldObj.isRemote) {
            IChestPairingState other = (IChestPairingState) (Object) foundPartner;
            if (other.etfu$getPairDirection() == IChestPairingState.UNKNOWN) {
                other.etfu$setPairDirection(ModernChestPairing.opposite(foundDirection));
            }
        }
    }

    @Unique
    private TileEntityChest etfu$partnerAt(byte direction) {
        if (!ModernChestPairing.isPair(direction) || this.worldObj == null) return null;
        int x = this.xCoord + ModernChestPairing.offsetX(direction);
        int z = this.zCoord + ModernChestPairing.offsetZ(direction);
        Block ownBlock = this.getBlockType();
        if (ownBlock == null || this.worldObj.getBlock(x, this.yCoord, z) != ownBlock) return null;
        int ownMeta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
        int otherMeta = this.worldObj.getBlockMetadata(x, this.yCoord, z);
        if (ownMeta != otherMeta || !ModernChestPairing.isLateralForFacing(direction, ownMeta)) return null;
        TileEntity tile = this.worldObj.getTileEntity(x, this.yCoord, z);
        return tile instanceof TileEntityChest ? (TileEntityChest) tile : null;
    }

    @Unique
    private void etfu$assignAdjacent(byte direction, TileEntityChest partner) {
        this.adjacentChestZNeg = null;
        this.adjacentChestXPos = null;
        this.adjacentChestXNeg = null;
        this.adjacentChestZPos = null;
        if (direction == IChestPairingState.WEST) this.adjacentChestXNeg = partner;
        else if (direction == IChestPairingState.EAST) this.adjacentChestXPos = partner;
        else if (direction == IChestPairingState.NORTH) this.adjacentChestZNeg = partner;
        else if (direction == IChestPairingState.SOUTH) this.adjacentChestZPos = partner;
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void etfu$readPairDirection(NBTTagCompound tag, CallbackInfo ci) {
        if (tag.hasKey(ModernChestPairing.NBT_PAIR_DIRECTION, 1)) {
            byte direction = tag.getByte(ModernChestPairing.NBT_PAIR_DIRECTION);
            this.etfu$pairDirection = ModernChestPairing.isStoredDirection(direction)
                    ? direction : IChestPairingState.NONE;
        } else {
            this.etfu$pairDirection = IChestPairingState.UNKNOWN;
        }
        this.adjacentChestChecked = false;
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void etfu$writePairDirection(NBTTagCompound tag, CallbackInfo ci) {
        if (ConfigBlocksItems.enableModernMapParityBlocks && this.worldObj != null
                && ModernChestPairing.isManagedChestBlock(this.getBlockType())) {
            if (this.etfu$pairDirection == IChestPairingState.UNKNOWN) {
                this.etfu$resolvePairing();
            }
            byte direction = this.etfu$pairDirection == IChestPairingState.UNKNOWN
                    ? IChestPairingState.NONE : this.etfu$pairDirection;
            tag.setByte(ModernChestPairing.NBT_PAIR_DIRECTION, direction);
        }
    }

    @Inject(method = "checkForAdjacentChests", at = @At("HEAD"), cancellable = true)
    private void etfu$useExplicitPairing(CallbackInfo ci) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks || this.worldObj == null
                || !ModernChestPairing.isManagedChestBlock(this.getBlockType())) return;
        if (this.adjacentChestChecked) {
            ci.cancel();
            return;
        }
        this.adjacentChestChecked = true;
        this.adjacentChestZNeg = null;
        this.adjacentChestXPos = null;
        this.adjacentChestXNeg = null;
        this.adjacentChestZPos = null;
        this.etfu$resolvePairing();
        this.adjacentChestChecked = true;
        ci.cancel();
    }

    @Inject(method = "receiveClientEvent", at = @At("HEAD"), cancellable = true)
    private void etfu$receivePairEvent(int id, int value, CallbackInfoReturnable<Boolean> cir) {
        if (ConfigBlocksItems.enableModernMapParityBlocks && this.worldObj != null
                && ModernChestPairing.isManagedChestBlock(this.getBlockType())
                && id == ModernChestPairing.PAIR_BLOCK_EVENT) {
            this.etfu$setPairDirectionInternal((byte) value, false);
            cir.setReturnValue(true);
        }
    }
}
