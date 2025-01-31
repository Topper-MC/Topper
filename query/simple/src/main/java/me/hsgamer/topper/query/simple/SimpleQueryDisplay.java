package me.hsgamer.topper.query.simple;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SimpleQueryDisplay<K, V> {
    @NotNull
    String getDisplayName(@Nullable K key);

    @NotNull
    String getDisplayValue(@Nullable V value, @NotNull String args);

    @NotNull
    String getDisplayKey(@Nullable K key);
}
