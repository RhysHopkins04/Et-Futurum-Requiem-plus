package efrtest;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.api.imported.ImportedBiomeData;
import ganymedes01.etfuturum.world.imported.ImportedPopulationGuard;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import java.io.File;
import java.lang.reflect.Proxy;
import static efrtest.EarthImportContracts.check;

/** Exercise the actual native/Forge population wrappers across an imported boundary. */
public final class ImportedBoundaryProbe {
    public static void run(WorldServer world) throws Exception {
        boolean restart=new File("imported-boundary-fixture-created").isFile();
        Chunk chunk=world.getChunkFromChunkCoords(8,8);
        if(restart)check(ImportedBiomeData.isImportedChunk(chunk),"import marker lost on disk reload");
        else ImportedBiomeData.attach(chunk,EarthImportContracts.fixture());
        int x=143,y=128,z=128;
        world.setBlock(x,y,z,ModBlocks.DEEPSLATE.get(),4,2);
        world.setBlock(x,y+1,z,Blocks.chest,2,2);
        TileEntityChest chest=(TileEntityChest)world.getTileEntity(x,y+1,z);
        ChunkProviderServer provider=(ChunkProviderServer)world.getChunkProvider();
        IChunkProvider original=provider.currentChunkProvider;
        int[] calls={0,0};
        Runnable attemptedWrites=()->{
            check(!world.setBlock(x,y,z,Blocks.coal_ore,0,2),"population overwrote imported deepslate");
            check(!world.setBlockMetadataWithNotify(x,y,z,0,2),"population overwrote imported metadata");
            world.setTileEntity(x,y+1,z,new TileEntityChest());
            check(world.getTileEntity(x,y+1,z)==null,"generator can access imported tile contents");
            world.removeTileEntity(x,y+1,z);
            world.setBlock(x+1,y,z,Blocks.coal_ore,0,2);
            check(world.getBlock(x+1,y,z)==Blocks.coal_ore,"ordinary target terrain generation was blocked");
        };
        GameRegistry.registerWorldGenerator((random,cx,cz,w,g,p)->{
            if(w==world && cx==9 && cz==8){calls[1]++;attemptedWrites.run();}
        },Integer.MAX_VALUE);
        provider.currentChunkProvider=(IChunkProvider)Proxy.newProxyInstance(IChunkProvider.class.getClassLoader(),new Class<?>[]{IChunkProvider.class},(proxy,method,args)->{
            if(method.getName().equals("populate")){calls[0]++;attemptedWrites.run();return null;}
            return method.invoke(original,args);
        });
        try {
            world.getChunkFromChunkCoords(9,8).isTerrainPopulated=false;
            provider.populate(provider,9,8);
            check(calls[0]==1 && calls[1]==1,"native and Forge population hooks were not both exercised");
        } finally {provider.currentChunkProvider=original;}
        check(world.getTileEntity(x,y+1,z)==chest,"population replaced or removed imported tile");
        check(world.getBlock(x,y,z)==ModBlocks.DEEPSLATE.get() && world.getBlockMetadata(x,y,z)==4,"imported boundary changed");
        try {ImportedPopulationGuard.run(world,()->{throw new IllegalStateException("probe");});}
        catch(IllegalStateException expected){check(expected.getMessage().equals("probe"),"unexpected failure");}
        check(world.setBlock(x,y,z,Blocks.gold_block,0,2),"guard leaked into ordinary gameplay after exception");
        check(world.setBlockMetadataWithNotify(x,y,z,1,2),"guard leaked into ordinary gameplay metadata");
        world.saveAllChunks(true,null);new File("imported-boundary-fixture-created").createNewFile();
        System.out.println("IMPORTED_BOUNDARY_"+(restart?"RESTART":"SAVE")+" PASSED");
    }
}
