package me.hsgamer.topper.storage.simple.builder;

import me.hsgamer.hscore.builder.Builder;
import me.hsgamer.topper.storage.simple.setting.DataStorageBuilderSetting;
import me.hsgamer.topper.storage.simple.supplier.*;

import java.util.function.Function;

public class DataStorageBuilder extends Builder<DataStorageBuilderSetting, DataStorageSupplier> {
    private final Function<DataStorageBuilderSetting, DataStorageSupplier> defaultSupplier;

    public DataStorageBuilder() {
        this.defaultSupplier = setting -> new FlatStorageSupplier(setting.getBaseFolder());
        register(defaultSupplier, "flat", "properties", "");
        register(setting -> new SqliteStorageSupplier(setting.getDatabaseSettingModifier(), setting.getBaseFolder()), "sqlite", "sqlite3");
        register(setting -> new NewSqliteStorageSupplier(setting.getDatabaseSettingModifier(), setting.getBaseFolder()), "new-sqlite", "new-sqlite3");
        register(setting -> new MySqlStorageSupplier(setting.getDatabaseSettingModifier()), "mysql", "mysql-connector-java", "mysql-connector");
    }

    public DataStorageSupplier buildSupplier(String type, DataStorageBuilderSetting setting) {
        return build(type, setting).orElseGet(() -> defaultSupplier.apply(setting));
    }
}