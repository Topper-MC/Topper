package me.hsgamer.topper.storage.core;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DataStorage<K, V> {
    Map<K, V> load();

    CompletableFuture<Void> save(Map<K, V> map, boolean urgent);

    CompletableFuture<Optional<V>> load(K key, boolean urgent);

    CompletableFuture<Void> remove(Collection<K> keys, boolean urgent);

    default void onRegister() {
        // EMPTY
    }

    default void onUnregister() {
        // EMPTY
    }
}
