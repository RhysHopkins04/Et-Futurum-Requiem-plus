package ganymedes01.etfuturum.core.utils;

import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.chunk.Chunk;

/** Scope belongs to the original Crucible inventory-owner lookup, never to normal chunk reads. */
public final class CrucibleInventoryHeightContext {
    private static final ThreadLocal<TileEntity> OWNER=new ThreadLocal<TileEntity>();
    private static volatile boolean used;
    private CrucibleInventoryHeightContext() { }
    public static Object enter(Object object) {
        TileEntity previous=OWNER.get();
        TileEntity tile=object instanceof TileEntity?(TileEntity)object:null;
        if(tile!=null && tile instanceof IInventory && WorldHeightCompat.extended(tile.getWorldObj())
                && tile.yCoord>=256 && tile.yCoord<WorldHeightCompat.height(tile.getWorldObj())) {
            used=true;OWNER.set(tile);
        } else OWNER.remove();
        return previous;
    }
    public static void exit(Object previous) {
        if(previous==null)OWNER.remove();else OWNER.set((TileEntity)previous);
    }
    /** Only this exact lookup may bypass CraftWorld's legacy Y mask. */
    public static boolean isOwnerPosition(int x, int y, int z) {
        TileEntity tile = OWNER.get();
        return tile != null && tile.xCoord == x && tile.yCoord == y && tile.zCoord == z;
    }
    public static int blockY(Chunk chunk,int x,int y,int z) {
        if(!used)return y;
        TileEntity tile=OWNER.get();
        return tile!=null && tile.getWorldObj()==chunk.worldObj && (tile.xCoord>>4)==chunk.xPosition
                && (tile.zCoord>>4)==chunk.zPosition && x==(tile.xCoord&15) && z==(tile.zCoord&15)
                && y==(tile.yCoord&255)?tile.yCoord:y;
    }
}
