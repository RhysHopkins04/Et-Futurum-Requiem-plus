package ganymedes01.etfuturum.api.tags;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.api.mappings.ItemMetaPair;
import ganymedes01.etfuturum.core.utils.RecipeHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;

/** EFR-owned item tag registry replacing the former HogUtils mixin-backed tags. */
public final class ItemTags {
    public static final String CONTAINER_ID = "minecraft:items";
    private static final Map<Item, Map<Integer, Set<String>>> TAGS = new IdentityHashMap<>();
    private static final TagInheritance INHERITANCE = new TagInheritance();

    private ItemTags() {}

    public static synchronized void addTags(Item item, int meta, String... tags) {
        requireTags(tags);
        if (!RecipeHelper.validateItems(item)) return;
        Set<String> set = TAGS.computeIfAbsent(item, ignored -> new HashMap<>())
            .computeIfAbsent(meta, ignored -> new LinkedHashSet<>());
        Collections.addAll(set, tags);
    }
    public static void addTags(Item item, String... tags) { addTags(item, OreDictionary.WILDCARD_VALUE, tags); }
    public static void addTags(ItemStack stack, String... tags) { if (stack != null && stack.getItem() != null) addTags(stack.getItem(), stack.getItemDamage(), tags); }

    public static void addTagsByID(String modid, String name, int meta, String... tags) {
        Item item = GameRegistry.findItem(modid, name);
        if (item != null) addTags(item, meta, tags);
    }
    public static void addTagsByID(String modid, String name, String... tags) { addTagsByID(modid, name, OreDictionary.WILDCARD_VALUE, tags); }

    public static synchronized void removeTags(Item item, int meta, String... tags) {
        requireTags(tags);
        Map<Integer, Set<String>> metas = TAGS.get(item);
        if (metas == null) return;
        Set<String> set = metas.get(meta);
        if (set == null) return;
        set.removeAll(Arrays.asList(tags));
        if (set.isEmpty()) metas.remove(meta);
        if (metas.isEmpty()) TAGS.remove(item);
    }
    public static void removeTags(Item item, String... tags) { removeTags(item, OreDictionary.WILDCARD_VALUE, tags); }
    public static void removeTags(ItemStack stack, String... tags) { if (stack != null && stack.getItem() != null) removeTags(stack.getItem(), stack.getItemDamage(), tags); }

    public static synchronized Set<String> getTags(Item item, int meta) {
        Map<Integer, Set<String>> metas = TAGS.get(item);
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
    public static Set<String> getTags(Item item) { return getTags(item, OreDictionary.WILDCARD_VALUE); }
    public static boolean hasTag(Item item, String tag) { return hasTag(item, OreDictionary.WILDCARD_VALUE, tag); }
    public static boolean hasTag(Item item, int meta, String tag) { return getTags(item, meta).contains(tag); }
    public static boolean hasTag(ItemStack stack, String tag) { return stack != null && stack.getItem() != null && hasTag(stack.getItem(), stack.getItemDamage(), tag); }

    public static synchronized Set<ItemMetaPair> getInTag(String tag) {
        LinkedHashSet<ItemMetaPair> result = new LinkedHashSet<>();
        for (Map.Entry<Item, Map<Integer, Set<String>>> itemEntry : TAGS.entrySet()) {
            for (Integer meta : itemEntry.getValue().keySet()) {
                if (hasTag(itemEntry.getKey(), meta, tag)) result.add(ItemMetaPair.intern(itemEntry.getKey(), meta));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public static void addInheritors(String parent, String... children) { INHERITANCE.add(parent, children); }
    public static void removeInheritors(String parent, String... children) { INHERITANCE.remove(parent, children); }
    public static Set<String> getInheritors(String tag) { return INHERITANCE.direct(tag); }

    private static void requireTags(String[] tags) {
        if (tags == null || tags.length == 0) throw new IllegalArgumentException("Cannot operate on zero item tags");
        for (String tag : tags) TagInheritance.validate(tag);
    }
}
