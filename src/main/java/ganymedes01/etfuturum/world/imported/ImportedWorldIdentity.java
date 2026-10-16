package ganymedes01.etfuturum.world.imported;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.event.world.WorldEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/** World-scoped ID guard: changing a config must not reinterpret saved biome bytes silently. */
public final class ImportedWorldIdentity extends WorldSavedData {
    public static final String KEY = "etfuturum_import_identity";
    public static final ImportedWorldIdentity EVENTS = new ImportedWorldIdentity(KEY);
    private final Map<String, Integer> ids = new LinkedHashMap<>();
    public ImportedWorldIdentity(String name) { super(name); }
    @Override public void readFromNBT(NBTTagCompound tag) {
        if (tag.getInteger("Version") != 1 || tag.getInteger("Height") != 384 || tag.getInteger("YOffset") != 64)
            throw new IllegalArgumentException("Unsupported imported Earth world contract");
        ids.clear();
        NBTTagList list = tag.getTagList("Biomes", 10);
        for (int i=0; i<list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            String source = entry.getString("Source"); int id = entry.getInteger("ID");
            if (source.isEmpty() || id < 0 || id >= 255 || ids.put(source, id) != null)
                throw new IllegalArgumentException("Malformed saved imported biome allocation");
        }
    }
    @Override public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Version", 1); tag.setInteger("Height", 384); tag.setInteger("YOffset", 64);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String,Integer> pair : ids.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound(); entry.setString("Source", pair.getKey());
            entry.setInteger("ID", pair.getValue()); list.appendTag(entry);
        }
        tag.setTag("Biomes", list);
    }
    public void validate(Map<String, Integer> actual) {
        if (!ids.equals(actual)) throw new IllegalStateException("Imported Earth biome IDs changed or identities disabled. Restore this world's original importedbiomes.cfg before loading it.");
    }
    @SubscribeEvent public void load(WorldEvent.Load event) {
        if (event.world.isRemote || event.world.provider.dimensionId != 0) return;
        ImportedBiomes.verifyRegistry();
        ImportedWorldIdentity saved = (ImportedWorldIdentity)event.world.mapStorage.loadData(ImportedWorldIdentity.class, KEY);
        if (saved != null && !ConfigMapCompatibility.importedEarthMode)
            throw new IllegalStateException("This world requires importedEarthMode and its 384-block engine on both client and server.");
        if (!ConfigMapCompatibility.importedEarthMode) return;
        if (saved == null && event.world.getSaveHandler().getMapFileFromName(KEY).isFile())
            throw new IllegalStateException("Saved imported Earth identity could not be read; refusing to replace it.");
        Map<String,Integer> actual = new LinkedHashMap<>();
        for (Map.Entry<String,BiomeGenBase> entry : ImportedBiomes.registered().entrySet()) actual.put(entry.getKey(),entry.getValue().biomeID);
        if (saved == null) {
            saved = new ImportedWorldIdentity(KEY);saved.ids.putAll(actual);
            event.world.mapStorage.setData(KEY,saved);saved.markDirty();
        } else saved.validate(actual);
    }
}
