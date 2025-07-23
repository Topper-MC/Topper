package me.hsgamer.topper.storage.bundle;

import me.hsgamer.topper.storage.core.DataStorage;

public interface DataStorageSupplier {
    <K, V> DataStorage<K, V> getStorage(String name, ValueConverter<K, V> valueConverter);
}
