package me.hsgamer.topper.storage.flat.converter;

import me.hsgamer.topper.storage.flat.core.FlatValueConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class SimpleFlatValueConverter<T> implements FlatValueConverter<T> {
    private final Function<@NotNull T, @NotNull String> toStringConverter;
    private final Function<@NotNull String, @Nullable T> fromStringConverter;

    public SimpleFlatValueConverter(Function<@NotNull T, @NotNull String> toStringConverter, Function<@NotNull String, @Nullable T> fromStringConverter) {
        this.toStringConverter = toStringConverter;
        this.fromStringConverter = fromStringConverter;
    }

    @Override
    public @NotNull String toString(@NotNull T value) {
        return toStringConverter.apply(value);
    }

    @Override
    public @Nullable T fromString(@NotNull String value) {
        return fromStringConverter.apply(value);
    }
}
