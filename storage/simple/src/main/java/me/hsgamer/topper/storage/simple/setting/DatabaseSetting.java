package me.hsgamer.topper.storage.simple.setting;

import java.util.Map;

public interface DatabaseSetting {
    String getHost();

    String getPort();

    String getDatabase();

    String getUsername();

    String getPassword();

    boolean isUseSSL();

    Map<String, Object> getDriverProperties();

    Map<String, Object> getClientProperties();
}
