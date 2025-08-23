package me.hsgamer.topper.storage.sql.converter;

import me.hsgamer.topper.storage.sql.core.SqlValueConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BooleanSqlValueConverter implements SqlValueConverter<Boolean> {
    private final String valueName;

    public BooleanSqlValueConverter(String valueName) {
        this.valueName = valueName;
    }

    @Override
    public String[] getSqlColumns() {
        return new String[]{valueName};
    }

    @Override
    public String[] getSqlColumnDefinitions() {
        return new String[]{"BOOLEAN NOT NULL"};
    }

    @Override
    public Object[] toSqlValues(@NotNull Boolean value) {
        return new Object[]{value};
    }

    @Override
    public @Nullable Boolean fromSqlResultSet(@NotNull ResultSet resultSet) throws SQLException {
        return resultSet.getBoolean(valueName);
    }
}
