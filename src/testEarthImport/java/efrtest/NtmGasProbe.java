package efrtest;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateNtmOre;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import java.util.*;
import static efrtest.EarthImportContracts.check;

public final class NtmGasProbe {
    private static final int X=208,Y=90,Z=208;
    public static void run(WorldServer world) {
        for(ModBlocks entry:new ModBlocks[]{ModBlocks.DEEPSLATE_HBM_URANIUM_ORE,ModBlocks.DEEPSLATE_HBM_ASBESTOS_ORE}) {
            BlockDeepslateNtmOre ore=(BlockDeepslateNtmOre)entry.get();Block nativeOre=ore.getBase();
            check(ore.getTickRandomly() && ore.tickRate(world)==nativeOre.tickRate(world),"native random ticking missing");
            for(String action:new String[]{"tick","walk","neighbour","break"}) {
                Map<String,String> expected=exercise(world,nativeOre,action);
                Map<String,String> actual=exercise(world,ore,action);
                check(expected.equals(actual),"native gas mismatch "+entry+" "+action+" expected="+expected+" actual="+actual);
                if(action.equals("tick") || action.equals("break"))check(!actual.isEmpty(),"gas not emitted "+entry+" "+action);
            }
        }
        clear(world);
        System.out.println("EARTH_IMPORT_NTM_GAS PASSED: native uranium/radon and asbestos random tick, walking, neighbour and break behaviour");
    }
    private static Map<String,String> exercise(WorldServer world,Block ore,String action) {
        clear(world);world.setBlock(X,Y,Z,ore,0,2);
        long seed=0;while(new Random(seed).nextInt(10)!=0)seed++;
        world.rand.setSeed(seed);
        if(action.equals("tick"))ore.updateTick(world,X,Y,Z,world.rand);
        if(action.equals("walk"))ore.onEntityWalking(world,X,Y,Z,FakePlayerFactory.getMinecraft(world));
        if(action.equals("neighbour"))ore.onNeighborBlockChange(world,X,Y,Z,Blocks.air);
        if(action.equals("break"))ore.dropBlockAsItemWithChance(world,X,Y,Z,0,1F,0);
        Map<String,String> result=new TreeMap<>();
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++) {
            Block block=world.getBlock(X+dx,Y+dy,Z+dz);
            if(block!=Blocks.air && block!=ore)result.put(dx+","+dy+","+dz,Block.blockRegistry.getNameForObject(block)+":"+world.getBlockMetadata(X+dx,Y+dy,Z+dz));
        }
        return result;
    }
    private static void clear(WorldServer world) {
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)world.setBlock(X+dx,Y+dy,Z+dz,Blocks.air,0,2);
    }
}
