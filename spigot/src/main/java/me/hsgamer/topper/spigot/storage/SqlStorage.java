package me.hsgamer.topper.spigot.storage;

import me.hsgamer.hscore.database.client.sql.StatementBuilder;
import me.hsgamer.topper.core.TopEntry;
import me.hsgamer.topper.core.TopHolder;
import me.hsgamer.topper.core.TopStorage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public abstract class SqlStorage implements TopStorage {
    public abstract Connection getConnection(String name) throws SQLException;

    public abstract void flushConnection(Connection connection);

    public Connection getAndCreateTable(String name) throws SQLException {
        Connection connection = getConnection(name);
        StatementBuilder.create(connection)
                .setStatement("CREATE TABLE IF NOT EXISTS `" + name + "` (`uuid` varchar(36) NOT NULL UNIQUE, `value` double DEFAULT 0);")
                .update();
        return connection;
    }

    @Override
    public CompletableFuture<Map<UUID, BigDecimal>> load(TopHolder holder) {
        String name = holder.getName();
        return CompletableFuture.supplyAsync(() -> {
            Connection connection = null;
            try {
                connection = getAndCreateTable(name);
                return StatementBuilder.create(connection)
                        .setStatement("SELECT * FROM `" + name + "`;")
                        .querySafe(resultSet -> {
                            Map<UUID, BigDecimal> map = new HashMap<>();
                            while (resultSet.next()) {
                                map.put(UUID.fromString(resultSet.getString("uuid")), BigDecimal.valueOf(resultSet.getDouble("value")));
                            }
                            return map;
                        })
                        .orElseGet(Collections::emptyMap);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load top holder", e);
                return Collections.emptyMap();
            } finally {
                if (connection != null) {
                    flushConnection(connection);
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> save(TopEntry topEntry, boolean onUnregister) {
        String name = topEntry.getTopHolder().getName();
        Runnable runnable = () -> {
            Connection connection = null;
            try {
                connection = getAndCreateTable(name);
                boolean exists = StatementBuilder.create(connection)
                        .setStatement("SELECT * FROM `" + name + "` WHERE `uuid` = ?;")
                        .addValues(topEntry.getUuid().toString())
                        .query(ResultSet::next);
                if (exists) {
                    StatementBuilder.create(connection)
                            .setStatement("UPDATE `" + name + "` SET `value` = ? WHERE `uuid` = ?;")
                            .addValues(topEntry.getValue().doubleValue())
                            .addValues(topEntry.getUuid().toString())
                            .update();
                } else {
                    StatementBuilder.create(connection)
                            .setStatement("INSERT INTO `" + name + "` (`uuid`, `value`) VALUES (?, ?);")
                            .addValues(topEntry.getUuid().toString())
                            .addValues(topEntry.getValue().doubleValue())
                            .update();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to save top entry", e);
            } finally {
                if (connection != null) {
                    flushConnection(connection);
                }
            }
        };
        if (onUnregister) {
            runnable.run();
            return CompletableFuture.completedFuture(null);
        } else {
            return CompletableFuture.runAsync(runnable);
        }
    }
}
