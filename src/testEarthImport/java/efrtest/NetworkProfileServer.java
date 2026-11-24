package efrtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.world.imported.ImportedBiomes;
import java.util.Arrays;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.Chunk;

/** Local-only multiplayer fixture, intentionally separate from save/restart probes. */
public final class NetworkProfileServer {
    static final int BED_X = 8;
    static final int BED_Z = 9;
    static final int LEGACY_BED_Y = 180;
    private final int bedY;
    private int ticks;

    private NetworkProfileServer(int bedY) { this.bedY = bedY; }

    static void start(WorldServer world) {
        int y = ConfigMapCompatibility.importedEarthMode ? 320 : LEGACY_BED_Y - 1;
        for (int x = 6; x <= 10; x++) for (int z = 6; z <= 10; z++) world.setBlock(x,y,z,Blocks.diamond_block,0,3);
        world.setBlock(BED_X, y + 1, BED_Z - 1, Blocks.bed, 0, 3);
        world.setBlock(BED_X, y + 1, BED_Z, Blocks.bed, 8, 3);
        world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        world.setWorldTime(18000);
        Chunk chunk = world.getChunkFromChunkCoords(0,0);
        Arrays.fill(chunk.getBiomeArray(), (byte)(ConfigMapCompatibility.importedEarthMode
            ? ImportedBiomes.resolve("minecraft:cherry_grove").biomeID : 1));
        chunk.setChunkModified();
        world.setSpawnLocation(8,y+1,8);
        FMLCommonHandler.instance().bus().register(new NetworkProfileServer(y + 1));
        System.out.println("PROFILE_NETWORK_SERVER READY " + y);
    }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        player.setGameType(WorldSettings.GameType.CREATIVE);
        player.playerNetServerHandler.setPlayerLocation(BED_X + 0.5D, bedY, BED_Z - 1.5D, 0.0F, 0.0F);
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && (++ticks > 6000 || new java.io.File("PROFILE_STOP").exists())) MinecraftServer.getServer().initiateShutdown();
    }
}
