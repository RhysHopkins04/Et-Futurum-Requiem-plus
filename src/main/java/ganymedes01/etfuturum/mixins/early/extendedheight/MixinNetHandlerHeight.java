package ganymedes01.etfuturum.mixins.early.extendedheight;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep non-Overworld placement/digging within that dimension's own provider ceiling. */
@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerHeight {
    @Shadow public EntityPlayerMP playerEntity;
    @Redirect(method = {"processPlayerDigging", "processPlayerBlockPlacement"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;getBuildLimit()I"))
    private int etfu$dimensionBuildCeiling(MinecraftServer server) {
        return Math.min(server.getBuildLimit(), playerEntity.worldObj.getHeight());
    }
}
