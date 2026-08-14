package ganymedes01.etfuturum.client.particle;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class CustomDripFX extends EntityFX {
	private final String dripSound;
	private final boolean splashes;
	private boolean dripSoundPlayed;
	private int bobTimer;

	public CustomDripFX(World worldIn, double p_i1203_2_, double p_i1203_4_, double p_i1203_6_, String dripSound, int color, boolean splashes) {
		this(worldIn, p_i1203_2_, p_i1203_4_, p_i1203_6_, dripSound, color, splashes, 40);
	}

	public CustomDripFX(World worldIn, double p_i1203_2_, double p_i1203_4_, double p_i1203_6_, String dripSound, int color, boolean splashes, int bobTicks) {
		super(worldIn, p_i1203_2_, p_i1203_4_, p_i1203_6_, 0, 0, 0);
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		this.motionX = this.motionY = this.motionZ = 0.0D;
		particleAlpha = (color >> 24 & 0xff) / 255F;
		particleRed = (color >> 16 & 0xff) / 255F;
		particleGreen = (color >> 8 & 0xff) / 255F;
		particleBlue = (color & 0xff) / 255F;
		this.dripSound = dripSound;
		this.splashes = splashes;

		this.setParticleTextureIndex(113);
		this.setSize(0.01F, 0.01F);
		// EntityFX#setSize does not recenter the bounding box in 1.7.10. Without this,
		// a droplet spawned just outside the pointed-dripstone shape can still collide as
		// though it occupied the old full-size box and disappear before one visible frame.
		this.setPosition(this.posX, this.posY, this.posZ);
		this.particleGravity = 0.06F;
		this.bobTimer = Math.max(0, bobTicks);
		this.particleMaxAge = (int) (64.0D / (Math.random() * 0.8D + 0.2D));
	}

	/**
	 * Called to update the entity's position/logic.
	 */
	@Override
	public void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;

		this.motionY -= this.particleGravity;

		if (this.bobTimer-- > 0) {
			this.motionX *= 0.02D;
			this.motionY *= 0.02D;
			this.motionZ *= 0.02D;
			this.setParticleTextureIndex(113);
		} else {
			this.setParticleTextureIndex(112);
		}

		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		this.motionX *= 0.9800000190734863D;
		this.motionY *= 0.9800000190734863D;
		this.motionZ *= 0.9800000190734863D;

		if (this.particleMaxAge-- <= 0) {
			this.setDead();
		}

		if (this.onGround) {
			if (dripSound != null && !dripSoundPlayed) {
				worldObj.playSound(posX, posY, posZ, dripSound, 1, 1, false);
				dripSoundPlayed = true;
			}
			if (splashes) {
				this.setDead();
				this.worldObj.spawnParticle("splash", this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);
			} else {
				this.setParticleTextureIndex(114);
			}

			this.motionX *= 0.699999988079071D;
			this.motionZ *= 0.699999988079071D;
		}

		int blockX = MathHelper.floor_double(this.posX);
		int blockY = MathHelper.floor_double(this.posY);
		int blockZ = MathHelper.floor_double(this.posZ);
		Block block = this.worldObj.getBlock(blockX, blockY, blockZ);
		Material material = block.getMaterial();

		if (material.isLiquid()) {
			double liquidSurface = (float) (blockY + 1)
					- BlockLiquid.getLiquidHeightPercent(this.worldObj.getBlockMetadata(blockX, blockY, blockZ));
			if (this.posY < liquidSurface) {
				this.setDead();
			}
		} else if (material.isSolid()) {
			// Do not treat every solid material as a full 1x1x1 cube. Pointed dripstone is
			// intentionally non-full, and its drip particle begins just outside the tip's
			// actual collision shape while still sharing the same block-coordinate cell.
			block.setBlockBoundsBasedOnState(worldObj, blockX, blockY, blockZ);
			AxisAlignedBB collision = block.getCollisionBoundingBoxFromPool(worldObj, blockX, blockY, blockZ);
			if (collision != null && collision.isVecInside(Vec3.createVectorHelper(this.posX, this.posY, this.posZ))) {
				this.setDead();
			}
		}
	}
}
