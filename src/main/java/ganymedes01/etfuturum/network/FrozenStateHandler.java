package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.*;
import net.minecraft.client.Minecraft;
import ganymedes01.etfuturum.core.utils.PowderSnowPhysics;

public final class FrozenStateHandler implements IMessageHandler<FrozenStateMessage,IMessage> {
    public IMessage onMessage(final FrozenStateMessage message,MessageContext context) {
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            public void run() {
                if (Minecraft.getMinecraft().thePlayer!=null)
                    Minecraft.getMinecraft().thePlayer.getEntityData().setInteger(PowderSnowPhysics.FROZEN,message.ticks);
            }
        });
        return null;
    }
}
