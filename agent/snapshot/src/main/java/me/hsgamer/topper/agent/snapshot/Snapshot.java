package me.hsgamer.topper.agent.snapshot;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Snapshot<K, V> {
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final Snapshot EMPTY = new Snapshot(Collections.emptyList(), Collections.emptyMap());

    private final List<Map.Entry<K, V>> entryList;
    private final Map<K, Integer> indexMap;

    Snapshot(List<Map.Entry<K, V>> entryList, Map<K, Integer> indexMap) {
        this.entryList = Collections.unmodifiableList(entryList);
        this.indexMap = Collections.unmodifiableMap(indexMap);
    }

    public static <K, V> Snapshot<K, V> empty() {
        //noinspection unchecked
        return (Snapshot<K, V>) EMPTY;
    }

    public boolean isEmpty() {
        return entryList.isEmpty();
    }

    public int size() {
        return entryList.size();
    }

    public int getIndex(K key) {
        return indexMap.getOrDefault(key, -1);
    }

    public Optional<Map.Entry<K, V>> getByIndex(int index) {
        if (index < 0 || index >= entryList.size()) return Optional.empty();
        return Optional.of(entryList.get(index));
    }

    public Optional<V> getValue(K key) {
        return Optional.of(getIndex(key)).filter(i -> i >= 0).flatMap(this::getByIndex).map(Map.Entry::getValue);
    }
}
