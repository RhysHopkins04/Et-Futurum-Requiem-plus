package efrtest;

import com.google.gson.*;
import ganymedes01.etfuturum.world.imported.NtmImportStructures;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.structure.*;

/** Native capture development probe; exports evidence, never production-ready templates. */
public final class NtmStructureCaptureProbe {
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void run(WorldServer real) throws Exception {
        File output=new File("../structure-captures");output.mkdirs();
        JsonObject catalogue=NtmImportStructures.describe(real);
        if(!catalogue.get("available").getAsBoolean())throw new AssertionError(catalogue);
        JsonArray results=new JsonArray();int count=0;
        NBTTagCompound deferredExample=null, deferredLooseExample=null;
        Class<?> registry=Class.forName("com.hbm.world.gen.nbt.NBTStructure");
        Class<?> spawnClass=Class.forName("com.hbm.world.gen.nbt.SpawnCondition");
        JsonArray jobs=new JsonArray();
        for(JsonElement entry:catalogue.getAsJsonArray("entries")){
            JsonObject job=entry.getAsJsonObject();
            if(job.has("name")&&!Arrays.asList("bunker","features").contains(job.get("name").getAsString())){
                job.addProperty("rotation",1);jobs.add(job);
                for(long seed:new long[]{4096L,0L,256L}){
                    JsonObject variant=new JsonObject();variant.addProperty("name",job.get("name").getAsString());
                    variant.addProperty("sampleSeed",seed);variant.addProperty("rotation",new Random(seed).nextInt(4));jobs.add(variant);
                }
            }else {if(job.has("name")&&job.get("name").getAsString().equals("bunker"))job.addProperty("variant",0);jobs.add(job);}
        }
        // Cover every native procedural child, rather than sampling only one.
        for(String child:new String[]{"silo","NTMHouse1","NTMHouse2","NTMLab1","NTMLab2","LargeOffice","LargeOfficeCorner","RuralHouse1"})
            for(long seed:new long[]{4096L,12345L,0L,256L}){
                JsonObject job=new JsonObject();job.addProperty("name","features");job.addProperty("child",child);
                job.addProperty("sampleSeed",seed);job.addProperty("rotation",new Random(seed).nextInt(4));jobs.add(job);
            }
        for(int variant=1;variant<16;variant++){
            JsonObject job=new JsonObject();job.addProperty("name","bunker");job.addProperty("variant",variant);
            job.addProperty("sampleSeed",variant*104729L);jobs.add(job);
        }
        for(String legacy:NtmLegacyCapture.NAMES)for(int variant=0;variant<4;variant++){
            JsonObject job=new JsonObject();job.addProperty("name","legacy-"+legacy);job.addProperty("variant",variant);
            job.addProperty("sampleSeed",new long[]{12345L,0L,256L,4096L}[variant]);jobs.add(job);
        }
        JsonObject highTomb=new JsonObject();highTomb.addProperty("name","legacy-ancient_tomb_high");highTomb.addProperty("variant",0);highTomb.addProperty("sampleSeed",12345L);jobs.add(highTomb);
        for(JsonElement entry:jobs){
            JsonObject row=entry.getAsJsonObject();if(!row.has("name"))continue;
            String name=row.get("name").getAsString();
            long captureSeed=row.has("sampleSeed")?row.get("sampleSeed").getAsLong():12345L;
            String captureFile=name+(row.has("child")?"-"+row.get("child").getAsString():"")+(row.has("variant")&&row.get("variant").getAsInt()>0?"-v"+row.get("variant").getAsInt():"")+(row.has("rotation")?"-r"+row.get("rotation").getAsInt():"")+".nbt";
            boolean ocean=Arrays.asList("oil_rig","lighthouse","aircraft_carrier","beached_patrol","submarine").contains(name);
            BiomeGenBase biome=ocean?BiomeGenBase.ocean:name.startsWith("desert_")||name.contains("vertibird")||name.equals("dead_dish_small")?BiomeGenBase.desert:BiomeGenBase.plains;
            if(name.equals("legacy-arctic_vault"))biome=BiomeGenBase.icePlains;
            if(name.equals("legacy-jungle_dungeon"))biome=BiomeGenBase.jungle;
            JsonObject result=new JsonObject();result.addProperty("name",name);result.addProperty("productionReady",false);result.addProperty("captureFile",captureFile);
            if(row.has("child"))result.addProperty("child",row.get("child").getAsString());
            if(row.has("rotation"))result.addProperty("rotation",row.get("rotation").getAsInt());
            boolean ownsJobs=false;
            if(row.has("variant"))result.addProperty("variant",row.get("variant").getAsInt());
            try{
                NtmRecordingWorld world=new NtmRecordingWorld(captureSeed,biome,ocean,name.equals("legacy-ancient_tomb_high")?288:name.equals("features")||name.equals("bunker")?128:64);
                assertNoTimedJobs();ownsJobs=true;
                Random rng=new Random(captureSeed);
                StructureStart generatedStart=null;
                if(name.startsWith("legacy-")){
                    NtmLegacyCapture.generate(world,rng,name.substring(7),row.get("variant").getAsInt());
                }else{
                Object spawn=registry.getMethod("getStructure",String.class).invoke(null,name);
                Function start=(Function)spawnClass.getField("start").get(spawn);
                StructureStart structure;
                if(row.has("child")){
                    structure=(StructureStart)Class.forName("com.hbm.world.gen.MapGenNTMFeatures$Start").getConstructor().newInstance();
                    String child=row.get("child").getAsString();
                    String path=child.equals("silo")?"SiloComponent":child.startsWith("LargeOffice")?"OfficeFeatures$"+child:"CivilianFeatures$"+child;
                    StructureComponent component=(StructureComponent)Class.forName("com.hbm.world.gen.component."+path)
                        .getConstructor(Random.class,int.class,int.class).newInstance(rng,8,8);
                    structure.getComponents().add(component);
                    Method update=StructureStart.class.getDeclaredMethod("updateBoundingBox");update.setAccessible(true);update.invoke(structure);
                }else if(start!=null){
                    Object tuple=Class.forName("com.hbm.util.Tuple$Quartet").getConstructors()[0].newInstance(world,rng,0,0);
                    structure=(StructureStart)start.apply(tuple);
                }else{
                    structure=(StructureStart)Class.forName("com.hbm.world.gen.nbt.NBTStructure$Start")
                        .getConstructor(net.minecraft.world.World.class,Random.class,spawnClass,int.class,int.class)
                        .newInstance(world,rng,spawn,0,0);
                }
                // Native generators average terrain over the current population
                // chunk, never over one giant box covering the whole structure.
                StructureBoundingBox bounds=structure.getBoundingBox();
                int minCX=Math.floorDiv(bounds.minX-8,16),maxCX=Math.floorDiv(bounds.maxX-8,16);
                int minCZ=Math.floorDiv(bounds.minZ-8,16),maxCZ=Math.floorDiv(bounds.maxZ-8,16);
                List<int[]> chunks=new ArrayList<>();
                for(int cz=minCZ;cz<=maxCZ;cz++)for(int cx=minCX;cx<=maxCX;cx++)chunks.add(new int[]{cx,cz});
                if(row.has("child")&&row.get("child").getAsString().equals("silo")){
                    StructureComponent component=(StructureComponent)structure.getComponents().getFirst();
                    Method mx=StructureComponent.class.getDeclaredMethod("getXWithOffset",int.class,int.class);
                    Method mz=StructureComponent.class.getDeclaredMethod("getZWithOffset",int.class,int.class);mx.setAccessible(true);mz.setAccessible(true);
                    int anchorCX=Math.floorDiv((Integer)mx.invoke(component,13,2)-8,16),anchorCZ=Math.floorDiv((Integer)mz.invoke(component,13,2)-8,16);
                    chunks.sort(Comparator.comparingInt(pair->pair[0]==anchorCX&&pair[1]==anchorCZ?0:1));
                }
                for(int[] pair:chunks)structure.generateStructure(world,rng,new StructureBoundingBox(pair[0]*16+8,0,pair[1]*16+8,pair[0]*16+23,383,pair[1]*16+23));
                generatedStart=structure;
                }
                drainTimedJobs(world);
                finalizeLogic(world);
                if(name.equals("legacy-jungle_dungeon")&&world.written.size()<10000)throw new AssertionError("Native timed jungle dungeon is incomplete");
                if(world.written.isEmpty())throw new AssertionError("Native structure generated no blocks");
                NBTTagCompound capture=new NBTTagCompound();NBTTagList cells=new NBTTagList();
                Map<String,Integer> tileTypes=new TreeMap<>();int deferredLoot=0;
                for(NtmRecordingWorld.Position pos:world.written){
                    Block block=world.getBlock(pos.x,pos.y,pos.z);
                    NBTTagCompound cell=new NBTTagCompound();cell.setInteger("x",pos.x);cell.setInteger("y",pos.y);cell.setInteger("z",pos.z);
                    cell.setString("block",Block.blockRegistry.getNameForObject(block));cell.setInteger("meta",world.getBlockMetadata(pos.x,pos.y,pos.z));
                    TileEntity tile=world.getTileEntity(pos.x,pos.y,pos.z);
                    if(tile!=null){
                        NBTTagCompound nbt=new NBTTagCompound();tile.writeToNBT(nbt);cell.setTag("tile",nbt);
                        tileTypes.put(nbt.getString("id"),tileTypes.getOrDefault(nbt.getString("id"),0)+1);
                        TileEntity restored=TileEntity.createAndLoadEntity((NBTTagCompound)nbt.copy());
                        if(restored==null||restored.getClass()!=tile.getClass())throw new AssertionError("Native tile NBT cannot reload: "+nbt.getString("id"));
                        restored.setWorldObj(world);NBTTagCompound normalized=new NBTTagCompound();restored.writeToNBT(normalized);nbt=normalized;cell.setTag("tile",nbt);
                        if(tile.getClass().getName().contains("TileEntityWandLoot")){
                            if(!nbt.getBoolean("trigger"))throw new AssertionError("Native loot placeholder lost its deferred trigger");
                            deferredLoot++;
                            if(deferredExample==null)deferredExample=(NBTTagCompound)nbt.copy();
                            if(deferredLooseExample==null&&nbt.getString("block").equals("hbm:tile.deco_loot"))deferredLooseExample=(NBTTagCompound)nbt.copy();
                        }
                    }
                    cells.appendTag(cell);
                }
                if(row.has("variant"))capture.setInteger("variant",row.get("variant").getAsInt());
                capture.setInteger("captureVersion",3);capture.setString("name",name);capture.setLong("seed",captureSeed);
                if(row.has("rotation"))capture.setInteger("rotation",row.get("rotation").getAsInt());capture.setInteger("referenceSurface",world.referenceSurface);
                if(row.has("child"))capture.setString("child",row.get("child").getAsString());
                capture.setBoolean("productionReady",false);capture.setTag("cells",cells);
                NBTTagList inventoryLoot=new NBTTagList();
                for(NBTTagCompound record:world.loot){
                    if(!record.hasKey("rolled",9))throw new AssertionError("Native inventory loot call did not complete");
                    inventoryLoot.appendTag(record);
                }
                capture.setTag("inventoryLoot",inventoryLoot);
                result.addProperty("inventoryLootCalls",world.loot.size());
                if(name.equals("bunker")&&world.loot.isEmpty())throw new AssertionError("Native bunker loot instrumentation did not run");
                if(generatedStart!=null)capture.setTag("structureStart",generatedStart.func_143021_a(0,0));
                try(FileOutputStream stream=new FileOutputStream(new File(output,captureFile))){CompressedStreamTools.writeCompressed(capture,stream);}
                result.addProperty("captureSha256",sha256(new File(output,captureFile)));
                result.addProperty("captured",true);result.addProperty("blocks",world.written.size());result.addProperty("deferredLoot",deferredLoot);
                result.add("tileTypes",new Gson().toJsonTree(tileTypes));result.add("entityAttempts",new Gson().toJsonTree(world.entities));
                result.add("pendingTicks",new Gson().toJsonTree(world.pendingTicks));
                List<NtmRecordingWorld.PendingTick> durableTicks=new ArrayList<>();
                Set<String> tickKeys=new HashSet<>();
                for(NtmRecordingWorld.PendingTick tick:world.pendingTicks){
                    if(!world.written.contains(new NtmRecordingWorld.Position(tick.x,tick.y,tick.z)))continue;
                    if(!tick.block.equals(Block.blockRegistry.getNameForObject(world.getBlock(tick.x,tick.y,tick.z))))continue;
                    String key=tick.x+","+tick.y+","+tick.z+":"+tick.block;
                    if(tickKeys.add(key))durableTicks.add(tick);
                }
                result.add("durablePendingTicks",new Gson().toJsonTree(durableTicks));count++;
            }catch(Exception|AssertionError|LinkageError ex){
                result.addProperty("captured",false);Throwable cause=ex;while(cause.getCause()!=null)cause=cause.getCause();
                result.addProperty("error",cause.toString());cause.printStackTrace();
            }
            if(ownsJobs)clearFixtureTimedJobs();
            results.add(result);System.out.println("NTM_STRUCTURE_CAPTURE "+name+" captured="+result.get("captured")+" blocks="+result.get("blocks")+" error="+result.get("error"));
        }
        JsonObject report=new JsonObject();report.addProperty("schemaVersion",1);report.addProperty("captured",count);report.addProperty("productionReady",false);
        report.addProperty("ntmJarSha256",sha256(new File(System.getProperty("efr.ntmOriginalJar"))));
        report.add("structures",results);report.add("structureFluidIds",catalogue.get("structureFluidIds"));
        JsonObject itemIds=new JsonObject();
        for(Object key:net.minecraft.item.Item.itemRegistry.getKeys()){
            net.minecraft.item.Item item=(net.minecraft.item.Item)net.minecraft.item.Item.itemRegistry.getObject(key);
            itemIds.addProperty(key.toString(),net.minecraft.item.Item.getIdFromItem(item));
        }
        report.add("itemIds",itemIds);report.addProperty("lootAmountFactor",catalogue.getAsJsonObject("settings").get("lootAmountFactor").getAsDouble());
        Files.write(new File(output,"report.json").toPath(),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
        int expected=0;for(JsonElement job:jobs)if(job.getAsJsonObject().has("name"))expected++;
        if(count!=expected)throw new AssertionError("Native structure capture incomplete: "+count+"/"+expected+"; see "+output);
        if(deferredExample==null)throw new AssertionError("No deferred native loot captured");
        verifyDeferredLoot(real,deferredExample,-320,"native-structure-loot-fixture.nbt");
        if(deferredLooseExample==null)throw new AssertionError("No native loose loot captured");
        verifyDeferredLoot(real,deferredLooseExample,-324,"native-structure-loose-loot-fixture.nbt");
        System.out.println("NTM_STRUCTURE_CAPTURE PASSED: "+count+"/"+expected+" captures; bounded memory world, transformed blocks and native tile NBT reload; capture evidence requires production adapters");
    }

    private static String sha256(File file) throws Exception {
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(file)) {
            byte[] buffer=new byte[65536];int count;
            while((count=in.read(buffer))!=-1)digest.update(buffer,0,count);
        }
        StringBuilder hex=new StringBuilder();
        for(byte value:digest.digest())hex.append(String.format("%02x",value&255));
        return hex.toString();
    }

    /** Native wand logic omits its transient replacement flag from NBT. Resolve
     * just that replacement before serialization, without ticking live logic. */
    private static void finalizeLogic(NtmRecordingWorld world) throws Exception {
        Class<?> type=Class.forName("com.hbm.blocks.generic.BlockWandLogic$TileEntityWandLogic");
        Method replace=type.getDeclaredMethod("replace");replace.setAccessible(true);
        for(NtmRecordingWorld.Position pos:new ArrayList<>(world.written)){
            TileEntity tile=world.getTileEntity(pos.x,pos.y,pos.z);
            if(tile!=null&&type.isInstance(tile)){
                replace.invoke(tile);
                TileEntity result=world.getTileEntity(pos.x,pos.y,pos.z);
                if(result==null||type.isInstance(result))throw new AssertionError("Native logic replacement failed");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<?> timedJobs() throws Exception {
        Field f=Class.forName("com.hbm.world.generator.TimedGenerator").getDeclaredField("operations");f.setAccessible(true);
        return ((Map<Integer,List<?>>)f.get(null)).get(0);
    }
    private static void assertNoTimedJobs() throws Exception {
        List<?> jobs=timedJobs();
        if(jobs!=null&&!jobs.isEmpty())throw new AssertionError("Isolated fixture has unexpected pending native jobs");
    }
    private static void clearFixtureTimedJobs() throws Exception {
        List<?> jobs=timedJobs();if(jobs!=null)jobs.clear();
    }

    /** Drain only jobs owned by this memory world. Never execute real-server jobs. */
    @SuppressWarnings("unchecked")
    private static void drainTimedJobs(NtmRecordingWorld world) throws Exception {
        Class<?> type=Class.forName("com.hbm.world.generator.TimedGenerator");
        Field field=type.getDeclaredField("operations");field.setAccessible(true);
        Map<Integer,ArrayList<?>> queues=(Map<Integer,ArrayList<?>>)field.get(null);
        ArrayList<?> jobs=queues.get(world.provider.dimensionId);
        if(jobs==null)return;
        int count=0;
        while(!jobs.isEmpty()){
            if(++count>100000)throw new AssertionError("Native timed structure exceeded job budget");
            Object job=jobs.remove(0);
            Method work=Class.forName("com.hbm.world.generator.TimedGenerator$ITimedJob").getMethod("work");
            work.invoke(job);
        }
    }

    private static void generateLegacy(NtmRecordingWorld world,Random rng,String name) throws Exception {
        Class<?> wc=net.minecraft.world.World.class;
        if(name.equals("arctic_vault")){
            Object generator=Class.forName("com.hbm.world.dungeon.ArcticVault").getConstructor().newInstance();
            generator.getClass().getMethod("trySpawn",wc,int.class,int.class,int.class).invoke(generator,world,0,32,0);
        }else if(name.equals("jungle_dungeon")){
            Object generator=Class.forName("com.hbm.world.generator.CellularDungeonFactory").getField("jungle").get(null);
            Method generate=generator.getClass().getMethod("generate",wc,int.class,int.class,int.class,Random.class);
            for(int y:new int[]{20,24,28}){generate.invoke(generator,world,0,y,0,world.rand);drainTimedJobs(world);}
            Block marker=(Block)Class.forName("com.hbm.blocks.ModBlocks").getField("deco_titanium").get(null);
            int y=world.getHeightValue(0,0);
            for(int n=0;n<3;n++)world.setBlock(0,y+n,0,marker);
            world.setBlock(0,y+3,0,net.minecraft.init.Blocks.redstone_block);
        }else{
            String type=name.equals("antenna")?"Antenna":name.equals("desert_atom")?"DesertAtom001":
                name.equals("library_dungeon")?"LibraryDungeon":"AncientTomb";
            Object generator=Class.forName("com.hbm.world.dungeon."+type).getConstructor().newInstance();
            generator.getClass().getMethod(name.equals("ancient_tomb")?"build":"generate",wc,Random.class,int.class,int.class,int.class)
                .invoke(generator,world,rng,0,name.equals("library_dungeon")?32:64,0);
        }
    }

    static void verifyDeferredLoot(WorldServer world,NBTTagCompound example,int x,String filename) throws Exception {
        int y=270,z=-320;
        File marker=new File(filename);
        if(marker.exists()){
            NBTTagCompound expected;
            try(FileInputStream stream=new FileInputStream(marker)){expected=CompressedStreamTools.readCompressed(stream);}
            TileEntity loaded=world.getTileEntity(x,y,z);
            if(loaded==null)throw new AssertionError("Resolved native structure loot did not persist");
            NBTTagCompound actual=new NBTTagCompound();loaded.writeToNBT(actual);
            // Native inventories normalize a null custom name to the empty
            // string when loading. Preserve all item/lock data exactly.
            if(!expected.hasKey("name")&&actual.hasKey("name",8)&&actual.getString("name").isEmpty())
                actual.removeTag("name");
            if(!expected.equals(actual)){
                System.out.println("NTM_LOOT_EXPECTED "+expected);
                System.out.println("NTM_LOOT_ACTUAL "+actual);
                for(Object key:expected.func_150296_c()){
                    String text=key.toString();
                    if(!Objects.equals(expected.getTag(text),actual.getTag(text)))
                        System.out.println("NTM_LOOT_DIFF "+text+" expected="+expected.getTag(text)+" actual="+actual.getTag(text));
                }
                throw new AssertionError("Resolved native loot changed or rerolled on restart");
            }
            System.out.println("NTM_STRUCTURE_LOOT_RESTART PASSED: native resolved loot retained exact item NBT");
            return;
        }
        NBTTagCompound moved=(NBTTagCompound)example.copy();moved.setInteger("x",x);moved.setInteger("y",y);moved.setInteger("z",z);
        Block placeholder=Block.getBlockFromName("hbm:tile.wand_loot");
        world.setBlock(x,y-1,z,net.minecraft.init.Blocks.stone,0,2);
        world.setBlock(x,y,z,placeholder,0,2);
        TileEntity tile=TileEntity.createAndLoadEntity(moved);world.setTileEntity(x,y,z,tile);
        tile.updateEntity();
        Block expectedBlock=Block.getBlockFromName(moved.getString("block"));
        if(world.getBlock(x,y,z)!=expectedBlock||world.getBlock(x,y,z)==placeholder)
            throw new AssertionError("Native deferred structure loot did not resolve after relocation");
        TileEntity resolved=world.getTileEntity(x,y,z);
        if(resolved==null)throw new AssertionError("Native resolved loot has no tile entity");
        NBTTagCompound saved=new NBTTagCompound();resolved.writeToNBT(saved);
        if(saved.getString("id").equals("tileentity_ntm_loot")&&saved.getInteger("count")<=0)
            throw new AssertionError("Native deferred loot resolved to an empty container");
        world.saveAllChunks(true,null);
        try(FileOutputStream stream=new FileOutputStream(marker)){CompressedStreamTools.writeCompressed(saved,stream);}
        System.out.println("NTM_STRUCTURE_LOOT_SAVE PASSED: relocated captured instructions resolved through native code and produced real loot");
    }

}
