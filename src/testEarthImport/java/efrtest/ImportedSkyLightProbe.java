package efrtest;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.api.imported.ImportedBiomeData;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.world.imported.ImportedSkyLightRepair;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.world.ChunkDataEvent;
import java.util.Arrays;
import static efrtest.EarthImportContracts.check;

final class ImportedSkyLightProbe {
    static void run(WorldServer world) {
        Chunk chunk=world.getChunkFromChunkCoords(8,8);
        ExtendedBlockStorage[] sections=chunk.getBlockStorageArray();
        check(sections.length==24,"probe needs extended world");
        Arrays.fill(sections,null);
        for(int sy:new int[]{8,16,18})sections[sy]=new ExtendedBlockStorage(sy*16,true);
        for(int sy:new int[]{8,16}) {
            ExtendedBlockStorage s=sections[sy];int dx=sy==16?8:0;
            for(int x=dx;x<dx+8;x++)for(int z=0;z<16;z++)s.func_150818_a(x,0,z,Blocks.stone);
            s.func_150818_a(dx+1,1,1,Blocks.double_plant);s.setExtBlockMetadata(dx+1,1,1,3);
            s.func_150818_a(dx+1,2,1,Blocks.double_plant);s.setExtBlockMetadata(dx+1,2,1,8);
            s.func_150818_a(dx+2,1,2,ModernMapParityBlocks.LEAF_LITTER.get());
            s.func_150818_a(dx+3,1,3,ModernMapParityBlocks.BUSH.get());
            s.func_150818_a(dx+4,3,4,Blocks.leaves);
            s.func_150818_a(dx+5,3,5,Blocks.stone); // Solid roof: no invented light below it.
            s.setExtBlocklightValue(dx+6,1,6,11);
            s.setExtSkylightValue(dx+5,1,5,8); // Existing lateral light must remain.
        }
        int loaded=((ChunkProviderServer)world.getChunkProvider()).loadedChunks.size();
        check(ImportedSkyLightRepair.repair(chunk)==0,"ordinary world chunk changed");
        ImportedBiomeData.attach(chunk,EarthImportContracts.fixture());
        NBTTagCompound original=new NBTTagCompound();
        original.setTag(ganymedes01.etfuturum.api.imported.ImportedBiomeVolume.NBT_KEY,EarthImportContracts.fixture().toNBT());
        ImportedSkyLightRepair.EVENTS.load(new ChunkDataEvent.Load(chunk,original));
        boolean enabled=ConfigMapCompatibility.importedEarthMode;
        ConfigMapCompatibility.importedEarthMode=false;
        check(ImportedSkyLightRepair.repair(chunk)==0,"disabled profile changed");
        ConfigMapCompatibility.importedEarthMode=enabled;
        check(ImportedSkyLightRepair.repair(chunk)>0,"zero skylight was not repaired");
        for(int sy:new int[]{8,16}) {
            ExtendedBlockStorage s=sections[sy];int dx=sy==16?8:0;
            check(s.getExtSkylightValue(dx+1,1,1)==15 && s.getExtSkylightValue(dx+1,2,1)==15,"fern halves remain black");
            check(s.getExtSkylightValue(dx+2,1,2)==15,"leaf litter remains black");
            check(s.getExtSkylightValue(dx+3,1,3)==15,"bush remains black");
            check(s.getExtSkylightValue(dx+4,3,4)==14 && s.getExtSkylightValue(dx+4,2,4)==13,"leaf attenuation mismatch");
            check(s.getExtSkylightValue(dx+5,2,5)==0 && s.getExtSkylightValue(dx+5,1,5)==8,"roof/lateral light changed");
            check(s.getExtBlocklightValue(dx+6,1,6)==11,"block light changed");
            check(chunk.heightMap[1*16+dx+1]==sy*16+1,"plant treated as roof");
        }
        check(sections[9]==null && sections[17]==null,"empty storage was allocated");
        check(((ChunkProviderServer)world.getChunkProvider()).loadedChunks.size()==loaded,"repair loaded adjacent chunks");
        check(ImportedSkyLightRepair.repair(chunk)==0,"repair repeated");
        NBTTagCompound saved=new NBTTagCompound();
        ImportedSkyLightRepair.EVENTS.save(new ChunkDataEvent.Save(chunk,saved));
        check(saved.getInteger(ImportedSkyLightRepair.KEY)==1,"one-shot marker not saved");
        saved.setTag(ganymedes01.etfuturum.api.imported.ImportedBiomeVolume.NBT_KEY,EarthImportContracts.fixture().toNBT());
        Chunk reopened=new Chunk(world,12,12);
        ImportedBiomeData.attach(reopened,EarthImportContracts.fixture());
        ImportedSkyLightRepair.EVENTS.load(new ChunkDataEvent.Load(reopened,saved));
        reopened.heightMap[0]=123;
        check(ImportedSkyLightRepair.repair(reopened)==0 && reopened.heightMap[0]==123,"saved marker did not suppress repeat repair");
        System.out.println("IMPORTED_SKYLIGHT PASSED: real fern/litter/bush opacity, upper sections, roof/leaf attenuation, lateral and block light preserved, sparse storage, no neighbour loading, config gates, one-shot persistence");
    }
}
