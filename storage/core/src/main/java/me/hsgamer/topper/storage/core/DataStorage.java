package me.hsgamer.topper.storage.core;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface DataStorage<K, V> {
    Map<K, V> load();

    void save(Map<K, V> map);

    Optional<V> load(K key);

    void remove(Collection<K> keys);

    default void onRegister() {
        // EMPTY
    }

    default void onUnregister() {
        // EMPTY
    }
}
