package efrtest.movement;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import ganymedes01.etfuturum.movement.MovementMode;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S28PacketEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;

import java.io.File;

@Mod(modid = "efr_movement_fixture", version = "1", acceptableRemoteVersions = "*", dependencies = "required-after:etfuturum")
public final class MovementFixture {
    static final int Y = 80;

    @Mod.EventHandler
    public void load(FMLLoadCompleteEvent event) throws Exception {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            Class.forName("efrtest.movement.MovementFixtureClient").getMethod("start").invoke(null);
        }
    }

    @Mod.EventHandler
    public void start(FMLServerStartedEvent event) {
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        world.setBlock(0, Y - 1, 0, Blocks.stone);
        for (int x = 3; x <= 20; x++) {
            for (int z = -2; z <= 2; z++) world.setBlock(x, Y - 1, z, Blocks.stone);
        }
        for (int x = 21; x <= 58; x++) world.setBlock(x, Y - 1, -2, Blocks.stone);
        world.setBlock(7, Y + 1, 0, Blocks.stone_slab, 8, 3);
        world.setBlock(9, Y + 1, 0, Blocks.stone);
        for (int x = 12; x <= 14; x++) {
            for (int y = Y; y <= Y + 2; y++) {
                for (int z = -1; z <= 1; z++) world.setBlock(x, y, z, Blocks.water);
            }
        }
        world.setBlock(16, Y - 1, 0, Blocks.stone_slab, 0, 3);
        world.setBlock(16, Y, 0, Blocks.stone_slab, 8, 3);
        require(NativeWaterlogging.fill(world, 16, Y - 1, 0), "fixture could not waterlog floor slab");
        require(NativeWaterlogging.fill(world, 16, Y, 0), "fixture could not waterlog ceiling slab");
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        System.out.println("MOVEMENT_FIXTURE_SERVER_READY " + MovementMode.local());
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && new File("FIXTURE_STOP").isFile())
            MinecraftServer.getServer().initiateShutdown();
    }

    @SubscribeEvent
    public void command(ServerChatEvent event) {
        if (!event.message.startsWith("fixture-")) return;
        event.setCanceled(true);
        EntityPlayerMP player = event.player;
        double y = Y;
        switch (event.message) {
            case "fixture-check-bridge":
                require(player.worldObj.getBlock(1, Y - 1, 0) == Blocks.cobblestone, "bridge placement missing on server");
                System.out.println("MOVEMENT_FIXTURE_SERVER_BRIDGE x=" + player.posX + " height=" + player.height
                        + " eye=" + (player.posY + player.getEyeHeight() - player.boundingBox.minY));
                break;
            case "fixture-correct-bridge":
                player.playerNetServerHandler.setPlayerLocation(1.19D, y, 0.5D, 90F, player.rotationPitch);
                player.playerNetServerHandler.sendPacket(new S28PacketEffect(1000, 1, Y, 0, 0, false));
                break;
            case "fixture-diagonal":
                player.worldObj.setBlockToAir(1, Y - 1, 0);
                player.playerNetServerHandler.setPlayerLocation(0.5D, y, 0.5D, -45F, 0);
                break;
            case "fixture-bridge":
                player.worldObj.setBlockToAir(1, Y - 1, 0);
                player.inventory.currentItem = 0;
                player.inventory.setInventorySlotContents(0, new ItemStack(Blocks.cobblestone, 32));
                player.playerNetServerHandler.setPlayerLocation(0.5D, y, 0.5D, -90F, 0);
                break;
            case "fixture-slab":
                player.playerNetServerHandler.setPlayerLocation(6.5D, y, 0.5D, -90F, 0);
                break;
            case "fixture-crawl":
                player.playerNetServerHandler.setPlayerLocation(9.5D, y, 0.5D, -90F, 0);
                break;
            case "fixture-swim":
                player.playerNetServerHandler.setPlayerLocation(12.5D, y + 0.15D, 0.5D, -90F, 0);
                break;
            case "fixture-waterlogged":
                player.playerNetServerHandler.setPlayerLocation(16.5D, y - 0.5D, 0.5D, -90F, 0);
                break;
            case "fixture-elytra":
                player.setCurrentItemOrArmor(3, ModItems.ELYTRA.newItemStack());
                player.playerNetServerHandler.setPlayerLocation(4.5D, y + 12.0D, -1.5D, -90F, 23);
                break;
            case "fixture-observe":
                player.playerNetServerHandler.setPlayerLocation(3.5D, y, 1.5D, 0, 0);
                break;
            default:
                return;
        }
        System.out.println("MOVEMENT_FIXTURE_SERVER " + event.message + " " + player.getCommandSenderName());
    }

    static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    static void near(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < 0.003, message + " actual=" + actual + " expected=" + expected);
    }
}
