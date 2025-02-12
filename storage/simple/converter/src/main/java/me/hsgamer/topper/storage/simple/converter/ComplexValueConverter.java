package me.hsgamer.topper.storage.simple.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class ComplexValueConverter<T> implements ValueConverter<T> {
    private final String stringSeparator;
    private final List<Entry<T>> entries;
    private final Supplier<T> constructor;
    private final String[] sqlColumns;
    private final String[] sqlColumnDefinitions;

    private ComplexValueConverter(String stringSeparator, List<Entry<T>> entries, Supplier<T> constructor) {
        this.stringSeparator = stringSeparator;
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
    public @NotNull String toRawString(@NotNull T value) {
        StringJoiner joiner = new StringJoiner(stringSeparator);
        for (Entry<T> entry : entries) {
            String rawString = entry.converter.toRawString(entry.getter.apply(value));
            joiner.add(rawString);
        }
        return joiner.toString();
    }

    @Override
    public @Nullable T fromRawString(@NotNull String value) {
        String[] values = value.split(Pattern.quote(stringSeparator), -1);
        if (values.length != entries.size()) {
            return null;
        }
        T instance = constructor.get();
        for (int i = 0; i < values.length; i++) {
            Entry<T> entry = entries.get(i);
            Object objectValue = entry.converter.fromRawString(values[i]);
            if (objectValue == null) {
                return null;
            }
            instance = entry.setter.apply(instance, objectValue);
        }
        return instance;
    }

    @Override
    public @NotNull Map<String, Object> toObjectMap(@NotNull T value) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Entry<T> entry : entries) {
            map.putAll(entry.converter.toObjectMap(entry.getter.apply(value)));
        }
        return map;
    }

    @Override
    public @Nullable T fromObjectMap(@NotNull Map<String, Object> map) {
        T instance = constructor.get();
        for (Entry<T> entry : entries) {
            Object objectValue = entry.converter.fromObjectMap(map);
            if (objectValue == null) {
                return null;
            }
            instance = entry.setter.apply(instance, objectValue);
        }
        return instance;
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
        private final ValueConverter<Object> converter;
        private final Function<T, Object> getter;
        private final BiFunction<T, Object, T> setter;

        private Entry(ValueConverter<Object> converter, Function<T, Object> getter, BiFunction<T, Object, T> setter) {
            this.converter = converter;
            this.getter = getter;
            this.setter = setter;
        }
    }

    public static class Builder<T> {
        private final List<Entry<T>> entries;
        private String stringSeparator;
        private Supplier<T> constructor;

        private Builder() {
            entries = new ArrayList<>();
            stringSeparator = "||";
        }

        public Builder<T> stringSeparator(String rawStringSeparator) {
            this.stringSeparator = rawStringSeparator;
            return this;
        }

        public Builder<T> constructor(Supplier<T> constructor) {
            this.constructor = constructor;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <V> Builder<T> entry(ValueConverter<V> converter, Function<T, V> getter, BiFunction<T, V, T> setter) {
            ValueConverter<Object> objectConverter = (ValueConverter<Object>) converter;
            Function<T, Object> objectGetter = (Function<T, Object>) getter;
            BiFunction<T, Object, T> objectSetter = (BiFunction<T, Object, T>) setter;
            Entry<T> entry = new Entry<>(objectConverter, objectGetter, objectSetter);
            entries.add(entry);
            return this;
        }

        public ComplexValueConverter<T> build() {
            if (constructor == null) {
                throw new IllegalStateException("Constructor is not set");
            }
            if (entries.isEmpty()) {
                throw new IllegalStateException("Entries are empty");
            }
            return new ComplexValueConverter<>(stringSeparator, entries, constructor);
        }
    }
}
