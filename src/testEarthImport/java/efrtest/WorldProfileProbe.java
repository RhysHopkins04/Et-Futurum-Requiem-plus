package efrtest;

import cpw.mods.fml.common.network.handshake.FMLHandshakeCodec;
import cpw.mods.fml.common.network.handshake.FMLHandshakeMessage;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.network.profile.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import static efrtest.EarthImportContracts.check;

/** Real transformed hello/packet methods plus world bounds. Runs only in disposable probe worlds. */
final class WorldProfileProbe {
    static void run(WorldServer world) throws Exception {
        boolean extended = ConfigWorld.extendedWorldHeight, imported = ConfigMapCompatibility.importedEarthMode;
        boolean compatibility = ConfigMapCompatibility.mapCompatibilityMode;
        try {
            FMLHandshakeCodec codec = new FMLHandshakeCodec();
            ByteBuf hello = Unpooled.buffer();
            try {
                codec.encodeInto(null, new FMLHandshakeMessage.ServerHello(0), hello);
                codec.decodeInto(null, hello, new FMLHandshakeMessage.ServerHello());
                check(ClientWorldProfile.server().equals(ServerWorldProfile.local()), "transformed Forge hello did not publish profile");
                check(!hello.isReadable(), "hello extension not consumed");
            } finally { hello.release(); }
            ByteBuf ack = Unpooled.buffer();
            try {
                codec.encodeInto(null, new FMLHandshakeMessage.ClientHello(), ack);
                codec.decodeInto(null, ack, new FMLHandshakeMessage.ClientHello());
                check(!ack.isReadable(), "client hello did not acknowledge server profile");
            } finally { ack.release(); }
            if (extended) {
                final String[] rejected = {null};
                cpw.mods.fml.common.network.handshake.NetworkDispatcher dispatcher =
                    new cpw.mods.fml.common.network.handshake.NetworkDispatcher(new net.minecraft.network.NetworkManager(false)) {
                        @Override public void rejectHandshake(String reason) { rejected[0] = reason; }
                    };
                io.netty.channel.embedded.EmbeddedChannel channel = new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
                channel.attr(cpw.mods.fml.common.network.handshake.NetworkDispatcher.FML_DISPATCHER).set(dispatcher);
                ByteBuf oldClient = Unpooled.buffer().writeByte(cpw.mods.fml.common.network.NetworkRegistry.FML_PROTOCOL);
                try {
                    codec.decodeInto(channel.pipeline().firstContext(), oldClient, new FMLHandshakeMessage.ClientHello());
                    check(rejected[0] != null && rejected[0].contains("Update Et Futurum Plus"), "old client was not rejected clearly");
                    check(Boolean.TRUE.equals(channel.attr(ProfileHandshake.REJECTED).get()), "rejected handshake still allowed progress");
                    Class state = Class.forName("cpw.mods.fml.common.network.handshake.FMLHandshakeServerState");
                    channel.pipeline().addFirst(new cpw.mods.fml.common.network.handshake.HandshakeMessageHandler(state));
                    check(!channel.writeInbound(new FMLHandshakeMessage.ClientHello()), "rejected login message escaped gate");
                } finally { oldClient.release(); channel.finish(); }
            }
            Map<String, Integer> swapped = new LinkedHashMap<>();
            swapped.put("minecraft:cherry_grove", 1); // Deliberately conflicts with local plains.
            swapped.put("minecraft:pale_garden", 2);
            ServerWorldProfile earth = new ServerWorldProfile(true, true, swapped);
            BiomeGenBase[] registry = BiomeGenBase.getBiomeGenArray().clone();
            ClientWorldProfile.accept(earth);
            check(ClientWorldProfile.biome(1).biomeName.equals("minecraft:cherry_grove"), "server mapping not authoritative");
            check(Arrays.equals(registry, BiomeGenBase.getBiomeGenArray()), "connection overwrote global biome registry");
            check(ConfigWorld.extendedWorldHeight == extended && ConfigMapCompatibility.importedEarthMode == imported,
                    "negotiation changed local configuration");
            if (extended) {
                check(ClientWorldProfile.biome(1).getFloatTemperature(0,128,0) == .5F, "Earth altitude climate not shifted");
                ClientWorldProfile.accept(new ServerWorldProfile(false,false,swapped));
                check(ClientWorldProfile.biome(1).getFloatTemperature(0,128,0) < .5F, "Earth altitude leaked into legacy biome");
            }
            net.minecraft.network.NetworkManager first = new net.minecraft.network.NetworkManager(false);
            net.minecraft.network.NetworkManager second = new net.minecraft.network.NetworkManager(false);
            ClientWorldProfile.accept(earth, first);
            ClientWorldProfile.disconnected(first); ClientWorldProfile.clearDisconnected();
            check(!ClientWorldProfile.server().extended, "failed login profile retained");
            ClientWorldProfile.accept(earth, second);
            ClientWorldProfile.disconnected(first); ClientWorldProfile.clearDisconnected();
            check(ClientWorldProfile.server().equals(earth), "late disconnect cleared a newer connection");
            if (extended) {
                NtmAnvilProofWorld remote = new NtmAnvilProofWorld(); remote.isRemote = true;
                check(remote.getHeight() == 384, "remote Earth height");
                Chunk high = new Chunk(remote, 0, 0); remote.supply(high);
                check(high.getBlockStorageArray().length == 24, "remote Earth storage");
                check(remote.setBlock(1, 320, 1, Blocks.stone, 0, 0), "remote upper block rejected");
                check(remote.getBlock(1, 320, 1) == Blocks.stone, "remote upper block lost");
                ClientWorldProfile.clear();
                NtmAnvilProofWorld legacy = new NtmAnvilProofWorld(); legacy.isRemote = true;
                check(legacy.getHeight() == 256, "Earth height leaked after disconnect");
                check(new Chunk(legacy, 0, 0).getBlockStorageArray().length == 16, "legacy storage expanded");
                check(!legacy.setBlock(1, 256, 1, Blocks.stone, 0, 0), "legacy upper bound changed");
                check(world.getHeight() == 384, "remote legacy profile changed integrated/server world height");
                // The installed transforms must support both wire formats regardless of startup profile.
                for (boolean mode : new boolean[]{false, true, false, true}) {
                    ConfigWorld.extendedWorldHeight = mode;
                    ConfigMapCompatibility.importedEarthMode = mode;
                    ConfigMapCompatibility.mapCompatibilityMode = false;
                    ClientWorldProfile.accept(mode ? earth : ServerWorldProfile.LEGACY);
                    int y = mode ? 320 : 200;
                    packet(digging(y, mode), new C07PacketPlayerDigging(), mode ? 12 : 11);
                    packet(placement(y, mode), new C08PacketPlayerBlockPlacement(), mode ? 16 : 15);
                    packet(new S23PacketBlockChange(12, y, -5, world), new S23PacketBlockChange(), -1);
                }
            }
            ClientWorldProfile.clear();
            check(ClientWorldProfile.biome(1) == null && !ClientWorldProfile.server().extended, "session mapping survived clear");
            System.out.println("WORLD_PROFILE_PROBE PASSED: Forge hello/ack, temporary biome IDs, registry isolation, session reset, world/packet bounds");
        } finally {
            ConfigWorld.extendedWorldHeight = extended;
            ConfigMapCompatibility.importedEarthMode = imported;
            ConfigMapCompatibility.mapCompatibilityMode = compatibility;
            ClientWorldProfile.accept(ServerWorldProfile.local()); // Existing packet probes emulate the receiving client.
        }
    }
    // The convenience constructors are client-only and stripped by Forge on dedicated servers.
    private static C07PacketPlayerDigging digging(int y, boolean mode) throws Exception {
        PacketBuffer data = new PacketBuffer(Unpooled.buffer());
        try {
            data.writeByte(0).writeInt(12); ProfilePacketFormat.writeY(data,y,mode);
            data.writeInt(-5).writeByte(1);
            C07PacketPlayerDigging packet = new C07PacketPlayerDigging(); packet.readPacketData(data); return packet;
        } finally { data.release(); }
    }
    private static C08PacketPlayerBlockPlacement placement(int y, boolean mode) throws Exception {
        PacketBuffer data = new PacketBuffer(Unpooled.buffer());
        try {
            data.writeInt(12); ProfilePacketFormat.writeY(data,y,mode);
            data.writeInt(-5).writeByte(1); data.writeItemStackToBuffer(null);
            data.writeByte(8).writeByte(8).writeByte(8);
            C08PacketPlayerBlockPlacement packet = new C08PacketPlayerBlockPlacement(); packet.readPacketData(data); return packet;
        } finally { data.release(); }
    }
    private static void packet(Packet sent, Packet received, int expectedBytes) throws Exception {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            sent.writePacketData(buffer);
            if (expectedBytes >= 0) check(buffer.readableBytes() == expectedBytes, "wrong packet size " + sent.getClass());
            byte[] expected = new byte[buffer.readableBytes()]; buffer.getBytes(0, expected);
            received.readPacketData(buffer);
            check(!buffer.isReadable(), "packet not fully consumed");
            buffer.clear(); received.writePacketData(buffer);
            byte[] actual = new byte[buffer.readableBytes()]; buffer.readBytes(actual);
            check(Arrays.equals(expected, actual), "packet Y/data did not round-trip " + sent.getClass());
        } finally { buffer.release(); }
    }
}
