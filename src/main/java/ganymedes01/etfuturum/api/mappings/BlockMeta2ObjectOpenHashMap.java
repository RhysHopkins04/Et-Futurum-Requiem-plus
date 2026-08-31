package ganymedes01.etfuturum.api.mappings;

import net.minecraft.block.Block;
import net.minecraftforge.oredict.OreDictionary;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Small block-identity + metadata map used by EFR.  It deliberately exposes
 * the same convenience operations EFR used from HogUtils without requiring
 * HogUtils' fastutil-backed implementation.
 */
public class BlockMeta2ObjectOpenHashMap<V> extends AbstractMap<BlockMetaPair, V> {
    private final Map<Block, Map<Integer, V>> data = new IdentityHashMap<>();
    private final boolean wildcardFallback;

    public BlockMeta2ObjectOpenHashMap() {
        this(false);
    }

    public BlockMeta2ObjectOpenHashMap(boolean wildcardFallback) {
        this.wildcardFallback = wildcardFallback;
    }

    public V put(Block block, int meta, V value) {
        return data.computeIfAbsent(block, ignored -> new HashMap<>()).put(meta, value);
    }

    public V putIfAbsent(Block block, int meta, V value) {
        return data.computeIfAbsent(block, ignored -> new HashMap<>()).putIfAbsent(meta, value);
    }

    public V computeIfAbsent(Block block, int meta, BiFunction<Block, Integer, V> factory) {
        Map<Integer, V> metas = data.computeIfAbsent(block, ignored -> new HashMap<>());
        V value = metas.get(meta);
        if (value == null) {
            value = factory.apply(block, meta);
            if (value != null) metas.put(meta, value);
        }
        return value;
    }

    public V get(Block block, int meta) {
        Map<Integer, V> metas = data.get(block);
        if (metas == null) return null;
        V value = metas.get(meta);
        if (value == null && wildcardFallback && meta != OreDictionary.WILDCARD_VALUE) {
            value = metas.get(OreDictionary.WILDCARD_VALUE);
        }
        return value;
    }

    public V getOrDefault(Block block, int meta, V defaultValue) {
        Map<Integer, V> metas = data.get(block);
        if (metas == null) return defaultValue;
        V value = metas.get(meta);
        if (value == null && wildcardFallback && meta != OreDictionary.WILDCARD_VALUE) {
            value = metas.get(OreDictionary.WILDCARD_VALUE);
            if (value != null || metas.containsKey(OreDictionary.WILDCARD_VALUE)) return value;
        }
        return value != null || metas.containsKey(meta) ? value : defaultValue;
    }

    public boolean containsKey(Block block, int meta) {
        Map<Integer, V> metas = data.get(block);
        return metas != null && metas.containsKey(meta);
    }

    public V remove(Block block, int meta) {
        Map<Integer, V> metas = data.get(block);
        if (metas == null) return null;
        V removed = metas.remove(meta);
        if (metas.isEmpty()) data.remove(block);
        return removed;
    }

    @Override
    public V put(BlockMetaPair key, V value) {
        return put(key.get(), key.getMeta(), value);
    }

    @Override
    public V get(Object key) {
        if (!(key instanceof BlockMetaPair pair)) return null;
        return get(pair.get(), pair.getMeta());
    }

    @Override
    public boolean containsKey(Object key) {
        return key instanceof BlockMetaPair pair && containsKey(pair.get(), pair.getMeta());
    }

    @Override
    public V remove(Object key) {
        if (!(key instanceof BlockMetaPair pair)) return null;
        return remove(pair.get(), pair.getMeta());
    }

    @Override
    public int size() {
        int size = 0;
        for (Map<Integer, V> metas : data.values()) size += metas.size();
        return size;
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Set<Entry<BlockMetaPair, V>> entrySet() {
        Set<Entry<BlockMetaPair, V>> entries = new LinkedHashSet<>();
        for (Map.Entry<Block, Map<Integer, V>> blockEntry : data.entrySet()) {
            for (Map.Entry<Integer, V> metaEntry : blockEntry.getValue().entrySet()) {
                entries.add(new SimpleImmutableEntry<>(BlockMetaPair.intern(blockEntry.getKey(), metaEntry.getKey()), metaEntry.getValue()));
            }
        }
        return entries;
    }
}
