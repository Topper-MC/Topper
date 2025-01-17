package me.hsgamer.topper.storage.simple.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public interface ValueConverter<T> {
    @NotNull String toRawString(@NotNull T value);

    @Nullable T fromRawString(@NotNull String value);

    @NotNull Map<String, Object> toObjectMap(@NotNull T value);

    @Nullable T fromObjectMap(@NotNull Map<String, Object> map);

    String[] getSqlColumns();

    String[] getSqlColumnDefinitions();

    Object[] toSqlValues(@NotNull T value);

    @Nullable T fromSqlResultSet(@NotNull ResultSet resultSet) throws SQLException;
}
