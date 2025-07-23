package me.hsgamer.topper.storage.flat.converter;

import me.hsgamer.topper.storage.flat.core.FlatValueConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class NumberFlatValueConverter<T extends Number> implements FlatValueConverter<T> {
    private final Function<@Nullable Number, @Nullable T> numberFunction;

    public NumberFlatValueConverter(Function<@Nullable Number, @Nullable T> numberFunction) {
        this.numberFunction = numberFunction;
    }

    @Override
    public @NotNull String toString(@NotNull T value) {
        return value.toString();
    }

    @Override
    public @Nullable T fromString(@NotNull String value) {
        try {
            return numberFunction.apply(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
