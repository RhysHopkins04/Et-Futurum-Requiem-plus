package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.network.PacketBuffer;
import ganymedes01.etfuturum.network.profile.ProfilePacketFormat;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import org.spongepowered.asm.mixin.Unique;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** Extends bulk chunk section masks from 16 bits to the 24-section positive-Y world. */
@Mixin(S26PacketMapChunkBulk.class)
public abstract class MixinS26PacketMapChunkBulk {
    @Unique private Boolean etfu$receivedExtendedFormat;
    @Unique private boolean etfu$writeExtendedFormat() {
        return etfu$receivedExtendedFormat != null ? etfu$receivedExtendedFormat : ProfilePacketFormat.serverExtended();
    }


    @Shadow private int[] field_149266_a;
    @Shadow private int[] field_149264_b;
    @Shadow private int[] field_149265_c;
    @Shadow private int[] field_149262_d;
    @Shadow private byte[] field_149263_e;
    @Shadow private byte[][] field_149260_f;
    @Shadow private int field_149261_g;
    @Shadow private boolean field_149267_h;
    // Do not shadow a static compression scratch buffer here: Crucible/Spigot
    // changes the vanilla static field into an instance field.

    @ModifyConstant(method = "<init>(Ljava/util/List;)V", constant = @Constant(intValue = 65535))
    private int etfu$sendAllExtendedSections(int original) {
        return ProfilePacketFormat.serverExtended() ? WorldHeightCompat.sectionMask(WorldHeightCompat.configuredHeight() / 16) : original;
    }

    /**
     * @author Et Futurum Requiem Plus
     * @reason Bulk chunk masks use ints and must count sections 0..23.
     */
    @Overwrite
    public void readPacketData(PacketBuffer data) throws IOException {
        etfu$receivedExtendedFormat = ProfilePacketFormat.clientExtended();
        short chunkCount = data.readShort();
        this.field_149261_g = data.readInt();
        this.field_149267_h = data.readBoolean();
        this.field_149266_a = new int[chunkCount];
        this.field_149264_b = new int[chunkCount];
        this.field_149265_c = new int[chunkCount];
        this.field_149262_d = new int[chunkCount];
        this.field_149260_f = new byte[chunkCount][];

        // Packet-local input works with both Forge's static and Crucible's
        // per-packet scratch buffer and avoids cross-thread buffer sharing.
        byte[] compressedInput = new byte[this.field_149261_g];
        data.readBytes(compressedInput, 0, this.field_149261_g);
        byte[] inflatedData = new byte[S21PacketChunkData.func_149275_c() * chunkCount];
        Inflater inflater = new Inflater();
        inflater.setInput(compressedInput, 0, this.field_149261_g);

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
            this.field_149265_c[index] = ProfilePacketFormat.readMask(data, ProfilePacketFormat.clientExtended());
            this.field_149262_d[index] = ProfilePacketFormat.readMask(data, ProfilePacketFormat.clientExtended());
            int sectionCount = 0;
            int msbCount = 0;

            for (int section = 0; section < (WorldHeightCompat.clientHeight() / 16); ++section) {
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
        this.etfu$ensureCompressed();

        data.writeShort(this.field_149266_a.length);
        data.writeInt(this.field_149261_g);
        data.writeBoolean(this.field_149267_h);
        data.writeBytes(this.field_149263_e, 0, this.field_149261_g);

        for (int i = 0; i < this.field_149266_a.length; ++i) {
            data.writeInt(this.field_149266_a[i]);
            data.writeInt(this.field_149264_b[i]);
            ProfilePacketFormat.writeMask(data, this.field_149265_c[i], etfu$writeExtendedFormat());
            ProfilePacketFormat.writeMask(data, this.field_149262_d[i], etfu$writeExtendedFormat());
        }
    }
    /**
     * Forge bulk packets use a private deflate() + deflateGate; Crucible/Spigot
     * instead uses compress() and does not have either of those members.
     * Compress from the shared, stable uncompressed chunk payload fields that
     * both packet implementations expose. Never rely on either private API.
     */
    @Unique
    private synchronized void etfu$ensureCompressed() {
        if (this.field_149263_e != null) {
            return;
        }

        int rawSize = 0;
        for (byte[] chunkData : this.field_149260_f) {
            rawSize = Math.addExact(rawSize, chunkData.length);
        }
        byte[] raw = new byte[rawSize];
        int offset = 0;
        for (byte[] chunkData : this.field_149260_f) {
            System.arraycopy(chunkData, 0, raw, offset, chunkData.length);
            offset += chunkData.length;
        }

        Deflater compressor = new Deflater(4); // Same level as Crucible's bulk compression.
        try {
            compressor.setInput(raw);
            compressor.finish();
            ByteArrayOutputStream result = new ByteArrayOutputStream(Math.max(64, rawSize / 3));
            byte[] scratch = new byte[8192];
            while (!compressor.finished()) {
                int written = compressor.deflate(scratch);
                if (written <= 0) {
                    throw new IllegalStateException("Bulk chunk deflater made no progress");
                }
                result.write(scratch, 0, written);
            }
            this.field_149263_e = result.toByteArray();
            this.field_149261_g = this.field_149263_e.length;
        } finally {
            compressor.end();
        }
    }

}
