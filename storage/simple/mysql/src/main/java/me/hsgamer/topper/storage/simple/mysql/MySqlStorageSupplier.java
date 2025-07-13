package me.hsgamer.topper.storage.simple.mysql;

import me.hsgamer.hscore.database.Setting;
import me.hsgamer.hscore.database.client.sql.SqlClient;
import me.hsgamer.hscore.database.driver.mysql.MySqlDriver;
import me.hsgamer.topper.storage.simple.setting.DatabaseSetting;
import me.hsgamer.topper.storage.simple.sql.SqlStorageSupplier;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class MySqlStorageSupplier extends SqlStorageSupplier {
    public static final String NAME = "mysql";

    public MySqlStorageSupplier(DatabaseSetting databaseSetting, Function<Setting, SqlClient<?>> clientFunction) {
        super(new MySqlDriver(), databaseSetting, clientFunction);
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
        return Collections.singletonList(statement.toString());
    }

    @Override
    protected List<Object[]> toSaveValues(Object[] keys, Object[] values) {
        Object[] queryValues = new Object[keys.length + values.length];
        System.arraycopy(keys, 0, queryValues, 0, keys.length);
        System.arraycopy(values, 0, queryValues, keys.length, values.length);
        return Collections.singletonList(queryValues);
    }

    @Override
    protected String getDriverName() {
        return NAME;
    }
}
