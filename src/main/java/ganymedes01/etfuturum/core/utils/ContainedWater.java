package ganymedes01.etfuturum.core.utils;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Source water in a deliberately small set of aquatic blocks, without material substitution. */
public final class ContainedWater {
    private ContainedWater() { }
    public static double surface(IBlockAccess world, int x, int y, int z) {
        return y + (ModernMapParityBlocks.containsWater(world,x,y+1,z)
                || world.getBlock(x,y+1,z).getMaterial() == Material.water ? 1.0D : 8.0D / 9.0D);
    }
    public static boolean at(IBlockAccess world, double x, double y, double z) {
        int bx = MathHelper.floor_double(x), by = MathHelper.floor_double(y), bz = MathHelper.floor_double(z);
        return ModernMapParityBlocks.containsWater(world,bx,by,bz) && y < surface(world,bx,by,bz);
    }
    public static boolean intersects(World world, AxisAlignedBB box) {
        int x0 = MathHelper.floor_double(box.minX), x1 = MathHelper.floor_double(box.maxX);
        int y0 = Math.max(0, MathHelper.floor_double(box.minY)), y1 = Math.min(world.getHeight() - 1, MathHelper.floor_double(box.maxY));
        if (y1 < y0) return false;
        int z0 = MathHelper.floor_double(box.minZ), z1 = MathHelper.floor_double(box.maxZ);
        if (!world.checkChunksExist(x0,y0,z0,x1,y1,z1)) return false;
        for (int x=x0; x<=x1; x++) for (int y=y0; y<=y1; y++) for (int z=z0; z<=z1; z++) {
            if (box.maxX > x && box.minX < x+1 && box.maxZ > z && box.minZ < z+1
                    && box.maxY > y && ModernMapParityBlocks.containsWater(world,x,y,z)
                    && box.minY < surface(world,x,y,z)) return true;
        }
        return false;
    }
}
