package me.hsgamer.topper.storage.simple.setting;

import me.hsgamer.hscore.database.Setting;
import me.hsgamer.hscore.database.client.sql.SqlClient;
import me.hsgamer.hscore.database.client.sql.java.JavaSqlClient;

import java.io.File;
import java.util.function.Function;

public interface DataStorageSetting {
    DatabaseSetting getDatabaseSetting();

    File getBaseFolder();

    default Function<Setting, SqlClient<?>> getSqlClientFunction() {
        return JavaSqlClient::new;
    }
}
