package me.hsgamer.topper.spigot.plugin.config;

import me.hsgamer.hscore.config.annotation.Comment;
import me.hsgamer.hscore.config.annotation.ConfigPath;
import me.hsgamer.hscore.database.Setting;
import me.hsgamer.topper.spigot.plugin.config.converter.StringObjectMapConverter;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

public interface DatabaseConfig extends Consumer<Setting> {
    @ConfigPath("host")
    @Comment("The host of the database")
    default String getHost() {
        return "localhost";
    }

    @ConfigPath("port")
    @Comment("The port of the database")
    default String getPort() {
        return "3306";
    }

    @ConfigPath("database")
    @Comment("The database name")
    default String getDatabase() {
        return "topper";
    }

    @ConfigPath("username")
    @Comment("The username to connect to the database")
    default String getUsername() {
        return "root";
    }

    @ConfigPath("password")
    @Comment("The password to connect to the database")
    default String getPassword() {
        return "";
    }

    @ConfigPath("use-ssl")
    @Comment("Whether to use SSL or not")
    default boolean isUseSSL() {
        return false;
    }

    @ConfigPath(value = "driver-properties", converter = StringObjectMapConverter.class)
    @Comment("The driver properties")
    default Map<String, Object> getDriverProperties() {
        return Collections.emptyMap();
    }

    @ConfigPath(value = "client-properties", converter = StringObjectMapConverter.class)
    @Comment("The client properties")
    default Map<String, Object> getClientProperties() {
        return Collections.emptyMap();
    }

    @Override
    default void accept(Setting setting) {
        setting.setHost(getHost());
        setting.setPort(getPort());
        setting.setDatabaseName(getDatabase());
        setting.setUsername(getUsername());
        setting.setPassword(getPassword());
        if (isUseSSL()) {
            setting.setDriverProperty("useSSL", "true");
        }
        setting.setDriverProperties(getDriverProperties());
        setting.setClientProperties(getClientProperties());
    }
}
