package ganymedes01.etfuturum.client.handlers;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.world.imported.ImportedWaterProfiles;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import org.lwjgl.opengl.GL11;

/** Client fog only: no world light arrays, water levels or source blocks are rewritten. */
public final class ImportedWaterHandler {
    private boolean applies(EntityViewRenderEvent event) {
        return ImportedWaterProfiles.enabled() && event.entity.worldObj.provider.dimensionId==0
                && event.block.getMaterial()==Material.water && !event.entity.isPotionActive(Potion.blindness);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void colors(EntityViewRenderEvent.FogColors event) {
        if(!applies(event))return;
        ImportedWaterProfiles.Profile profile=ImportedWaterProfiles.forBiome(event.entity.worldObj.getBiomeGenForCoords(
                MathHelper.floor_double(event.entity.posX),MathHelper.floor_double(event.entity.posZ)));
        float r=((profile.fogColor>>16)&255)/255F,g=((profile.fogColor>>8)&255)/255F,b=(profile.fogColor&255)/255F;
        if(event.entity.isPotionActive(Potion.nightVision)) {
            float scale=1F/Math.max(r,Math.max(g,b));r*=scale;g*=scale;b*=scale;
        }
        event.red=r;event.green=g;event.blue=b;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void fog(EntityViewRenderEvent.FogDensity event) {
        if(!applies(event))return;
        ImportedWaterProfiles.Profile profile=ImportedWaterProfiles.forBiome(event.entity.worldObj.getBiomeGenForCoords(
                MathHelper.floor_double(event.entity.posX),MathHelper.floor_double(event.entity.posZ)));
        GL11.glFogi(GL11.GL_FOG_MODE,GL11.GL_LINEAR);
        GL11.glFogf(GL11.GL_FOG_START,profile.start);
        GL11.glFogf(GL11.GL_FOG_END,Math.min(profile.end,Minecraft.getMinecraft().gameSettings.renderDistanceChunks*16F));
        event.density=0;event.setCanceled(true);
    }
}
