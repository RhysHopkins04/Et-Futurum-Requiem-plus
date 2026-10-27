package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.network.profile.ClientWorldProfile;
import ganymedes01.etfuturum.network.profile.ProfilePacketFormat;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S28PacketEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import java.io.IOException;

/** Negotiated v2 keeps block-break/door effects at their actual upper-world Y. V1 replays retain byte Y. */
@Mixin(S28PacketEffect.class)
public class MixinS28PacketEffect {
    @Unique private Boolean etfu$receivedExtendedFormat;
    @Unique private boolean etfu$writeExtendedFormat() {
        return etfu$receivedExtendedFormat != null ? etfu$receivedExtendedFormat : ProfilePacketFormat.serverExtended();
    }

    @Shadow private int field_149251_a;
    @Shadow private int field_149249_b;
    @Shadow private int field_149250_c;
    @Shadow private int field_149247_d;
    @Shadow private int field_149248_e;
    @Shadow private boolean field_149246_f;

    /** @author EFR Plus @reason Negotiate the effect packet's vertical coordinate width. */
    @Overwrite public void readPacketData(PacketBuffer data) throws IOException {
        etfu$receivedExtendedFormat = ganymedes01.etfuturum.network.profile.ClientWorldProfile.server().packetVersion >= 2;
        field_149251_a = data.readInt(); field_149250_c = data.readInt();
        field_149247_d = ProfilePacketFormat.readY(data, ClientWorldProfile.server().packetVersion >= 2);
        field_149248_e = data.readInt(); field_149249_b = data.readInt(); field_149246_f = data.readBoolean();
    }
    /** @author EFR Plus @reason Preserve Y above 255 without changing old server/replay reads. */
    @Overwrite public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(field_149251_a); data.writeInt(field_149250_c);
        ProfilePacketFormat.writeY(data, field_149247_d, etfu$writeExtendedFormat());
        data.writeInt(field_149248_e); data.writeInt(field_149249_b); data.writeBoolean(field_149246_f);
    }
}
