package efrtest;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent;
import java.util.*;
import static efrtest.EarthImportContracts.check;

public final class NtmDustProbe {
    private static final int X=224,Y=90,Z=224;
    public static void run(WorldServer world) throws Exception {
        Class<?> type=Class.forName("com.hbm.main.ModEventHandler");Object handler=type.getConstructor().newInstance();
        java.lang.reflect.Method method=type.getMethod("onBlockBreak",BlockEvent.BreakEvent.class);
        Block lignite=cpw.mods.fml.common.registry.GameRegistry.findBlock("hbm","tile.ore_lignite");
        for(Block[] pair:new Block[][]{{Blocks.coal_ore,ModBlocks.DEEPSLATE_COAL_ORE.get()},{lignite,ModBlocks.DEEPSLATE_HBM_LIGNITE_ORE.get()}}) {
            Map<String,String> expected=exercise(world,handler,method,pair[0]);Map<String,String> actual=exercise(world,handler,method,pair[1]);
            check(!expected.isEmpty() && expected.equals(actual),"coal dust parity failed "+pair[1].getUnlocalizedName());
        }
        Block gas=cpw.mods.fml.common.registry.GameRegistry.findBlock("hbm","tile.gas_coal");
        FakePlayer player=FakePlayerFactory.getMinecraft(world);
        Class<?> props=Class.forName("com.hbm.extprop.HbmLivingProps");
        java.lang.reflect.Method get=props.getMethod("getBlackLung",net.minecraft.entity.EntityLivingBase.class);
        int before=((Number)get.invoke(null,player)).intValue();
        gas.onEntityCollidedWithBlock(world,X+1,Y,Z,player);
        check(((Number)get.invoke(null,player)).intValue()==before+10,"native dust breathing hazard missing");
        clear(world);
        System.out.println("EARTH_IMPORT_NTM_DUST PASSED: real native break handler, coal/lignite gas parity and black-lung exposure");
    }
    private static Map<String,String> exercise(WorldServer world,Object handler,java.lang.reflect.Method method,Block ore) throws Exception {
        clear(world);world.setBlock(X,Y,Z,ore,0,2);
        BlockEvent.BreakEvent event=new BlockEvent.BreakEvent(X,Y,Z,world,ore,0,FakePlayerFactory.getMinecraft(world));
        int xp=event.getExpToDrop();world.rand.setSeed(8675309L);method.invoke(handler,event);
        check(event.block==ore && !event.isCanceled() && event.getExpToDrop()==xp,"dust adapter mutated break event");
        Map<String,String> result=new TreeMap<>();
        for(net.minecraftforge.common.util.ForgeDirection direction:net.minecraftforge.common.util.ForgeDirection.VALID_DIRECTIONS) {
            Block block=world.getBlock(X+direction.offsetX,Y+direction.offsetY,Z+direction.offsetZ);
            if(block!=Blocks.air)result.put(direction.name(),Block.blockRegistry.getNameForObject(block));
        }
        return result;
    }
    private static void clear(WorldServer world) {
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)world.setBlock(X+dx,Y+dy,Z+dz,Blocks.air,0,2);
    }
}
