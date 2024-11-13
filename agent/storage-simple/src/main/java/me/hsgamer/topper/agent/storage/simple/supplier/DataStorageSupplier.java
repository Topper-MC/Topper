package me.hsgamer.topper.agent.storage.simple.supplier;

import me.hsgamer.topper.agent.storage.DataStorage;
import me.hsgamer.topper.agent.storage.simple.setting.DataStorageSetting;

public interface DataStorageSupplier {
    <K, V> DataStorage<K, V> getStorage(String name, DataStorageSetting<K, V> setting);

    default void enable() {
    }

    default void disable() {
    }
}
