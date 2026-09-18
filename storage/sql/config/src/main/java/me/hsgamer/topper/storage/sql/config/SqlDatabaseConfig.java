package me.hsgamer.topper.storage.sql.config;

import io.github.projectunified.craftconfig.common.Config;
import io.github.projectunified.craftconfig.common.ConfigNode;
import me.hsgamer.topper.storage.sql.core.SqlDatabaseSetting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SqlDatabaseConfig implements SqlDatabaseSetting {
    private final Config config;
    private final ConfigNode hostNode;
    private final ConfigNode portNode;
    private final ConfigNode databaseNode;
    private final ConfigNode usernameNode;
    private final ConfigNode passwordNode;
    private final ConfigNode useSSLNode;
    private final ConfigNode driverPropertiesNode;
    private final ConfigNode clientPropertiesNode;

    public SqlDatabaseConfig(String defaultDatabaseName, Config config, boolean setup) {
        this.config = config;
        hostNode = config.node("host");
        portNode = config.node("port");
        databaseNode = config.node("database");
        usernameNode = config.node("username");
        passwordNode = config.node("password");
        useSSLNode = config.node("use-ssl");
        driverPropertiesNode = config.node("driver-properties");
        clientPropertiesNode = config.node("client-properties");
        if (setup) {
            config.setup();
            hostNode.setIfAbsent("localhost");
            hostNode.setComment(Collections.singletonList("The host of the database"));
            portNode.setIfAbsent("3306");
            portNode.setComment(Collections.singletonList("The port of the database"));
            databaseNode.setIfAbsent(defaultDatabaseName);
            databaseNode.setComment(Collections.singletonList("The database name"));
            usernameNode.setIfAbsent("root");
            usernameNode.setComment(Collections.singletonList("The username to connect to the database"));
            passwordNode.setIfAbsent("");
            passwordNode.setComment(Collections.singletonList("The password to connect to the database"));
            useSSLNode.setIfAbsent(false);
            useSSLNode.setComment(Collections.singletonList("Whether to use SSL or not"));
            driverPropertiesNode.setIfAbsent(new HashMap<>());
            driverPropertiesNode.setComment(Collections.singletonList("The driver properties"));
            clientPropertiesNode.setIfAbsent(new HashMap<>());
            clientPropertiesNode.setComment(Collections.singletonList("The client properties"));
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
        return hostNode.get(String.class);
    }

    @Override
    public String getPort() {
        return portNode.get(String.class);
    }

    @Override
    public String getDatabase() {
        return databaseNode.get(String.class);
    }

    @Override
    public String getUsername() {
        return usernameNode.get(String.class);
    }

    @Override
    public String getPassword() {
        return passwordNode.get(String.class);
    }

    @Override
    public boolean isUseSSL() {
        return useSSLNode.get(Boolean.class);
    }

    @Override
    public Map<String, Object> getDriverProperties() {
        return driverPropertiesNode.getChildren().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getNormalized()
        ));
    }

    @Override
    public Map<String, Object> getClientProperties() {
        return clientPropertiesNode.getChildren().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getNormalized()
        ));
    }
}
