package ganymedes01.etfuturum.client.renderer;

import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.api.client.EndFlashAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Method;

public final class EndFlashRenderer {
	private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MC_ASSET_VER,
			"textures/environment/celestial/end_flash.png");

	private EndFlashRenderer() {}

	public static boolean usesShaderFlash() {
		return ShaderFlash.isActive();
	}

	public static void render(float partialTicks) {
		float intensity = EndFlashAPI.getIntensity(partialTicks);
		if (intensity <= 1.0E-5F || usesShaderFlash()) {
			return;
		}

		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
				| GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT);
		GL11.glPushMatrix();
		try {
			GL11.glDepthMask(false);
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			GL11.glDisable(GL11.GL_FOG);
			GL11.glEnable(GL11.GL_BLEND);
			OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			TextureManager textures = Minecraft.getMinecraft().getTextureManager();
			textures.bindTexture(TEXTURE);
			if (textures.getTexture(TEXTURE) == TextureUtil.missingTexture) {
				return;
			}

			// Use the same orientation as Angelica's endFlashPosition uniform.
			GL11.glRotatef(180.0F - EndFlashAPI.getYAngle(), 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(-90.0F - EndFlashAPI.getXAngle(), 1.0F, 0.0F, 0.0F);
			GL11.glTranslatef(0.0F, 100.0F, 0.0F);

			Tessellator tessellator = Tessellator.instance;
			tessellator.startDrawingQuads();
			tessellator.setColorRGBA_F(intensity, intensity, intensity, intensity);
			tessellator.addVertexWithUV(-60.0D, 0.0D, -60.0D, 0.0D, 0.0D);
			tessellator.addVertexWithUV(60.0D, 0.0D, -60.0D, 1.0D, 0.0D);
			tessellator.addVertexWithUV(60.0D, 0.0D, 60.0D, 1.0D, 1.0D);
			tessellator.addVertexWithUV(-60.0D, 0.0D, 60.0D, 0.0D, 1.0D);
			tessellator.draw();
		} finally {
			GL11.glPopMatrix();
			GL11.glPopAttrib();
		}
	}

	/** WDG's Iris pipeline supplies the flash to shader packs through EndFlashAPI. */
	private static final class ShaderFlash {
		private static final Method IN_USE = findIrisMethod();
		private static Object iris;

		private static Method findIrisMethod() {
			ClassLoader loader = EndFlashRenderer.class.getClassLoader();
			if (loader.getResource("com/gtnewhorizons/angelica/compat/etfuturum/EndFlashCompat.class") == null) {
				return null;
			}
			try {
				Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi", false, loader);
				return api.getMethod("isShaderPackInUse");
			} catch (ReflectiveOperationException | LinkageError ignored) {
				return null;
			}
		}

		private static boolean isActive() {
			if (IN_USE == null) {
				return false;
			}
			try {
				if (iris == null) {
					iris = IN_USE.getDeclaringClass().getMethod("getInstance").invoke(null);
				}
				return Boolean.TRUE.equals(IN_USE.invoke(iris));
			} catch (ReflectiveOperationException | LinkageError ignored) {
				return false;
			}
		}
	}
}
