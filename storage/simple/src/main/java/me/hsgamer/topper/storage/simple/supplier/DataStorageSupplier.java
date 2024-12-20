package me.hsgamer.topper.storage.simple.supplier;

import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.simple.setting.DataStorageSetting;

public interface DataStorageSupplier {
    <K, V> DataStorage<K, V> getStorage(String name, DataStorageSetting<K, V> setting);

    default void enable() {
    }

    default void disable() {
    }
}
