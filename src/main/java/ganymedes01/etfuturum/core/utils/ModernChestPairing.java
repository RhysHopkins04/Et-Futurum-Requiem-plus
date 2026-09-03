package ganymedes01.etfuturum.core.utils;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/** Small direction helpers shared by the chest block/tile mixins and Copper Chest renderer. */
public final class ModernChestPairing {
    public static final String NBT_PAIR_DIRECTION = "EFRPairDirection";
    public static final int PAIR_BLOCK_EVENT = 2;

    private ModernChestPairing() { }


    private static final ThreadLocal<PlacementClick> PLACEMENT_CLICK = new ThreadLocal<PlacementClick>();

    /**
     * Captures the original block/face clicked by ItemBlock before 1.7 offsets the placement
     * coordinates. Modern Java uses this context for secondary-use chest placement: sneaking on a
     * chest's horizontal face intentionally targets that single chest, while sneaking on the floor
     * or another block keeps the new chest independent.
     */
    public static void capturePlacementClick(World world, Block placedBlock, int clickedX, int clickedY, int clickedZ,
            int clickedSide) {
        if (world == null || placedBlock == null || !isManagedChestBlock(placedBlock)) {
            PLACEMENT_CLICK.remove();
            return;
        }
        PLACEMENT_CLICK.set(new PlacementClick(world, placedBlock, clickedX, clickedY, clickedZ, clickedSide));
    }

    /**
     * Consumes the captured secondary-placement target and returns the direction from the new chest
     * to the chest that was explicitly clicked. A non-horizontal click, different block identity,
     * non-adjacent target or stale context returns NONE.
     */
    public static byte consumeClickedPartnerDirection(World world, Block placedBlock, int placedX, int placedY,
            int placedZ) {
        PlacementClick click = PLACEMENT_CLICK.get();
        PLACEMENT_CLICK.remove();
        if (click == null || click.world != world || click.placedBlock != placedBlock) return IChestPairingState.NONE;
        if (click.clickedSide < 2 || click.clickedSide > 5 || click.clickedY != placedY) return IChestPairingState.NONE;
        if (world.getBlock(click.clickedX, click.clickedY, click.clickedZ) != placedBlock) {
            return IChestPairingState.NONE;
        }

        byte direction = fromOffset(click.clickedX - placedX, click.clickedZ - placedZ);
        if (!isPair(direction) || direction != directionToClickedBlock(click.clickedSide)) {
            return IChestPairingState.NONE;
        }
        return direction;
    }

    /** Clears an unused ItemBlock placement context after a failed/cancelled placement. */
    public static void clearPlacementClick(World world, Block placedBlock) {
        PlacementClick click = PLACEMENT_CLICK.get();
        if (click != null && click.world == world && click.placedBlock == placedBlock) {
            PLACEMENT_CLICK.remove();
        }
    }

    private static byte directionToClickedBlock(int clickedSide) {
        switch (clickedSide) {
            case 2: return IChestPairingState.SOUTH; // placed north of clicked block
            case 3: return IChestPairingState.NORTH; // placed south of clicked block
            case 4: return IChestPairingState.EAST;  // placed west of clicked block
            case 5: return IChestPairingState.WEST;  // placed east of clicked block
            default: return IChestPairingState.NONE;
        }
    }

    private static final class PlacementClick {
        private final World world;
        private final Block placedBlock;
        private final int clickedX;
        private final int clickedY;
        private final int clickedZ;
        private final int clickedSide;

        private PlacementClick(World world, Block placedBlock, int clickedX, int clickedY, int clickedZ,
                int clickedSide) {
            this.world = world;
            this.placedBlock = placedBlock;
            this.clickedX = clickedX;
            this.clickedY = clickedY;
            this.clickedZ = clickedZ;
            this.clickedSide = clickedSide;
        }
    }

    /** Keep the vanilla mixin bounded to normal/trapped chests plus EFR Copper Chests. */
    public static boolean isManagedChestBlock(Block block) {
        return block == Blocks.chest || block == Blocks.trapped_chest
                || ModernMapParityBlocks.isCopperChestBlock(block);
    }

    public static boolean isStoredDirection(byte direction) {
        return direction >= IChestPairingState.NONE && direction <= IChestPairingState.SOUTH;
    }

    public static boolean isPair(byte direction) {
        return direction >= IChestPairingState.WEST && direction <= IChestPairingState.SOUTH;
    }

    public static byte opposite(byte direction) {
        switch (direction) {
            case IChestPairingState.WEST: return IChestPairingState.EAST;
            case IChestPairingState.EAST: return IChestPairingState.WEST;
            case IChestPairingState.NORTH: return IChestPairingState.SOUTH;
            case IChestPairingState.SOUTH: return IChestPairingState.NORTH;
            default: return IChestPairingState.NONE;
        }
    }

    public static int offsetX(byte direction) {
        if (direction == IChestPairingState.WEST) return -1;
        if (direction == IChestPairingState.EAST) return 1;
        return 0;
    }

    public static int offsetZ(byte direction) {
        if (direction == IChestPairingState.NORTH) return -1;
        if (direction == IChestPairingState.SOUTH) return 1;
        return 0;
    }

    public static byte fromOffset(int dx, int dz) {
        if (dx == -1 && dz == 0) return IChestPairingState.WEST;
        if (dx == 1 && dz == 0) return IChestPairingState.EAST;
        if (dx == 0 && dz == -1) return IChestPairingState.NORTH;
        if (dx == 0 && dz == 1) return IChestPairingState.SOUTH;
        return IChestPairingState.NONE;
    }

    /** A partner must sit to the left/right of the chest, never in front/behind it. */
    public static boolean isLateralForFacing(byte direction, int chestMetadata) {
        if (chestMetadata == 2 || chestMetadata == 3) {
            return direction == IChestPairingState.WEST || direction == IChestPairingState.EAST;
        }
        if (chestMetadata == 4 || chestMetadata == 5) {
            return direction == IChestPairingState.NORTH || direction == IChestPairingState.SOUTH;
        }
        return false;
    }
}
