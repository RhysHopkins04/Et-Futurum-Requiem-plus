package ganymedes01.etfuturum.client.handlers;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.world.imported.ImportedWaterProfiles;
import net.minecraftforge.event.terraingen.BiomeEvent;

/** Also serves renderers such as Angelica which bypass BlockLiquid's color hook. */
public final class ImportedWaterTintHandler {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void waterColor(BiomeEvent.GetWaterColor event) {
        if(ImportedWaterProfiles.enabled())
            event.newColor=ImportedWaterProfiles.color(event.biome,event.newColor);
    }
}
