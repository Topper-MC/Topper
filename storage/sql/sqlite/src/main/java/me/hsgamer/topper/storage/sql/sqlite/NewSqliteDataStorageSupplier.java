package me.hsgamer.topper.storage.sql.sqlite;

import me.hsgamer.hscore.database.Setting;
import me.hsgamer.hscore.database.client.sql.SqlClient;
import me.hsgamer.topper.storage.sql.core.SqlDatabaseSetting;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class NewSqliteDataStorageSupplier extends SqliteDataStorageSupplier {
    public NewSqliteDataStorageSupplier(File baseHolder, SqlDatabaseSetting databaseSetting, Function<Setting, SqlClient<?>> clientFunction) {
        super(baseHolder, databaseSetting, clientFunction);
    }

    @Override
    protected List<String> toSaveStatement(String name, String[] keyColumns, String[] valueColumns) {
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
        statement.append(")");
        statement.append(" ON CONFLICT (");
        for (int i = 0; i < keyColumns.length; i++) {
            statement.append("`")
                    .append(keyColumns[i])
                    .append("`");
            if (i != keyColumns.length - 1) {
                statement.append(", ");
            }
        }
        statement.append(") DO UPDATE SET ");
        for (int i = 0; i < valueColumns.length; i++) {
            statement.append("`")
                    .append(valueColumns[i])
                    .append("` = EXCLUDED.`")
                    .append(valueColumns[i])
                    .append("`");
            if (i != valueColumns.length - 1) {
                statement.append(", ");
            }
        }
        statement.append(";");
        return Collections.singletonList(statement.toString());
    }

    @Override
    protected List<Object[]> toSaveValues(Object[] keys, Object[] values) {
        Object[] queryValues = new Object[keys.length + values.length];
        System.arraycopy(keys, 0, queryValues, 0, keys.length);
        System.arraycopy(values, 0, queryValues, keys.length, values.length);
        return Collections.singletonList(queryValues);
    }
}
