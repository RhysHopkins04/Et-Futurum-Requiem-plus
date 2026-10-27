package efrtest;

import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.network.profile.*;
import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import static efrtest.EarthImportContracts.check;

/** Real transformed storage, provider, stale server-property ceiling, and compressed packet boundary checks. */
final class ConfiguredHeightProbe {
    enum Click { LEFT_CLICK_BLOCK, RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR }
    static void run(WorldServer world) throws Exception {
        int height = ConfigWorld.extendedWorldHeightBlocks;
        check(world.getHeight() == height, "server height");
        check(net.minecraft.server.MinecraftServer.getServer().getBuildLimit() == height, "stale max-build-height=256 won");
        ClientWorldProfile.accept(ServerWorldProfile.local());
        check(WorldHeightCompat.clientHeight() == height, "negotiated client height");
        Chunk chunk = world.getChunkFromChunkCoords(0, 0);
        java.io.File marker = new java.io.File("height-fixture-created");
        if (marker.isFile()) check(world.getBlock(2, height - 1, 2) == Blocks.stone, "top block lost across restart");
        check(chunk.getBlockStorageArray().length == height / 16, "section array size");
        for (int y : new int[]{255, 256, 383, height - 1}) {
            check(world.setBlock(2, y, 2, Blocks.stone, 0, 3) || world.getBlock(2, y, 2) == Blocks.stone, "upper write " + y);
            check(world.getBlock(2, y, 2) == Blocks.stone, "upper read " + y);
        }
        check(!world.setBlock(2, height, 2, Blocks.stone), "write above ceiling accepted");
        PacketBuffer wire = new PacketBuffer(Unpooled.buffer());
        try {
            S21PacketChunkData sent = new S21PacketChunkData(chunk, true, WorldHeightCompat.sectionMask(height / 16));
            sent.writePacketData(wire);
            check((wire.getInt(9) & (1 << (height / 16 - 1))) != 0, "S21 top section bit lost");
            S21PacketChunkData received = new S21PacketChunkData(); received.readPacketData(wire);
            check(!wire.isReadable(), "S21 trailing bytes");
            ConfigWorld.extendedWorldHeight = false;
            try {
                wire.clear(); received.writePacketData(wire);
                check((wire.getInt(9) & (1 << (height / 16 - 1))) != 0, "ReplayMod serialization narrowed chunk masks using client config");
            } finally { ConfigWorld.extendedWorldHeight = true; }

            wire.clear();
            new net.minecraft.network.play.server.S28PacketEffect(2001, 2, height - 1, 2, 1, false).writePacketData(wire);
            check(wire.readableBytes() == 19 && wire.getUnsignedShort(8) == height - 1, "effect Y truncated");
            net.minecraft.network.play.server.S28PacketEffect effect = new net.minecraft.network.play.server.S28PacketEffect();
            effect.readPacketData(wire);
            check(!wire.isReadable(), "effect trailing bytes");
            ConfigWorld.extendedWorldHeight = false;
            try { wire.clear(); effect.writePacketData(wire); check(wire.readableBytes() == 19, "recorded effect width changed"); }
            finally { ConfigWorld.extendedWorldHeight = true; }
            wire.clear();
            ClientWorldProfile.clear();
            try {
                wire.writeInt(2001).writeInt(2).writeByte(255).writeInt(2).writeInt(1).writeBoolean(false);
                net.minecraft.network.play.server.S28PacketEffect legacyEffect = new net.minecraft.network.play.server.S28PacketEffect();
                legacyEffect.readPacketData(wire); wire.clear(); legacyEffect.writePacketData(wire);
                check(wire.readableBytes() == 18, "legacy recorded effect widened using local config");
            } finally { ClientWorldProfile.accept(ServerWorldProfile.local()); }
            wire.clear();
            new S26PacketMapChunkBulk(Collections.singletonList(chunk)).writePacketData(wire);
            check((wire.getInt(7 + wire.getInt(2) + 8) & (1 << (height / 16 - 1))) != 0, "S26 top section bit lost");
            S26PacketMapChunkBulk bulk = new S26PacketMapChunkBulk(); bulk.readPacketData(wire);
            check(!wire.isReadable(), "S26 trailing bytes");
            ConfigWorld.extendedWorldHeight = false;
            try {
                wire.clear(); bulk.writePacketData(wire);
                check((wire.getInt(7 + wire.getInt(2) + 8) & (1 << (height / 16 - 1))) != 0, "recorded bulk masks narrowed");
            } finally { ConfigWorld.extendedWorldHeight = true; }

        } finally { wire.release(); }
        net.minecraftforge.common.util.FakePlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(world);
        player.theItemInWorldManager.setGameType(net.minecraft.world.WorldSettings.GameType.CREATIVE);
        net.minecraft.network.NetHandlerPlayServer connection = new net.minecraft.network.NetHandlerPlayServer(
                net.minecraft.server.MinecraftServer.getServer(), new net.minecraft.network.NetworkManager(false), player) {
            @Override public void sendPacket(net.minecraft.network.Packet packet) { }
        };
        player.playerNetServerHandler = connection;
        for (Click action : new Click[]{Click.LEFT_CLICK_BLOCK,Click.RIGHT_CLICK_BLOCK})
            check(ganymedes01.etfuturum.core.utils.CrucibleHeightCompat.maximumInteractionY(player,action,255) == height - 1,
                    "Crucible bridge did not use actual world height");
        check(ganymedes01.etfuturum.core.utils.CrucibleHeightCompat.maximumInteractionY(player,Click.RIGHT_CLICK_AIR,255) == 255,
                "Crucible AIR sentinel changed");
        ConfigWorld.extendedWorldHeight = false;
        try { check(ganymedes01.etfuturum.core.utils.CrucibleHeightCompat.maximumInteractionY(player,Click.RIGHT_CLICK_BLOCK,255) == 255,
                "Crucible bridge changed legacy mode"); }
        finally { ConfigWorld.extendedWorldHeight = true; }

        for (int target : new int[]{256, 384, height - 1}) {
            if (target >= height) continue;
            world.setBlock(4, target - 1, 4, Blocks.stone, 0, 3);
            world.setBlockToAir(4, target, 4);
            player.setPosition(4.5, target, 5.5);
            player.inventory.mainInventory[player.inventory.currentItem] = new net.minecraft.item.ItemStack(Blocks.stone, 64);
            PacketBuffer request = new PacketBuffer(Unpooled.buffer());
            try {
                request.writeInt(4).writeShort(target - 1).writeInt(4).writeByte(1);
                request.writeItemStackToBuffer(player.inventory.getCurrentItem());
                request.writeByte(8).writeByte(8).writeByte(8);
                net.minecraft.network.play.client.C08PacketPlayerBlockPlacement placement = new net.minecraft.network.play.client.C08PacketPlayerBlockPlacement();
                placement.readPacketData(request); connection.processPlayerBlockPlacement(placement);
            } finally { request.release(); }
            check(world.getBlock(4, target, 4) == Blocks.stone, "actual placement packet rejected at " + target);
        }
        // Exercise transformed Anvil methods, not a parallel test codec.
        net.minecraft.world.chunk.storage.AnvilChunkLoader loader = new net.minecraft.world.chunk.storage.AnvilChunkLoader(new java.io.File("height-roundtrip"));
        java.lang.reflect.Method write = loader.getClass().getDeclaredMethod("writeChunkToNBT", Chunk.class, net.minecraft.world.World.class, NBTTagCompound.class);
        java.lang.reflect.Method read = loader.getClass().getDeclaredMethod("readChunkFromNBT", net.minecraft.world.World.class, NBTTagCompound.class);
        write.setAccessible(true); read.setAccessible(true);
        NBTTagCompound stored = new NBTTagCompound(); write.invoke(loader, chunk, world, stored);
        Chunk restored = (Chunk)read.invoke(loader, world, stored);
        check(restored.getBlockStorageArray().length == height / 16, "Anvil storage length");
        check(restored.getBlock(2, height - 1, 2) == Blocks.stone, "Anvil top block lost");
        WorldServer nether = net.minecraft.server.MinecraftServer.getServer().worldServerForDimension(-1);
        check(nether.getHeight() == 256 && nether.getChunkFromChunkCoords(0,0).getBlockStorageArray().length == 16, "Overworld height leaked into Nether");
        world.saveAllChunks(true, null);
        marker.createNewFile();
        System.out.println("CONFIGURED_HEIGHT_PROBE PASSED: " + height + " blocks; provider, build limit, storage, bounds, S21/S26 top bits, placement packets, Anvil roundtrip, Nether isolation");
    }
}
