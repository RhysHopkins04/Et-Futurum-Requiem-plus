package ganymedes01.etfuturum.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Lightweight weighted random list used by EFR. Entries are unique by
 * {@link Objects#equals(Object, Object)} and {@link #put(Object, double)}
 * replaces the weight of an existing entry.
 */
public final class WeighedRandomList<T> {
    private static final class Entry<T> {
        private final T value;
        private double weight;

        private Entry(T value, double weight) {
            this.value = value;
            this.weight = weight;
        }
    }

    private final List<Entry<T>> entries = new ArrayList<>();
    private final T defaultEntry;
    private double totalWeight;

    public WeighedRandomList() {
        this((T) null);
    }

    public WeighedRandomList(T defaultEntry) {
        this.defaultEntry = defaultEntry;
    }

    public WeighedRandomList(Map<T, Double> chanceMap) {
        this(chanceMap, null);
    }

    public WeighedRandomList(Map<T, Double> chanceMap, T defaultEntry) {
        this(defaultEntry);
        addAll(chanceMap);
    }

    public WeighedRandomList<T> addAll(Map<T, Double> chanceMap) {
        if (chanceMap == null) throw new NullPointerException("chanceMap");
        chanceMap.forEach(this::put);
        return this;
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
        totalWeight = 0.0D;
    }

    public double put(T object, double weight) {
        return insert(object, weight, true);
    }

    public double putIfAbsent(T object, double weight) {
        return insert(object, weight, false);
    }

    private double insert(T object, double weight, boolean reassign) {
        if (weight <= 0.0D) throw new IllegalArgumentException("Weight must be greater than 0!");
        int index = indexOf(object);
        if (index < 0) {
            entries.add(new Entry<>(object, weight));
            totalWeight += weight;
            return -1.0D;
        }

        Entry<T> entry = entries.get(index);
        double previousWeight = entry.weight;
        if (reassign) {
            totalWeight += weight - previousWeight;
            entry.weight = weight;
        }
        return previousWeight;
    }

    public T getOrDefault(Random random, T fallback) {
        if (random == null) throw new NullPointerException("random");
        if (entries.size() == 1) return entries.get(0).value;
        if (entries.isEmpty()) return fallback;

        double selected = random.nextDouble() * totalWeight;
        double accumulated = 0.0D;
        for (Entry<T> entry : entries) {
            accumulated += entry.weight;
            if (accumulated >= selected) return entry.value;
        }
        throw new IllegalStateException("Weighted random selection exceeded the total weight");
    }

    public T get(Random random) {
        return getOrDefault(random, defaultEntry);
    }

    public double remove(T object, double weight) {
        int index = indexOf(object);
        if (index < 0) return -1.0D;
        Entry<T> entry = entries.get(index);
        if (weight > 0.0D && entry.weight != weight) return -1.0D;
        entries.remove(index);
        totalWeight -= entry.weight;
        return entry.weight;
    }

    public double remove(T object) {
        return remove(object, -1.0D);
    }

    public boolean contains(T object) {
        return indexOf(object) >= 0;
    }

    public boolean contains(T object, double weight) {
        int index = indexOf(object);
        return index >= 0 && entries.get(index).weight == weight;
    }

    public double getWeight(T object) {
        int index = indexOf(object);
        return index >= 0 ? entries.get(index).weight : -1.0D;
    }

    private int indexOf(T object) {
        for (int i = 0; i < entries.size(); i++) {
            if (Objects.equals(entries.get(i).value, object)) return i;
        }
        return -1;
    }
}
