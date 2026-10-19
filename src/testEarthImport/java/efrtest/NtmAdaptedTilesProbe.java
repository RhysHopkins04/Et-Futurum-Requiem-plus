package efrtest;

import java.io.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import static efrtest.EarthImportContracts.check;

/** Reloads the backporter's adapted tile payloads through the actual NTM classes. */
final class NtmAdaptedTilesProbe {
    static void run() throws Exception {
        NBTTagCompound root;
        try(InputStream in=new FileInputStream(System.getProperty("efr.adaptedNtmTiles"))) {
            root=CompressedStreamTools.readCompressed(in);
        }
        NBTTagList records=root.getTagList("tiles",10);
        check(records.tagCount()>1000,"incomplete adapted native tile fixture");
        NtmRecordingWorld world=new NtmRecordingWorld(1,net.minecraft.world.biome.BiomeGenBase.plains,false,128);
        for(int i=0;i<records.tagCount();i++) {
            NBTTagCompound record=records.getCompoundTagAt(i);
            NBTTagCompound input=record.getCompoundTag("tile");
            TileEntity tile=TileEntity.createAndLoadEntity(input);
            check(tile!=null,"adapted native tile cannot load: "+input);
            world.setBlock(input.getInteger("x"),input.getInteger("y"),input.getInteger("z"),(net.minecraft.block.Block)net.minecraft.block.Block.blockRegistry.getObject(record.getString("block")),record.getInteger("meta"),2);
            world.setTileEntity(input.getInteger("x"),input.getInteger("y"),input.getInteger("z"),tile);
            NBTTagCompound output=new NBTTagCompound();tile.writeToNBT(output);
            check(input.equals(output),"adapted native tile changed on reload: "+input+" -> "+output);
        }
        System.out.println("NTM_ADAPTED_TILES PASSED: "+records.tagCount()+" payloads round-trip through actual NTM classes");
    }
}
