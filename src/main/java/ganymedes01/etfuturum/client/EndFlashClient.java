package ganymedes01.etfuturum.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.sound.EndFlashSound;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;

@SideOnly(Side.CLIENT)
public final class EndFlashClient {
	private static final int SOUND_DELAY = 30;

	private static World activeWorld;
	private static EndFlashState flash;
	private static int soundDelay;
	private static EndFlashSound pendingSound;
	private static EndFlashSound sound;

	private EndFlashClient() {}

	public static void tick(Minecraft mc) {
		World world = mc.theWorld;
		if (!ConfigWorld.endFlashes || world == null || !(world.provider instanceof WorldProviderEnd)) {
			clear(mc);
			return;
		}
		if (world != activeWorld) {
			clear(mc);
			activeWorld = world;
			flash = new EndFlashState();
		}
		if (mc.isGamePaused() || mc.thePlayer == null) {
			return;
		}

		if (soundDelay > 0 && --soundDelay == 0) {
			if (!(mc.currentScreen instanceof GuiWinGame)) {
				sound = pendingSound;
				sound.update();
				mc.getSoundHandler().playSound(sound);
			}
			pendingSound = null;
		}
		if (flash.tick(world.getTotalWorldTime()) && !(mc.currentScreen instanceof GuiWinGame)) {
			pendingSound = new EndFlashSound(flash.getXAngle(), flash.getYAngle());
			soundDelay = SOUND_DELAY;
		}
	}

	public static void onWorldUnload(Minecraft mc, World world) {
		if (world == activeWorld) {
			clear(mc);
		}
	}

	private static void clear(Minecraft mc) {
		if (sound != null) {
			mc.getSoundHandler().stopSound(sound);
			sound = null;
		}
		soundDelay = 0;
		pendingSound = null;
		activeWorld = null;
		flash = null;
	}

	private static boolean isActive() {
		return ConfigWorld.endFlashes && activeWorld != null && activeWorld == Minecraft.getMinecraft().theWorld;
	}

	public static float getIntensity(float partialTicks) {
		return isActive() ? flash.getIntensity(partialTicks) : 0.0F;
	}

	public static float getXAngle() {
		return isActive() ? flash.getXAngle() : 0.0F;
	}

	public static float getYAngle() {
		return isActive() ? flash.getYAngle() : 0.0F;
	}
}
