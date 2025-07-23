package me.hsgamer.topper.storage.sql.converter;

import me.hsgamer.topper.storage.sql.core.SqlValueConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class ComplexSqlValueConverter<T> implements SqlValueConverter<T> {
    private final List<Entry<T>> entries;
    private final Supplier<T> constructor;
    private final String[] sqlColumns;
    private final String[] sqlColumnDefinitions;

    private ComplexSqlValueConverter(List<Entry<T>> entries, Supplier<T> constructor) {
        this.entries = Collections.unmodifiableList(entries);
        this.constructor = constructor;

        List<String> sqlColumns = new ArrayList<>();
        List<String> sqlColumnDefinitions = new ArrayList<>();
        for (Entry<T> entry : entries) {
            String[] columns = entry.converter.getSqlColumns();
            String[] columnDefinitions = entry.converter.getSqlColumnDefinitions();
            sqlColumns.addAll(Arrays.asList(columns));
            sqlColumnDefinitions.addAll(Arrays.asList(columnDefinitions));
        }
        this.sqlColumns = sqlColumns.toArray(new String[0]);
        this.sqlColumnDefinitions = sqlColumnDefinitions.toArray(new String[0]);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public String[] getSqlColumns() {
        return sqlColumns;
    }

    @Override
    public String[] getSqlColumnDefinitions() {
        return sqlColumnDefinitions;
    }

    @Override
    public Object[] toSqlValues(@NotNull T value) {
        List<Object> values = new ArrayList<>();
        for (Entry<T> entry : entries) {
            Object[] objectValues = entry.converter.toSqlValues(entry.getter.apply(value));
            values.addAll(Arrays.asList(objectValues));
        }
        return values.toArray(new Object[0]);
    }

    @Override
    public @Nullable T fromSqlResultSet(@NotNull ResultSet resultSet) throws SQLException {
        T instance = constructor.get();
        for (Entry<T> entry : entries) {
            Object objectValue = entry.converter.fromSqlResultSet(resultSet);
            if (objectValue == null) {
                return null;
            }
            instance = entry.setter.apply(instance, objectValue);
        }
        return instance;
    }

    private static class Entry<T> {
        private final SqlValueConverter<Object> converter;
        private final Function<T, Object> getter;
        private final BiFunction<T, Object, T> setter;

        private Entry(SqlValueConverter<Object> converter, Function<T, Object> getter, BiFunction<T, Object, T> setter) {
            this.converter = converter;
            this.getter = getter;
            this.setter = setter;
        }
    }

    public static class Builder<T> {
        private final List<Entry<T>> entries;
        private Supplier<T> constructor;

        private Builder() {
            entries = new ArrayList<>();
        }

        public Builder<T> constructor(Supplier<T> constructor) {
            this.constructor = constructor;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <V> Builder<T> entry(SqlValueConverter<V> converter, Function<T, V> getter, BiFunction<T, V, T> setter) {
            SqlValueConverter<Object> objectConverter = (SqlValueConverter<Object>) converter;
            Function<T, Object> objectGetter = (Function<T, Object>) getter;
            BiFunction<T, Object, T> objectSetter = (BiFunction<T, Object, T>) setter;
            Entry<T> entry = new Entry<>(objectConverter, objectGetter, objectSetter);
            entries.add(entry);
            return this;
        }

        public ComplexSqlValueConverter<T> build() {
            if (constructor == null) {
                throw new IllegalStateException("Constructor is not set");
            }
            if (entries.isEmpty()) {
                throw new IllegalStateException("Entries are empty");
            }
            return new ComplexSqlValueConverter<>(entries, constructor);
        }
    }
}
