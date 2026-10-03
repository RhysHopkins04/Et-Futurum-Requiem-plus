package ganymedes01.etfuturum.client.handlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.core.utils.PowderSnowPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

public final class PowderSnowClientHandler {
    private static final ResourceLocation OUTLINE=new ResourceLocation(Tags.MC_ASSET_VER,"textures/misc/powder_snow_outline.png");
    private boolean inside(EntityViewRenderEvent e) {
        Vec3 v=ActiveRenderInfo.projectViewFromEntity(e.entity,e.renderPartialTicks);
        return e.entity.worldObj.getBlock(MathHelper.floor_double(v.xCoord),MathHelper.floor_double(v.yCoord),MathHelper.floor_double(v.zCoord))==ModernMapParityBlocks.POWDER_SNOW.get();
    }
    @SubscribeEvent public void fog(EntityViewRenderEvent.FogDensity e) {
        if (!inside(e)) return;
        GL11.glFogi(GL11.GL_FOG_MODE,GL11.GL_LINEAR);
        boolean spectator=ganymedes01.etfuturum.api.spectator.SpectatorUtils.isSpectator(e.entity);
        GL11.glFogf(GL11.GL_FOG_START,spectator ? -8 : 0);
        GL11.glFogf(GL11.GL_FOG_END,spectator ? Minecraft.getMinecraft().gameSettings.renderDistanceChunks*8.0F : 2);
        e.density=0; e.setCanceled(true);
    }
    @SubscribeEvent public void colour(EntityViewRenderEvent.FogColors e) {
        if (!inside(e)) return;
        e.red=159.0F/255; e.green=187.0F/255; e.blue=204.0F/255;
    }
    @SubscribeEvent public void overlay(RenderGameOverlayEvent.Post e) {
        Minecraft mc=Minecraft.getMinecraft();
        if (e.type!=RenderGameOverlayEvent.ElementType.ALL || mc.thePlayer==null || mc.gameSettings.thirdPersonView!=0) return;
        int ticks=mc.thePlayer.getEntityData().getInteger(PowderSnowPhysics.FROZEN);
        if (ticks<=0) return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_CURRENT_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDepthMask(false); GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1,1,1,Math.min(1,ticks/140.0F)); mc.getTextureManager().bindTexture(OUTLINE);
            double w=e.resolution.getScaledWidth(),h=e.resolution.getScaledHeight();
            Tessellator t=Tessellator.instance; t.startDrawingQuads();
            t.addVertexWithUV(0,h,-90,0,1); t.addVertexWithUV(w,h,-90,1,1);
            t.addVertexWithUV(w,0,-90,1,0); t.addVertexWithUV(0,0,-90,0,0); t.draw();
        } finally { GL11.glPopAttrib(); }
    }
}
