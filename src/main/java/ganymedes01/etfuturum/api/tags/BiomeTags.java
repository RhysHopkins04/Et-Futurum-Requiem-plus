package ganymedes01.etfuturum.api.tags;

import net.minecraft.world.biome.BiomeGenBase;

import java.util.*;

/** EFR-owned biome tag registry replacing the former HogUtils mixin-backed tags. */
public final class BiomeTags {
    public static final String CONTAINER_ID = "minecraft:worldgen/biome";
    private static final Map<BiomeGenBase, Set<String>> TAGS = new IdentityHashMap<>();
    private static final TagInheritance INHERITANCE = new TagInheritance();

    private BiomeTags() {}

    public static synchronized void addTags(BiomeGenBase biome, String... tags) {
        requireTags(tags);
        if (biome == null) return;
        Collections.addAll(TAGS.computeIfAbsent(biome, ignored -> new LinkedHashSet<>()), tags);
    }
    public static void addTags(int id, String... tags) { addTags(getBiome(id), tags); }

    public static synchronized void removeTags(BiomeGenBase biome, String... tags) {
        requireTags(tags);
        Set<String> set = TAGS.get(biome);
        if (set == null) return;
        set.removeAll(Arrays.asList(tags));
        if (set.isEmpty()) TAGS.remove(biome);
    }
    public static void removeTags(int id, String... tags) { removeTags(getBiome(id), tags); }

    public static synchronized Set<String> getTags(BiomeGenBase biome) {
        Set<String> direct = TAGS.get(biome);
        if (direct == null || direct.isEmpty()) return Collections.emptySet();
        LinkedHashSet<String> result = new LinkedHashSet<>(direct);
        INHERITANCE.expand(result);
        return Collections.unmodifiableSet(result);
    }
    public static Set<String> getTags(int id) { return getTags(getBiome(id)); }
    public static boolean hasTag(BiomeGenBase biome, String tag) { return getTags(biome).contains(tag); }

    public static synchronized Set<BiomeGenBase> getInTag(String tag) {
        Set<BiomeGenBase> result = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BiomeGenBase biome : TAGS.keySet()) if (hasTag(biome, tag)) result.add(biome);
        return Collections.unmodifiableSet(result);
    }

    public static void addInheritors(String parent, String... children) { INHERITANCE.add(parent, children); }
    public static void removeInheritors(String parent, String... children) { INHERITANCE.remove(parent, children); }
    public static Set<String> getInheritors(String tag) { return INHERITANCE.direct(tag); }

    private static BiomeGenBase getBiome(int id) {
        BiomeGenBase[] biomes = BiomeGenBase.getBiomeGenArray();
        return id >= 0 && id < biomes.length ? biomes[id] : null;
    }
    private static void requireTags(String[] tags) {
        if (tags == null || tags.length == 0) throw new IllegalArgumentException("Cannot operate on zero biome tags");
        for (String tag : tags) TagInheritance.validate(tag);
    }
}
