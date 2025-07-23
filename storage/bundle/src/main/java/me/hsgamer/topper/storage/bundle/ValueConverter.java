package me.hsgamer.topper.storage.bundle;

import me.hsgamer.topper.storage.flat.core.FlatValueConverter;
import me.hsgamer.topper.storage.sql.core.SqlValueConverter;

public interface ValueConverter<K, V> {
    static <K, V> ValueConverter<K, V> of(
            FlatValueConverter<K> keyFlatValueConverter,
            FlatValueConverter<V> valueFlatValueConverter,
            SqlValueConverter<K> keySqlValueConverter,
            SqlValueConverter<V> valueSqlValueConverter
    ) {
        return new ValueConverter<K, V>() {
            @Override
            public FlatValueConverter<K> getKeyFlatValueConverter() {
                return keyFlatValueConverter;
            }

            @Override
            public FlatValueConverter<V> getValueFlatValueConverter() {
                return valueFlatValueConverter;
            }

            @Override
            public SqlValueConverter<K> getKeySqlValueConverter(String type) {
                return keySqlValueConverter;
            }

            @Override
            public SqlValueConverter<V> getValueSqlValueConverter(String type) {
                return valueSqlValueConverter;
            }
        };
    }

    FlatValueConverter<K> getKeyFlatValueConverter();

    FlatValueConverter<V> getValueFlatValueConverter();

    SqlValueConverter<K> getKeySqlValueConverter(String type);

    SqlValueConverter<V> getValueSqlValueConverter(String type);
}
