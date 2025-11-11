package me.hsgamer.topper.template.storagesupplier.storage;

import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.flat.core.FlatValueConverter;
import me.hsgamer.topper.storage.sql.core.SqlValueConverter;

public interface SqlDataStorageSupplier extends DataStorageSupplier {
    static SqlDataStorageSupplier of(me.hsgamer.topper.storage.sql.core.SqlDataStorageSupplier supplier) {
        return supplier::getStorage;
    }

    <K, V> DataStorage<K, V> getStorage(String name, SqlValueConverter<K> sqlKeyConverter, SqlValueConverter<V> sqlValueConverter);

    @Override
    default <K, V> DataStorage<K, V> getStorage(String name, FlatValueConverter<K> keyConverter, FlatValueConverter<V> valueConverter, SqlValueConverter<K> sqlKeyConverter, SqlValueConverter<V> sqlValueConverter) {
        return getStorage(name, sqlKeyConverter, sqlValueConverter);
    }
}
