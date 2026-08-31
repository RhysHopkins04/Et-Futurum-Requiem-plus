package ganymedes01.etfuturum.api.tags;

import java.util.*;

/** Shared, lightweight tag inheritance graph for EFR-local tags. */
final class TagInheritance {
    private final Map<String, Set<String>> inherited = new HashMap<>();

    synchronized void add(String parent, String... children) {
        validate(parent);
        for (String child : children) {
            validate(child);
            if (parent.equals(child) || reaches(child, parent, new HashSet<>())) {
                throw new UnsupportedOperationException("Recursion detected when adding tag inheritance " + parent + " -> " + child);
            }
            inherited.computeIfAbsent(parent, ignored -> new LinkedHashSet<>()).add(child);
        }
    }

    synchronized void remove(String parent, String... children) {
        Set<String> values = inherited.get(parent);
        if (values == null) return;
        values.removeAll(Arrays.asList(children));
        if (values.isEmpty()) inherited.remove(parent);
    }

    synchronized Set<String> direct(String tag) {
        Set<String> values = inherited.get(tag);
        return values == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    synchronized void expand(Set<String> tags) {
        ArrayDeque<String> queue = new ArrayDeque<>(tags);
        while (!queue.isEmpty()) {
            String parent = queue.removeFirst();
            Set<String> children = inherited.get(parent);
            if (children == null) continue;
            for (String child : children) if (tags.add(child)) queue.addLast(child);
        }
    }

    private boolean reaches(String from, String target, Set<String> visited) {
        if (!visited.add(from)) return false;
        Set<String> children = inherited.get(from);
        if (children == null) return false;
        if (children.contains(target)) return true;
        for (String child : children) if (reaches(child, target, visited)) return true;
        return false;
    }

    static void validate(String tag) {
        if (tag == null || tag.isEmpty() || !tag.contains(":")) {
            throw new IllegalArgumentException("Tags must be non-empty namespaced identifiers: " + tag);
        }
    }
}
