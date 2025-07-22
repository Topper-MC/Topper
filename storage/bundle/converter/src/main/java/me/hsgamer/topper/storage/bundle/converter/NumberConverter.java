package me.hsgamer.topper.storage.bundle.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

public class NumberConverter<T extends Number> implements BundleValueConverter<T> {
    private final String valueName;
    private final boolean isDoubleValue;
    private final Function<@NotNull Number, @NotNull T> numberFunction;

    public NumberConverter(String valueName, boolean isDoubleValue, Function<@NotNull Number, @NotNull T> numberFunction) {
        this.valueName = valueName;
        this.isDoubleValue = isDoubleValue;
        this.numberFunction = numberFunction;
    }

    @Override
    public @NotNull String toString(@NotNull Number value) {
        return value.toString();
    }

    @Override
    public @Nullable T fromString(@NotNull String value) {
        try {
            return numberFunction.apply(isDoubleValue ? Double.parseDouble(value) : Long.parseLong(value));
        } catch (NumberFormatException e) {
            return null;
        }
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
