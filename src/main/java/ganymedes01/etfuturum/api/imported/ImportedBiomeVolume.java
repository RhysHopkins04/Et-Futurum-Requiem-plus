package ganymedes01.etfuturum.api.imported;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Immutable, bounded source-name storage. V1 is the modern -64..319 -> physical 0..383 contract. */
public final class ImportedBiomeVolume {
    public static final String NBT_KEY = "etfuturum:imported_biomes";
    public static final int VERSION = 1;
    private static final Pattern NAME = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private final Map<Integer, Section> sections;

    public static final class Section {
        private final String[] palette;
        private final byte[] indices;
        public Section(String[] palette, byte[] indices) {
            if (palette == null || palette.length < 1 || palette.length > 64 || indices == null || indices.length != 64)
                throw new IllegalArgumentException("Biome section requires 1..64 names and exactly 64 quart indices");
            for (String name : palette) if (name == null || name.length() > 256 || !NAME.matcher(name).matches())
                throw new IllegalArgumentException("Invalid biome source identity: " + name);
            for (byte index : indices) if ((index & 255) >= palette.length)
                throw new IllegalArgumentException("Biome palette index out of bounds");
            this.palette = palette.clone(); this.indices = indices.clone();
        }
        public String at(int quartX, int quartY, int quartZ) {
            if ((quartX | quartY | quartZ) < 0 || quartX > 3 || quartY > 3 || quartZ > 3)
                throw new IllegalArgumentException("Quart coordinates must be 0..3");
            return palette[indices[quartX + quartZ * 4 + quartY * 16] & 255];
        }
    }
    public ImportedBiomeVolume(Map<Integer, Section> sections) {
        if (sections == null || sections.size() > 24) throw new IllegalArgumentException("Too many biome sections");
        Map<Integer, Section> copy = new LinkedHashMap<>();
        for (Map.Entry<Integer, Section> entry : sections.entrySet()) {
            if (entry.getKey() == null || entry.getKey() < -4 || entry.getKey() > 19 || entry.getValue() == null)
                throw new IllegalArgumentException("Source section must be -4..19");
            copy.put(entry.getKey(), entry.getValue());
        }
        this.sections = Collections.unmodifiableMap(copy);
    }
    /** x/z are chunk-local blocks; Y is positive physical engine Y. Missing section returns null. */
    public String sourceAt(int localX, int physicalY, int localZ) {
        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) throw new IllegalArgumentException("Local x/z must be 0..15");
        if (physicalY < 0 || physicalY > 383) return null;
        Section section = sections.get((physicalY >> 4) - 4);
        return section == null ? null : section.at(localX >> 2, (physicalY & 15) >> 2, localZ >> 2);
    }
    public NBTTagCompound toNBT() {
        NBTTagCompound root = new NBTTagCompound(); root.setInteger("Version", VERSION); root.setInteger("YOffset", 64);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Integer, Section> entry : sections.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound(); tag.setByte("Y", entry.getKey().byteValue());
            NBTTagList palette = new NBTTagList();
            for (String name : entry.getValue().palette) palette.appendTag(new NBTTagString(name));
            tag.setTag("Palette", palette); tag.setByteArray("Indices", entry.getValue().indices.clone()); list.appendTag(tag);
        }
        root.setTag("Sections", list); return root;
    }
    public static ImportedBiomeVolume fromNBT(NBTTagCompound root) {
        if (!root.hasKey("Version", 3) || root.getInteger("Version") != VERSION
                || !root.hasKey("YOffset", 3) || root.getInteger("YOffset") != 64 || !root.hasKey("Sections", 9))
            throw new IllegalArgumentException("Unsupported imported biome schema/height transform");
        NBTTagList list = root.getTagList("Sections", 10);
        if (((NBTTagList) root.getTag("Sections")).tagCount() != list.tagCount() || list.tagCount() > 24)
            throw new IllegalArgumentException("Invalid biome section list");
        Map<Integer, Section> sections = new LinkedHashMap<>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            if (!tag.hasKey("Y", 1) || !tag.hasKey("Palette", 9) || !tag.hasKey("Indices", 7))
                throw new IllegalArgumentException("Malformed imported biome section");
            NBTTagList names = tag.getTagList("Palette", 8);
            String[] palette = new String[names.tagCount()];
            for (int j = 0; j < palette.length; j++) palette[j] = names.getStringTagAt(j);
            if (sections.put((int) tag.getByte("Y"), new Section(palette, tag.getByteArray("Indices"))) != null)
                throw new IllegalArgumentException("Duplicate source biome section");
        }
        return new ImportedBiomeVolume(sections);
    }
}
