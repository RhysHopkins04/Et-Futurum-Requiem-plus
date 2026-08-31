package ganymedes01.etfuturum.api.mappings;

import net.minecraft.item.Item;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class ItemMetaPair extends ObjMetaPair<Item> {
    private static final Map<Item, Map<Integer, ItemMetaPair>> INTERNER = new IdentityHashMap<>();

    public ItemMetaPair(Item item, int meta) {
        super(item, meta);
    }

    public static synchronized ItemMetaPair intern(Item item, int meta) {
        return INTERNER.computeIfAbsent(item, ignored -> new HashMap<>())
            .computeIfAbsent(meta, ignored -> new ItemMetaPair(item, meta));
    }

    public static ItemMetaPair intern(Item item) {
        return intern(item, OreDictionary.WILDCARD_VALUE);
    }

    public static ItemMetaPair of(Item item, int meta) {
        return new ItemMetaPair(item, meta);
    }
}
