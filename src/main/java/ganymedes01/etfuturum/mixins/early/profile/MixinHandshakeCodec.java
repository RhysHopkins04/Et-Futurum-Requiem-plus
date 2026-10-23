package ganymedes01.etfuturum.mixins.early.profile;

import cpw.mods.fml.common.network.handshake.FMLHandshakeCodec;
import cpw.mods.fml.common.network.handshake.FMLHandshakeMessage;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import ganymedes01.etfuturum.network.profile.ClientWorldProfile;
import ganymedes01.etfuturum.network.profile.ServerWorldProfile;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge's hello messages precede JoinGame and all chunk traffic, including local/LAN connections. */
@Mixin(value = FMLHandshakeCodec.class, remap = false)
public class MixinHandshakeCodec {
    @Inject(method = "encodeInto(Lio/netty/channel/ChannelHandlerContext;Lcpw/mods/fml/common/network/handshake/FMLHandshakeMessage;Lio/netty/buffer/ByteBuf;)V", at = @At("RETURN"), require = 1)
    private void etfu$offerProfile(ChannelHandlerContext ctx, FMLHandshakeMessage msg, ByteBuf target, CallbackInfo ci) {
        if (msg instanceof FMLHandshakeMessage.ServerHello) ServerWorldProfile.local().write(target);
        if (msg instanceof FMLHandshakeMessage.ClientHello) ClientWorldProfile.server().write(target);
    }

    @Inject(method = "decodeInto(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Lcpw/mods/fml/common/network/handshake/FMLHandshakeMessage;)V", at = @At("RETURN"), require = 1)
    private void etfu$acceptProfile(ChannelHandlerContext ctx, ByteBuf source, FMLHandshakeMessage msg, CallbackInfo ci) {
        if (!(msg instanceof FMLHandshakeMessage.ServerHello) && !(msg instanceof FMLHandshakeMessage.ClientHello)) return;
        try {
            ServerWorldProfile offered = ServerWorldProfile.read(source);
            if (msg instanceof FMLHandshakeMessage.ServerHello) {
                ClientWorldProfile.accept(offered, ctx == null ? null : ctx.channel().attr(NetworkDispatcher.FML_DISPATCHER).get().manager);
            } else if (!ServerWorldProfile.local().equals(offered)) {
                throw new IllegalArgumentException("This server requires automatic EFR world-profile support. Update Et Futurum Plus on the client to the server's build.");
            }
        } catch (IllegalArgumentException failure) {
            ctx.channel().attr(ganymedes01.etfuturum.network.profile.ProfileHandshake.REJECTED).set(true);
            ctx.channel().attr(NetworkDispatcher.FML_DISPATCHER).get().rejectHandshake(failure.getMessage());
        }
    }
}
