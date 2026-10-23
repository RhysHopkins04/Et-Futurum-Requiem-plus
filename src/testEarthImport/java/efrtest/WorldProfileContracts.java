package efrtest;

import ganymedes01.etfuturum.network.profile.ServerWorldProfile;
import ganymedes01.etfuturum.network.profile.ProfilePacketFormat;
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
            check(ServerWorldProfile.read(Unpooled.EMPTY_BUFFER).equals(ServerWorldProfile.LEGACY), "old peer fallback");
            for (int length = 1; length < bytes.length; length++) {
                ByteBuf truncated = Unpooled.wrappedBuffer(Arrays.copyOf(bytes, length));
                try { rejects(() -> ServerWorldProfile.read(truncated)); } finally { truncated.release(); }
            }
            byte[] future = bytes.clone(); future[4] = 2;
            ByteBuf bad = Unpooled.wrappedBuffer(future);
            try { rejects(() -> ServerWorldProfile.read(bad)); } finally { bad.release(); }
            rejects(() -> new ServerWorldProfile(false, true, Collections.emptyMap()));
            Map<String,Integer> duplicate = new LinkedHashMap<>(); duplicate.put("minecraft:grove",210); duplicate.put("minecraft:cherry_grove",210);
            rejects(() -> new ServerWorldProfile(true,true,duplicate));
            rejects(() -> new ServerWorldProfile(true,true,Collections.singletonMap("minecraft:unknown",210)));
            rejects(() -> new ServerWorldProfile(true,true,Collections.singletonMap("minecraft:grove",255)));
            for (boolean extended : new boolean[]{false,true}) {
                wire.clear();
                ProfilePacketFormat.writeMask(wire, extended ? 0x800001 : 0x8001, extended);
                ProfilePacketFormat.writeY(wire, extended ? 383 : 255, extended);
                check(wire.readableBytes() == (extended ? 6 : 3), "wire width");
                check(ProfilePacketFormat.readMask(wire,extended) == (extended ? 0x800001 : 0x8001), "section mask lost");
                check(ProfilePacketFormat.readY(wire,extended) == (extended ? 383 : 255), "Y lost");
            }
        } finally { wire.release(); }
        System.out.println("WORLD_PROFILE_CONTRACTS PASSED");
    }
    private static void rejects(Runnable operation) {
        try { operation.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid profile accepted");
    }
}
