package efrtest;

import ganymedes01.etfuturum.network.profile.ServerWorldProfile;
import ganymedes01.etfuturum.network.profile.ProfilePacketFormat;
import ganymedes01.etfuturum.network.profile.ClientWorldProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.*;
import static efrtest.EarthImportContracts.check;

final class WorldProfileContracts {
    static void run() {
        Map<String,Integer> ids = new LinkedHashMap<>();
        ids.put("minecraft:cherry_grove", 214); ids.put("minecraft:grove", 210);
        ServerWorldProfile original = new ServerWorldProfile(true, true, ids);
        ids.clear(); check(original.biomes.size() == 2, "mutable profile map");
        ByteBuf wire = Unpooled.buffer();
        try {
            original.write(wire);
            byte[] bytes = new byte[wire.readableBytes()]; wire.getBytes(0, bytes);
            check(original.equals(ServerWorldProfile.read(wire)), "profile round trip");
            // V1 recordings and old servers keep their exact byte layout when acknowledged.
            ByteBuf old = Unpooled.buffer().writeInt(0x45465250).writeByte(1).writeByte(1).writeByte(0);
            ByteBuf oldAck = Unpooled.buffer();
            try {
                ServerWorldProfile oldProfile = ServerWorldProfile.read(old);
                check(oldProfile.height == 384 && oldProfile.packetVersion == 1, "v1 profile interpretation");
                oldProfile.write(oldAck);
                check(oldAck.readableBytes() == 7 && oldAck.getUnsignedByte(4) == 1, "v1 acknowledgement changed");
            } finally {old.release(); oldAck.release();}
            check(ServerWorldProfile.read(Unpooled.EMPTY_BUFFER).equals(ServerWorldProfile.LEGACY), "old peer fallback");
            for (int length = 1; length < bytes.length; length++) {
                ByteBuf truncated = Unpooled.wrappedBuffer(Arrays.copyOf(bytes, length));
                try { rejects(() -> ServerWorldProfile.read(truncated)); } finally { truncated.release(); }
            }
            byte[] future = bytes.clone(); future[4] = 3;
            ByteBuf bad = Unpooled.wrappedBuffer(future);
            try { rejects(() -> ServerWorldProfile.read(bad)); } finally { bad.release(); }
            rejects(() -> new ServerWorldProfile(false, true, Collections.emptyMap()));
            Map<String,Integer> duplicate = new LinkedHashMap<>(); duplicate.put("minecraft:grove",210); duplicate.put("minecraft:cherry_grove",210);
            rejects(() -> new ServerWorldProfile(true,true,duplicate));
            rejects(() -> new ServerWorldProfile(true,true,Collections.singletonMap("minecraft:unknown",210)));
            rejects(() -> new ServerWorldProfile(true,true,Collections.singletonMap("minecraft:grove",255)));
            ClientWorldProfile.accept(original);
            for (boolean extended : new boolean[]{false,true}) {
                wire.clear();
                ProfilePacketFormat.writeMask(wire, extended ? 0x800001 : 0x8001, extended);
                ProfilePacketFormat.writeY(wire, extended ? 383 : 255, extended);
                check(wire.readableBytes() == (extended ? 6 : 3), "wire width");
                check(ProfilePacketFormat.readMask(wire,extended) == (extended ? 0x800001 : 0x8001), "section mask lost");
                check(ProfilePacketFormat.readY(wire,extended) == (extended ? 383 : 255), "Y lost");
            }
        } finally { wire.release(); ClientWorldProfile.clear(); }
        for (int height : new int[]{384, 400, 448, 496, 512}) {
            ServerWorldProfile tall = new ServerWorldProfile(true, true, height, Collections.emptyMap());
            ByteBuf data = Unpooled.buffer();
            try {
                tall.write(data);
                check(tall.equals(ServerWorldProfile.read(data)), "height negotiation " + height);
                ClientWorldProfile.accept(tall);
                int mask = ganymedes01.etfuturum.core.utils.WorldHeightCompat.sectionMask(height / 16);
                data.clear(); ProfilePacketFormat.writeMask(data, mask, true);
                check(ProfilePacketFormat.readMask(data, true) == mask, "top mask bit lost " + height);
                data.clear(); ProfilePacketFormat.writeY(data, height - 1, true);
                check(ProfilePacketFormat.readY(data, true) == height - 1, "top Y lost " + height);
            } finally { data.release(); ClientWorldProfile.clear(); }
        }
        for (int height : new int[]{0, 256, 383, 399, 513, 528})
            rejects(() -> new ServerWorldProfile(true, true, height, Collections.emptyMap()));
        System.out.println("WORLD_PROFILE_CONTRACTS PASSED");
    }
    private static void rejects(Runnable operation) {
        try { operation.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid profile accepted");
    }
}
