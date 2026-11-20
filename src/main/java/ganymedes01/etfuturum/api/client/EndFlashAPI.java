package ganymedes01.etfuturum.api.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.EndFlashClient;

/** Client-side End flash intensity and direction for renderers such as Angelica. */
@SideOnly(Side.CLIENT)
public final class EndFlashAPI {

	private EndFlashAPI() {}

	/** Returns the interpolated flash intensity, or zero outside an active End flash. */
	public static float getIntensity(float partialTicks) {
		return EndFlashClient.getIntensity(partialTicks);
	}

	/** Returns the flash pitch in degrees. */
	public static float getXAngle() {
		return EndFlashClient.getXAngle();
	}

	/** Returns the flash yaw in degrees. */
	public static float getYAngle() {
		return EndFlashClient.getYAngle();
	}
}
