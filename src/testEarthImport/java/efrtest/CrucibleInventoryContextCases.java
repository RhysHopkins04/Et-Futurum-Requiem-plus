package efrtest;

import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.core.utils.CrucibleInventoryHeightContext;
import java.lang.reflect.Field;
import net.minecraft.tileentity.*;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import sun.misc.Unsafe;

/** No chunks, saves, worlds or inventories are mutated by this detached coordinate fixture. */
public final class CrucibleInventoryContextCases {
    public static void main(String[] args)throws Exception {
        java.io.File runtime=new java.io.File(System.getProperty("java.io.tmpdir"),"efr-inventory-context-fixture");
        runtime.mkdirs();net.minecraft.launchwrapper.Launch.minecraftHome=runtime;
        Field home=cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");home.setAccessible(true);home.set(null,runtime);
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);Unsafe unsafe=(Unsafe)f.get(null);
        Field provider=World.class.getDeclaredField("provider");provider.setAccessible(true);
        World world=(World)unsafe.allocateInstance(WorldServer.class);provider.set(world,new WorldProviderSurface());
        World other=(World)unsafe.allocateInstance(WorldServer.class);provider.set(other,new WorldProviderSurface());
        ConfigWorld.extendedWorldHeight=true;ConfigWorld.extendedWorldHeightBlocks=512;
        TileEntityHopper tile=new TileEntityHopper();tile.setWorldObj(world);tile.xCoord=9795;tile.yCoord=384;tile.zCoord=-2181;
        Chunk chunk=(Chunk)unsafe.allocateInstance(Chunk.class);set(chunk,"worldObj",world);set(chunk,"xPosition",612);set(chunk,"zPosition",-137);
        check(CrucibleInventoryHeightContext.blockY(chunk,3,128,11)==128,"ordinary reads unchanged");
        Object previous=CrucibleInventoryHeightContext.enter(tile);
        try {
            check(CrucibleInventoryHeightContext.blockY(chunk,3,128,11)==384,"exact owner Y restored");
            check(CrucibleInventoryHeightContext.blockY(chunk,3,384,11)==384,"corrected call cannot recurse");
            check(CrucibleInventoryHeightContext.blockY(chunk,4,128,11)==128,"adjacent block unchanged");
            check(CrucibleInventoryHeightContext.blockY(chunk,3,127,11)==127,"other height unchanged");
            Object nested=CrucibleInventoryHeightContext.enter(null);
            try {check(CrucibleInventoryHeightContext.blockY(chunk,3,128,11)==128,"unrelated nested owner isolated");}
            finally{CrucibleInventoryHeightContext.exit(nested);}
            check(CrucibleInventoryHeightContext.blockY(chunk,3,128,11)==384,"nested context restored");
            set(chunk,"worldObj",other);check(CrucibleInventoryHeightContext.blockY(chunk,3,128,11)==128,"other world isolated");set(chunk,"worldObj",world);
            set(chunk,"xPosition",613);check(CrucibleInventoryHeightContext.blockY(chunk,3,128,11)==128,"other chunk isolated");set(chunk,"xPosition",612);
        }finally{CrucibleInventoryHeightContext.exit(previous);}
        check(CrucibleInventoryHeightContext.blockY(chunk,3,128,11)==128,"scope cleaned up");
        ConfigWorld.extendedWorldHeight=false;previous=CrucibleInventoryHeightContext.enter(tile);
        try{check(CrucibleInventoryHeightContext.blockY(chunk,3,128,11)==128,"legacy worlds unchanged");}finally{CrucibleInventoryHeightContext.exit(previous);}
        System.out.println("PASS native inventory context: Y384, isolation, nesting, cleanup, legacy worlds and no chunk loads.");
    }
    static void set(Object object,String name,Object value)throws Exception{Field f=Chunk.class.getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
}
