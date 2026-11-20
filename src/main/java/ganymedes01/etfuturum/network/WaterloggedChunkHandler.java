package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.WaterloggedChunkReceiver;

public final class WaterloggedChunkHandler implements IMessageHandler<WaterloggedChunkMessage, IMessage> {
    // Forge instantiates CLIENT handlers on dedicated servers too. Strip the client entry point,
    // and keep its scheduled lambda in a separate class so no synthetic method loads WorldClient.
    @SideOnly(Side.CLIENT)
    @Override public IMessage onMessage(WaterloggedChunkMessage message, MessageContext context) {
        WaterloggedChunkReceiver.receive(message.dimension, message.chunkX, message.chunkZ,
                message.cell, message.wet, message.bytes, context);
        return null;
    }
}
