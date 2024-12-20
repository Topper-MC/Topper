package me.hsgamer.topper.storage.simple.supplier;

import me.hsgamer.hscore.database.Setting;
import me.hsgamer.hscore.database.client.sql.java.JavaSqlClient;
import me.hsgamer.hscore.database.driver.sqlite.SqliteFileDriver;
import me.hsgamer.hscore.logger.common.LogLevel;

import java.io.File;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class SqliteStorageSupplier extends SqlStorageSupplier {
    private final JavaSqlClient client;
    private final AtomicReference<Connection> connectionReference = new AtomicReference<>();

    public SqliteStorageSupplier(Consumer<Setting> databaseSettingConsumer, File baseHolder) {
        Setting setting = Setting.create(new SqliteFileDriver(baseHolder));
        databaseSettingConsumer.accept(setting);
        client = new JavaSqlClient(setting);
    }

    @Override
    protected Connection getConnection() {
        return connectionReference.updateAndGet(connection -> {
            try {
                if (connection == null || connection.isClosed()) {
                    return client.getConnection();
                } else {
                    return connection;
                }
            } catch (Exception e) {
                logger.log(LogLevel.ERROR, "Failed to get the connection", e);
                return null;
            }
        });
    }

    @Override
    protected void flushConnection(Connection connection) {
        // EMPTY
    }

    @Override
    protected String toSaveStatement(String name, String[] keyColumns, String[] valueColumns) {
        StringBuilder statement = new StringBuilder("INSERT OR REPLACE INTO `")
                .append(name)
                .append("` (");
        for (int i = 0; i < keyColumns.length + valueColumns.length; i++) {
            statement.append("`")
                    .append(i < keyColumns.length ? keyColumns[i] : valueColumns[i - keyColumns.length])
                    .append("`");
            if (i != keyColumns.length + valueColumns.length - 1) {
                statement.append(", ");
            }
        }
        statement.append(") VALUES (");
        for (int i = 0; i < keyColumns.length + valueColumns.length; i++) {
            statement.append("?");
            if (i != keyColumns.length + valueColumns.length - 1) {
                statement.append(", ");
            }
        }
        statement.append(");");
        return statement.toString();
    }

    @Override
    protected Object[] toSaveValues(Object[] keys, Object[] values) {
        Object[] queryValues = new Object[keys.length + values.length];
        System.arraycopy(keys, 0, queryValues, 0, keys.length);
        System.arraycopy(values, 0, queryValues, keys.length, values.length);
        return queryValues;
    }

    @Override
    public void disable() {
        Connection connection = connectionReference.getAndSet(null);
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            logger.log(LogLevel.ERROR, "Failed to close the connection", e);
        }
    }
}
