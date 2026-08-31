package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import ganymedes01.etfuturum.tileentities.TileEntityWoodSign;
import io.netty.buffer.ByteBuf;

public class WoodSignOpenMessage implements IMessage {
    public int tileX;
    public int tileY;
    public int tileZ;
    public int id;
    public boolean back;

    public WoodSignOpenMessage() {}

    public WoodSignOpenMessage(TileEntityWoodSign tileentitysign, int id) {
        this(tileentitysign, id, false);
    }

    public WoodSignOpenMessage(TileEntityWoodSign tileentitysign, int id, boolean back) {
        tileX = tileentitysign.xCoord;
        tileY = tileentitysign.yCoord;
        tileZ = tileentitysign.zCoord;
        this.id = id;
        this.back = back;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        tileX = buf.readInt();
        tileY = buf.readInt();
        tileZ = buf.readInt();
        id = buf.readInt();
        back = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(tileX);
        buf.writeInt(tileY);
        buf.writeInt(tileZ);
        buf.writeInt(id);
        buf.writeBoolean(back);
    }
}
