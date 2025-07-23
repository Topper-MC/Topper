package me.hsgamer.topper.storage.bundle;

import me.hsgamer.topper.storage.flat.core.FlatValueConverter;
import me.hsgamer.topper.storage.sql.core.SqlValueConverter;

public interface ValueConverter<K, V> {
    FlatValueConverter<K> getKeyFlatValueConverter();

    FlatValueConverter<V> getValueFlatValueConverter();

    SqlValueConverter<K> getKeySqlValueConverter(String type);

    SqlValueConverter<V> getValueSqlValueConverter(String type);
}
