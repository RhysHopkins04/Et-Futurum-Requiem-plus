package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** Extends bulk chunk section masks from 16 bits to the 24-section positive-Y world. */
@Mixin(S26PacketMapChunkBulk.class)
public abstract class MixinS26PacketMapChunkBulk {

    @Shadow private int[] field_149266_a;
    @Shadow private int[] field_149264_b;
    @Shadow private int[] field_149265_c;
    @Shadow private int[] field_149262_d;
    @Shadow private byte[] field_149263_e;
    @Shadow private byte[][] field_149260_f;
    @Shadow private int field_149261_g;
    @Shadow private boolean field_149267_h;
    @Shadow private static byte[] field_149268_i;
    @Shadow(remap = false) private Semaphore deflateGate;

    @Shadow(remap = false)
    private void deflate() {
        throw new AssertionError();
    }

    @ModifyConstant(method = "<init>(Ljava/util/List;)V", constant = @Constant(intValue = 65535))
    private int etfu$sendAllExtendedSections(int original) {
        return WorldHeightCompat.FULL_SECTION_MASK;
    }

    /**
     * @author Et Futurum Requiem Plus
     * @reason Bulk chunk masks use ints and must count sections 0..23.
     */
    @Overwrite
    public void readPacketData(PacketBuffer data) throws IOException {
        short chunkCount = data.readShort();
        this.field_149261_g = data.readInt();
        this.field_149267_h = data.readBoolean();
        this.field_149266_a = new int[chunkCount];
        this.field_149264_b = new int[chunkCount];
        this.field_149265_c = new int[chunkCount];
        this.field_149262_d = new int[chunkCount];
        this.field_149260_f = new byte[chunkCount][];

        if (field_149268_i.length < this.field_149261_g) {
            field_149268_i = new byte[this.field_149261_g];
        }

        data.readBytes(field_149268_i, 0, this.field_149261_g);
        byte[] inflatedData = new byte[S21PacketChunkData.func_149275_c() * chunkCount];
        Inflater inflater = new Inflater();
        inflater.setInput(field_149268_i, 0, this.field_149261_g);

        try {
            inflater.inflate(inflatedData);
        } catch (DataFormatException exception) {
            throw new IOException("Bad compressed data format");
        } finally {
            inflater.end();
        }

        int offset = 0;
        for (int index = 0; index < chunkCount; ++index) {
            this.field_149266_a[index] = data.readInt();
            this.field_149264_b[index] = data.readInt();
            this.field_149265_c[index] = data.readInt();
            this.field_149262_d[index] = data.readInt();
            int sectionCount = 0;
            int msbCount = 0;

            for (int section = 0; section < WorldHeightCompat.EXTENDED_SECTION_COUNT; ++section) {
                sectionCount += this.field_149265_c[index] >> section & 1;
                msbCount += this.field_149262_d[index] >> section & 1;
            }

            int length = 8192 * sectionCount + 256;
            length += 2048 * msbCount;
            if (this.field_149267_h) {
                length += 2048 * sectionCount;
            }

            this.field_149260_f[index] = new byte[length];
            System.arraycopy(inflatedData, offset, this.field_149260_f[index], 0, length);
            offset += length;
        }
    }

    /**
     * @author Et Futurum Requiem Plus
     * @reason Write bulk chunk section/Add masks as ints so upper eight section bits are preserved.
     */
    @Overwrite
    public void writePacketData(PacketBuffer data) throws IOException {
        if (this.field_149263_e == null) {
            this.deflateGate.acquireUninterruptibly();
            if (this.field_149263_e == null) {
                this.deflate();
            }
            this.deflateGate.release();
        }

        data.writeShort(this.field_149266_a.length);
        data.writeInt(this.field_149261_g);
        data.writeBoolean(this.field_149267_h);
        data.writeBytes(this.field_149263_e, 0, this.field_149261_g);

        for (int i = 0; i < this.field_149266_a.length; ++i) {
            data.writeInt(this.field_149266_a[i]);
            data.writeInt(this.field_149264_b[i]);
            data.writeInt(this.field_149265_c[i]);
            data.writeInt(this.field_149262_d[i]);
        }
    }
}
