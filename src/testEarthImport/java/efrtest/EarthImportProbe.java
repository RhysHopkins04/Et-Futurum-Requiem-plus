package efrtest;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Loader;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateHBMOre;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.api.imported.ImportedBiomeData;
import ganymedes01.etfuturum.api.imported.ImportedBiomeVolume;
import ganymedes01.etfuturum.configuration.configs.*;
import ganymedes01.etfuturum.world.imported.*;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.File;
import static efrtest.EarthImportContracts.check;

@Mod(modid="earth_import_probe",name="Earth import runtime probe",version="test",dependencies="required-after:etfuturum",acceptableRemoteVersions="*")
public final class EarthImportProbe {
    @Mod.EventHandler public void start(FMLServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("efr.earthImportProbe")) return;
        MinecraftServer server=MinecraftServer.getServer(); WorldServer world=server.worldServerForDimension(0);
        try {
            if ("legacy".equals(System.getProperty("efr.earthImportProfile"))) {
                check(ConfigMapCompatibility.mapCompatibilityMode && !ConfigMapCompatibility.importedEarthMode,"legacy profile flags");
                check(!ConfigWorld.extendedWorldHeight && world.getHeight()==256,"legacy height changed");
                check(!ConfigModCompat.moddedDeepslateOres && !ConfigMapCompatibility.allowsExtendedHeight(),"legacy content-only restrictions changed");
                check(ImportedBiomes.registered().isEmpty(),"import biomes registered in legacy profile");
                SmithingRecipeProbe.run(world, true);
                System.out.println("EARTH_IMPORT_LEGACY PASSED");return;
            }
            check(ConfigMapCompatibility.importedEarthMode && ConfigMapCompatibility.isEnabled(),"wrong profile");
            SmithingRecipeProbe.run(world, false);
            check(ConfigWorld.extendedWorldHeight && world.getHeight()==384 && world.getActualHeight()==384,"height contract");
            check(world.provider.getAverageGroundLevel()==128,"imported spawn reference not shifted");
            check(!ConfigWorld.modernOverworldGeneration && !ConfigWorld.modernOreGeneration && ConfigWorld.tileReplacementMode==-1,"automatic generation/replacement enabled");
            java.lang.reflect.Field generators=cpw.mods.fml.common.registry.GameRegistry.class.getDeclaredField("worldGenerators");generators.setAccessible(true);
            for (Object generator:(java.util.Set<?>)generators.get(null))
                check(!generator.getClass().getName().startsWith("ganymedes01.etfuturum.world."),"automatic EFR generator registered");
            check(ConfigModCompat.moddedDeepslateOres,"profile disabled ore registrations");
            check(ModBlocks.INFESTED_DEEPSLATE.get()!=null && ModBlocks.INFESTED_DEEPSLATE.isEnabled(),"infested deepslate unavailable");
            check(ImportedBiomes.registered().size()==ImportedBiome.values().length,"missing modern biome identity");
            ImportedBiomes.verifyRegistry();
            BiomeGenBase cherry=ImportedBiomes.resolve("minecraft:cherry_grove");
            // At logical Y64 (physical128), the height-adjusted vanilla lapse must not cool this biome.
            check(Math.abs(cherry.getFloatTemperature(0,128,0)-.5F)<.00001,"imported altitude translated incorrectly");
            check(BiomeGenBase.plains.getFloatTemperature(0,128,0)<BiomeGenBase.plains.temperature,"vanilla altitude behavior changed");
            int x=-32,z=48; Chunk chunk=world.getChunkFromBlockCoords(x,z);
            boolean restart=new File("earth-import-fixture-created").exists();
            if (restart) {
                for(int y:new int[]{0,64,255,256,383}) check(world.getBlock(x,y,z)==Blocks.gold_block,"block restart at Y"+y);
                check(chunk.isTerrainPopulated,"population flag lost after restart");
                check("minecraft:cherry_grove".equals(ImportedBiomeData.sourceAt(world,x,320,z)),"surface identity lost on disk reload");
                check("minecraft:lush_caves".equals(ImportedBiomeData.sourceAt(world,x,32,z)),"underground identity lost on disk reload");
                check((chunk.getBiomeArray()[0]&255)==cherry.biomeID,"surface biome ID lost on disk reload");
                System.out.println("EARTH_IMPORT_RESTART PASSED: actual Anvil blocks above255 + surface ID + 3D biome persistence");
            }
            for(int y:new int[]{0,64,255,256,383}) {
                check(world.setBlock(x,y,z,Blocks.gold_block,0,2)||world.getBlock(x,y,z)==Blocks.gold_block,"write Y"+y);
                check(world.getBlock(x,y,z)==Blocks.gold_block,"read Y"+y);
            }
            // Execute the transformed server wire codec: section bits above 15 and compressed payload must survive.
            net.minecraft.network.play.server.S21PacketChunkData packet=new net.minecraft.network.play.server.S21PacketChunkData(chunk,true,0xFFFFFF);
            net.minecraft.network.PacketBuffer buffer=new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());
            try {
                packet.writePacketData(buffer);buffer.readInt();buffer.readInt();buffer.readBoolean();
                int mask=buffer.readInt();check((mask&(1<<23))!=0 && (mask&(1<<16))!=0,"high section bits lost on wire");
                buffer.readerIndex(0);
                net.minecraft.network.play.server.S21PacketChunkData decoded=new net.minecraft.network.play.server.S21PacketChunkData();decoded.readPacketData(buffer);
                java.lang.reflect.Field payload=decoded.getClass().getDeclaredField("field_149278_f");payload.setAccessible(true);
                net.minecraft.network.play.server.S21PacketChunkData.Extracted expected=net.minecraft.network.play.server.S21PacketChunkData.func_149269_a(chunk,true,0xFFFFFF);
                byte[] received=(byte[])payload.get(decoded);
                check(received.length>=expected.field_150282_a.length && java.util.Arrays.equals(expected.field_150282_a,java.util.Arrays.copyOf(received,expected.field_150282_a.length)),"compressed chunk wire payload differs");
            } finally {buffer.release();}
            System.out.println("EARTH_IMPORT_PACKET PASSED: 24-bit section mask and compressed chunk payload roundtrip");
            Chunk second=world.getChunkFromBlockCoords(x+16,z);world.setBlock(x+16,300,z,ModBlocks.INFESTED_DEEPSLATE.get(),0,2);
            net.minecraft.network.play.server.S26PacketMapChunkBulk bulk=new net.minecraft.network.play.server.S26PacketMapChunkBulk(java.util.Arrays.asList(chunk,second));
            net.minecraft.network.PacketBuffer bulkBuffer=new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());
            try {
                bulk.writePacketData(bulkBuffer);
                net.minecraft.network.play.server.S26PacketMapChunkBulk received=new net.minecraft.network.play.server.S26PacketMapChunkBulk();received.readPacketData(bulkBuffer);
                java.lang.reflect.Field masks=received.getClass().getDeclaredField("field_149265_c");masks.setAccessible(true);
                java.lang.reflect.Field data=received.getClass().getDeclaredField("field_149260_f");data.setAccessible(true);
                int[] bits=(int[])masks.get(received);byte[][] decoded=(byte[][])data.get(received);
                check(bits.length==2 && (bits[0]&(1<<23))!=0 && (bits[1]&(1<<18))!=0,"bulk high section mask");
                check(java.util.Arrays.equals(decoded[0],net.minecraft.network.play.server.S21PacketChunkData.func_149269_a(chunk,true,0xFFFFFF).field_150282_a),"first bulk payload");
                check(java.util.Arrays.equals(decoded[1],net.minecraft.network.play.server.S21PacketChunkData.func_149269_a(second,true,0xFFFFFF).field_150282_a),"second bulk payload / MSB block IDs");
            } finally {bulkBuffer.release();}
            System.out.println("EARTH_IMPORT_BULK_PACKET PASSED: two chunks, high section masks, high block IDs and compressed payloads");
            boolean incompatible=false;
            try {ConfigMapCompatibility.mapCompatibilityMode=true;ConfigMapCompatibility.applyCompatibilityOverrides();}
            catch(IllegalStateException expected){incompatible=true;}
            finally {ConfigMapCompatibility.mapCompatibilityMode=false;}
            check(incompatible && ConfigWorld.extendedWorldHeight,"conflicting profiles accepted or mutated settings");
            Map<Integer,ImportedBiomeVolume.Section> sections=new LinkedHashMap<>();
            sections.put(-2,new ImportedBiomeVolume.Section(new String[]{"minecraft:lush_caves"},new byte[64]));
            sections.put(16,new ImportedBiomeVolume.Section(new String[]{"minecraft:cherry_grove"},new byte[64]));
            ImportedBiomeData.attach(chunk,new ImportedBiomeVolume(sections));
            chunk.getBiomeArray()[0]=(byte)cherry.biomeID;chunk.isTerrainPopulated=true;chunk.setChunkModified();
            check("minecraft:lush_caves".equals(ImportedBiomeData.sourceAt(world,x,32,z)),"lower source identity");
            check(ImportedBiomeData.biomeAt(world,x,320,z)==cherry,"3D named surface resolution");
            check(ImportedBiomeData.sourceAt(world,20000000,32,20000000)==null,"query generated absent chunk");
            // Collision validation must reject before overwriting a vanilla biome.
            Map<String,Integer> collision=new LinkedHashMap<>(ConfigImportedBiomes.IDS);
            collision.put(ImportedBiome.CHERRY_GROVE.sourceName(),BiomeGenBase.plains.biomeID);
            boolean occupied=false;try {ImportedBiomes.validateAllocation(collision);}catch(IllegalStateException expected){occupied=true;}
            check(occupied && BiomeGenBase.getBiomeGenArray()[1]==BiomeGenBase.plains,"occupied ID accepted or overwritten");
            collision.put(ImportedBiome.CHERRY_GROVE.sourceName(),255);
            boolean sentinel=false;try {ImportedBiomes.validateAllocation(collision);}catch(IllegalStateException expected){sentinel=true;}
            check(sentinel,"legacy unset biome sentinel accepted as imported ID");
            // Registry is startup-only and must not permit repeated calls.
            boolean rejected=false;try {ImportedBiomes.register();}catch(IllegalStateException expected){rejected=true;}
            check(rejected && BiomeGenBase.getBiomeGenArray()[cherry.biomeID]==cherry,"repeat registration corrupted registry");
            for (WorldServer other:net.minecraftforge.common.DimensionManager.getWorlds())
                if (other.provider.dimensionId!=0) check(other.getHeight()!=384,"Earth height leaked to dimension "+other.provider.dimensionId);
            RawRetirementProbe.run(world);
            if (Loader.isModLoaded("hbm")) {
                NtmDustProbe.run(world);
                check(world.provider.getClass().getName().equals("com.hbm.dim.WorldProviderEarth"),"NTM Earth provider was replaced");
                check(ModBlocks.DEEPSLATE_HBM_ORE.isEnabled(),"NTM variant registry unavailable");
                BlockDeepslateHBMOre wrapper=(BlockDeepslateHBMOre)ModBlocks.DEEPSLATE_HBM_ORE.get();
                NtmToolProbe.run(world, wrapper);
                for (int meta=0;meta<16;meta++) {
                    Block nativeOre=wrapper.getBase(meta);check(nativeOre!=null,"native ore "+meta);
                    net.minecraft.inventory.InventoryCrafting grid = new net.minecraft.inventory.InventoryCrafting(new net.minecraft.inventory.Container() {
                        @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {return true;}
                    },3,3);
                    grid.setInventorySlotContents(0,new ItemStack(wrapper,1,meta));
                    ItemStack converted=net.minecraft.item.crafting.CraftingManager.getInstance().findMatchingRecipe(grid,world);
                    check(ItemStack.areItemStacksEqual(converted,new ItemStack(nativeOre)),"silk ore processing conversion "+meta);
                    for(int fortune:new int[]{0,3}) {
                        world.setBlock(144,80,144,wrapper,meta,2);world.rand.setSeed(8675309L);
                        ArrayList<ItemStack> actual=wrapper.getDrops(world,144,80,144,meta,fortune);
                        world.setBlock(144,80,144,nativeOre,0,2);world.rand.setSeed(8675309L);
                        ArrayList<ItemStack> expected=nativeOre.getDrops(world,144,80,144,0,fortune);
                        net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent harvest = new net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent(
                                144,80,144,world,wrapper,meta,fortune,1F,actual,null,false);
                        ganymedes01.etfuturum.core.handlers.ServerEventHandler.INSTANCE.harvestEvent(harvest);
                        check(actual.size()==expected.size(),"NTM drop count "+meta);
                        for(int n=0;n<actual.size();n++) check(ItemStack.areItemStacksEqual(ganymedes01.etfuturum.compat.NtmOreCompat.processingInput(actual.get(n)),expected.get(n)),"NTM drop payload "+meta);
                    }
                }
                check(ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems.enableRawOres,"raw-ore regression test requires raw ores enabled");
                check(!ganymedes01.etfuturum.api.RawOreRegistry.hasOre("oreTitanium"),"retired raw titanium mapping remains");
                for (String name:new String[]{"cluster_iron","cluster_titanium","cluster_aluminium","cluster_copper","cluster_depth_iron","cluster_depth_titanium","cluster_depth_tungsten","ore_basalt","ore_uranium","ore_asbestos"}) {
                    Block nativeBlock=cpw.mods.fml.common.registry.GameRegistry.findBlock("hbm","tile."+name);
                    check(nativeBlock!=null,"native special ore missing "+name);
                    for(int meta=0;meta<16;meta++) {
                        ganymedes01.etfuturum.api.mappings.BlockMetaPair pair=ganymedes01.etfuturum.api.DeepslateOreRegistry.getOre(nativeBlock,meta);
                        boolean deepslate=meta==0 && !name.equals("ore_basalt") && !name.startsWith("cluster_depth_");
                        check(pair!=null && (deepslate ? pair.get() instanceof ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateNtmOre && pair.getMeta()==0 : pair.get()==nativeBlock && pair.getMeta()==meta),"special ore metadata replaced "+name+":"+meta);
                    }
                    for(int meta=0;meta<(name.equals("ore_basalt")?5:1);meta++) {
                        world.setBlock(144,80,144,nativeBlock,meta,2);world.rand.setSeed(1234L);
                        ArrayList<ItemStack> drops=nativeBlock.getDrops(world,144,80,144,meta,3);
                        ArrayList<ItemStack> before=new ArrayList<>();for(ItemStack drop:drops)before.add(drop.copy());
                        ganymedes01.etfuturum.core.handlers.ServerEventHandler.INSTANCE.harvestEvent(new net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent(144,80,144,world,nativeBlock,meta,3,1F,drops,null,false));
                        check(before.size()==drops.size(),"special ore drop count "+name);
                        for(int i=0;i<before.size();i++)check(ItemStack.areItemStacksEqual(before.get(i),drops.get(i)),"special ore harvest changed "+name);
                    }
                }
                check(ganymedes01.etfuturum.compat.NtmOreCompat.hasOnlyNtmSources("oreTitanium"),"NTM-only duplicate not hidden");
                net.minecraftforge.oredict.OreDictionary.registerOre("oreTitanium",new ItemStack(Blocks.gold_ore));
                check(!ganymedes01.etfuturum.compat.NtmOreCompat.hasOnlyNtmSources("oreTitanium"),"other mod's generic ore hidden");
                // Vanilla raw-ore behaviour must remain enabled.
                ArrayList<ItemStack> iron=new ArrayList<>();iron.add(new ItemStack(Blocks.iron_ore));
                ganymedes01.etfuturum.core.handlers.ServerEventHandler.INSTANCE.harvestEvent(new net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent(144,80,144,world,Blocks.iron_ore,0,0,1F,iron,null,false));
                check(iron.get(0).getItem()==ganymedes01.etfuturum.ModItems.RAW_ORE.get(),"vanilla raw iron regressed");
                System.out.println("EARTH_IMPORT_NTM_HARVEST PASSED: global harvest, all native special metadata, clusters, basalt, retired raw forms and mixed-mod menu");
                System.out.println("EARTH_IMPORT_NTM_ORES PASSED: 16 native drop variants at fortune 0 and 3, native provider, other dimensions retain height");
            }
            File report=ImportCapabilities.export(world);check(report.isFile(),"capability export missing");
            System.out.println("EARTH_IMPORT_PROVIDER "+world.provider.getClass().getName());
            world.saveAllChunks(true,null);new File("earth-import-fixture-created").createNewFile();
            System.out.println("EARTH_IMPORT_PROBE PASSED");
        } catch(Throwable failure) {failure.printStackTrace();System.out.println("EARTH_IMPORT_PROBE FAILED");throw new RuntimeException(failure);}
        finally {server.initiateShutdown();}
    }
}
