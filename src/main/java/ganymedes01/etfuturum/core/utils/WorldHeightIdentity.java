package ganymedes01.etfuturum.core.utils;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.event.world.WorldEvent;

/** Refuse config downgrades before any normal world tick can rewrite upper sections. */
public final class WorldHeightIdentity extends WorldSavedData {
    public static final String KEY = "etfuturum_world_height";
    public static final WorldHeightIdentity EVENTS = new WorldHeightIdentity(KEY);
    private int height;
    public WorldHeightIdentity(String name) { super(name); }
    @Override public void readFromNBT(NBTTagCompound tag) {
        height = tag.getInteger("Height");
        if (tag.getInteger("Version") != 1 || height < 384 || height > 512 || (height & 15) != 0)
            throw new IllegalArgumentException("Invalid saved EFR height contract");
    }
    @Override public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Version", 1); tag.setInteger("Height", height);
    }
    @SubscribeEvent public void load(WorldEvent.Load event) {
        if (event.world.isRemote || event.world.provider.dimensionId != 0) return;
        WorldHeightIdentity saved = (WorldHeightIdentity)event.world.mapStorage.loadData(WorldHeightIdentity.class, KEY);
        if (saved == null && event.world.getSaveHandler().getMapFileFromName(KEY).isFile())
            throw new IllegalStateException("Cannot read saved EFR height; refusing to replace it");
        int current = WorldHeightCompat.height(event.world);
        if (saved != null && current < saved.height)
            throw new IllegalStateException("This world requires EFR height " + saved.height +
                    "; restore extendedWorldHeight and extendedWorldHeightBlocks before loading. Height reductions are unsafe.");
        if (current <= 256) return;
        if (saved == null) {
            saved = new WorldHeightIdentity(KEY);
            event.world.mapStorage.setData(KEY, saved);
        }
        if (current != saved.height) { saved.height = current; saved.markDirty(); }
    }
}
