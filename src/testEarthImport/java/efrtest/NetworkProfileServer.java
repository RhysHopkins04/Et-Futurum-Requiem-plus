package efrtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.world.imported.ImportedBiomes;
import java.util.Arrays;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

/** Local-only multiplayer fixture, intentionally separate from save/restart probes. */
public final class NetworkProfileServer {
    private int ticks;
    static void start(WorldServer world) {
        int y = ConfigMapCompatibility.importedEarthMode ? 320 : 100;
        for (int x = 6; x <= 10; x++) for (int z = 6; z <= 10; z++) world.setBlock(x,y,z,Blocks.diamond_block,0,3);
        Chunk chunk = world.getChunkFromChunkCoords(0,0);
        Arrays.fill(chunk.getBiomeArray(), (byte)(ConfigMapCompatibility.importedEarthMode
            ? ImportedBiomes.resolve("minecraft:cherry_grove").biomeID : 1));
        chunk.setChunkModified();
        world.setSpawnLocation(8,y+1,8);
        FMLCommonHandler.instance().bus().register(new NetworkProfileServer());
        System.out.println("PROFILE_NETWORK_SERVER READY " + y);
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && (++ticks > 6000 || new java.io.File("PROFILE_STOP").exists())) MinecraftServer.getServer().initiateShutdown();
    }
}
