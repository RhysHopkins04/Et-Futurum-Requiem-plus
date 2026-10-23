package ganymedes01.etfuturum.mixins.early.profile.client;

import ganymedes01.etfuturum.network.profile.ClientWorldProfile;
import ganymedes01.etfuturum.world.imported.ImportedWaterProfiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Unique private boolean etfu$waterTextures;
    @Inject(method = "runTick", at = @At("HEAD"), require = 1)
    private void etfu$clearFailedLogin(CallbackInfo ci) {
        if (((Minecraft)(Object)this).theWorld == null) ClientWorldProfile.clearDisconnected();
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void etfu$applyConnectionResources(WorldClient world, String message, CallbackInfo ci) {
        if (world == null) ClientWorldProfile.clear();
        boolean water = ImportedWaterProfiles.enabled();
        if (water != etfu$waterTextures) {
            etfu$waterTextures = water;
            ((Minecraft)(Object)this).refreshResources();
        }
    }
}
