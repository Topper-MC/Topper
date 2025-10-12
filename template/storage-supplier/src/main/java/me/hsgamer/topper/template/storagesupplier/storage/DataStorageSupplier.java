package me.hsgamer.topper.template.storagesupplier.storage;

import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.flat.core.FlatValueConverter;
import me.hsgamer.topper.storage.sql.core.SqlValueConverter;

public interface DataStorageSupplier {
    <K, V> DataStorage<K, V> getStorage(
            String name,
            FlatValueConverter<K> keyConverter,
            FlatValueConverter<V> valueConverter,
            SqlValueConverter<K> sqlKeyConverter,
            SqlValueConverter<V> sqlValueConverter
    );
}
