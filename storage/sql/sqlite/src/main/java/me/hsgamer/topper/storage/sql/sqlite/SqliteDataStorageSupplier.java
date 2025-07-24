package me.hsgamer.topper.storage.sql.sqlite;

import me.hsgamer.hscore.database.Setting;
import me.hsgamer.hscore.database.client.sql.SqlClient;
import me.hsgamer.hscore.database.driver.sqlite.SqliteFileDriver;
import me.hsgamer.topper.storage.sql.core.SqlDataStorageSupplier;
import me.hsgamer.topper.storage.sql.core.SqlDatabaseSetting;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class SqliteDataStorageSupplier extends SqlDataStorageSupplier {
    public SqliteDataStorageSupplier(File baseHolder, SqlDatabaseSetting databaseSetting, Options options) {
        super(new SqliteFileDriver(baseHolder), databaseSetting, options);
    }

    public SqliteDataStorageSupplier(File baseHolder, SqlDatabaseSetting databaseSetting, Function<Setting, SqlClient<?>> clientFunction) {
        super(new SqliteFileDriver(baseHolder), databaseSetting, clientFunction);
    }

    @Override
    protected boolean isSingleThread() {
        return true;
    }

    @Override
    protected String getIncrementalKeyDefinition() {
        return "INTEGER AUTOINCREMENT";
    }

    @Override
    protected List<String> toSaveStatement(String name, String[] keyColumns, String[] valueColumns) {
        StringBuilder insertStatement = new StringBuilder("INSERT OR IGNORE INTO `")
                .append(name)
                .append("` (");
        for (int i = 0; i < keyColumns.length + valueColumns.length; i++) {
            insertStatement.append("`")
                    .append(i < keyColumns.length ? keyColumns[i] : valueColumns[i - keyColumns.length])
                    .append("`");
            if (i != keyColumns.length + valueColumns.length - 1) {
                insertStatement.append(", ");
            }
        }
        insertStatement.append(") VALUES (");
        for (int i = 0; i < keyColumns.length + valueColumns.length; i++) {
            insertStatement.append("?");
            if (i != keyColumns.length + valueColumns.length - 1) {
                insertStatement.append(", ");
            }
        }
        insertStatement.append(");");

        StringBuilder updateStatement = new StringBuilder("UPDATE `")
                .append(name)
                .append("` SET ");
        for (int i = 0; i < valueColumns.length; i++) {
            updateStatement.append("`")
                    .append(valueColumns[i])
                    .append("` = ?");
            if (i != valueColumns.length - 1) {
                updateStatement.append(", ");
            }
        }
        updateStatement.append(" WHERE ");
        for (int i = 0; i < keyColumns.length; i++) {
            updateStatement.append("`")
                    .append(keyColumns[i])
                    .append("` = ?");
            if (i != keyColumns.length - 1) {
                updateStatement.append(" AND ");
            }
        }
        updateStatement.append(";");

        return Arrays.asList(
                insertStatement.toString(),
                updateStatement.toString()
        );
    }

    @Override
    protected List<Object[]> toSaveValues(Object[] keys, Object[] values) {
        Object[] insertValues = new Object[keys.length + values.length];
        System.arraycopy(keys, 0, insertValues, 0, keys.length);
        System.arraycopy(values, 0, insertValues, keys.length, values.length);

        Object[] updateValues = new Object[values.length + keys.length];
        System.arraycopy(values, 0, updateValues, 0, values.length);
        System.arraycopy(keys, 0, updateValues, values.length, keys.length);

        return Arrays.asList(
                insertValues,
                updateValues
        );
    }
}
