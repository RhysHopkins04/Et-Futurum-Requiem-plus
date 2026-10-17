package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.*;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.inventory.ContainerLectern;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Network threads only queue; the server tick validates the current window/book before dispatch. */
public final class LecternBookActionHandler implements IMessageHandler<LecternBookActionMessage,IMessage> {
    private static final ConcurrentHashMap<UUID,Pending> PENDING=new ConcurrentHashMap<>();
    private static final class Pending {
        final EntityPlayerMP player;final LecternBookActionMessage message;
        Pending(EntityPlayerMP player,LecternBookActionMessage message) { this.player=player;this.message=message; }
    }
    @Override public IMessage onMessage(LecternBookActionMessage message,MessageContext context) {
        EntityPlayerMP player=context.getServerHandler().playerEntity;
        if(ConfigFunctions.allowLecternBookClickActions && player!=null)PENDING.putIfAbsent(player.getUniqueID(),new Pending(player,message));
        return null;
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        for(Pending pending:PENDING.values())if(PENDING.remove(pending.player.getUniqueID(),pending))dispatch(pending.player,pending.message);
    }
    public static boolean dispatch(EntityPlayerMP player,LecternBookActionMessage message) {
        if(!ConfigFunctions.allowLecternBookClickActions || player==null || player.worldObj==null || MinecraftServer.getServer()==null
            || MinecraftServer.getServer().worldServerForDimension(player.dimension)!=player.worldObj
            || !(player.openContainer instanceof ContainerLectern) || player.openContainer.windowId!=message.window)return false;
        return ((ContainerLectern)player.openContainer).runBookCommand(player,message.page,message.span,message.expected);
    }
}
