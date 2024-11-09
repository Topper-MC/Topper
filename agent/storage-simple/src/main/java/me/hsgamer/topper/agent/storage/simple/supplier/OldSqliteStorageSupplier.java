package me.hsgamer.topper.agent.storage.simple.supplier;

import me.hsgamer.topper.agent.storage.simple.converter.SqlEntryConverter;
import me.hsgamer.topper.agent.storage.simple.setting.DatabaseSetting;

import java.io.File;

public class OldSqliteStorageSupplier<K, V> extends SqliteStorageSupplier<K, V> {
    public OldSqliteStorageSupplier(DatabaseSetting databaseSetting, File holderBaseFolder, SqlEntryConverter<K, V> converter) {
        super(databaseSetting, holderBaseFolder, converter);
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
}
