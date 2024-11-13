package me.hsgamer.topper.agent.storage.simple.builder;

import me.hsgamer.hscore.builder.Builder;
import me.hsgamer.topper.agent.storage.simple.setting.DataStorageBuilderSetting;
import me.hsgamer.topper.agent.storage.simple.supplier.*;

import java.util.function.Function;

public class DataStorageBuilder<K, V> extends Builder<DataStorageBuilderSetting<K, V>, DataStorageSupplier<K, V>> {
    private final Function<DataStorageBuilderSetting<K, V>, DataStorageSupplier<K, V>> defaultSupplier;

    public DataStorageBuilder() {
        this.defaultSupplier = setting -> new FlatStorageSupplier<>(setting.getBaseFolder(), setting.getFlatEntryConverter());
        register(defaultSupplier, "flat", "properties", "");
        register(setting -> new SqliteStorageSupplier<>(setting.getDatabaseSetting(), setting.getBaseFolder(), setting.getSqlEntryConverter()), "sqlite", "sqlite3");
        register(setting -> new NewSqliteStorageSupplier<>(setting.getDatabaseSetting(), setting.getBaseFolder(), setting.getSqlEntryConverter()), "new-sqlite", "new-sqlite3");
        register(setting -> new MySqlStorageSupplier<>(setting.getDatabaseSetting(), setting.getSqlEntryConverter()), "mysql", "mysql-connector-java", "mysql-connector");
    }

    public DataStorageSupplier<K, V> buildSupplier(String type, DataStorageBuilderSetting<K, V> setting) {
        return build(type, setting).orElseGet(() -> defaultSupplier.apply(setting));
    }
}
