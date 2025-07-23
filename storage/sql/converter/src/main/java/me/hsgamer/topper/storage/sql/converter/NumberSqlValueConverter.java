package me.hsgamer.topper.storage.sql.converter;

import me.hsgamer.topper.storage.sql.core.SqlValueConverter;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

public class NumberSqlValueConverter<T extends Number> implements SqlValueConverter<T> {
    private final String valueName;
    private final boolean isDoubleValue;
    private final Function<@NotNull Number, @NotNull T> numberFunction;

    public NumberSqlValueConverter(String valueName, boolean isDoubleValue, Function<@NotNull Number, @NotNull T> numberFunction) {
        this.valueName = valueName;
        this.isDoubleValue = isDoubleValue;
        this.numberFunction = numberFunction;
    }

    @Override
    public String[] getSqlColumns() {
        return new String[]{valueName};
    }

    @Override
    public String[] getSqlColumnDefinitions() {
        String columnType = isDoubleValue ? "DOUBLE" : "BIGINT";
        return new String[]{columnType + " NOT NULL"};
    }

    @Override
    public Object[] toSqlValues(@NotNull Number value) {
        return new Object[]{value};
    }

    @Override
    public @NotNull T fromSqlResultSet(@NotNull ResultSet resultSet) throws SQLException {
        Number number = isDoubleValue ? resultSet.getDouble(valueName) : resultSet.getLong(valueName);
        return numberFunction.apply(number);
    }
}
