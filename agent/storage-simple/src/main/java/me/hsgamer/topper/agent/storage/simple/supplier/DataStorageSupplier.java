package me.hsgamer.topper.agent.storage.simple.supplier;

import me.hsgamer.topper.agent.storage.DataStorage;

public interface DataStorageSupplier<K, V> {
    DataStorage<K, V> getStorage(String name);

    default void enable() {
    }

    default void disable() {
    }
}
