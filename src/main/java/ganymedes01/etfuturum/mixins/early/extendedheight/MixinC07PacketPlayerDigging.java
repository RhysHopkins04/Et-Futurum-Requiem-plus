package ganymedes01.etfuturum.mixins.early.extendedheight;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;

/** The digging packet's Y coordinate is an unsigned byte in vanilla; 384 height needs an unsigned short. */
@Mixin(C07PacketPlayerDigging.class)
public class MixinC07PacketPlayerDigging {

    @Shadow private int field_149511_a;
    @Shadow private int field_149509_b;
    @Shadow private int field_149510_c;
    @Shadow private int field_149507_d;
    @Shadow private int field_149508_e;

    /** @author Et Futurum Requiem Plus @reason Extend packet Y from 8 to 16 bits. */
    @Overwrite
    public void readPacketData(PacketBuffer data) throws IOException {
        this.field_149508_e = data.readUnsignedByte();
        this.field_149511_a = data.readInt();
        this.field_149509_b = data.readUnsignedShort();
        this.field_149510_c = data.readInt();
        this.field_149507_d = data.readUnsignedByte();
    }

    /** @author Et Futurum Requiem Plus @reason Extend packet Y from 8 to 16 bits. */
    @Overwrite
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeByte(this.field_149508_e);
        data.writeInt(this.field_149511_a);
        data.writeShort(this.field_149509_b);
        data.writeInt(this.field_149510_c);
        data.writeByte(this.field_149507_d);
    }
}
