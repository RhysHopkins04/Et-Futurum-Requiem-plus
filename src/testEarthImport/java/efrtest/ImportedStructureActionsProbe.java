package efrtest;

import ganymedes01.etfuturum.world.imported.ImportedStructureActions;
import java.io.*;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.*;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.*;

/** Live Forge event path, one-shot initialization, refused overwrites and disk restart. */
public final class ImportedStructureActionsProbe {
    private static NBTTagCompound instruction(int x,int y,int z,String kind,long seed){
        NBTTagCompound entry=new NBTTagCompound();entry.setInteger("x",x);entry.setInteger("y",y);entry.setInteger("z",z);
        entry.setString("Kind",kind);entry.setLong("Seed",seed);return entry;
    }
    private static NBTTagCompound protocol(NBTTagList entries){
        NBTTagCompound data=new NBTTagCompound();data.setInteger("Version",1);data.setTag("Entries",entries);return data;
    }
    public static void run(WorldServer world) throws Exception {
        int x=-352,y=272,z=-320;File marker=new File("imported-dungeon-actions-fixture.nbt");
        Chunk chunk=world.getChunkFromChunkCoords(x>>4,z>>4);
        if(marker.exists()){
            NBTTagCompound expected;try(InputStream in=new FileInputStream(marker)){expected=CompressedStreamTools.readCompressed(in);}
            NBTTagCompound actual=snapshot(world,x,y,z);
            if(!actual.equals(expected))throw new AssertionError("Imported live dungeon loot/spawner changed on disk restart: "+actual+" expected "+expected);
            MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk));
            if(!snapshot(world,x,y,z).equals(expected))throw new AssertionError("Chunk reload rerolled dungeon content");
            System.out.println("IMPORTED_STRUCTURE_ACTIONS_RESTART PASSED");return;
        }
        world.setBlock(x,y-1,z,Blocks.stone,0,2);world.setBlock(x+2,y-1,z,Blocks.stone,0,2);
        world.setBlock(x,y,z,Blocks.chest,0,2);world.setBlock(x+2,y,z,Blocks.mob_spawner,0,2);
        NBTTagList entries=new NBTTagList();entries.appendTag(instruction(x,y,z,"dungeon-chest",123456));
        entries.appendTag(instruction(x+2,y,z,"dungeon-spawner",654321));
        NBTTagCompound root=new NBTTagCompound();root.setTag(ImportedStructureActions.KEY,protocol(entries));
        MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(chunk,root));
        NBTTagCompound pending=new NBTTagCompound();MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save(chunk,pending));
        if(!pending.getCompoundTag(ImportedStructureActions.KEY).equals(protocol(entries)))throw new AssertionError("Pending action data was not saved intact");
        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk));
        TileEntityChest chest=(TileEntityChest)world.getTileEntity(x,y,z);boolean items=false;
        for(int slot=0;slot<chest.getSizeInventory();slot++)items|=chest.getStackInSlot(slot)!=null;
        if(!items)throw new AssertionError("Live fixture dungeon table produced no loot");
        NBTTagCompound before=snapshot(world,x,y,z);
        if(ImportedStructureActions.resolve(chunk)!=0||!snapshot(world,x,y,z).equals(before))throw new AssertionError("One-shot actions rerolled loot");
        NBTTagCompound saved=new NBTTagCompound();MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save(chunk,saved));
        if(saved.hasKey(ImportedStructureActions.KEY))throw new AssertionError("Fulfilled actions remained in chunk NBT");
        // A malformed request must be rejected before any target changes.
        NBTTagCompound invalid=protocol(entries);invalid.setInteger("Version",99);
        try{ImportedStructureActions.attach(chunk,invalid);throw new AssertionError("Bad protocol was accepted");}
        catch(IllegalArgumentException expected){}
        if(!snapshot(world,x,y,z).equals(before))throw new AssertionError("Rejected actions changed content");
        // Nonempty imported chests are never overwritten. Clean this fixture-only
        // failed request by fulfilling it while empty, then restore the snapshot.
        ImportedStructureActions.attach(chunk,protocol(entries));
        try{ImportedStructureActions.resolve(chunk);throw new AssertionError("Nonempty chest was overwritten");}
        catch(IllegalStateException expected){}
        if(!snapshot(world,x,y,z).equals(before))throw new AssertionError("Refused overwrite changed content");
        for(int slot=0;slot<chest.getSizeInventory();slot++)chest.setInventorySlotContents(slot,null);
        ImportedStructureActions.resolve(chunk);
        chest.readFromNBT(before.getCompoundTag("Chest"));world.getTileEntity(x+2,y,z).readFromNBT(before.getCompoundTag("Spawner"));chest.markDirty();
        world.saveAllChunks(true,null);
        try(OutputStream out=new FileOutputStream(marker)){CompressedStreamTools.writeCompressed(before,out);}
        System.out.println("IMPORTED_STRUCTURE_ACTIONS_SAVE PASSED: real Forge events, live tables, refused overwrite, one-shot state and Y272");
    }
    private static NBTTagCompound snapshot(WorldServer world,int x,int y,int z){
        NBTTagCompound result=new NBTTagCompound(),chest=new NBTTagCompound(),spawner=new NBTTagCompound();
        world.getTileEntity(x,y,z).writeToNBT(chest);world.getTileEntity(x+2,y,z).writeToNBT(spawner);
        result.setTag("Chest",chest);result.setTag("Spawner",spawner);return result;
    }
}
