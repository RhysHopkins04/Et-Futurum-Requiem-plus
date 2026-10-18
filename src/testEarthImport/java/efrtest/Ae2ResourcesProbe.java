package efrtest;

import com.google.gson.*;
import ganymedes01.etfuturum.world.imported.Ae2ImportResources;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

/** Reads converter artifacts with actual AE2 classes; disposable fixture only. */
public final class Ae2ResourcesProbe {
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void run(WorldServer world) throws Exception {
        JsonObject contract=Ae2ImportResources.describe(world);
        check(contract.get("available").getAsBoolean(),"AE2 export failed: "+contract);
        String path=System.getProperty("efr.ae2Artifacts");if(path==null)return;
        File root=new File(path);
        try(InputStream in=new FileInputStream(new File(root,"chest.nbt.gz"))) {
            NBTTagCompound nbt=CompressedStreamTools.readCompressed(in);
            TileEntity tile=TileEntity.createAndLoadEntity(nbt);
            check(tile instanceof IInventory && tile.getClass().getName().equals("appeng.tile.storage.TileSkyChest"),"converter sky chest does not load");
            IInventory inv=(IInventory)tile;int count=0;
            for(int i=0;i<inv.getSizeInventory();i++)if(inv.getStackInSlot(i)!=null)count++;
            check(count>0,"converter sky chest is empty");
            NBTTagCompound again=new NBTTagCompound();tile.writeToNBT(again);
            check(nbt.equals(again),"converter sky chest NBT changed after native load/save");
            long seed=Long.parseLong(new String(java.nio.file.Files.readAllBytes(new File(root,"seed.txt").toPath()),"UTF-8").trim());
            NBTTagCompound expected=Ae2ImportResources.nativeChest(world,seed);
            check(expected.getCompoundTag("inv").equals(nbt.getCompoundTag("inv")),"converter loot differs from installed AE2 MeteoritePlacer");
            // Place exactly this converted tile and retain it for process restart.
            int x=nbt.getInteger("x"),y=nbt.getInteger("y"),z=nbt.getInteger("z");
            if(new File("ae2-artifacts-created").exists()) {
                TileEntity saved=world.getTileEntity(x,y,z);check(saved!=null,"converted chest missing on restart");
                NBTTagCompound savedNBT=new NBTTagCompound();saved.writeToNBT(savedNBT);
                check(savedNBT.getCompoundTag("inv").equals(nbt.getCompoundTag("inv")),"converted chest inventory changed after restart");
            }
            world.setBlock(x,y,z,(Block)Block.blockRegistry.getObject("appliedenergistics2:tile.BlockSkyChest"),0,2);
            tile.setWorldObj(world);world.setTileEntity(x,y,z,tile);
        }
        // Use an independent native SpawnData reader so fixture-world state cannot
        // mask malformed offline data. Neither reader generates terrain.
        Class<?> spawnClass=Class.forName("appeng.core.worlddata.SpawnData");
        Constructor<?> constructor=spawnClass.getDeclaredConstructor(File.class);constructor.setAccessible(true);
        Object spawn=constructor.newInstance(new File(root,"AE2/spawndata"));
        Method near=spawnClass.getDeclaredMethod("getNearByMeteorites",int.class,int.class,int.class);near.setAccessible(true);
        Method generated=spawnClass.getDeclaredMethod("hasGenerated",int.class,int.class,int.class);generated.setAccessible(true);
        int[] position=new Gson().fromJson(new FileReader(new File(root,"position.json")),int[].class);
        check((Boolean)generated.invoke(spawn,0,position[0]>>4,position[2]>>4),"offline generated-chunk flags not readable by AE2");
        Collection<?> records=(Collection<?>)near.invoke(spawn,0,position[0]>>4,position[2]>>4);
        check(!records.isEmpty(),"native AE2 cannot discover offline meteorite settings");
        Class<?> reader=Class.forName("appeng.services.compass.CompassReader");
        Object compass=reader.getConstructor(int.class,File.class).newInstance(0,new File(root,"AE2/compass"));
        check((Boolean)reader.getMethod("hasBeacon",int.class,int.class).invoke(compass,position[0]>>4,position[2]>>4),"native compass cannot find converted meteorite");
        reader.getMethod("close").invoke(compass);
        Block sky=(Block)Block.blockRegistry.getObject("appliedenergistics2:tile.BlockSkyStone");
        int[][] blocks=new Gson().fromJson(new FileReader(new File(root,"blocks.json")),int[][].class);
        for(int[] b:blocks)world.setBlock(b[0],b[1],b[2],sky,0,2);
        Object wd=Class.forName("appeng.core.worlddata.WorldData").getMethod("instance").invoke(null);
        Object cd=wd.getClass().getMethod("compassData").invoke(wd);
        Method serviceMethod=Class.forName("appeng.core.worlddata.IWorldCompassData").getMethod("service");
        Object service=serviceMethod.invoke(cd);
        java.util.concurrent.Future<?> update=(java.util.concurrent.Future<?>)service.getClass().getMethod("updateArea",net.minecraft.world.World.class,int.class,int.class,int.class).invoke(service,world,position[0],position[1],position[2]);
        update.get(10,java.util.concurrent.TimeUnit.SECONDS);
        Object live=reader.getConstructor(int.class,File.class).newInstance(0,new File(world.getSaveHandler().getWorldDirectory(),"AE2/compass"));
        check((Boolean)reader.getMethod("hasBeacon",int.class,int.class).invoke(live,position[0]>>4,position[2]>>4),"native compass rescan lost converted sky stone");
        reader.getMethod("close").invoke(live);
        Class<?> coord=Class.forName("appeng.api.util.DimensionalCoord");
        Object nearby=coord.getConstructor(net.minecraft.world.World.class,int.class,int.class,int.class).newInstance(world,position[0]+80,position[1],position[2]);
        Class<?> callback=Class.forName("appeng.services.compass.ICompassCallback");
        java.util.concurrent.atomic.AtomicBoolean directed=new java.util.concurrent.atomic.AtomicBoolean();
        Object receiver=Proxy.newProxyInstance(callback.getClassLoader(),new Class<?>[]{callback},(proxy,method,args)->{
            if(method.getName().equals("calculatedDirection")) directed.set((Boolean)args[0] && !(Boolean)args[1] && !Double.isNaN((Double)args[2]) && (Double)args[3]>0);
            return null;
        });
        java.util.concurrent.Future<?> direction=(java.util.concurrent.Future<?>)service.getClass().getMethod("getCompassDirection",coord,int.class,callback).invoke(service,nearby,32,receiver);
        direction.get(10,java.util.concurrent.TimeUnit.SECONDS);
        check(directed.get(),"native meteorite compass did not point toward converted sky stone");
        new File("ae2-artifacts-created").createNewFile();
        System.out.println("AE2_CONVERTER_ARTIFACTS PASSED: exact native loot/chest NBT, restart, spawn data and compass beacon");
    }
}
