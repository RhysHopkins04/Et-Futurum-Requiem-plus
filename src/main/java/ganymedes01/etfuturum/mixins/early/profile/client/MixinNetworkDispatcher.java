package ganymedes01.etfuturum.mixins.early.profile.client;

import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import ganymedes01.etfuturum.network.profile.ClientWorldProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkDispatcher.class, remap = false)
public class MixinNetworkDispatcher {
    @Shadow @Final public net.minecraft.network.NetworkManager manager;
    @Shadow @Final private cpw.mods.fml.relauncher.Side side;

    @Inject(method = {"close", "disconnect"}, at = @At("HEAD"), require = 1)
    private void etfu$markDisconnected(io.netty.channel.ChannelHandlerContext ctx, io.netty.channel.ChannelPromise promise, CallbackInfo ci) {
        if (side.isClient()) ClientWorldProfile.disconnected(manager);
    }

    @Inject(method = "clientListenForServerHandshake", at = @At("HEAD"), require = 1)
    private void etfu$resetBeforeHello(CallbackInfo ci) { ClientWorldProfile.clear(); }
}
