package me.hsgamer.topper.storage.flat.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FlatValueConverter<T> {
    @NotNull String toString(@NotNull T value);

    @Nullable T fromString(@NotNull String value);
}
