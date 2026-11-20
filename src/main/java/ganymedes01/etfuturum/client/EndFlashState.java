package ganymedes01.etfuturum.client;

import net.minecraft.util.MathHelper;

import java.util.Random;

/** The periodic End flash schedule from Eclipse-Sol's EFR backport. */
final class EndFlashState {
	private static final int INTERVAL = 600;
	private static final int MAX_OFFSET = 200;
	private static final int MIN_DURATION = 100;
	private static final int MAX_DURATION = 380;
	private static final int TIME_SYNC_INTERVAL = 20;

	private long interval = Long.MIN_VALUE;
	private long lastGameTime = Long.MIN_VALUE;
	private long lastSoundInterval = Long.MIN_VALUE;
	private int offset;
	private int duration;
	private float intensity;
	private float previousIntensity;
	private float xAngle;
	private float yAngle;

	/** Returns true only for a flash onset observed while ticking this world. */
	boolean tick(long gameTime) {
		long currentInterval = Math.floorDiv(gameTime, INTERVAL);
		if (currentInterval != interval) {
			Random random = new Random(currentInterval);
			random.nextFloat(); // Preserve the angles and timing of the upstream schedule.
			offset = MathHelper.getRandomIntegerInRange(random, 0, MAX_OFFSET);
			duration = MathHelper.getRandomIntegerInRange(random, MIN_DURATION, MAX_DURATION);
			xAngle = -60.0F + random.nextFloat() * 70.0F;
			yAngle = -180.0F + random.nextFloat() * 360.0F;
			interval = currentInterval;
		}

		int elapsed = (int) Math.floorMod(gameTime, (long) INTERVAL) - offset;
		long timeStep = gameTime - lastGameTime;
		previousIntensity = intensity;
		intensity = elapsed > 0 && elapsed < duration
				? MathHelper.sin(elapsed * (float) Math.PI / duration)
				: 0.0F;
		// Server time packets can skip the exact first tick or move the clock backwards.
		boolean started = lastGameTime != Long.MIN_VALUE && timeStep > 0 && timeStep <= TIME_SYNC_INTERVAL
				&& elapsed > 0 && elapsed <= timeStep && currentInterval != lastSoundInterval;
		lastGameTime = gameTime;
		if (timeStep != 1) {
			previousIntensity = intensity;
		}
		if (started) {
			lastSoundInterval = currentInterval;
		}
		return started;
	}

	float getIntensity(float partialTicks) {
		return previousIntensity + (intensity - previousIntensity) * partialTicks;
	}

	float getXAngle() {
		return xAngle;
	}

	float getYAngle() {
		return yAngle;
	}
}
