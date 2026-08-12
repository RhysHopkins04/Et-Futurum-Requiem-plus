package ganymedes01.etfuturum.mixins.early.extendedheight;

import net.minecraft.block.Block;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S23PacketBlockChange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;

/** Extends single block-change packet Y coordinates to an unsigned short. */
@Mixin(S23PacketBlockChange.class)
public class MixinS23PacketBlockChange {

    @Shadow private int field_148887_a;
    @Shadow private int field_148885_b;
    @Shadow private int field_148886_c;
    @Shadow public Block field_148883_d;
    @Shadow public int field_148884_e;

    /** @author Et Futurum Requiem Plus @reason Extend packet Y from 8 to 16 bits. */
    @Overwrite
    public void readPacketData(PacketBuffer data) throws IOException {
        this.field_148887_a = data.readInt();
        this.field_148885_b = data.readUnsignedShort();
        this.field_148886_c = data.readInt();
        this.field_148883_d = Block.getBlockById(data.readVarIntFromBuffer());
        this.field_148884_e = data.readUnsignedByte();
    }

    /** @author Et Futurum Requiem Plus @reason Extend packet Y from 8 to 16 bits. */
    @Overwrite
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(this.field_148887_a);
        data.writeShort(this.field_148885_b);
        data.writeInt(this.field_148886_c);
        data.writeVarIntToBuffer(Block.getIdFromBlock(this.field_148883_d));
        data.writeByte(this.field_148884_e);
    }
}
