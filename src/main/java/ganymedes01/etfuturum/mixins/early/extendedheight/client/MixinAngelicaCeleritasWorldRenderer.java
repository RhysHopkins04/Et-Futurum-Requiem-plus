package ganymedes01.etfuturum.mixins.early.extendedheight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Angelica/Celeritas provides its own chunk renderer and does not use the
 * vanilla RenderGlobal world-renderer grid. Its default non-CubicChunks
 * build ceiling is 256. Report the active Overworld height rather than a
 * global 384 constant, leaving other dimensions and CubicChunks alone.
 *
 * Registered only when extendedWorldHeight is active and Celeritas is
 * present; never loads Angelica classes on a normal Forge client/server.
 */
@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.CeleritasWorldRenderer", remap = false)
public abstract class MixinAngelicaCeleritasWorldRenderer {

    @Inject(method = "getMaximumBuildHeight", at = @At("HEAD"), cancellable = true, remap = false)
    private void etfu$extendCeleritasWorldHeight(CallbackInfoReturnable<Integer> cir) {
        // Angelica 2.2.14 inherits its world state; getWorld() is not declared
        // on CeleritasWorldRenderer, so it cannot be shadowed here.
        // Use Minecraft's active client world for this client-only getter hook.
        WorldClient world = Minecraft.getMinecraft().theWorld;
        if (world != null && world.provider != null && world.provider.dimensionId == 0) {
            // Preserve provider-defined bounds in other dimensions and let
            // Celeritas retain its own CubicChunks-specific handling.
            cir.setReturnValue(world.getHeight());
        }
    }
}
