package ganymedes01.etfuturum.mixins.early.profile;

import cpw.mods.fml.common.network.handshake.FMLHandshakeMessage;
import cpw.mods.fml.common.network.handshake.HandshakeMessageHandler;
import ganymedes01.etfuturum.network.profile.ProfileHandshake;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HandshakeMessageHandler.class, remap = false)
public class MixinHandshakeHandler {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lcpw/mods/fml/common/network/handshake/FMLHandshakeMessage;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void etfu$stopRejectedLogin(ChannelHandlerContext ctx, FMLHandshakeMessage msg, CallbackInfo ci) {
        if (Boolean.TRUE.equals(ctx.channel().attr(ProfileHandshake.REJECTED).get())) ci.cancel();
    }
}
