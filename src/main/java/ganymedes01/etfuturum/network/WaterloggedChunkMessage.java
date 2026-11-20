package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.chunk.Chunk;

public final class WaterloggedChunkMessage implements IMessage {
    int dimension, chunkX, chunkZ, cell;
    boolean wet;
    byte[] bytes;

    public WaterloggedChunkMessage() {}

    private WaterloggedChunkMessage(Chunk chunk, int cell, boolean wet, byte[] bytes) {
        dimension = chunk.worldObj.provider.dimensionId;
        chunkX = chunk.xPosition;
        chunkZ = chunk.zPosition;
        this.cell = cell;
        this.wet = wet;
        this.bytes = bytes;
    }

    public static WaterloggedChunkMessage snapshot(Chunk chunk, byte[] bytes) {
        return new WaterloggedChunkMessage(chunk, -1, false, bytes);
    }

    public static WaterloggedChunkMessage cell(Chunk chunk, int cell, boolean wet) {
        return new WaterloggedChunkMessage(chunk, cell, wet, null);
    }

    @Override public void fromBytes(ByteBuf in) {
        dimension = in.readInt();
        chunkX = in.readInt();
        chunkZ = in.readInt();
        cell = in.readInt();
        if (cell == -1) {
            if (in.readableBytes() > NativeWaterlogging.MAX_BYTES)
                throw new IllegalArgumentException("Waterlogging snapshot exceeds chunk height");
            bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);
        } else {
            if (cell < 0 || cell >= NativeWaterlogging.MAX_Y * 256 || in.readableBytes() != 1)
                throw new IllegalArgumentException("Invalid waterlogging cell update");
            wet = in.readBoolean();
        }
    }

    @Override public void toBytes(ByteBuf out) {
        out.writeInt(dimension).writeInt(chunkX).writeInt(chunkZ).writeInt(cell);
        if (cell < 0) out.writeBytes(bytes);
        else out.writeBoolean(wet);
    }
}
