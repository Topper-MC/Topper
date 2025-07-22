package me.hsgamer.topper.storage.sql.core;

import java.util.Map;

public interface SqlDatabaseSetting {
    String getHost();

    String getPort();

    String getDatabase();

    String getUsername();

    String getPassword();

    boolean isUseSSL();

    Map<String, Object> getDriverProperties();

    Map<String, Object> getClientProperties();
}
