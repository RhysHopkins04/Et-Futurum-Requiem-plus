package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Vanilla S22 multi-block changes pack Y into eight bits. Until a later optimization replaces that
 * packet format, upper-world changes are safely sent as individual extended S23 updates instead.
 */
@Mixin(targets = "net.minecraft.server.management.PlayerManager$PlayerInstance")
public abstract class MixinPlayerInstance {

    @Shadow @Final private ChunkCoordIntPair chunkLocation;
    @Shadow @Final private List playersWatchingChunk;

    @Shadow
    public abstract void sendToAllPlayersWatchingChunk(Packet packet);

    @Inject(method = "flagChunkForUpdate", at = @At("HEAD"), cancellable = true)
    private void etfu$sendUpperBlockChangeDirectly(int localX, int y, int localZ, CallbackInfo ci) {
        if (y < WorldHeightCompat.LEGACY_HEIGHT) {
            return;
        }

        if (!this.playersWatchingChunk.isEmpty()) {
            EntityPlayerMP player = (EntityPlayerMP)this.playersWatchingChunk.get(0);
            WorldServer world = player.getServerForPlayer();
            int worldX = this.chunkLocation.chunkXPos * 16 + localX;
            int worldZ = this.chunkLocation.chunkZPos * 16 + localZ;
            this.sendToAllPlayersWatchingChunk(new S23PacketBlockChange(worldX, y, worldZ, world));

            if (world.getBlock(worldX, y, worldZ).hasTileEntity(world.getBlockMetadata(worldX, y, worldZ))) {
                TileEntity tile = world.getTileEntity(worldX, y, worldZ);
                if (tile != null) {
                    Packet description = tile.getDescriptionPacket();
                    if (description != null) {
                        this.sendToAllPlayersWatchingChunk(description);
                    }
                }
            }
        }

        ci.cancel();
    }
}
