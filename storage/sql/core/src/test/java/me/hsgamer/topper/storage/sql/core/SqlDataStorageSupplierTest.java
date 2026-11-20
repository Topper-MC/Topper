package me.hsgamer.topper.storage.sql.core;

import io.github.projectunified.craftdatabase.Driver;
import io.github.projectunified.craftdatabase.Setting;
import io.github.projectunified.craftdatabase.client.sql.SqlClient;
import me.hsgamer.topper.storage.core.DataStorage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SqlDataStorageSupplierTest {
    private static final SqlDatabaseSetting DATABASE_SETTING = new SqlDatabaseSetting() {
        @Override
        public String getHost() {
            return "localhost";
        }

        @Override
        public String getPort() {
            return "3306";
        }

        @Override
        public String getDatabase() {
            return "test";
        }

        @Override
        public String getUsername() {
            return "test";
        }

        @Override
        public String getPassword() {
            return "";
        }

        @Override
        public boolean isUseSSL() {
            return false;
        }

        @Override
        public Map<String, Object> getDriverProperties() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, Object> getClientProperties() {
            return Collections.emptyMap();
        }
    };

    private static final SqlValueConverter<String> CONVERTER = new SqlValueConverter<String>() {
        @Override
        public String[] getSqlColumns() {
            return new String[]{"id"};
        }

        @Override
        public String[] getSqlColumnDefinitions() {
            return new String[]{"VARCHAR(36)"};
        }

        @Override
        public Object[] toSqlValues(String value) {
            return new Object[]{value};
        }

        @Override
        public String fromSqlResultSet(ResultSet resultSet) throws SQLException {
            return null;
        }
    };

    private static SqlDataStorageSupplier supplier(SqlClient<?> client) {
        Driver driver = new Driver() {
            @Override
            public Class<? extends java.sql.Driver> getDriverClass() {
                return java.sql.Driver.class;
            }

            @Override
            public String convertURL(Setting setting) {
                return "jdbc:test";
            }
        };
        return new SqlDataStorageSupplier(driver, DATABASE_SETTING, setting -> client) {
            @Override
            protected String getIncrementalKeyDefinition() {
                return "INTEGER PRIMARY KEY";
            }

            @Override
            protected List<String> toSaveStatement(String name, String[] keyColumns, String[] valueColumns) {
                return Collections.emptyList();
            }

            @Override
            protected List<Object[]> toSaveValues(Object[] keys, Object[] values) {
                return Collections.emptyList();
            }
        };
    }

    @Test
    public void connectionFailureIsNotCached() {
        CountingClient client = new CountingClient();
        client.fail = true;
        DataStorage<String, String> storage = supplier(client).getStorage("test", CONVERTER, CONVERTER);

        assertTrue(storage.load().isEmpty());
        assertTrue(storage.load().isEmpty());
        assertEquals(2, client.connections.get());
    }

    @Test
    public void commitFailureIsPropagatedAndClosesOnce() {
        CountingClient client = new CountingClient();
        client.fail = false;
        boolean[] closed = {false};
        client.connection = failingCommitConnection(closed);

        DataStorage<String, String> storage = supplier(client).getStorage("test", CONVERTER, CONVERTER);
        DataStorage.Modifier<String, String> modifier = storage.modify().orElseThrow(() -> new AssertionError("modifier expected"));

        try {
            modifier.commit();
            fail("commit failure must be propagated");
        } catch (IllegalStateException expected) {
        }

        modifier.rollback();
        assertTrue(closed[0]);
    }

    private static Connection failingCommitConnection(boolean[] closed) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "commit":
                            throw new SQLException("commit failed");
                        case "close":
                            if (closed[0]) {
                                throw new SQLException("already closed");
                            }
                            closed[0] = true;
                            return null;
                        case "setAutoCommit":
                        case "rollback":
                            return null;
                        case "toString":
                            return "FailingCommitConnection";
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            Class<?> returnType = method.getReturnType();
                            if (returnType == boolean.class) return false;
                            if (returnType == int.class) return 0;
                            if (returnType == long.class) return 0L;
                            return null;
                    }
                }
        );
    }

    private static final class CountingClient implements SqlClient<Object> {
        final AtomicInteger connections = new AtomicInteger();
        boolean fail = true;
        Connection connection;

        @Override
        public Connection getConnection() throws SQLException {
            connections.incrementAndGet();
            if (fail) {
                throw new SQLException("database is down");
            }
            return connection;
        }

        @Override
        public Object getOriginal() {
            return null;
        }

        @Override
        public Setting getSetting() {
            return null;
        }
    }
}
