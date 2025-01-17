package me.hsgamer.topper.storage.simple.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public interface ValueConverter<T> {
    @NotNull String parseString(@NotNull T value);

    @Nullable T parseString(@NotNull String value);

    @NotNull Map<String, Object> parseObjectMap(@NotNull T value);

    @Nullable T parseObjectMap(@NotNull Map<String, Object> map);

    String[] getSqlColumns();

    String[] getSqlColumnDefinitions();

    Object[] toSqlValues(@NotNull T value);

    @Nullable T parseSqlResultSet(@NotNull ResultSet resultSet) throws SQLException;
}
