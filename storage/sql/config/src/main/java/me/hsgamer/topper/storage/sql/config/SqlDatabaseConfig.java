package me.hsgamer.topper.storage.sql.config;

import me.hsgamer.hscore.config.Config;
import me.hsgamer.hscore.config.PathString;
import me.hsgamer.topper.storage.sql.core.SqlDatabaseSetting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SqlDatabaseConfig implements SqlDatabaseSetting {
    private final Config config;

    public SqlDatabaseConfig(String defaultDatabaseName, Config config, boolean setup) {
        this.config = config;
        if (setup) {
            config.setup();
            config.setIfAbsent("localhost", "host");
            config.setComment(Collections.singletonList("The host of the database"), "host");
            config.setIfAbsent("3306", "port");
            config.setComment(Collections.singletonList("The port of the database"), "port");
            config.setIfAbsent(defaultDatabaseName, "database");
            config.setComment(Collections.singletonList("The database name"), "database");
            config.setIfAbsent("root", "username");
            config.setComment(Collections.singletonList("The username to connect to the database"), "username");
            config.setIfAbsent("", "password");
            config.setComment(Collections.singletonList("The password to connect to the database"), "password");
            config.setIfAbsent(false, "use-ssl");
            config.setComment(Collections.singletonList("Whether to use SSL or not"), "use-ssl");
            config.setIfAbsent(new HashMap<>(), "driver-properties");
            config.setComment(Collections.singletonList("The driver properties"), "driver-properties");
            config.setIfAbsent(new HashMap<>(), "client-properties");
            config.setComment(Collections.singletonList("The client properties"), "client-properties");
            config.save();
        }
    }

    public SqlDatabaseConfig(String defaultDatabaseConfig, Config config) {
        this(defaultDatabaseConfig, config, true);
    }

    public Config getConfig() {
        return config;
    }

    @Override
    public String getHost() {
        return Objects.toString(config.getNormalized("host"));
    }

    @Override
    public String getPort() {
        return Objects.toString(config.getNormalized("port"));
    }

    @Override
    public String getDatabase() {
        return Objects.toString(config.getNormalized("database"));
    }

    @Override
    public String getUsername() {
        return Objects.toString(config.getNormalized("username"));
    }

    @Override
    public String getPassword() {
        return Objects.toString(config.getNormalized("password"));
    }

    @Override
    public boolean isUseSSL() {
        return Boolean.parseBoolean(Objects.toString(config.getNormalized("use-ssl")));
    }

    @Override
    public Map<String, Object> getDriverProperties() {
        return PathString.join(".", config.getNormalizedValues(false, "driver-properties"));
    }

    @Override
    public Map<String, Object> getClientProperties() {
        return PathString.join(".", config.getNormalizedValues(false, "client-properties"));
    }
}
