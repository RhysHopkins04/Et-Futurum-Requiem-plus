package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/** Page/span and expected text bind a click to the book the reader actually saw. */
public final class LecternBookActionMessage implements IMessage {
    public int window,page,span;
    public String expected="";
    public LecternBookActionMessage() {}
    public LecternBookActionMessage(int window,int page,int span,String expected) { this.window=window;this.page=page;this.span=span;this.expected=expected; }
    @Override public void fromBytes(ByteBuf buffer) {
        window=buffer.readInt();page=buffer.readInt();span=buffer.readInt();int length=buffer.readUnsignedShort();
        if(length>400 || length>buffer.readableBytes())throw new IllegalArgumentException("Invalid lectern action length");
        byte[] bytes=new byte[length];buffer.readBytes(bytes);expected=new String(bytes,StandardCharsets.UTF_8);
        if(expected.length()>100)throw new IllegalArgumentException("Oversized lectern command");
    }
    @Override public void toBytes(ByteBuf buffer) {
        byte[] bytes=expected.getBytes(StandardCharsets.UTF_8);
        if(expected.length()>100 || bytes.length>400)throw new IllegalArgumentException("Oversized lectern command");
        buffer.writeInt(window);buffer.writeInt(page);buffer.writeInt(span);buffer.writeShort(bytes.length);buffer.writeBytes(bytes);
    }
}
