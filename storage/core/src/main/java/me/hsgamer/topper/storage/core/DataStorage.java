package me.hsgamer.topper.storage.core;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface DataStorage<K, V> {
    Map<K, V> load();

    Optional<V> load(K key);

    Optional<Modifier<K, V>> modify();

    default Collection<K> keys() {
        return load().keySet();
    }

    default void onRegister() {
        // EMPTY
    }

    default void onUnregister() {
        // EMPTY
    }

    interface Modifier<K, V> {
        void save(Map<K, V> map) throws Exception;

        void remove(Collection<K> keys) throws Exception;

        void commit();

        void rollback();
    }
}
