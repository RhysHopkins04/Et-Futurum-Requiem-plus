package ganymedes01.etfuturum.mixins.early.fmlicon.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import cpw.mods.fml.client.FMLClientHandler;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.multiplayer.ServerData;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Forge's transparent compatibility badges must not inherit opaque GUI state. */
@Mixin(ServerListEntryNormal.class)
public abstract class MixinServerListEntryNormal {
    @WrapOperation(method = "drawEntry", at = @At(value = "INVOKE", remap = false,
            target = "Lcpw/mods/fml/client/FMLClientHandler;enhanceServerListEntry(Lnet/minecraft/client/gui/ServerListEntryNormal;Lnet/minecraft/client/multiplayer/ServerData;IIIII)Ljava/lang/String;"))
    private String etfu$transparentFmlBadge(FMLClientHandler handler, ServerListEntryNormal entry, ServerData server,
            int x, int width, int y, int mouseX, int mouseY, Operation<String> original) {
        // Include enable flags, alpha function and blend factors. Angelica redirects
        // these GL calls through its tracked state stack on core-profile clients.
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            return original.call(handler, entry, server, x, width, y, mouseX, mouseY);
        } finally {
            GL11.glPopAttrib();
        }
    }
}
