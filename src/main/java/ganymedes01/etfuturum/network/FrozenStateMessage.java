package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public final class FrozenStateMessage implements IMessage {
    public int ticks;
    public FrozenStateMessage() { }
    public FrozenStateMessage(int ticks) { this.ticks=ticks; }
    public void fromBytes(ByteBuf buf) { ticks=Math.min(140,buf.readUnsignedByte()); }
    public void toBytes(ByteBuf buf) { buf.writeByte(Math.max(0,Math.min(140,ticks))); }
}
