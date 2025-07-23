package me.hsgamer.topper.storage.bundle;

import me.hsgamer.hscore.builder.Builder;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.flat.properties.PropertiesDataStorage;
import me.hsgamer.topper.storage.sql.mysql.MySqlDataStorageSupplier;
import me.hsgamer.topper.storage.sql.sqlite.NewSqliteDataStorageSupplier;
import me.hsgamer.topper.storage.sql.sqlite.SqliteDataStorageSupplier;

import java.util.function.Function;

public class DataStorageBuilder extends Builder<DataStorageSetting, DataStorageSupplier> {
    private final Function<DataStorageSetting, DataStorageSupplier> defaultSupplier;

    public DataStorageBuilder() {
        this.defaultSupplier = setting -> new DataStorageSupplier() {
            @Override
            public <K, V> DataStorage<K, V> getStorage(String name, ValueConverter<K, V> valueConverter) {
                return new PropertiesDataStorage<>(setting.getBaseFolder(), name, valueConverter.getKeyFlatValueConverter(), valueConverter.getValueFlatValueConverter());
            }
        };
        register(defaultSupplier, "flat", "properties", "");
        register(setting -> {
            final String type = "SQLite";
            SqliteDataStorageSupplier supplier = new SqliteDataStorageSupplier(setting.getBaseFolder(), setting.getDatabaseSetting(), setting.getSqlClientFunction());
            return new DataStorageSupplier() {
                @Override
                public <K, V> DataStorage<K, V> getStorage(String name, ValueConverter<K, V> valueConverter) {
                    return supplier.getStorage(name, valueConverter.getKeySqlValueConverter(type), valueConverter.getValueSqlValueConverter(type));
                }
            };
        }, "sqlite", "sqlite3");
        register(setting -> {
            final String type = "SQLite";
            SqliteDataStorageSupplier supplier = new NewSqliteDataStorageSupplier(setting.getBaseFolder(), setting.getDatabaseSetting(), setting.getSqlClientFunction());
            return new DataStorageSupplier() {
                @Override
                public <K, V> DataStorage<K, V> getStorage(String name, ValueConverter<K, V> valueConverter) {
                    return supplier.getStorage(name, valueConverter.getKeySqlValueConverter(type), valueConverter.getValueSqlValueConverter(type));
                }
            };
        }, "new-sqlite", "new-sqlite3");
        register(setting -> {
            final String type = "MySQL";
            MySqlDataStorageSupplier supplier = new MySqlDataStorageSupplier(setting.getDatabaseSetting(), setting.getSqlClientFunction());
            return new DataStorageSupplier() {
                @Override
                public <K, V> DataStorage<K, V> getStorage(String name, ValueConverter<K, V> valueConverter) {
                    return supplier.getStorage(name, valueConverter.getKeySqlValueConverter(type), valueConverter.getValueSqlValueConverter(type));
                }
            };
        }, "mysql", "mysql-connector-java", "mysql-connector");
    }

    public DataStorageSupplier buildSupplier(String type, DataStorageSetting setting) {
        return build(type, setting).orElseGet(() -> defaultSupplier.apply(setting));
    }
}
