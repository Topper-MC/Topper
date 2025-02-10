package me.hsgamer.topper.core;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DataHolder<K, V> {
    private final Map<K, DataEntry<K, V>> entryMap = new ConcurrentHashMap<>();
    private final String name;

    protected DataHolder(String name) {
        this.name = name;
    }

    protected V getDefaultValue() {
        return null;
    }

    protected void onCreate(DataEntry<K, V> entry) {
    }

    protected void onRemove(DataEntry<K, V> entry) {
    }

    protected void onUpdate(DataEntry<K, V> entry, V oldValue, V newValue) {
    }

    public final String getName() {
        return name;
    }

    public final Map<K, DataEntry<K, V>> getEntryMap() {
        return Collections.unmodifiableMap(entryMap);
    }

    public final Optional<DataEntry<K, V>> getEntry(K key) {
        return Optional.ofNullable(entryMap.get(key));
    }

    public final DataEntry<K, V> getOrCreateEntry(K key) {
        return entryMap.computeIfAbsent(key, u -> {
            DataEntry<K, V> entry = new DataEntry<>(u, this);
            onCreate(entry);
            return entry;
        });
    }

    public final void removeEntry(K key) {
        Optional.ofNullable(entryMap.remove(key)).ifPresent(this::onRemove);
    }

    public final void clear() {
        entryMap.clear();
    }
}
