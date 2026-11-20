package ganymedes01.etfuturum.client;

import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
public final class WaterloggedChunkReceiver {
    private WaterloggedChunkReceiver() {}

    public static void receive(int dimension, int chunkX, int chunkZ, int cell, boolean wet,
            byte[] bytes, MessageContext context) {
        NetHandlerPlayClient owner = context.getClientHandler();
        Minecraft client = Minecraft.getMinecraft();
        client.func_152344_a(() -> {
            World world = client.theWorld;
            if (client.getNetHandler() == owner && world != null && world.provider.dimensionId == dimension)
                NativeWaterlogging.receive(world, chunkX, chunkZ, cell, wet, bytes);
        });
    }
}
