package me.hsgamer.topper.agent.storage.simple.supplier;

import me.hsgamer.topper.agent.storage.simple.setting.DatabaseSetting;

import java.io.File;

public class NewSqliteStorageSupplier extends SqliteStorageSupplier {
    public NewSqliteStorageSupplier(DatabaseSetting databaseSetting, File baseFolder) {
        super(databaseSetting, baseFolder);
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
        return statement.toString();
    }
}
