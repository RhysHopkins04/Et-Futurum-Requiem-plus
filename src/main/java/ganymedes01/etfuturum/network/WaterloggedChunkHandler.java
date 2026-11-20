package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.world.World;

public final class WaterloggedChunkHandler implements IMessageHandler<WaterloggedChunkMessage, IMessage> {
    @Override public IMessage onMessage(WaterloggedChunkMessage message, MessageContext context) {
        NetHandlerPlayClient owner = context.getClientHandler();
        Minecraft client = Minecraft.getMinecraft();
        client.func_152344_a(() -> {
            World world = client.theWorld;
            if (client.getNetHandler() == owner && world != null && world.provider.dimensionId == message.dimension)
                NativeWaterlogging.receive(world, message.chunkX, message.chunkZ, message.cell, message.wet, message.bytes);
        });
        return null;
    }
}
