package ganymedes01.etfuturum.mixins.early.extendedheight;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;

/** Extends the placement/activation packet Y coordinate to an unsigned short. */
@Mixin(C08PacketPlayerBlockPlacement.class)
public class MixinC08PacketPlayerBlockPlacement {

    @Shadow private int field_149583_a;
    @Shadow private int field_149581_b;
    @Shadow private int field_149582_c;
    @Shadow private int field_149579_d;
    @Shadow private ItemStack field_149580_e;
    @Shadow private float field_149577_f;
    @Shadow private float field_149578_g;
    @Shadow private float field_149584_h;

    /** @author Et Futurum Requiem Plus @reason Extend packet Y from 8 to 16 bits. */
    @Overwrite
    public void readPacketData(PacketBuffer data) throws IOException {
        this.field_149583_a = data.readInt();
        this.field_149581_b = data.readUnsignedShort();
        this.field_149582_c = data.readInt();
        this.field_149579_d = data.readUnsignedByte();
        this.field_149580_e = data.readItemStackFromBuffer();
        this.field_149577_f = (float)data.readUnsignedByte() / 16.0F;
        this.field_149578_g = (float)data.readUnsignedByte() / 16.0F;
        this.field_149584_h = (float)data.readUnsignedByte() / 16.0F;
    }

    /** @author Et Futurum Requiem Plus @reason Extend packet Y from 8 to 16 bits. */
    @Overwrite
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(this.field_149583_a);
        data.writeShort(this.field_149581_b);
        data.writeInt(this.field_149582_c);
        data.writeByte(this.field_149579_d);
        data.writeItemStackToBuffer(this.field_149580_e);
        data.writeByte((int)(this.field_149577_f * 16.0F));
        data.writeByte((int)(this.field_149578_g * 16.0F));
        data.writeByte((int)(this.field_149584_h * 16.0F));
    }
}
