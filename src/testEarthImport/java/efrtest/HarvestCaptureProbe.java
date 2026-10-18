package efrtest;

import com.google.gson.*;
import ganymedes01.etfuturum.world.imported.*;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGenerator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import static efrtest.EarthImportContracts.check;

/** Exact native HarvestCraft geometry, independent of real-save terrain. Never shipped. */
final class HarvestCaptureProbe {
    static void run(WorldServer live) throws Exception {
        JsonObject contract=HarvestImportResources.describe();
        check(contract.get("available").getAsBoolean(),"HarvestCraft export: "+contract);
        check(contract.getAsJsonArray("gardens").size()==12,"garden inventory");
        check(contract.getAsJsonArray("trees").size()==34,"native tree inventory");
        JsonArray captures=new JsonArray();Class<?> registry=Class.forName("com.pam.harvestcraft.BlockRegistry");
        for(JsonElement entry:contract.getAsJsonArray("trees")) {
            JsonObject row=entry.getAsJsonObject();
            for(int variant=0;variant<4;variant++) {
                long seed=90210L+variant;
                NtmRecordingWorld world=new NtmRecordingWorld(seed,BiomeGenBase.forest,false);
                Block fruit=(Block)registry.getField(row.get("field").getAsString()).get(null);
                WorldGenerator generator=(WorldGenerator)Class.forName("com.pam.harvestcraft."+row.get("generator").getAsString())
                    .getConstructor(boolean.class,int.class,int.class,int.class,Block.class).newInstance(false,5,row.get("wood").getAsInt(),row.get("leaves").getAsInt(),fruit);
                NtmRecordingWorld high=new NtmRecordingWorld(seed,BiomeGenBase.forest,false,304);
                check(generator.generate(high,new Random(seed),0,304,0),"native HarvestCraft tree cannot grow above255: "+row);
                check(generator.generate(world,new Random(seed),0,64,0),"native tree did not generate: "+row);
                check(world.entities.isEmpty() && world.pendingTicks.isEmpty(),"unexpected native tree effects");
                JsonArray cells=new JsonArray();boolean hasFruit=false;
                for(NtmRecordingWorld.Position pos:world.written) {
                    Block block=world.getBlock(pos.x,pos.y,pos.z);
                    check(world.getTileEntity(pos.x,pos.y,pos.z)==null,"unexpected HarvestCraft tree tile");
                    check(Math.abs(pos.x)<=4 && Math.abs(pos.z)<=4 && pos.y>=63 && pos.y<80,"tree bounds");
                    JsonArray cell=new JsonArray();cell.add(new JsonPrimitive(pos.x));cell.add(new JsonPrimitive(pos.y-64));cell.add(new JsonPrimitive(pos.z));
                    cell.add(new JsonPrimitive(Block.blockRegistry.getNameForObject(block)));cell.add(new JsonPrimitive(world.getBlockMetadata(pos.x,pos.y,pos.z)));cells.add(cell);
                    if(block==fruit)hasFruit=true;
                }
                check(hasFruit,"native tree has no fruit/log output: "+row);
                JsonObject capture=new JsonObject();capture.addProperty("group",row.get("group").getAsString());capture.addProperty("field",row.get("field").getAsString());
                capture.addProperty("variant",variant);capture.addProperty("seed",seed);capture.add("cells",cells);captures.add(capture);
            }
        }
        // Actual canPlaceBlockAt restrictions, including mushroom's native oak-log support.
        JsonObject supports=new JsonObject();
        for(JsonElement entry:contract.getAsJsonArray("gardens")) {
            JsonObject row=entry.getAsJsonObject();Block garden=(Block)Block.blockRegistry.getObject(row.get("block").getAsString());JsonArray allowed=new JsonArray();
            for(Block ground:new Block[]{Blocks.grass,Blocks.dirt,Blocks.sand,Blocks.water,Blocks.flowing_water,Blocks.log,Blocks.stone}) {
                NtmRecordingWorld fixture=new NtmRecordingWorld(1,BiomeGenBase.plains,false);fixture.setBlock(0,63,0,ground,0,2);
                if(garden.canPlaceBlockAt(fixture,0,64,0))allowed.add(new JsonPrimitive(Block.blockRegistry.getNameForObject(ground)));
            }
            supports.add(row.get("name").getAsString(),allowed);
        }
        JsonObject root=new JsonObject();root.addProperty("schemaVersion",1);root.add("contract",contract);root.add("gardenSupports",supports);root.add("trees",captures);
        File persisted=new File("../harvest-persistence-fixture.json");
        if(persisted.isFile()) {
            JsonArray expected=new JsonParser().parse(new InputStreamReader(new FileInputStream(persisted),StandardCharsets.UTF_8)).getAsJsonArray();
            for(JsonElement entry:expected) {
                JsonArray cell=entry.getAsJsonArray();int x=cell.get(0).getAsInt(),y=cell.get(1).getAsInt(),z=cell.get(2).getAsInt();
                check(cell.get(3).getAsString().equals(Block.blockRegistry.getNameForObject(live.getBlock(x,y,z))) && cell.get(4).getAsInt()==live.getBlockMetadata(x,y,z),"HarvestCraft native block changed on disk reload: "+cell);
            }
            System.out.println("HARVEST_NATIVE_RESTART PASSED: "+expected.size()+" cells above Y255");
        } else {
            JsonArray expected=new JsonArray();int site=0;
            for(JsonElement entry:captures) {
                JsonObject tree=entry.getAsJsonObject();if(tree.get("variant").getAsInt()!=0)continue;
                int anchor=256+(site++*16);live.setBlock(anchor,303,320,Blocks.grass,0,2);
                for(JsonElement element:tree.getAsJsonArray("cells")) {
                    JsonArray cell=element.getAsJsonArray();int x=anchor+cell.get(0).getAsInt(),y=304+cell.get(1).getAsInt(),z=320+cell.get(2).getAsInt();
                    Block block=(Block)Block.blockRegistry.getObject(cell.get(3).getAsString());int meta=cell.get(4).getAsInt();
                    live.setBlock(x,y,z,block,meta,2);JsonArray saved=new JsonArray();
                    saved.add(new JsonPrimitive(x));saved.add(new JsonPrimitive(y));saved.add(new JsonPrimitive(z));saved.add(new JsonPrimitive(Block.blockRegistry.getNameForObject(block)));saved.add(new JsonPrimitive(meta));expected.add(saved);
                }
            }
            for(JsonElement entry:contract.getAsJsonArray("gardens")) {
                JsonObject row=entry.getAsJsonObject();int x=256+(site++*16),y=304;
                live.setBlock(x,y-1,320,row.get("name").getAsString().equals("mushroom")?Blocks.log:row.get("name").getAsString().equals("water")?Blocks.water:Blocks.grass,0,2);
                Block block=(Block)Block.blockRegistry.getObject(row.get("block").getAsString());
                check(block.canPlaceBlockAt(live,x,y,320),"native garden placement above255: "+row);
                live.setBlock(x,y,320,block,0,2);JsonArray saved=new JsonArray();
                saved.add(new JsonPrimitive(x));saved.add(new JsonPrimitive(y));saved.add(new JsonPrimitive(320));saved.add(new JsonPrimitive(row.get("block").getAsString()));saved.add(new JsonPrimitive(0));expected.add(saved);
            }
            live.saveAllChunks(true,null);Files.write(persisted.toPath(),new Gson().toJson(expected).getBytes(StandardCharsets.UTF_8));
            System.out.println("HARVEST_NATIVE_SAVE PASSED: "+expected.size()+" cells above Y255");
        }
        File target=new File("../harvest-native-captures.json");target.getParentFile().mkdirs();
        Files.write(target.toPath(),new GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(StandardCharsets.UTF_8));
        check(captures.size()==136,"native capture count");System.out.println("HARVEST_NATIVE_CAPTURE PASSED: 136 native trees + 12 live garden support contracts");
    }
}
