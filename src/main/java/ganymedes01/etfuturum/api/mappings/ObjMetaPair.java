package ganymedes01.etfuturum.api.mappings;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** Immutable identity + metadata pair used by EFR registries and tags. */
public class ObjMetaPair<T> {
    private final T object;
    private final int meta;

    protected ObjMetaPair(T object, int meta) {
        this.object = object;
        this.meta = meta;
    }

    public T get() { return object; }
    public int getMeta() { return meta; }
    public T getLeft() { return object; }
    public Integer getRight() { return meta; }

    public boolean matches(Object compareObject, int compareMeta) {
        return object == compareObject && (meta == compareMeta
            || meta == OreDictionary.WILDCARD_VALUE
            || compareMeta == OreDictionary.WILDCARD_VALUE);
    }

    public ItemStack newItemStack() { return newItemStack(1); }
    public ItemStack newItemStack(int count) {
        if (object instanceof Item item) return new ItemStack(item, count, meta);
        if (object instanceof Block block) return new ItemStack(block, count, meta);
        throw new IllegalStateException("Cannot create an ItemStack for " + object);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof ObjMetaPair<?> pair && matches(pair.get(), pair.getMeta());
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(object);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{object=" + object + ", meta=" + meta + '}';
    }
}
