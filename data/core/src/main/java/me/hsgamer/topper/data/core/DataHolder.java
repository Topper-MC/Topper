package me.hsgamer.topper.data.core;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public interface DataHolder<K, V> {
    String getName();

    @Nullable V getDefaultValue();

    DataEntry<K, V> getOrCreateEntry(K key);

    Optional<DataEntry<K, V>> getEntry(K key);

    void removeEntry(K key);

    Map<K, DataEntry<K, V>> getEntryMap();

    void clear();

    default void onCreate(DataEntry<K, V> entry) {
    }

    default void onRemove(DataEntry<K, V> entry) {
    }

    default void onUpdate(DataEntry<K, V> entry, V oldValue, V newValue) {
    }
}
