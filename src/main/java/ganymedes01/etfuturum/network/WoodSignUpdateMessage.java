package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** Server-bound modern two-sided sign text update. */
public class WoodSignUpdateMessage implements IMessage {
    public int x;
    public int y;
    public int z;
    public boolean back;
    public final String[] lines = new String[]{"", "", "", ""};

    public WoodSignUpdateMessage() {}

    public WoodSignUpdateMessage(int x, int y, int z, boolean back, String[] text) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.back = back;
        for (int i = 0; i < 4; i++) lines[i] = text != null && i < text.length && text[i] != null ? text[i] : "";
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        back = buf.readBoolean();
        for (int i = 0; i < 4; i++) lines[i] = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeBoolean(back);
        for (int i = 0; i < 4; i++) ByteBufUtils.writeUTF8String(buf, lines[i] == null ? "" : lines[i]);
    }
}
