package ganymedes01.etfuturum.mixins.early.chestpairing;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.core.utils.IChestPairingState;
import ganymedes01.etfuturum.core.utils.ModernChestPairing;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;

import static net.minecraftforge.common.util.ForgeDirection.DOWN;

/** Modern Java chest placement/pairing semantics for vanilla, trapped, and Copper Chests. */
@Mixin(BlockChest.class)
public abstract class MixinBlockChest {

    @Inject(method = "canPlaceBlockAt", at = @At("HEAD"), cancellable = true)
    private void etfu$allowAdjacentSingles(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (ConfigBlocksItems.enableModernMapParityBlocks
                && ModernChestPairing.isManagedChestBlock((Block) (Object) this)) cir.setReturnValue(true);
    }

    /**
     * Keep 1.7's environment-facing helper for legacy/unknown chests, but never let raw adjacency
     * rotate a chest whose explicit Pass-32c relationship is already known.
     */
    @Inject(method = "func_149954_e", at = @At("HEAD"), cancellable = true)
    private void etfu$protectExplicitFacing(World world, int x, int y, int z, CallbackInfo ci) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks
                || !ModernChestPairing.isManagedChestBlock((Block) (Object) this)) return;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IChestPairingState
                && ((IChestPairingState) tile).etfu$getPairDirection() != IChestPairingState.UNKNOWN) {
            ci.cancel();
        }
    }

    @Inject(method = "onBlockPlacedBy", at = @At("HEAD"), cancellable = true)
    private void etfu$modernPlacement(World world, int x, int y, int z, EntityLivingBase placer,
            ItemStack item, CallbackInfo ci) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks
                || !ModernChestPairing.isManagedChestBlock((Block) (Object) this)) return;

        BlockChest self = (BlockChest) (Object) this;
        int facing = etfu$facingFromPlacer(placer);

        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityChest) || !(tile instanceof IChestPairingState)) {
            ci.cancel();
            return;
        }

        TileEntityChest chest = (TileEntityChest) tile;
        IChestPairingState pairing = (IChestPairingState) tile;
        pairing.etfu$setPairDirection(IChestPairingState.NONE);

        if (item != null && item.hasDisplayName()) chest.func_145976_a(item.getDisplayName());

        /*
         * Modern Java secondary-use placement has one intentional exception to "sneak = single":
         * if the player clicked the horizontal face of a compatible SINGLE chest, that exact chest
         * is targeted and the new chest inherits its facing. Sneaking on the floor/top/non-chest
         * still places an independent single.
         */
        byte clickedPartner = ModernChestPairing.consumeClickedPartnerDirection(world, self, x, y, z);
        boolean paired = false;
        if (placer.isSneaking() && ModernChestPairing.isPair(clickedPartner)) {
            int nx = x + ModernChestPairing.offsetX(clickedPartner);
            int nz = z + ModernChestPairing.offsetZ(clickedPartner);
            TileEntity otherTile = world.getTileEntity(nx, y, nz);
            if (ModernChestPairing.areCompatibleChestBlocks(world.getBlock(nx, y, nz), self)
                    && otherTile instanceof TileEntityChest && otherTile instanceof IChestPairingState) {
                IChestPairingState other = (IChestPairingState) otherTile;
                other.etfu$resolvePairing();
                int otherFacing = world.getBlockMetadata(nx, y, nz);
                if (other.etfu$getPairDirection() == IChestPairingState.NONE
                        && ModernChestPairing.isLateralForFacing(clickedPartner, otherFacing)) {
                    facing = otherFacing;
                    world.setBlockMetadataWithNotify(x, y, z, facing, 3);
                    pairing.etfu$setPairDirection(clickedPartner);
                    other.etfu$setPairDirection(ModernChestPairing.opposite(clickedPartner));
                    paired = true;
                }
            }
        }

        if (!paired) {
            world.setBlockMetadataWithNotify(x, y, z, facing, 3);
            if (!placer.isSneaking()) {
                byte candidate = etfu$preferredCompatibleNeighbour(world, self, x, y, z, facing);
                if (ModernChestPairing.isPair(candidate)) {
                    int nx = x + ModernChestPairing.offsetX(candidate);
                    int nz = z + ModernChestPairing.offsetZ(candidate);
                    TileEntity otherTile = world.getTileEntity(nx, y, nz);
                    if (ModernChestPairing.areCompatibleChestBlocks(world.getBlock(nx, y, nz), self) && otherTile instanceof IChestPairingState) {
                        IChestPairingState other = (IChestPairingState) otherTile;
                        other.etfu$resolvePairing();
                        if (other.etfu$getPairDirection() == IChestPairingState.NONE) {
                            pairing.etfu$setPairDirection(candidate);
                            other.etfu$setPairDirection(ModernChestPairing.opposite(candidate));
                        }
                    }
                }
            }
        }

        if (ModernMapParityBlocks.isCopperChestBlock(self)) {
            ModernMapParityBlocks.normalizeCopperChestPairIdentity(world, x, y, z);
            TileEntity refreshed = world.getTileEntity(x, y, z);
            if (refreshed instanceof TileEntityChest) ((TileEntityChest) refreshed).checkForAdjacentChests();
        } else {
            chest.checkForAdjacentChests();
        }
        ci.cancel();
    }

    /**
     * Vanilla immediately invalidates chest adjacency on a neighbour change. Resolve our explicit
     * state in the same callback so Copper Chest halves collapse back to SINGLE on the first client
     * block update rather than waiting for the later TE description packet.
     */
    @Inject(method = "onNeighborBlockChange", at = @At("TAIL"))
    private void etfu$resolvePairAfterNeighbourChange(World world, int x, int y, int z, Block neighbour,
            CallbackInfo ci) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks
                || !ModernChestPairing.isManagedChestBlock((Block) (Object) this)) return;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityChest && tile instanceof IChestPairingState) {
            ((TileEntityChest) tile).updateContainingBlockInfo();
            ((IChestPairingState) tile).etfu$resolvePairing();
            if (world.isRemote) {
                world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
            }
        }
    }

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void etfu$unlinkPartnerBeforeBreak(World world, int x, int y, int z, Block oldBlock, int meta,
            CallbackInfo ci) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks || world.isRemote
                || !ModernChestPairing.isManagedChestBlock((Block) (Object) this)) return;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IChestPairingState)) return;
        IChestPairingState pairing = (IChestPairingState) tile;
        pairing.etfu$resolvePairing();
        byte direction = pairing.etfu$getPairDirection();
        if (!ModernChestPairing.isPair(direction)) return;
        int nx = x + ModernChestPairing.offsetX(direction);
        int nz = z + ModernChestPairing.offsetZ(direction);
        TileEntity otherTile = world.getTileEntity(nx, y, nz);
        if (otherTile instanceof IChestPairingState) {
            IChestPairingState other = (IChestPairingState) otherTile;
            if (other.etfu$getPairDirection() == ModernChestPairing.opposite(direction)) {
                other.etfu$setPairDirection(IChestPairingState.NONE);
            }
        }
    }

    @Inject(method = "setBlockBoundsBasedOnState", at = @At("HEAD"), cancellable = true)
    private void etfu$explicitDoubleBounds(IBlockAccess world, int x, int y, int z, CallbackInfo ci) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks
                || !ModernChestPairing.isManagedChestBlock((Block) (Object) this)) return;
        Block block = (Block) (Object) this;
        TileEntity tile = world.getTileEntity(x, y, z);
        byte direction = IChestPairingState.NONE;
        if (tile instanceof TileEntityChest && tile instanceof IChestPairingState) {
            ((TileEntityChest) tile).checkForAdjacentChests();
            direction = ((IChestPairingState) tile).etfu$getPairDirection();
        }
        if (direction == IChestPairingState.NORTH) {
            block.setBlockBounds(0.0625F, 0.0F, 0.0F, 0.9375F, 0.875F, 0.9375F);
        } else if (direction == IChestPairingState.SOUTH) {
            block.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 1.0F);
        } else if (direction == IChestPairingState.WEST) {
            block.setBlockBounds(0.0F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
        } else if (direction == IChestPairingState.EAST) {
            block.setBlockBounds(0.0625F, 0.0F, 0.0625F, 1.0F, 0.875F, 0.9375F);
        } else {
            block.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
        }
        ci.cancel();
    }

    @Inject(method = "func_149951_m", at = @At("HEAD"), cancellable = true)
    private void etfu$explicitInventory(World world, int x, int y, int z, CallbackInfoReturnable<IInventory> cir) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks
                || !ModernChestPairing.isManagedChestBlock((Block) (Object) this)) return;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityChest) || !(tile instanceof IChestPairingState)) return;
        TileEntityChest chest = (TileEntityChest) tile;
        IChestPairingState pairing = (IChestPairingState) tile;
        pairing.etfu$resolvePairing();

        if (etfu$isBlocked(world, x, y, z)) {
            cir.setReturnValue(null);
            return;
        }

        byte direction = pairing.etfu$getPairDirection();
        if (!ModernChestPairing.isPair(direction)) {
            cir.setReturnValue(chest);
            return;
        }

        int nx = x + ModernChestPairing.offsetX(direction);
        int nz = z + ModernChestPairing.offsetZ(direction);
        TileEntity otherTile = world.getTileEntity(nx, y, nz);
        if (!(otherTile instanceof TileEntityChest) || !(otherTile instanceof IChestPairingState)
                || !ModernChestPairing.areCompatibleChestBlocks(world.getBlock(nx, y, nz), (Block) (Object) this)) {
            pairing.etfu$setPairDirection(IChestPairingState.NONE);
            cir.setReturnValue(chest);
            return;
        }
        if (etfu$isBlocked(world, nx, y, nz)) {
            cir.setReturnValue(null);
            return;
        }

        TileEntityChest other = (TileEntityChest) otherTile;
        if (direction == IChestPairingState.WEST || direction == IChestPairingState.NORTH) {
            cir.setReturnValue(new InventoryLargeChest("container.chestDouble", other, chest));
        } else {
            cir.setReturnValue(new InventoryLargeChest("container.chestDouble", chest, other));
        }
    }

    private static int etfu$facingFromPlacer(EntityLivingBase placer) {
        int quadrant = MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        if (quadrant == 0) return 2;
        if (quadrant == 1) return 5;
        if (quadrant == 2) return 3;
        return 4;
    }

    /**
     * Modern getChestType checks the clockwise lateral neighbour first, then counter-clockwise.
     * This matters when a new chest is placed between two independent singles: it joins exactly
     * one of them instead of falling back to an artificial 1.7 "ambiguous adjacency" restriction.
     */
    private static byte etfu$preferredCompatibleNeighbour(World world, BlockChest self,
            int x, int y, int z, int facing) {
        byte clockwise;
        byte counterClockwise;
        if (facing == 2) { // north
            clockwise = IChestPairingState.EAST;
            counterClockwise = IChestPairingState.WEST;
        } else if (facing == 3) { // south
            clockwise = IChestPairingState.WEST;
            counterClockwise = IChestPairingState.EAST;
        } else if (facing == 4) { // west
            clockwise = IChestPairingState.NORTH;
            counterClockwise = IChestPairingState.SOUTH;
        } else if (facing == 5) { // east
            clockwise = IChestPairingState.SOUTH;
            counterClockwise = IChestPairingState.NORTH;
        } else {
            return IChestPairingState.NONE;
        }

        if (etfu$isAvailableCandidate(world, self, x, y, z, facing, clockwise)) return clockwise;
        if (etfu$isAvailableCandidate(world, self, x, y, z, facing, counterClockwise)) return counterClockwise;
        return IChestPairingState.NONE;
    }

    private static boolean etfu$isAvailableCandidate(World world, BlockChest self,
            int x, int y, int z, int facing, byte direction) {
        int nx = x + ModernChestPairing.offsetX(direction);
        int nz = z + ModernChestPairing.offsetZ(direction);
        if (!ModernChestPairing.areCompatibleChestBlocks(world.getBlock(nx, y, nz), self) || world.getBlockMetadata(nx, y, nz) != facing) return false;
        TileEntity tile = world.getTileEntity(nx, y, nz);
        if (!(tile instanceof TileEntityChest) || !(tile instanceof IChestPairingState)) return false;
        IChestPairingState pairing = (IChestPairingState) tile;
        pairing.etfu$resolvePairing();
        return pairing.etfu$getPairDirection() == IChestPairingState.NONE;
    }

    private static boolean etfu$isBlocked(World world, int x, int y, int z) {
        if (world.isSideSolid(x, y + 1, z, DOWN)) return true;
        Iterator iterator = world.getEntitiesWithinAABB(EntityOcelot.class,
                AxisAlignedBB.getBoundingBox((double) x, (double) (y + 1), (double) z,
                        (double) (x + 1), (double) (y + 2), (double) (z + 1))).iterator();
        while (iterator.hasNext()) {
            Entity entity = (Entity) iterator.next();
            if (((EntityOcelot) entity).isSitting()) return true;
        }
        return false;
    }
}
