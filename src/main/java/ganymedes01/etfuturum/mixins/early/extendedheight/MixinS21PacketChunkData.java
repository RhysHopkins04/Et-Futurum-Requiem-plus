package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S21PacketChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** Extends the S21 section masks from the vanilla 16-bit wire format to 24 usable bits. */
@Mixin(S21PacketChunkData.class)
public abstract class MixinS21PacketChunkData {

    @Shadow private int field_149284_a;
    @Shadow private int field_149282_b;
    @Shadow private int field_149283_c;
    @Shadow private int field_149280_d;
    @Shadow private byte[] field_149281_e;
    @Shadow private byte[] field_149278_f;
    @Shadow private boolean field_149279_g;
    @Shadow private int field_149285_h;
    @Shadow private static byte[] field_149286_i;
    @Shadow(remap = false) private Semaphore deflateGate;

    @Shadow(remap = false)
    private void deflate() {
        throw new AssertionError();
    }

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void etfu$resizeSharedChunkBuffer(CallbackInfo ci) {
        field_149286_i = new byte[WorldHeightCompat.MAX_CHUNK_DATA_BYTES];
    }

    /**
     * @author Et Futurum Requiem Plus
     * @reason The vanilla method returns a fixed buffer ceiling sized for 16 sections.
     */
    @Overwrite
    public static int func_149275_c() {
        return WorldHeightCompat.MAX_CHUNK_DATA_BYTES;
    }

    /**
     * @author Et Futurum Requiem Plus
     * @reason Read 24-bit section masks as ints and count all 24 physical chunk sections.
     */
    @Overwrite
    public void readPacketData(PacketBuffer data) throws IOException {
        this.field_149284_a = data.readInt();
        this.field_149282_b = data.readInt();
        this.field_149279_g = data.readBoolean();
        this.field_149283_c = data.readInt();
        this.field_149280_d = data.readInt();
        this.field_149285_h = data.readInt();

        if (field_149286_i.length < this.field_149285_h) {
            field_149286_i = new byte[this.field_149285_h];
        }

        data.readBytes(field_149286_i, 0, this.field_149285_h);
        int sectionCount = 0;
        int msbCount = 0;

        for (int section = 0; section < WorldHeightCompat.EXTENDED_SECTION_COUNT; ++section) {
            sectionCount += this.field_149283_c >> section & 1;
            msbCount += this.field_149280_d >> section & 1;
        }

        // S21 does not encode a separate has-sky flag. Keep the vanilla worst-case allocation
        // strategy so either Overworld or no-sky payloads fit; Chunk.fillChunk consumes only the
        // arrays implied by the receiving world's provider and masks.
        int length = 12288 * sectionCount;
        length += 2048 * msbCount;
        if (this.field_149279_g) {
            length += 256;
        }

        this.field_149278_f = new byte[length];
        Inflater inflater = new Inflater();
        inflater.setInput(field_149286_i, 0, this.field_149285_h);

        try {
            inflater.inflate(this.field_149278_f);
        } catch (DataFormatException exception) {
            throw new IOException("Bad compressed data format");
        } finally {
            inflater.end();
        }
    }

    /**
     * @author Et Futurum Requiem Plus
     * @reason Write the section and Add/MSB masks as ints so bits 16..23 survive the wire format.
     */
    @Overwrite
    public void writePacketData(PacketBuffer data) throws IOException {
        if (this.field_149281_e == null) {
            this.deflateGate.acquireUninterruptibly();
            if (this.field_149281_e == null) {
                this.deflate();
            }
            this.deflateGate.release();
        }

        data.writeInt(this.field_149284_a);
        data.writeInt(this.field_149282_b);
        data.writeBoolean(this.field_149279_g);
        data.writeInt(this.field_149283_c);
        data.writeInt(this.field_149280_d);
        data.writeInt(this.field_149285_h);
        data.writeBytes(this.field_149281_e, 0, this.field_149285_h);
    }
}
