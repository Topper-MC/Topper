package me.hsgamer.topper.storage.simple.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class SimpleValueConverter<T> implements ValueConverter<T> {
    private final Function<@NotNull T, @NotNull String> toStringConverter;
    private final Function<@NotNull String, @Nullable T> fromStringConverter;
    private final String valueName;
    private final String sqlType;
    private final boolean isStringNationalized;

    public SimpleValueConverter(Function<@NotNull T, @NotNull String> toStringConverter, Function<@NotNull String, @Nullable T> fromStringConverter, String valueName, String sqlType, boolean isStringNationalized) {
        this.toStringConverter = toStringConverter;
        this.fromStringConverter = fromStringConverter;
        this.valueName = valueName;
        this.sqlType = sqlType;
        this.isStringNationalized = isStringNationalized;
    }

    public SimpleValueConverter(Function<@NotNull T, @NotNull String> toStringConverter, Function<@NotNull String, @Nullable T> fromStringConverter, String valueName, String sqlType) {
        this(toStringConverter, fromStringConverter, valueName, sqlType, sqlType.toUpperCase(Locale.ROOT).startsWith("N"));
    }

    public SimpleValueConverter(Function<@NotNull T, @NotNull String> toStringConverter, Function<@NotNull String, @Nullable T> fromStringConverter, String valueName, boolean isStringNationalized, int stringMaxLength) {
        this(toStringConverter, fromStringConverter, valueName, (isStringNationalized ? "N" : "") + "VARCHAR(" + stringMaxLength + ")", isStringNationalized);
    }

    @Override
    public @NotNull String toRawString(@NotNull T value) {
        return toStringConverter.apply(value);
    }

    @Override
    public @Nullable T fromRawString(@NotNull String value) {
        return fromStringConverter.apply(value);
    }

    @Override
    public @NotNull Map<String, Object> toObjectMap(@NotNull T value) {
        return Collections.singletonMap(valueName, value);
    }

    @Override
    public @Nullable T fromObjectMap(@NotNull Map<String, Object> map) {
        Object value = map.get(valueName);
        return value == null ? null : fromRawString(value.toString());
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
                return new String[]{sqlType + " NOT NULL"};
            }

            @Override
            public Object[] toSqlValues(@NotNull T value) {
                return new Object[]{toRawString(value)};
            }

            @Override
            public @Nullable T fromSqlResultSet(@NotNull ResultSet resultSet) throws SQLException {
                String value = isStringNationalized ? resultSet.getNString(valueName) : resultSet.getString(valueName);
                return value == null ? null : fromRawString(value);
            }
        };
    }
}
