package efrtest;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import java.io.File;
import java.lang.reflect.Method;
import static efrtest.EarthImportContracts.check;

public final class CampfireWaterProbe {
    public static void run(WorldServer world) throws Exception {
        boolean restart=new File("campfire-water-fixture-created").isFile();
        Method query=BlockDynamicLiquid.class.getDeclaredMethod("func_149809_q",World.class,int.class,int.class,int.class);
        Method flow=BlockDynamicLiquid.class.getDeclaredMethod("func_149813_h",World.class,int.class,int.class,int.class,int.class);
        query.setAccessible(true);flow.setAccessible(true);
        int x=-192,z=128;
        for(ModernMapParityBlocks entry:new ModernMapParityBlocks[]{ModernMapParityBlocks.CAMPFIRE,ModernMapParityBlocks.SOUL_CAMPFIRE}) {
            Block block=entry.get();
            for(int face=0;face<4;face++,x++) {
                if(restart)check(world.getBlock(x,128,z)==block && world.getBlockMetadata(x,128,z)==(face|8),"campfire water/facing disk persistence");
                world.setBlock(x,127,z,Blocks.stone,0,2);
                world.setBlock(x,128,z,block,face|4,2);
                check((Boolean)query.invoke(Blocks.flowing_water,world,x,128,z),"dry campfire blocks water flow");
                check(world.getBlockMetadata(x,128,z)==(face|4),"flow query mutated dry campfire");
                flow.invoke(Blocks.flowing_water,world,x,128,z,1);
                check(world.getBlock(x,128,z)==block && world.getBlockMetadata(x,128,z)==(face|8),"flow destroyed campfire or failed to extinguish");
                check(ModernMapParityBlocks.containsWater(world,x,128,z),"campfire source detection");
                check(!(Boolean)query.invoke(Blocks.flowing_water,world,x,128,z),"incoming flow can displace wet campfire");
                check(block.getLightValue(world,x,128,z)==0,"wet campfire emits light");
                check(ModernMapParityBlocks.drainContainedWater(world,x,128,z),"campfire bucket drain");
                check(world.getBlockMetadata(x,128,z)==face,"draining relit or rotated campfire");
                check(ModernMapParityBlocks.fillContainedWater(world,x,128,z),"campfire bucket refill");
            }
        }
        x=-192;z=132;
        world.setBlock(x,127,z,Blocks.stone,0,2);
        Block fire=ModernMapParityBlocks.CAMPFIRE.get();
        world.setBlock(x,128,z,fire,8,2);world.setBlockToAir(x,128,z);
        check(world.getBlock(x,128,z)==Blocks.water,"breaking wet campfire discarded source");
        world.saveAllChunks(true,null);new File("campfire-water-fixture-created").createNewFile();
        System.out.println("CAMPFIRE_WATER_"+(restart?"RESTART":"SAVE")+" PASSED");
    }
}
