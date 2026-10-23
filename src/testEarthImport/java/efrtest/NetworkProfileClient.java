package efrtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.network.profile.ClientWorldProfile;
import ganymedes01.etfuturum.world.imported.ImportedSurfaceBiome;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.init.Blocks;
import net.minecraft.world.ChunkCache;
import static efrtest.EarthImportContracts.check;

/** Runs real Forge TCP login with a default-config client: Earth -> legacy -> Earth, without restart. */
public final class NetworkProfileClient {
    private int ticks, phase, wait;
    public static void start() { System.out.println("PROFILE_NETWORK_CLIENT STARTING"); FMLCommonHandler.instance().bus().register(new NetworkProfileClient()); }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (new java.io.File("PROFILE_STOP").exists()) { mc.shutdown(); return; }
        try {
            if (ticks == 0) System.out.println("PROFILE_NETWORK_CLIENT FIRST TICK");
            if (++ticks > 4000) throw new AssertionError("Multiplayer profile probe timed out, phase " + phase);
            if (++wait < 30) return;
            if ((phase & 1) == 0) {
                check(mc.theWorld == null, "old world still loaded");
                check(!ClientWorldProfile.server().extended && ClientWorldProfile.biome(210) == null, "profile leaked after disconnect");
                check(!ConfigMapCompatibility.importedEarthMode && !ConfigWorld.extendedWorldHeight, "client settings changed");
                int port = phase == 2 ? 25579 : 25578;
                cpw.mods.fml.client.FMLClientHandler.instance().setupServerList();
                cpw.mods.fml.client.FMLClientHandler.instance().connectToServer(new GuiMainMenu(), new ServerData("Profile test", "127.0.0.1:" + port));
                phase++; wait = 0; return;
            }
            if (mc.theWorld == null) return;
            boolean earth = phase != 3;
            int y = earth ? 320 : 100;
            if (mc.theWorld.getBlock(8,y,8) != Blocks.diamond_block) return;
            check(mc.theWorld.getHeight() == (earth ? 384 : 256), "wrong negotiated world height");
            check(mc.theWorld.getChunkFromChunkCoords(0,0).getBlockStorageArray().length == (earth ? 24 : 16), "wrong chunk storage");
            check(ClientWorldProfile.server().imported == earth, "wrong session profile");
            check((mc.theWorld.getBiomeGenForCoords(8,8) instanceof ImportedSurfaceBiome) == earth, "server biome lookup failed");
            if (earth) {
                check(mc.theWorld.getBiomeGenForCoords(8,8).biomeID == 209, "server biome allocation was not applied");
                check(ganymedes01.etfuturum.configuration.configs.ConfigImportedBiomes.IDS.get("minecraft:cherry_grove") == 210, "client biome config was changed");
            }
            ChunkCache cache = new ChunkCache(mc.theWorld,0,0,0,15,y+1,15,0);
            check(cache.getHeight() == (earth ? 384 : 256), "cache height mismatch");
            check(cache.getBlock(8,y,8) == Blocks.diamond_block, "upper block invisible to renderer cache");
            check(!ConfigMapCompatibility.importedEarthMode && !ConfigWorld.extendedWorldHeight, "downloaded profile changed local config");
            System.out.println("PROFILE_NETWORK_CLIENT SESSION PASSED " + phase);
            mc.theWorld.sendQuittingDisconnectingPacket(); mc.loadWorld(null); mc.displayGuiScreen(new GuiMainMenu());
            if (phase == 5) {
                System.out.println("PROFILE_NETWORK_CLIENT PASSED: Earth -> legacy -> Earth with default client config");
                mc.shutdown();
            } else { phase++; wait = 0; }
        } catch (Throwable failure) {
            failure.printStackTrace(); System.out.println("PROFILE_NETWORK_CLIENT FAILED"); mc.shutdown();
        }
    }
}
