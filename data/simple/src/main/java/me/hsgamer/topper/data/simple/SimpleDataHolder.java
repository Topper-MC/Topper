package me.hsgamer.topper.data.simple;

import me.hsgamer.topper.data.core.DataEntry;
import me.hsgamer.topper.data.core.DataHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleDataHolder<K, V> implements DataHolder<K, V> {
    private final Map<K, DataEntry<K, V>> entryMap = new ConcurrentHashMap<>();
    private final String name;

    public SimpleDataHolder(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @Nullable V getDefaultValue() {
        return null;
    }

    @Override
    public DataEntry<K, V> getOrCreateEntry(K key) {
        return entryMap.computeIfAbsent(key, u -> {
            DataEntry<K, V> entry = new SimpleDataEntry<>(u, this);
            onCreate(entry);
            return entry;
        });
    }

    @Override
    public Optional<DataEntry<K, V>> getEntry(K key) {
        return Optional.empty();
    }

    @Override
    public void removeEntry(K key) {
        Optional.ofNullable(entryMap.remove(key)).ifPresent(this::onRemove);
    }

    @Override
    public Map<K, DataEntry<K, V>> getEntryMap() {
        return Collections.unmodifiableMap(entryMap);
    }

    @Override
    public void clear() {
        entryMap.values().forEach(this::onRemove);
        entryMap.clear();
    }
}
