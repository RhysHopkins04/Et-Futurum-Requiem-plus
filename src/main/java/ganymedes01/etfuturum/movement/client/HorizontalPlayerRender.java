package ganymedes01.etfuturum.movement.client;

import ganymedes01.etfuturum.movement.MovementPose;
import net.minecraft.entity.Entity;

public final class HorizontalPlayerRender {
    private static final ThreadLocal<HorizontalPlayerRender> ACTIVE = new ThreadLocal<>();

    private final Entity player;
    private final MovementPose pose;

    private HorizontalPlayerRender(Entity player, MovementPose pose) {
        this.player = player;
        this.pose = pose;
    }

    public static void render(Entity player, MovementPose pose, Runnable draw) {
        HorizontalPlayerRender previous = ACTIVE.get();
        ACTIVE.set(new HorizontalPlayerRender(player, pose));
        try {
            draw.run();
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    public static MovementPose pose(Entity player) {
        HorizontalPlayerRender active = ACTIVE.get();
        return active != null && active.player == player ? active.pose : MovementPose.VANILLA;
    }

    public static float bodyPitch(MovementPose pose, float lookPitch) {
        return pose == MovementPose.SWIM ? -90.0F - lookPitch : -90.0F;
    }
}
