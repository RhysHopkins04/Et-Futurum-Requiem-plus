package efrtest;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.gen.structure.*;

/** Reads actual converter chunks through the patched native Anvil decoder. */
public final class NtmConvertedRuinsProbe {
    public static void run(WorldServer world,File directory) throws Exception {
        JsonObject manifest;
        try(Reader reader=new InputStreamReader(new FileInputStream(new File(directory,"manifest.json")),"UTF-8")){
            manifest=new JsonParser().parse(reader).getAsJsonObject();
        }
        AnvilChunkLoader loader=new AnvilChunkLoader(new File(directory,"unused-native-loader"));
        Method decode=null;
        for(Method method:AnvilChunkLoader.class.getDeclaredMethods()){
            if(method.getReturnType()==Chunk.class&&Arrays.equals(method.getParameterTypes(),new Class[]{World.class,NBTTagCompound.class})){
                if(decode!=null)throw new AssertionError("Ambiguous native Anvil decoder");decode=method;
            }
        }
        Method encode=null;
        for(Method method:AnvilChunkLoader.class.getDeclaredMethods())
            if(method.getReturnType()==void.class&&Arrays.equals(method.getParameterTypes(),new Class[]{Chunk.class,World.class,NBTTagCompound.class}))encode=method;
        if(encode==null)throw new AssertionError("Native Anvil encoder unavailable");encode.setAccessible(true);
        if(decode==null)throw new AssertionError("Native Anvil decoder unavailable");decode.setAccessible(true);
        boolean legacy=manifest.has("legacy")&&manifest.get("legacy").getAsBoolean();int startCount=0;
        Set<String> expectedFamilies=new HashSet<>();
        if(manifest.has("expectedFamilies"))for(JsonElement name:manifest.getAsJsonArray("expectedFamilies"))expectedFamilies.add(name.getAsString());
        else for(char letter='A';letter<='J';letter++)expectedFamilies.add("ruin"+letter);
        NtmAnvilProofWorld context=new NtmAnvilProofWorld();
        Map<String,NBTTagCompound> inputs=new HashMap<>();Map<String,Chunk> decoded=new HashMap<>();
        for(JsonElement element:manifest.getAsJsonArray(manifest.has("contextChunks")?"contextChunks":"sites")){
            String filename=element.getAsJsonObject().get("chunkFile").getAsString();
            if(inputs.containsKey(filename))continue;
            NBTTagCompound input;
            try(InputStream stream=new FileInputStream(new File(directory,filename))){input=CompressedStreamTools.readCompressed(stream);}
            Chunk chunk=(Chunk)decode.invoke(loader,context,input.getCompoundTag("Level"));context.supply(chunk);inputs.put(filename,input);decoded.put(filename,chunk);
        }
        for(String filename:inputs.keySet())loader.loadEntities(context,inputs.get(filename).getCompoundTag("Level"),decoded.get(filename));
        Set<String> families=new HashSet<>();Set<String> rotations=new HashSet<>();Set<String> variants=new HashSet<>();int blocks=0,tiles=0,ticks=0;boolean high=false,highTick=false;
        NBTTagCompound loot=null;
        for(JsonElement element:manifest.getAsJsonArray("sites")){
            JsonObject site=element.getAsJsonObject();String name=site.get("name").getAsString();
            families.add(name);rotations.add(name+":"+site.get("rotation").getAsInt());
            if(site.has("variantKey"))variants.add(site.get("variantKey").getAsString());
            NBTTagCompound root;
            File file=new File(directory,site.get("chunkFile").getAsString());
            if(file.length()>8*1024*1024)throw new AssertionError("Converted chunk artifact too large");
            byte[] encoded=Files.readAllBytes(file.toPath());
            StringBuilder hash=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(encoded))hash.append(String.format(java.util.Locale.ROOT,"%02x",value&255));
            if(!hash.toString().equals(site.get("chunkSha256").getAsString()))throw new AssertionError("Converted artifact checksum changed");
            try(InputStream input=new ByteArrayInputStream(encoded)){root=CompressedStreamTools.readCompressed(input);}
            Chunk chunk=decoded.get(site.get("chunkFile").getAsString());
            for(JsonElement value:site.getAsJsonArray("cells")){
                JsonArray cell=value.getAsJsonArray();int x=cell.get(0).getAsInt(),y=cell.get(1).getAsInt(),z=cell.get(2).getAsInt();
                int id=cell.get(3).getAsInt(),meta=cell.get(4).getAsInt();
                Block block=chunk.getBlock(x&15,y,z&15);
                if(Block.getIdFromBlock(block)!=id||chunk.getBlockMetadata(x&15,y,z&15)!=meta
                        ||!Block.blockRegistry.getNameForObject(block).equals(cell.get(5).getAsString()))
                    throw new AssertionError("Converted native ruin block/rotation mismatch: "+name+"@"+x+","+y+","+z);
                blocks++;high|=y>255;
            }
            NBTTagList pending=root.getCompoundTag("Level").getTagList("TileTicks",10);
            for(int i=0;i<pending.tagCount();i++){
                NBTTagCompound tick=pending.getCompoundTagAt(i);
                Block block=chunk.getBlock(tick.getInteger("x")&15,tick.getInteger("y"),tick.getInteger("z")&15);
                if(Block.getIdFromBlock(block)!=tick.getInteger("i")||tick.getInteger("t")<0)
                    throw new AssertionError("Converted native scheduled update lost its block/delay");
                List<?> scheduled=context.getPendingBlockUpdates(chunk,false);boolean found=false;
                if(scheduled!=null)for(Object value:scheduled){
                    net.minecraft.world.NextTickListEntry entry=(net.minecraft.world.NextTickListEntry)value;
                    if(entry.xCoord==tick.getInteger("x")&&entry.yCoord==tick.getInteger("y")&&entry.zCoord==tick.getInteger("z")
                        &&entry.func_151351_a()==block&&entry.priority==tick.getInteger("p")
                        &&entry.scheduledTime==context.getTotalWorldTime()+tick.getInteger("t"))found=true;
                }
                if(!found)throw new AssertionError("Native loader did not restore the scheduled block update");
                highTick|=tick.getInteger("y")>255;
                ticks++;
            }
            if(pending.tagCount()>0){
                NBTTagCompound saved=new NBTTagCompound();encode.invoke(loader,chunk,context,saved);
                NBTTagList persisted=saved.getTagList("TileTicks",10);
                for(int i=0;i<pending.tagCount();i++){
                    boolean found=false;NBTTagCompound expected=pending.getCompoundTagAt(i);
                    for(int j=0;j<persisted.tagCount();j++)if(expected.equals(persisted.getCompoundTagAt(j)))found=true;
                    if(!found)throw new AssertionError("Native Anvil save lost or altered a scheduled update");
                }
            }
            Set<String> capturedPositions=new HashSet<>();
            for(JsonElement value:site.getAsJsonArray("cells")){
                JsonArray c=value.getAsJsonArray();
                capturedPositions.add(c.get(0).getAsInt()+","+c.get(1).getAsInt()+","+c.get(2).getAsInt());
            }
            NBTTagList entities=root.getCompoundTag("Level").getTagList("TileEntities",10);
            for(int i=0;i<entities.tagCount();i++){
                NBTTagCompound nbt=entities.getCompoundTagAt(i);String id=nbt.getString("id");
                if(!capturedPositions.contains(nbt.getInteger("x")+","+nbt.getInteger("y")+","+nbt.getInteger("z")))continue;
                Block actualBlock=chunk.getBlock(nbt.getInteger("x")&15,nbt.getInteger("y"),nbt.getInteger("z")&15);
                TileEntity prototype=actualBlock.createTileEntity(context,chunk.getBlockMetadata(nbt.getInteger("x")&15,nbt.getInteger("y"),nbt.getInteger("z")&15));
                // Native forest-post templates retain an inert barrel tile on the
                // corroded decorative variant, whose fresh tile factory returns null.
                boolean nativeDecorativeBarrel=id.equals("tileentity_fluid_barrel")&&Block.blockRegistry.getNameForObject(actualBlock).equals("hbm:tile.barrel_corroded");
                boolean nativeVanillaSign=id.equals("Sign")&&actualBlock==net.minecraft.init.Blocks.wall_sign
                    &&prototype instanceof net.minecraft.tileentity.TileEntitySign
                    &&TileEntity.createAndLoadEntity(nbt).getClass()==net.minecraft.tileentity.TileEntitySign.class;
                if(!nativeDecorativeBarrel&&!nativeVanillaSign&&(prototype==null||prototype.getClass()!=TileEntity.createAndLoadEntity(nbt).getClass()))
                    throw new AssertionError("Native loaded tile is attached to the wrong block: "+id+" block="+Block.blockRegistry.getNameForObject(actualBlock)+" meta="+chunk.getBlockMetadata(nbt.getInteger("x")&15,nbt.getInteger("y"),nbt.getInteger("z")&15)+" prototype="+prototype+" nbt="+nbt);
                TileEntity tile=null;
                for(Object value:chunk.chunkTileEntityMap.values()){
                    TileEntity candidate=(TileEntity)value;
                    if(candidate.xCoord==nbt.getInteger("x")&&candidate.yCoord==nbt.getInteger("y")&&candidate.zCoord==nbt.getInteger("z"))tile=candidate;
                }
                if(tile==null)throw new AssertionError("Actual converter emitted unknown native tile ID");
                NBTTagCompound written=new NBTTagCompound();tile.writeToNBT(written);
                if(legacy&&id.equals("Chest")&&!nbt.hasKey("EFRPairDirection")&&written.hasKey("EFRPairDirection",1)&&written.getByte("EFRPairDirection")==0){
                    written=(NBTTagCompound)written.copy();written.removeTag("EFRPairDirection");
                }
                if(!written.equals(nbt))throw new AssertionError("Actual native ruin tile changed on native load: "+nbt+" -> "+written);
                if(id.equals("tileentity_wand_loot")){
                    if(!nbt.getBoolean("trigger"))throw new AssertionError("Converter lost native loot trigger");
                    if(loot==null)loot=(NBTTagCompound)nbt.copy();
                }
                tiles++;
            }
        }
        Set<String> expectedVariants=new HashSet<>();
        if(manifest.has("expectedVariants"))for(JsonElement key:manifest.getAsJsonArray("expectedVariants"))expectedVariants.add(key.getAsString());
        boolean variantCoverage=expectedVariants.isEmpty()?rotations.size()==expectedFamilies.size()*4:variants.equals(expectedVariants);
        if(!families.equals(expectedFamilies)||!variantCoverage||blocks==0||tiles==0||!high||(!legacy&&loot==null))
            throw new AssertionError("Actual converter artifacts do not cover all native ruins/rotations/extended height");
        if(!legacy&&expectedFamilies.size()>=17&&!highTick)throw new AssertionError("Expanded surface proof must include native scheduled updates above Y255");
        if(!legacy){
        NBTTagCompound savedStarts;
        byte[] startBytes=Files.readAllBytes(new File(directory,"structure-starts.nbt.gz").toPath());
        StringBuilder startHash=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(startBytes))startHash.append(String.format(java.util.Locale.ROOT,"%02x",value&255));
        if(!startHash.toString().equals(manifest.get("startsSha256").getAsString()))throw new AssertionError("Native start metadata checksum changed");
        try(InputStream input=new ByteArrayInputStream(startBytes)){savedStarts=CompressedStreamTools.readCompressed(input);}
        MapGenStructureData nativeData=new MapGenStructureData("NBTStructures");nativeData.readFromNBT(savedStarts.getCompoundTag("data"));
        MapGenStructureData stored=(MapGenStructureData)world.perWorldStorage.loadData(MapGenStructureData.class,"NBTStructures");
        if(stored==null||!stored.func_143041_a().equals(nativeData.func_143041_a()))
            throw new AssertionError("Actual native world storage did not load converted structure starts");
        NBTTagCompound features=stored.func_143041_a();
        if(features.func_150296_c().size()!=(expectedVariants.isEmpty()?expectedFamilies.size()*4:expectedVariants.size()))throw new AssertionError("Native structure-start count mismatch");
        for(Object key:features.func_150296_c()){
            NBTTagCompound record=features.getCompoundTag(key.toString());
            StructureStart start=MapGenStructureIO.func_143035_a(record,world);
            if(start==null||!Arrays.asList("com.hbm.world.gen.nbt.NBTStructure$Start","com.hbm.world.gen.MapGenNTMFeatures$Start","com.hbm.world.gen.component.BunkerComponents$BunkerStart").contains(start.getClass().getName()))
                throw new AssertionError("Native start factory could not load imported structure");
            if(!start.func_143021_a(record.getInteger("ChunkX"),record.getInteger("ChunkZ")).equals(record))
                throw new AssertionError("Native structure-start metadata failed exact roundtrip");
            if(expectedVariants.isEmpty()&&!rotations.contains(record.getString("name")+":"+record.getTagList("Children",10).getCompoundTagAt(0).getInteger("O")))
                throw new AssertionError("Native start lookup identity/rotation mismatch");
        }
        startCount=features.func_150296_c().size();
        }
        if(legacy)NtmKeyholeProbe.run(world);
        if(!expectedVariants.isEmpty())NtmConvertedInventoryProbe.verify(context,inputs,decoded,directory,encode,decode,loader);
        if(!legacy)NtmStructureCaptureProbe.verifyDeferredLoot(world,loot,-328,"ntm-converted-ruin-loot.nbt");
        System.out.println("NTM_CONVERTED_RUINS PASSED: "+blocks+" native-decoded blocks, "+tiles+" native tile payloads, "+ticks+" scheduled updates, "+families.size()+" families/four rotations, above Y255, "+startCount+" native structure-start records and native loot persistence");
    }
}
