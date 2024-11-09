package me.hsgamer.topper.agent.storage.simple.supplier;

import me.hsgamer.hscore.database.Setting;
import me.hsgamer.hscore.database.client.sql.java.JavaSqlClient;
import me.hsgamer.hscore.database.driver.mysql.MySqlDriver;
import me.hsgamer.hscore.logger.common.LogLevel;
import me.hsgamer.topper.agent.storage.simple.converter.SqlEntryConverter;
import me.hsgamer.topper.agent.storage.simple.setting.DatabaseSetting;

import java.sql.Connection;
import java.sql.SQLException;

public class MySqlStorageSupplier<K, V> extends SqlStorageSupplier<K, V> {
    private final JavaSqlClient client;

    public MySqlStorageSupplier(DatabaseSetting databaseSetting, SqlEntryConverter<K, V> converter) {
        super(converter);
        Setting setting = Setting.create(new MySqlDriver())
                .setDatabaseName(databaseSetting.getDatabase())
                .setHost(databaseSetting.getHost())
                .setPort(databaseSetting.getPort())
                .setUsername(databaseSetting.getUsername())
                .setPassword(databaseSetting.getPassword())
                .setClientProperties(databaseSetting.getClientProperties())
                .setDriverProperties(databaseSetting.getDriverProperties());
        if (databaseSetting.isUseSSL()) {
            setting.setDriverProperty("useSSL", "true");
        }
        client = new JavaSqlClient(setting);
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return client.getConnection();
    }

    @Override
    protected void flushConnection(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            logger.log(LogLevel.ERROR, "Failed to close connection", e);
        }
    }

    @Override
    protected String toSaveStatement(String name, String[] keyColumns, String[] valueColumns) {
        StringBuilder statement = new StringBuilder("INSERT INTO `")
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
        statement.append(") ON DUPLICATE KEY UPDATE ");
        for (int i = 0; i < valueColumns.length; i++) {
            statement.append("`")
                    .append(valueColumns[i])
                    .append("` = VALUES(`")
                    .append(valueColumns[i])
                    .append("`)");
            if (i != valueColumns.length - 1) {
                statement.append(", ");
            }
        }
        statement.append(";");
        return statement.toString();
    }

    @Override
    protected Object[] toSaveValues(Object[] keys, Object[] values) {
        Object[] queryValues = new Object[keys.length + values.length];
        System.arraycopy(keys, 0, queryValues, 0, keys.length);
        System.arraycopy(values, 0, queryValues, keys.length, values.length);
        return queryValues;
    }
}
