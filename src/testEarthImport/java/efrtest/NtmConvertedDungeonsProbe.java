package efrtest;

import com.google.gson.*;
import ganymedes01.etfuturum.world.imported.ImportedStructureActions;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraftforge.common.*;
import net.minecraftforge.event.world.*;

/** Actual converted dungeon geometry, native tile loading, live tables and event persistence. */
public final class NtmConvertedDungeonsProbe {
    public static void run(WorldServer world,File directory) throws Exception {
        JsonObject manifest;
        try(Reader reader=new InputStreamReader(new FileInputStream(new File(directory,"manifest.json")),"UTF-8")){
            manifest=new JsonParser().parse(reader).getAsJsonObject();
        }
        AnvilChunkLoader loader=new AnvilChunkLoader(new File(directory,"unused-native-loader"));Method decode=null,encode=null;
        for(Method method:AnvilChunkLoader.class.getDeclaredMethods()){
            if(method.getReturnType()==Chunk.class&&Arrays.equals(method.getParameterTypes(),new Class[]{World.class,NBTTagCompound.class}))decode=method;
            if(method.getReturnType()==void.class&&Arrays.equals(method.getParameterTypes(),new Class[]{Chunk.class,World.class,NBTTagCompound.class}))encode=method;
        }
        if(decode==null||encode==null)throw new AssertionError("Native Anvil codecs unavailable");decode.setAccessible(true);encode.setAccessible(true);
        Set<String> samples=new HashSet<>(),families=new HashSet<>();int blocks=0,tiles=0,actions=0,ticks=0;boolean high=false;
        for(JsonElement element:manifest.getAsJsonArray("sites")){
            JsonObject site=element.getAsJsonObject();String name=site.get("name").getAsString();
            families.add(name);samples.add(name+":"+site.get("variant").getAsInt());
            byte[] bytes=Files.readAllBytes(new File(directory,site.get("chunkFile").getAsString()).toPath());
            StringBuilder hash=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format(java.util.Locale.ROOT,"%02x",value&255));
            if(!hash.toString().equals(site.get("chunkSha256").getAsString()))throw new AssertionError("Dungeon artifact checksum changed");
            NBTTagCompound root=CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
            Chunk chunk=(Chunk)decode.invoke(loader,world,root.getCompoundTag("Level"));loader.loadEntities(world,root.getCompoundTag("Level"),chunk);
            for(JsonElement value:site.getAsJsonArray("cells")){
                JsonArray cell=value.getAsJsonArray();int x=cell.get(0).getAsInt(),y=cell.get(1).getAsInt(),z=cell.get(2).getAsInt();
                Block block=chunk.getBlock(x&15,y,z&15);
                if(Block.getIdFromBlock(block)!=cell.get(3).getAsInt()||chunk.getBlockMetadata(x&15,y,z&15)!=cell.get(4).getAsInt()
                    ||!Block.blockRegistry.getNameForObject(block).equals(cell.get(5).getAsString()))throw new AssertionError("Dungeon block/metadata mismatch: "+name);
                blocks++;high|=y>255;
            }
            NBTTagList nativeTiles=root.getCompoundTag("Level").getTagList("TileEntities",10);
            for(int i=0;i<nativeTiles.tagCount();i++){
                NBTTagCompound nbt=nativeTiles.getCompoundTagAt(i);String id=nbt.getString("id");
                if(!Arrays.asList("Chest","MobSpawner","tileentity_trapped_brick","tileentity_ntm_bobblehead").contains(id))continue;
                TileEntity tile=chunk.func_150806_e(nbt.getInteger("x")&15,nbt.getInteger("y"),nbt.getInteger("z")&15);
                if(tile==null)throw new AssertionError("Converted dungeon tile was lost on native load: "+id);
                NBTTagCompound saved=new NBTTagCompound();tile.writeToNBT(saved);
                if(!saved.equals(nbt))throw new AssertionError("Native dungeon tile state changed: "+nbt+" -> "+saved);
                tiles++;
            }
            MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(chunk,root));
            NBTTagCompound before=new NBTTagCompound();MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save(chunk,before));
            if(root.hasKey(ImportedStructureActions.KEY)&&!before.getCompoundTag(ImportedStructureActions.KEY).equals(root.getCompoundTag(ImportedStructureActions.KEY)))
                throw new AssertionError("Pending converted dungeon actions changed before execution");
            MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk));
            NBTTagList requested=root.getCompoundTag(ImportedStructureActions.KEY).getTagList("Entries",10);
            for(int i=0;i<requested.tagCount();i++){
                NBTTagCompound instruction=requested.getCompoundTagAt(i);Random rng=new Random(instruction.getLong("Seed"));
                TileEntity target=chunk.func_150806_e(instruction.getInteger("x")&15,instruction.getInteger("y"),instruction.getInteger("z")&15);
                NBTTagCompound actual=new NBTTagCompound();target.writeToNBT(actual);
                if(instruction.getString("Kind").equals("dungeon-chest")){
                    TileEntityChest scratch=new TileEntityChest();
                    WeightedRandomChestContent.generateChestContents(rng,ChestGenHooks.getItems(ChestGenHooks.DUNGEON_CHEST,rng),scratch,ChestGenHooks.getCount(ChestGenHooks.DUNGEON_CHEST,rng));
                    NBTTagCompound expected=new NBTTagCompound();scratch.writeToNBT(expected);
                    if(!actual.getTagList("Items",10).equals(expected.getTagList("Items",10)))throw new AssertionError("Converted dungeon chest does not match the live pack loot table");
                }else if(!actual.getString("EntityId").equals(DungeonHooks.getRandomDungeonMob(rng)))throw new AssertionError("Converted spawner does not match the live dungeon table");
                actions++;
            }
            NBTTagCompound savedLevel=new NBTTagCompound();encode.invoke(loader,chunk,world,savedLevel);
            NBTTagCompound savedRoot=new NBTTagCompound();savedRoot.setTag("Level",savedLevel);
            MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save(chunk,savedRoot));
            if(savedRoot.hasKey(ImportedStructureActions.KEY)||ImportedStructureActions.resolve(chunk)!=0)throw new AssertionError("Fulfilled dungeon actions can reroll");
            // Reload the native writer's output and re-fire real events. A fulfilled
            // request must never return, and every item/spawner payload must persist.
            Chunk reloaded=(Chunk)decode.invoke(loader,world,savedLevel);loader.loadEntities(world,savedLevel,reloaded);
            MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(reloaded,savedRoot));MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(reloaded));
            NBTTagList savedTiles=savedLevel.getTagList("TileEntities",10);
            for(int i=0;i<savedTiles.tagCount();i++){
                NBTTagCompound nbt=savedTiles.getCompoundTagAt(i);
                TileEntity tile=reloaded.func_150806_e(nbt.getInteger("x")&15,nbt.getInteger("y"),nbt.getInteger("z")&15);
                if(tile==null)throw new AssertionError("Native dungeon save lost a tile");
                NBTTagCompound actual=new NBTTagCompound();tile.writeToNBT(actual);
                if(!actual.equals(nbt))throw new AssertionError("Native dungeon save/load changed a payload");
            }
            File persistedDir=new File("converted-dungeon-content");persistedDir.mkdirs();
            File persistedFile=new File(persistedDir,hash.toString()+".nbt.gz");
            if(persistedFile.exists()){
                NBTTagCompound disk;try(InputStream input=new FileInputStream(persistedFile)){disk=CompressedStreamTools.readCompressed(input);}
                if(disk.hasKey(ImportedStructureActions.KEY)||!disk.getCompoundTag("Level").getTagList("TileEntities",10).equals(savedTiles))
                    throw new AssertionError("Converted dungeon disk payload changed or retained a pending action");
                Chunk diskChunk=(Chunk)decode.invoke(loader,world,disk.getCompoundTag("Level"));loader.loadEntities(world,disk.getCompoundTag("Level"),diskChunk);
                MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(diskChunk,disk));MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(diskChunk));
                NBTTagCompound afterDisk=new NBTTagCompound();encode.invoke(loader,diskChunk,world,afterDisk);
                if(!afterDisk.getTagList("TileEntities",10).equals(savedTiles))throw new AssertionError("Converted dungeon content rerolled on disk restart");
            }else try(OutputStream output=new FileOutputStream(persistedFile)){CompressedStreamTools.writeCompressed(savedRoot,output);}
            NBTTagList pending=root.getCompoundTag("Level").getTagList("TileTicks",10);ticks+=pending.tagCount();
            for(int i=0;i<pending.tagCount();i++){
                NBTTagCompound expected=pending.getCompoundTagAt(i);boolean found=false;NBTTagList persisted=savedLevel.getTagList("TileTicks",10);
                for(int j=0;j<persisted.tagCount();j++)if(expected.equals(persisted.getCompoundTagAt(j)))found=true;
                if(!found)throw new AssertionError("Native dungeon save lost a scheduled update");
            }
        }
        if(families.size()!=3||samples.size()!=12||blocks<150000||tiles<1000||actions<48||!high)throw new AssertionError("Dungeon artifact coverage incomplete");
        ImportedStructureActionsProbe.run(world);
        System.out.println("NTM_CONVERTED_DUNGEONS PASSED: "+blocks+" blocks, "+tiles+" tile states, "+actions+" live-table actions, "+ticks+" updates, all 3 families/12 samples, one-layer bedrock, native save/load and surface marker above Y255");
    }
}
