package ganymedes01.etfuturum.movement;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/** Optional bridge for detached player renderers. */
public final class PlayerRenderPose {
    private static final ThreadLocal<PlayerRenderPose> ACTIVE = new ThreadLocal<>();

    private final EntityPlayer actor;
    private final MovementPose pose;

    private PlayerRenderPose(EntityPlayer actor, MovementPose pose) {
        this.actor = actor;
        this.pose = pose;
    }

    /**
     * LOD packets already carry a bed yaw, which is only meaningful while sleeping.
     * For awake actors, negative pose IDs fit that field without changing either packet layout.
     */
    public static float captureLodOrientation(EntityPlayer player, float bedYaw) {
        if (PlayerMovement.vanillaPoseRequired(player)) return bedYaw;
        MovementPose pose = PlayerMovement.pose(player);
        return pose == MovementPose.VANILLA ? bedYaw : -pose.wireId;
    }

    /** Scope a decoded pose to this client actor's draw, without touching its entity data. */
    public static Runnable beginLodRender(EntityPlayer actor, float poseOrBedYaw) {
        int id = (int) -poseOrBedYaw;
        if (id <= 0 || id > Byte.MAX_VALUE || poseOrBedYaw != -id
                || !actor.worldObj.isRemote || actor.isPlayerSleeping()) return null;
        MovementPose pose = MovementPose.fromWire((byte) id);
        if (pose == MovementPose.VANILLA) return null;

        PlayerRenderPose previous = ACTIVE.get();
        ACTIVE.set(new PlayerRenderPose(actor, pose));
        return () -> {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        };
    }

    public static MovementPose pose(Entity player) {
        PlayerRenderPose active = ACTIVE.get();
        return active != null && active.actor == player ? active.pose : PlayerMovement.pose(player);
    }
}
