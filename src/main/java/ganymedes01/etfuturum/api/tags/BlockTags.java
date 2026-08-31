package ganymedes01.etfuturum.api.tags;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.api.mappings.BlockMetaPair;
import ganymedes01.etfuturum.core.utils.RecipeHelper;
import net.minecraft.block.Block;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;

/** EFR-owned block tag registry replacing the former HogUtils mixin-backed tags. */
public final class BlockTags {
    public static final String CONTAINER_ID = "minecraft:blocks";
    private static final Map<Block, Map<Integer, Set<String>>> TAGS = new IdentityHashMap<>();
    private static final TagInheritance INHERITANCE = new TagInheritance();

    private BlockTags() {}

    public static synchronized void addTags(Block block, int meta, String... tags) {
        requireTags(tags);
        if (!RecipeHelper.validateItems(block)) return;
        Set<String> set = TAGS.computeIfAbsent(block, ignored -> new HashMap<>())
            .computeIfAbsent(meta, ignored -> new LinkedHashSet<>());
        Collections.addAll(set, tags);
    }

    public static void addTags(Block block, String... tags) { addTags(block, OreDictionary.WILDCARD_VALUE, tags); }

    public static void addTagsByID(String modid, String name, int meta, String... tags) {
        Block block = GameRegistry.findBlock(modid, name);
        if (block != null) addTags(block, meta, tags);
    }
    public static void addTagsByID(String modid, String name, String... tags) { addTagsByID(modid, name, OreDictionary.WILDCARD_VALUE, tags); }

    public static synchronized void removeTags(Block block, int meta, String... tags) {
        requireTags(tags);
        Map<Integer, Set<String>> metas = TAGS.get(block);
        if (metas == null) return;
        Set<String> set = metas.get(meta);
        if (set == null) return;
        set.removeAll(Arrays.asList(tags));
        if (set.isEmpty()) metas.remove(meta);
        if (metas.isEmpty()) TAGS.remove(block);
    }
    public static void removeTags(Block block, String... tags) { removeTags(block, OreDictionary.WILDCARD_VALUE, tags); }

    public static synchronized Set<String> getTags(Block block, int meta) {
        Map<Integer, Set<String>> metas = TAGS.get(block);
        if (metas == null) return Collections.emptySet();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Set<String> exact = metas.get(meta);
        if (exact != null) result.addAll(exact);
        if (meta != OreDictionary.WILDCARD_VALUE) {
            Set<String> wildcard = metas.get(OreDictionary.WILDCARD_VALUE);
            if (wildcard != null) result.addAll(wildcard);
        }
        INHERITANCE.expand(result);
        return result.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(result);
    }
    public static Set<String> getTags(Block block) { return getTags(block, OreDictionary.WILDCARD_VALUE); }
    public static boolean hasTag(Block block, String tag) { return hasTag(block, OreDictionary.WILDCARD_VALUE, tag); }
    public static boolean hasTag(Block block, int meta, String tag) { return getTags(block, meta).contains(tag); }

    public static synchronized Set<BlockMetaPair> getInTag(String tag) {
        LinkedHashSet<BlockMetaPair> result = new LinkedHashSet<>();
        for (Map.Entry<Block, Map<Integer, Set<String>>> blockEntry : TAGS.entrySet()) {
            for (Integer meta : blockEntry.getValue().keySet()) {
                if (hasTag(blockEntry.getKey(), meta, tag)) result.add(BlockMetaPair.intern(blockEntry.getKey(), meta));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public static void addInheritors(String parent, String... children) { INHERITANCE.add(parent, children); }
    public static void removeInheritors(String parent, String... children) { INHERITANCE.remove(parent, children); }
    public static Set<String> getInheritors(String tag) { return INHERITANCE.direct(tag); }

    private static void requireTags(String[] tags) {
        if (tags == null || tags.length == 0) throw new IllegalArgumentException("Cannot operate on zero block tags");
        for (String tag : tags) TagInheritance.validate(tag);
    }
}
