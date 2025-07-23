package me.hsgamer.topper.storage.flat.converter;

import me.hsgamer.topper.storage.flat.core.FlatValueConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class ComplexFlatValueConverter<T> implements FlatValueConverter<T> {
    private final String stringSeparator;
    private final List<Entry<T>> entries;
    private final Supplier<T> constructor;

    private ComplexFlatValueConverter(String stringSeparator, List<Entry<T>> entries, Supplier<T> constructor) {
        this.stringSeparator = stringSeparator;
        this.entries = entries;
        this.constructor = constructor;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public @NotNull String toString(@NotNull T value) {
        StringJoiner joiner = new StringJoiner(stringSeparator);
        for (Entry<T> entry : entries) {
            String rawString = entry.converter.toString(entry.getter.apply(value));
            joiner.add(rawString);
        }
        return joiner.toString();
    }

    @Override
    public @Nullable T fromString(@NotNull String value) {
        String[] values = value.split(Pattern.quote(stringSeparator), -1);
        if (values.length != entries.size()) {
            return null;
        }
        T instance = constructor.get();
        for (int i = 0; i < values.length; i++) {
            Entry<T> entry = entries.get(i);
            Object objectValue = entry.converter.fromString(values[i]);
            if (objectValue == null) {
                return null;
            }
            instance = entry.setter.apply(instance, objectValue);
        }
        return instance;
    }

    private static class Entry<T> {
        private final FlatValueConverter<Object> converter;
        private final Function<T, Object> getter;
        private final BiFunction<T, Object, T> setter;

        private Entry(FlatValueConverter<Object> converter, Function<T, Object> getter, BiFunction<T, Object, T> setter) {
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
        public <V> Builder<T> entry(FlatValueConverter<V> converter, Function<T, V> getter, BiFunction<T, V, T> setter) {
            FlatValueConverter<Object> objectConverter = (FlatValueConverter<Object>) converter;
            Function<T, Object> objectGetter = (Function<T, Object>) getter;
            BiFunction<T, Object, T> objectSetter = (BiFunction<T, Object, T>) setter;
            Entry<T> entry = new Entry<>(objectConverter, objectGetter, objectSetter);
            entries.add(entry);
            return this;
        }

        public ComplexFlatValueConverter<T> build() {
            if (constructor == null) {
                throw new IllegalStateException("Constructor is not set");
            }
            if (entries.isEmpty()) {
                throw new IllegalStateException("Entries are empty");
            }
            return new ComplexFlatValueConverter<>(stringSeparator, entries, constructor);
        }
    }
}
