package me.hsgamer.topper.storage.sql.sqlite;

import io.github.projectunified.craftdatabase.Setting;
import io.github.projectunified.craftdatabase.client.sql.SqlClient;
import io.github.projectunified.craftdatabase.client.sql.StatementBuilder;
import io.github.projectunified.craftdatabase.driver.sqlite.SqliteFileDriver;
import me.hsgamer.topper.storage.sql.core.SqlDatabaseSetting;
import me.hsgamer.topper.storage.sql.core.SqlDataStorageSupplier;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SqliteDataStorageSupplier extends SqlDataStorageSupplier {
    // ON CONFLICT ... DO UPDATE was added in SQLite 3.24.0
    private static final int UPSERT_MIN_MAJOR = 3;
    private static final int UPSERT_MIN_MINOR = 24;
    private Boolean upsertSupported;

    public SqliteDataStorageSupplier(File baseHolder, SqlDatabaseSetting databaseSetting, Function<Setting, SqlClient<?>> clientFunction) {
        super(new SqliteFileDriver(baseHolder), databaseSetting, clientFunction);
    }

    static boolean isUpsertSupported(String version) {
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return major > UPSERT_MIN_MAJOR || (major == UPSERT_MIN_MAJOR && minor >= UPSERT_MIN_MINOR);
    }

    @Override
    protected boolean isSingleThread() {
        return true;
    }

    @Override
    protected String getIncrementalKeyDefinition() {
        return "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    private boolean isUpsertSupported() {
        if (upsertSupported == null) {
            upsertSupported = queryUpsertSupport();
        }
        return upsertSupported;
    }

    private boolean queryUpsertSupport() {
        try (Connection connection = client.getConnection()) {
            String version = StatementBuilder.create(connection)
                    .setStatement("SELECT sqlite_version();")
                    .query(resultSet -> resultSet.next() ? resultSet.getString(1) : null);
            if (version == null) {
                logger.warning("Cannot get the SQLite version, falling back to the legacy save statements");
                return false;
            }
            return isUpsertSupported(version);
        } catch (SQLException | RuntimeException e) {
            logger.log(Level.WARNING, "Cannot get the SQLite version, falling back to the legacy save statements", e);
            return false;
        }
    }

    private static String columns(String[] keyColumns, String[] valueColumns) {
        return Stream.concat(Arrays.stream(keyColumns), Arrays.stream(valueColumns))
                .map(column -> "`" + column + "`")
                .collect(Collectors.joining(", "));
    }

    private static String assignments(String... columns) {
        return Arrays.stream(columns)
                .map(column -> "`" + column + "` = ?")
                .collect(Collectors.joining(", "));
    }

    @Override
    protected List<String> toSaveStatement(String name, String[] keyColumns, String[] valueColumns) {
        int columnCount = keyColumns.length + valueColumns.length;
        String placeholders = String.join(", ", Collections.nCopies(columnCount, "?"));
        String insertSuffix = " (" + columns(keyColumns, valueColumns) + ") VALUES (" + placeholders + ")";

        if (isUpsertSupported()) {
            String upsert = "INSERT INTO `" + name + "`" + insertSuffix
                    + " ON CONFLICT (" + columns(keyColumns, new String[0]) + ") DO UPDATE SET "
                    + Arrays.stream(valueColumns)
                    .map(column -> "`" + column + "` = EXCLUDED.`" + column + "`")
                    .collect(Collectors.joining(", "))
                    + ";";
            return Collections.singletonList(upsert);
        }

        return Arrays.asList(
                "INSERT OR IGNORE INTO `" + name + "`" + insertSuffix + ";",
                "UPDATE `" + name + "` SET " + assignments(valueColumns)
                        + " WHERE " + Arrays.stream(keyColumns)
                        .map(column -> "`" + column + "` = ?")
                        .collect(Collectors.joining(" AND "))
                        + ";"
        );
    }

    @Override
    protected List<Object[]> toSaveValues(Object[] keys, Object[] values) {
        if (isUpsertSupported()) {
            Object[] queryValues = new Object[keys.length + values.length];
            System.arraycopy(keys, 0, queryValues, 0, keys.length);
            System.arraycopy(values, 0, queryValues, keys.length, values.length);
            return Collections.singletonList(queryValues);
        }

        Object[] insertValues = new Object[keys.length + values.length];
        System.arraycopy(keys, 0, insertValues, 0, keys.length);
        System.arraycopy(values, 0, insertValues, keys.length, values.length);

        Object[] updateValues = new Object[values.length + keys.length];
        System.arraycopy(values, 0, updateValues, 0, values.length);
        System.arraycopy(keys, 0, updateValues, values.length, keys.length);

        return Arrays.asList(insertValues, updateValues);
    }
}
