package me.hsgamer.topper.storage.simple.supplier;

import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.simple.converter.ValueConverter;

public interface DataStorageSupplier {
    <K, V> DataStorage<K, V> getStorage(String name, ValueConverter<K> keyConverter, ValueConverter<V> valueConverter);

    default void enable() {
    }

    default void disable() {
    }
}
