package me.hsgamer.topper.storage.simple.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public class NumberConverter<T extends Number> implements ValueConverter<T> {
    private final String valueName;
    private final boolean isDoubleValue;
    private final Function<@NotNull Number, @NotNull T> numberFunction;

    public NumberConverter(String valueName, boolean isDoubleValue, Function<@NotNull Number, @NotNull T> numberFunction) {
        this.valueName = valueName;
        this.isDoubleValue = isDoubleValue;
        this.numberFunction = numberFunction;
    }

    @Override
    public @NotNull String toRawString(@NotNull Number value) {
        return value.toString();
    }

    @Override
    public @Nullable T fromRawString(@NotNull String value) {
        try {
            return numberFunction.apply(isDoubleValue ? Double.parseDouble(value) : Long.parseLong(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public @NotNull Map<String, Object> toObjectMap(@NotNull Number value) {
        return Collections.singletonMap(valueName, value);
    }

    @Override
    public @Nullable T fromObjectMap(@NotNull Map<String, Object> map) {
        Object value = map.get(valueName);
        Number number;
        if (value instanceof Number) {
            number = (Number) value;
        } else {
            try {
                number = isDoubleValue ? Double.parseDouble(value.toString()) : Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return numberFunction.apply(number);
    }

    @Override
    public @NotNull SqlValueConverter<T> getSqlValueConverter(String driverType) {
        return new SqlValueConverter<T>() {
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
        };
    }
}
