package me.hsgamer.topper.storage.bundle.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.function.Function;

public class SimpleValueConverter<T> implements BundleValueConverter<T> {
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
    public @NotNull String toString(@NotNull T value) {
        return toStringConverter.apply(value);
    }

    @Override
    public @Nullable T fromString(@NotNull String value) {
        return fromStringConverter.apply(value);
    }

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
        return new Object[]{toString(value)};
    }

    @Override
    public @Nullable T fromSqlResultSet(@NotNull ResultSet resultSet) throws SQLException {
        String value = isStringNationalized ? resultSet.getNString(valueName) : resultSet.getString(valueName);
        return value == null ? null : fromString(value);
    }
}
