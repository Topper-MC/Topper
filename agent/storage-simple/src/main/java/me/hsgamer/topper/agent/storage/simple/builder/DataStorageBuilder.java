package me.hsgamer.topper.agent.storage.simple.builder;

import me.hsgamer.hscore.builder.Builder;
import me.hsgamer.topper.agent.storage.simple.converter.FlatEntryConverter;
import me.hsgamer.topper.agent.storage.simple.converter.SqlEntryConverter;
import me.hsgamer.topper.agent.storage.simple.setting.DatabaseSetting;
import me.hsgamer.topper.agent.storage.simple.supplier.*;

import java.io.File;
import java.util.function.Supplier;

public class DataStorageBuilder<K, V> extends Builder<Void, DataStorageSupplier<K, V>> {
    private final File holderBaseFolder;
    private final FlatEntryConverter<K, V> flatEntryConverter;
    private final SqlEntryConverter<K, V> sqlEntryConverter;
    private final Supplier<DataStorageSupplier<K, V>> defaultSupplier;

    public DataStorageBuilder(
            Supplier<DatabaseSetting> databaseSettingSupplier,
            File holderBaseFolder,
            FlatEntryConverter<K, V> flatEntryConverter,
            SqlEntryConverter<K, V> sqlEntryConverter
    ) {
        this.holderBaseFolder = holderBaseFolder;
        this.flatEntryConverter = flatEntryConverter;
        this.sqlEntryConverter = sqlEntryConverter;
        this.defaultSupplier = () -> new FlatStorageSupplier<>(holderBaseFolder, flatEntryConverter);
        register(defaultSupplier, "flat", "properties", "");
        register(v -> new SqliteStorageSupplier<>(databaseSettingSupplier.get(), holderBaseFolder, sqlEntryConverter), "sqlite", "sqlite3");
        register(v -> new NewSqliteStorageSupplier<>(databaseSettingSupplier.get(), holderBaseFolder, sqlEntryConverter), "new-sqlite", "new-sqlite3");
        register(v -> new MySqlStorageSupplier<>(databaseSettingSupplier.get(), sqlEntryConverter), "mysql", "mysql-connector-java", "mysql-connector");
    }

    public DataStorageSupplier<K, V> buildSupplier(String type) {
        return build(type, null).orElseGet(defaultSupplier);
    }

    public File getHolderBaseFolder() {
        return holderBaseFolder;
    }

    public FlatEntryConverter<K, V> getFlatEntryConverter() {
        return flatEntryConverter;
    }

    public SqlEntryConverter<K, V> getSqlEntryConverter() {
        return sqlEntryConverter;
    }
}
