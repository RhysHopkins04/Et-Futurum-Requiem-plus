package ganymedes01.etfuturum.core.utils;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public interface WaterloggedPistonMove {
    void etfu$recordMovedWater();

    void etfu$retainWaterOnCompletion();

    static void record(World world, int x, int y, int z, TileEntity moving) {
        if (!world.isRemote && NativeWaterlogging.contains(world, x, y, z))
            ((WaterloggedPistonMove) moving).etfu$recordMovedWater();
    }

    static void retainOnShortPulse(TileEntity moving) {
        if (moving instanceof WaterloggedPistonMove)
            ((WaterloggedPistonMove) moving).etfu$retainWaterOnCompletion();
    }
}
