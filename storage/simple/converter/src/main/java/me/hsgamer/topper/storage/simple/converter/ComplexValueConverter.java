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

    private ComplexValueConverter(String stringSeparator, List<Entry<T>> entries, Supplier<T> constructor) {
        this.stringSeparator = stringSeparator;
        this.entries = Collections.unmodifiableList(entries);
        this.constructor = constructor;
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
    public @NotNull SqlValueConverter<T> getSqlValueConverter(String driverType) {
        List<SqlValueConverter<Object>> sqlValueConverters = new ArrayList<>();
        List<String> sqlColumnList = new ArrayList<>();
        List<String> sqlColumnDefinitionList = new ArrayList<>();
        for (Entry<T> entry : entries) {
            SqlValueConverter<Object> sqlValueConverter = entry.converter.getSqlValueConverter(driverType);
            sqlValueConverters.add(sqlValueConverter);

            String[] columns = sqlValueConverter.getSqlColumns();
            String[] columnDefinitions = sqlValueConverter.getSqlColumnDefinitions();
            sqlColumnList.addAll(Arrays.asList(columns));
            sqlColumnDefinitionList.addAll(Arrays.asList(columnDefinitions));
        }
        String[] sqlColumns = sqlColumnList.toArray(new String[0]);
        String[] sqlColumnDefinitions = sqlColumnDefinitionList.toArray(new String[0]);

        return new SqlValueConverter<T>() {
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
                for (SqlValueConverter<Object> sqlValueConverter : sqlValueConverters) {
                    Object[] objectValues = sqlValueConverter.toSqlValues(value);
                    values.addAll(Arrays.asList(objectValues));
                }
                return values.toArray(new Object[0]);
            }

            @Override
            public @Nullable T fromSqlResultSet(@NotNull ResultSet resultSet) throws SQLException {
                T instance = constructor.get();
                for (int i = 0; i < sqlValueConverters.size(); i++) {
                    SqlValueConverter<Object> sqlValueConverter = sqlValueConverters.get(i);
                    Object objectValue = sqlValueConverter.fromSqlResultSet(resultSet);
                    if (objectValue == null) {
                        return null;
                    }
                    instance = entries.get(i).setter.apply(instance, objectValue);
                }
                return instance;
            }
        };
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
