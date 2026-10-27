package ganymedes01.etfuturum.network.profile;

import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import io.netty.buffer.ByteBuf;

/** Client readers/writers use the negotiated peer; server readers/writers use only local settings. */
public final class ProfilePacketFormat {
    public static boolean serverExtended() {
        return ConfigWorld.extendedWorldHeight && ConfigMapCompatibility.allowsExtendedHeight();
    }
    public static boolean clientExtended() { return ClientWorldProfile.server().extended; }
    public static int readMask(ByteBuf data, boolean extended) {
        int mask = extended ? data.readInt() : data.readUnsignedShort();
        if ((mask & ~(extended ? ganymedes01.etfuturum.core.utils.WorldHeightCompat.sectionMask(ClientWorldProfile.server().height / 16) : 0xffff)) != 0)
            throw new IllegalArgumentException("Invalid EFR chunk section mask");
        return mask;
    }
    public static void writeMask(ByteBuf data, int mask, boolean extended) {
        if (extended) data.writeInt(mask); else data.writeShort(mask);
    }
    public static int readY(ByteBuf data, boolean extended) {
        return extended ? data.readUnsignedShort() : data.readUnsignedByte();
    }
    public static void writeY(ByteBuf data, int y, boolean extended) {
        if (extended) data.writeShort(y); else data.writeByte(y);
    }
    private ProfilePacketFormat() {}
}
