package ganymedes01.etfuturum.client.particle;

import ganymedes01.etfuturum.Tags;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;

/**
 * 1.21.9+ Copper Torch flame using the real AssetDirector-provided
 * minecraft:copper_fire_flame texture rather than bundling Mojang assets.
 */
public final class CopperFireFlameFX extends EtFuturumFXParticle {
    private final float flameScale;

    public CopperFireFlameFX(World world, double x, double y, double z, double mX, double mY, double mZ) {
        super(world, x, y, z, mX, mY, mZ,
                (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4,
                (particleRand.nextFloat() * 0.5F + 0.5F) * 2.0F,
                0xFFFFFFFF, Tags.MC_ASSET_VER + ":textures/particle/copper_fire_flame.png", 1);
        motionX = motionX * 0.009999999776482582D + mX;
        motionY = motionY * 0.009999999776482582D + mY;
        motionZ = motionZ * 0.009999999776482582D + mZ;
        flameScale = particleScale;
        particleRed = particleGreen = particleBlue = 1.0F;
        particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
        noClip = true;
    }

    public CopperFireFlameFX(World world, double x, double y, double z) {
        this(world, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public void renderParticle(Tessellator tessellator, float partialTicks, float rx, float rxz, float rz,
            float ryz, float rxy) {
        float age = ((float) particleAge + partialTicks) / (float) particleMaxAge;
        particleScale = flameScale * (1.0F - age * age * 0.5F);
        super.renderParticle(tessellator, partialTicks, rx, rxz, rz, ryz, rxy);
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        float age = ((float) particleAge + partialTicks) / (float) particleMaxAge;
        age = Math.max(0.0F, Math.min(1.0F, age));
        int packed = super.getBrightnessForRender(partialTicks);
        int block = packed & 255;
        int sky = packed >> 16 & 255;
        block = Math.min(240, block + (int) (age * 15.0F * 16.0F));
        return block | sky << 16;
    }

    @Override
    public float getBrightness(float partialTicks) {
        float age = ((float) particleAge + partialTicks) / (float) particleMaxAge;
        age = Math.max(0.0F, Math.min(1.0F, age));
        float normal = super.getBrightness(partialTicks);
        return normal * age + (1.0F - age);
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) setDead();
        moveEntity(motionX, motionY, motionZ);
        motionX *= 0.9599999785423279D;
        motionY *= 0.9599999785423279D;
        motionZ *= 0.9599999785423279D;
        if (onGround) {
            motionX *= 0.699999988079071D;
            motionZ *= 0.699999988079071D;
        }
    }
}
