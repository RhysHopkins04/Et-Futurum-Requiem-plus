package ganymedes01.etfuturum.client.particle;

import net.minecraft.world.World;

/** Green spore-blossom particle using the modern drip_fall sprite. */
public class SporeBlossomFX extends EtFuturumFXParticle {

    private final boolean falling;

    public SporeBlossomFX(World world, double x, double y, double z, boolean falling) {
        super(world, x, y, z,
                falling ? 0.0D : ambientHorizontalMotion(),
                falling ? -0.012D : ambientVerticalMotion(),
                falling ? 0.0D : ambientHorizontalMotion(),
                falling ? 80 : 100,
                falling ? 0.62F : 0.42F,
                falling ? 0xFF7DBB45 : 0xFF6EAD3D,
                "minecraft:textures/particle/drip_fall.png",
                1);
        this.falling = falling;
        this.particleGravity = falling ? 0.006F : 0.0004F;
        this.fadeAway = true;
        this.noClip = true;
    }

    private static double ambientHorizontalMotion() {
        return (particleRand.nextDouble() - 0.5D) * 0.003D;
    }

    private static double ambientVerticalMotion() {
        return -0.002D - particleRand.nextDouble() * 0.003D;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!falling) {
            motionX *= 0.995D;
            motionZ *= 0.995D;
            motionY *= 0.995D;
        }
    }
}
