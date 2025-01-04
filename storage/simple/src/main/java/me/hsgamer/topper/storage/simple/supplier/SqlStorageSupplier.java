package me.hsgamer.topper.storage.simple.supplier;

import me.hsgamer.hscore.database.client.sql.BatchBuilder;
import me.hsgamer.hscore.database.client.sql.StatementBuilder;
import me.hsgamer.hscore.logger.common.LogLevel;
import me.hsgamer.hscore.logger.common.Logger;
import me.hsgamer.hscore.logger.provider.LoggerProvider;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.simple.converter.SqlEntryConverter;
import me.hsgamer.topper.storage.simple.setting.DataStorageSetting;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class SqlStorageSupplier implements DataStorageSupplier {
    protected final Logger logger = LoggerProvider.getLogger(getClass());

    protected abstract Connection getConnection() throws SQLException;

    protected abstract void flushConnection(Connection connection);

    protected abstract List<String> toSaveStatement(String name, String[] keyColumns, String[] valueColumns);

    protected abstract List<Object[]> toSaveValues(Object[] keys, Object[] values);

    @Override
    public <K, V> DataStorage<K, V> getStorage(String name, DataStorageSetting<K, V> setting) {
        SqlEntryConverter<K, V> converter = setting.getSqlEntryConverter();

        return new DataStorage<K, V>() {
            @Override
            public Map<K, V> load() {
                Connection connection = null;
                try {
                    connection = getConnection();
                    return StatementBuilder.create(connection)
                            .setStatement("SELECT * FROM `" + name + "`;")
                            .queryList(resultSet -> new AbstractMap.SimpleEntry<>(converter.getKey(resultSet), converter.getValue(resultSet)))
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                } catch (SQLException e) {
                    logger.log(LogLevel.ERROR, "Failed to load top holder", e);
                    return Collections.emptyMap();
                } finally {
                    if (connection != null) {
                        flushConnection(connection);
                    }
                }
            }

            @Override
            public CompletableFuture<Void> save(Map<K, V> map, boolean urgent) {
                Runnable runnable = () -> {
                    Connection connection = null;
                    try {
                        connection = getConnection();
                        String[] keyColumns = converter.getKeyColumns();
                        String[] valueColumns = converter.getValueColumns();

                        List<String> statement = toSaveStatement(name, keyColumns, valueColumns);
                        List<List<Object[]>> values = new ArrayList<>();

                        map.forEach((key, value) -> {
                            Object[] keyValues = converter.toKeyQueryValues(key);
                            Object[] valueValues = converter.toValueQueryValues(value);
                            values.add(toSaveValues(keyValues, valueValues));
                        });

                        for (int i = 0; i < statement.size(); i++) {
                            BatchBuilder batchBuilder = BatchBuilder.create(connection, statement.get(i));
                            for (List<Object[]> value : values) {
                                batchBuilder.addValues(value.get(i));
                            }
                            batchBuilder.execute();
                        }
                    } catch (SQLException e) {
                        logger.log(LogLevel.ERROR, "Failed to save top holder", e);
                    } finally {
                        if (connection != null) {
                            flushConnection(connection);
                        }
                    }
                };
                if (urgent) {
                    runnable.run();
                    return CompletableFuture.completedFuture(null);
                } else {
                    return CompletableFuture.runAsync(runnable);
                }
            }

            @Override
            public CompletableFuture<Optional<V>> load(K key, boolean urgent) {
                Supplier<Optional<V>> supplier = () -> {
                    Connection connection = null;
                    try {
                        connection = getConnection();
                        String[] keyColumns = converter.getKeyColumns();
                        Object[] keyValues = converter.toKeyQueryValues(key);

                        StringBuilder statement = new StringBuilder("SELECT * FROM `")
                                .append(name)
                                .append("` WHERE ");
                        for (int i = 0; i < keyColumns.length; i++) {
                            statement.append("`")
                                    .append(keyColumns[i])
                                    .append("` = ?");
                            if (i != keyColumns.length - 1) {
                                statement.append(" AND ");
                            }
                        }
                        return StatementBuilder.create(connection)
                                .setStatement(statement.toString())
                                .addValues(keyValues)
                                .query(resultSet -> resultSet.next()
                                        ? Optional.of(converter.getValue(resultSet))
                                        : Optional.empty()
                                );
                    } catch (SQLException e) {
                        logger.log(LogLevel.ERROR, "Failed to load top holder", e);
                        return Optional.empty();
                    } finally {
                        if (connection != null) {
                            flushConnection(connection);
                        }
                    }
                };
                if (urgent) {
                    return CompletableFuture.completedFuture(supplier.get());
                } else {
                    return CompletableFuture.supplyAsync(supplier);
                }
            }

            @Override
            public CompletableFuture<Void> remove(Collection<K> keys, boolean urgent) {
                Runnable runnable = () -> {
                    Connection connection = null;
                    try {
                        connection = getConnection();
                        String[] keyColumns = converter.getKeyColumns();

                        StringBuilder statement = new StringBuilder("DELETE FROM `")
                                .append(name)
                                .append("` WHERE ");
                        for (int i = 0; i < keyColumns.length; i++) {
                            statement.append("`")
                                    .append(keyColumns[i])
                                    .append("` = ?");
                            if (i != keyColumns.length - 1) {
                                statement.append(" AND ");
                            }
                        }

                        BatchBuilder batchBuilder = BatchBuilder.create(connection, statement.toString());
                        keys.forEach(key -> {
                            Object[] keyValues = converter.toKeyQueryValues(key);
                            batchBuilder.addValues(keyValues);
                        });
                        batchBuilder.execute();
                    } catch (SQLException e) {
                        logger.log(LogLevel.ERROR, "Failed to remove top holder", e);
                    } finally {
                        if (connection != null) {
                            flushConnection(connection);
                        }
                    }
                };
                if (urgent) {
                    runnable.run();
                    return CompletableFuture.completedFuture(null);
                } else {
                    return CompletableFuture.runAsync(runnable);
                }
            }

            @Override
            public void onRegister() {
                Connection connection = null;
                try {
                    connection = getConnection();
                    String[] keyColumns = converter.getKeyColumns();
                    String[] keyColumnDefinitions = converter.getKeyColumnDefinitions();
                    String[] valueColumnDefinitions = converter.getValueColumnDefinitions();
                    StringBuilder statement = new StringBuilder("CREATE TABLE IF NOT EXISTS `")
                            .append(name)
                            .append("` (");
                    for (int i = 0; i < keyColumnDefinitions.length + valueColumnDefinitions.length; i++) {
                        if (i < keyColumnDefinitions.length) {
                            statement.append(keyColumnDefinitions[i]);
                        } else {
                            statement.append(valueColumnDefinitions[i - keyColumnDefinitions.length]);
                        }
                        if (i != keyColumnDefinitions.length + valueColumnDefinitions.length - 1) {
                            statement.append(", ");
                        }
                    }
                    statement.append(", PRIMARY KEY (");
                    for (int i = 0; i < keyColumns.length; i++) {
                        statement.append("`")
                                .append(keyColumns[i])
                                .append("`");
                        if (i != keyColumns.length - 1) {
                            statement.append(", ");
                        }
                    }
                    statement.append(")").append(");");
                    StatementBuilder.create(connection)
                            .setStatement(statement.toString())
                            .update();
                } catch (SQLException e) {
                    logger.log(LogLevel.ERROR, "Failed to create table", e);
                } finally {
                    if (connection != null) {
                        flushConnection(connection);
                    }
                }
            }
        };
    }
}
