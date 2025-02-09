package me.hsgamer.topper.storage.simple.builder;

import me.hsgamer.hscore.builder.Builder;
import me.hsgamer.topper.storage.simple.flat.FlatStorageSupplier;
import me.hsgamer.topper.storage.simple.mysql.MySqlStorageSupplier;
import me.hsgamer.topper.storage.simple.setting.DataStorageSetting;
import me.hsgamer.topper.storage.simple.sqlite.NewSqliteStorageSupplier;
import me.hsgamer.topper.storage.simple.sqlite.SqliteStorageSupplier;
import me.hsgamer.topper.storage.simple.supplier.DataStorageSupplier;

import java.util.function.Function;

public class DataStorageBuilder extends Builder<DataStorageSetting, DataStorageSupplier> {
    private final Function<DataStorageSetting, DataStorageSupplier> defaultSupplier;

    public DataStorageBuilder() {
        this.defaultSupplier = setting -> new FlatStorageSupplier(setting.getBaseFolder());
        register(defaultSupplier, "flat", "properties", "");
        register(setting -> new SqliteStorageSupplier(setting.getBaseFolder(), setting.getDatabaseSetting(), setting.getSqlClientFunction()), "sqlite", "sqlite3");
        register(setting -> new NewSqliteStorageSupplier(setting.getBaseFolder(), setting.getDatabaseSetting(), setting.getSqlClientFunction()), "new-sqlite", "new-sqlite3");
        register(setting -> new MySqlStorageSupplier(setting.getDatabaseSetting(), setting.getSqlClientFunction()), "mysql", "mysql-connector-java", "mysql-connector");
    }

    public DataStorageSupplier buildSupplier(String type, DataStorageSetting setting) {
        return build(type, setting).orElseGet(() -> defaultSupplier.apply(setting));
    }
}
