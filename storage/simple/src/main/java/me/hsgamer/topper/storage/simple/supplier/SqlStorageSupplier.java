package me.hsgamer.topper.storage.simple.supplier;

import me.hsgamer.hscore.database.Driver;
import me.hsgamer.hscore.database.Setting;
import me.hsgamer.hscore.database.client.sql.BatchBuilder;
import me.hsgamer.hscore.database.client.sql.SqlClient;
import me.hsgamer.hscore.database.client.sql.StatementBuilder;
import me.hsgamer.hscore.database.client.sql.java.JavaSqlClient;
import me.hsgamer.hscore.logger.common.LogLevel;
import me.hsgamer.hscore.logger.common.Logger;
import me.hsgamer.hscore.logger.provider.LoggerProvider;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.simple.converter.ValueConverter;
import me.hsgamer.topper.storage.simple.setting.DatabaseSetting;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public abstract class SqlStorageSupplier implements DataStorageSupplier {
    protected final Logger logger = LoggerProvider.getLogger(getClass());
    private final SqlClient<?> client;
    private final Lock lock = new ReentrantLock();

    protected SqlStorageSupplier(SqlClient<?> client) {
        this.client = client;
    }

    protected SqlStorageSupplier(Driver driver, DatabaseSetting databaseSetting) {
        this(new JavaSqlClient(applyDatabaseSetting(databaseSetting, Setting.create(driver))));
    }

    protected static Setting applyDatabaseSetting(DatabaseSetting databaseSetting, Setting setting) {
        setting.setHost(databaseSetting.getHost());
        setting.setPort(databaseSetting.getPort());
        setting.setDatabaseName(databaseSetting.getDatabase());
        setting.setUsername(databaseSetting.getUsername());
        setting.setPassword(databaseSetting.getPassword());
        if (databaseSetting.isUseSSL()) {
            setting.setDriverProperty("useSSL", "true");
        }
        setting.setDriverProperties(databaseSetting.getDriverProperties());
        setting.setClientProperties(databaseSetting.getClientProperties());
        return setting;
    }

    protected boolean isSingleThread() {
        return false;
    }

    protected abstract List<String> toSaveStatement(String name, String[] keyColumns, String[] valueColumns);

    protected abstract List<Object[]> toSaveValues(Object[] keys, Object[] values);

    private void lock() {
        if (isSingleThread()) {
            lock.lock();
        }
    }

    private void unlock() {
        if (isSingleThread()) {
            lock.unlock();
        }
    }

    @Override
    public <K, V> DataStorage<K, V> getStorage(String name, ValueConverter<K> keyConverter, ValueConverter<V> valueConverter) {
        return new DataStorage<K, V>() {
            @Override
            public Map<K, V> load() {
                lock();
                try (Connection connection = client.getConnection()) {
                    return StatementBuilder.create(connection)
                            .setStatement("SELECT * FROM `" + name + "`;")
                            .queryList(resultSet -> new AbstractMap.SimpleEntry<>(keyConverter.fromSqlResultSet(resultSet), valueConverter.fromSqlResultSet(resultSet)))
                            .stream()
                            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                } catch (SQLException e) {
                    logger.log(LogLevel.ERROR, "Failed to load top holder", e);
                    return Collections.emptyMap();
                } finally {
                    unlock();
                }
            }

            @Override
            public Optional<V> load(K key) {
                lock();
                try (Connection connection = client.getConnection()) {
                    String[] keyColumns = keyConverter.getSqlColumns();
                    Object[] keyValues = keyConverter.toSqlValues(key);

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
                                    ? Optional.ofNullable(valueConverter.fromSqlResultSet(resultSet))
                                    : Optional.empty()
                            );
                } catch (SQLException e) {
                    logger.log(LogLevel.ERROR, "Failed to load top holder", e);
                    return Optional.empty();
                } finally {
                    unlock();
                }
            }

            @Override
            public Optional<Modifier<K, V>> modify() {
                lock();
                try {
                    Connection connection = client.getConnection();
                    connection.setAutoCommit(false);
                    Modifier<K, V> modifier = new Modifier<K, V>() {
                        @Override
                        public void save(Map<K, V> map) throws SQLException {
                            String[] keyColumns = keyConverter.getSqlColumns();
                            String[] valueColumns = valueConverter.getSqlColumns();

                            List<String> statement = toSaveStatement(name, keyColumns, valueColumns);
                            List<List<Object[]>> values = new ArrayList<>();

                            map.forEach((key, value) -> {
                                Object[] keyValues = keyConverter.toSqlValues(key);
                                Object[] valueValues = valueConverter.toSqlValues(value);
                                values.add(toSaveValues(keyValues, valueValues));
                            });

                            for (int i = 0; i < statement.size(); i++) {
                                BatchBuilder batchBuilder = BatchBuilder.create(connection, statement.get(i));
                                for (List<Object[]> value : values) {
                                    batchBuilder.addValues(value.get(i));
                                }
                                batchBuilder.execute();
                            }
                        }

                        @Override
                        public void remove(Collection<K> keys) throws SQLException {
                            String[] keyColumns = keyConverter.getSqlColumns();

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
                                Object[] keyValues = keyConverter.toSqlValues(key);
                                batchBuilder.addValues(keyValues);
                            });
                            batchBuilder.execute();
                        }

                        private void close() {
                            try {
                                connection.close();
                            } catch (SQLException e) {
                                logger.log(LogLevel.ERROR, "Failed to close connection", e);
                            }
                        }

                        @Override
                        public void commit() {
                            try {
                                connection.commit();
                            } catch (SQLException e) {
                                logger.log(LogLevel.ERROR, "Failed to commit", e);
                            } finally {
                                close();
                                unlock();
                            }
                        }

                        @Override
                        public void rollback() {
                            try {
                                connection.rollback();
                            } catch (SQLException e) {
                                logger.log(LogLevel.ERROR, "Failed to rollback", e);
                            } finally {
                                close();
                                unlock();
                            }
                        }
                    };
                    return Optional.of(modifier);
                } catch (SQLException e) {
                    logger.log(LogLevel.ERROR, "Failed to get connection", e);
                    unlock();
                    return Optional.empty();
                }
            }

            @Override
            public void onRegister() {
                lock();
                try (Connection connection = client.getConnection()) {
                    String[] keyColumns = keyConverter.getSqlColumns();
                    String[] keyColumnDefinitions = keyConverter.getSqlColumnDefinitions();
                    String[] valueColumns = valueConverter.getSqlColumns();
                    String[] valueColumnDefinitions = valueConverter.getSqlColumnDefinitions();
                    StringBuilder statement = new StringBuilder("CREATE TABLE IF NOT EXISTS `")
                            .append(name)
                            .append("` (");
                    for (int i = 0; i < keyColumns.length + valueColumns.length; i++) {
                        boolean isKey = i < keyColumns.length;
                        int index = isKey ? i : i - keyColumns.length;
                        statement.append("`")
                                .append(isKey ? keyColumns[index] : valueColumns[index])
                                .append("` ")
                                .append(isKey ? keyColumnDefinitions[index] : valueColumnDefinitions[index]);
                        if (i != keyColumns.length + valueColumns.length - 1) {
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
                    unlock();
                }
            }
        };
    }
}
