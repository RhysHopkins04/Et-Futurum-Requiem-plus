package efrtest;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import static efrtest.EarthImportContracts.check;
final class ImportedWaterFlowProbe {
    static void run(WorldServer world) {
        for(int y:new int[]{140,330}) {
            int x=320,z=320;
            world.setBlock(x,y+1,z,Blocks.water,0,2);
            world.setBlock(x,y,z,Blocks.flowing_water,8,2);
            world.setBlockToAir(x,y-1,z);
            Blocks.flowing_water.updateTick(world,x,y,z,new Random(1));
            check(world.getBlock(x,y-1,z)==Blocks.flowing_water && world.getBlockMetadata(x,y-1,z)==8,"imported falling water did not continue downward at "+y);
        }
        System.out.println("IMPORTED_WATER_FLOW PASSED: native downward updates below and above Y255");
    }
}
