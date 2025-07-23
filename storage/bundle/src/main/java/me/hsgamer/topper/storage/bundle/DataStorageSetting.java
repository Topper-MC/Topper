package me.hsgamer.topper.storage.bundle;

import me.hsgamer.hscore.database.Setting;
import me.hsgamer.hscore.database.client.sql.SqlClient;
import me.hsgamer.hscore.database.client.sql.java.JavaSqlClient;
import me.hsgamer.topper.storage.sql.core.SqlDatabaseSetting;

import java.io.File;
import java.util.function.Function;

public interface DataStorageSetting {
    SqlDatabaseSetting getDatabaseSetting();

    File getBaseFolder();

    default Function<Setting, SqlClient<?>> getSqlClientFunction() {
        return JavaSqlClient::new;
    }
}
