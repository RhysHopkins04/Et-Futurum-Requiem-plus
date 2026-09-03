package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Pass 31 shared modern wall state resolver.
 *
 * <p>1.7.10 has no spare wall metadata for the modern north/east/south/west
 * NONE/LOW/TALL values or the UP post bit, so the state is deliberately derived
 * from the live neighbourhood.  That matches modern wall update behaviour and
 * keeps subtype metadata available to the mature EFR wall families.</p>
 */
public final class ModernWallState {

    public enum Side {
        NONE("none"), LOW("low"), TALL("tall");

        private final String serializedName;

        Side(String serializedName) {
            this.serializedName = serializedName;
        }

        public String getSerializedName() {
            return serializedName;
        }

        public boolean isConnected() {
            return this != NONE;
        }
    }

    public static final class State {
        public final Side north;
        public final Side east;
        public final Side south;
        public final Side west;
        public final boolean up;

        private State(Side north, Side east, Side south, Side west, boolean up) {
            this.north = north;
            this.east = east;
            this.south = south;
            this.west = west;
            this.up = up;
        }

        /** 2 * 3^4 = 162 stable model combinations. */
        public int getModelIndex() {
            return modelIndex(north, east, south, west, up);
        }

        public float getVisualMaxY() {
            return up || north == Side.TALL || east == Side.TALL || south == Side.TALL || west == Side.TALL
                    ? 1.0F : 14.0F / 16.0F;
        }
    }

    private ModernWallState() {}

    public static State derive(IBlockAccess world, int x, int y, int z) {
        return derive(world.getBlock(x, y, z), world, x, y, z, 0);
    }

    public static State derive(Block self, IBlockAccess world, int x, int y, int z) {
        return derive(self, world, x, y, z, 0);
    }

    private static State derive(Block self, IBlockAccess world, int x, int y, int z, int depth) {
        boolean northConnected = canConnectWallTo(self, world, x, y, z - 1);
        boolean eastConnected = canConnectWallTo(self, world, x + 1, y, z);
        boolean southConnected = canConnectWallTo(self, world, x, y, z + 1);
        boolean westConnected = canConnectWallTo(self, world, x - 1, y, z);

        Block above = world.getBlock(x, y + 1, z);
        boolean solidDownFace = above != null && (world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN, false)
                || above.isOpaqueCube());
        boolean aboveWall = isWallLike(above);

        Side north = sideState(northConnected, aboveWall
                ? canConnectWallTo(above, world, x, y + 1, z - 1) : solidDownFace);
        Side east = sideState(eastConnected, aboveWall
                ? canConnectWallTo(above, world, x + 1, y + 1, z) : solidDownFace);
        Side south = sideState(southConnected, aboveWall
                ? canConnectWallTo(above, world, x, y + 1, z + 1) : solidDownFace);
        Side west = sideState(westConnected, aboveWall
                ? canConnectWallTo(above, world, x - 1, y + 1, z) : solidDownFace);

        boolean aboveWallPost = false;
        if (aboveWall && depth < 255) {
            aboveWallPost = derive(above, world, x, y + 1, z, depth + 1).up;
        }

        boolean noSides = !northConnected && !eastConnected && !southConnected && !westConnected;
        boolean oppositeMismatch = northConnected != southConnected || eastConnected != westConnected;
        boolean oppositeTallPair = north == Side.TALL && south == Side.TALL
                || east == Side.TALL && west == Side.TALL;

        // Match modern WallBlock.shouldHavePost/shouldRaisePost ordering.  A straight
        // opposite TALL pair is specifically the case where the center post is suppressed;
        // this must be checked before generic post-override/support coverage.
        boolean up;
        if (aboveWallPost) {
            up = true;
        } else if (noSides || oppositeMismatch) {
            up = true;
        } else if (oppositeTallPair) {
            up = false;
        } else {
            up = isWallPostOverride(above) || solidDownFace;
        }

        return new State(north, east, south, west, up);
    }

    private static Side sideState(boolean connected, boolean tall) {
        if (!connected) return Side.NONE;
        return tall ? Side.TALL : Side.LOW;
    }

    /**
     * Connection policy shared by vanilla walls, BaseWall families and parity walls.
     * It intentionally preserves the pre-Pass-31 gate/solid-block behaviour while
     * making EFR/parity wall connections reciprocal.
     */
    public static boolean canConnectWallTo(Block self, IBlockAccess world, int x, int y, int z) {
        Block other = world.getBlock(x, y, z);
        if (other == null) return false;
        if (other instanceof BlockWall || other instanceof BaseWall || other instanceof BlockFenceGate) return true;

        ModernMapParityBlocks parity = ModernMapParityBlocks.fromBlock(other);
        if (parity != null && (parity.getStyle() == ModernMapParityBlocks.Style.WALL
                || parity.getStyle() == ModernMapParityBlocks.Style.FENCE_GATE)) return true;

        return other.isOpaqueCube()
                || other.blockMaterial.isOpaque() && other.renderAsNormalBlock() && other.blockMaterial != Material.gourd;
    }

    public static boolean isWallLike(Block block) {
        if (block instanceof BlockWall || block instanceof BaseWall) return true;
        ModernMapParityBlocks parity = ModernMapParityBlocks.fromBlock(block);
        return parity != null && parity.getStyle() == ModernMapParityBlocks.Style.WALL;
    }

    /** Modern 1.21.11 #wall_post_override compatibility without adding a tag system to 1.7.10. */
    private static boolean isWallPostOverride(Block block) {
        if (block == null) return false;
        Object key = Block.blockRegistry.getNameForObject(block);
        if (key == null) return false;
        String name = String.valueOf(key).toLowerCase(java.util.Locale.ROOT);
        int colon = name.indexOf(':');
        if (colon >= 0) name = name.substring(colon + 1);
        return "torch".equals(name)
                || "soul_torch".equals(name)
                || "redstone_torch".equals(name)
                || "unlit_redstone_torch".equals(name)
                || "copper_torch".equals(name)
                || "tripwire".equals(name)
                || "cactus_flower".equals(name)
                || name.endsWith("_sign")
                || name.endsWith("_banner")
                || name.endsWith("_pressure_plate");
    }

    public static int modelIndex(Side north, Side east, Side south, Side west, boolean up) {
        return north.ordinal()
                + east.ordinal() * 3
                + south.ordinal() * 9
                + west.ordinal() * 27
                + (up ? 81 : 0);
    }
}
