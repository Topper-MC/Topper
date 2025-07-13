package me.hsgamer.topper.storage.simple.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface SqlValueConverter<T> {
    String[] getSqlColumns();

    String[] getSqlColumnDefinitions();

    Object[] toSqlValues(@NotNull T value);

    @Nullable T fromSqlResultSet(@NotNull ResultSet resultSet) throws SQLException;
}
