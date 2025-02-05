package me.hsgamer.topper.value.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ValueWrapper<T> {
    public final @NotNull State state;
    public final @Nullable T value;
    public final @NotNull String errorMessage;
    public final @Nullable Throwable throwable;

    public ValueWrapper(@NotNull State state, @Nullable T value, @NotNull String errorMessage, @Nullable Throwable throwable) {
        this.state = state;
        this.value = value;
        this.errorMessage = errorMessage;
        this.throwable = throwable;
    }

    public static <T> ValueWrapper<T> handled(@Nullable T value) {
        return new ValueWrapper<>(State.HANDLED, value, "", null);
    }

    public static <T> ValueWrapper<T> notHandled() {
        return new ValueWrapper<>(State.NOT_HANDLED, null, "", null);
    }

    public static <T> ValueWrapper<T> error(@NotNull String errorMessage) {
        return new ValueWrapper<>(State.ERROR, null, errorMessage, null);
    }

    public static <T> ValueWrapper<T> error(@NotNull Throwable throwable) {
        return new ValueWrapper<>(State.ERROR, null, Optional.ofNullable(throwable.getMessage()).orElse("There is an error when handling the value"), throwable);
    }

    public static <T> ValueWrapper<T> error(@NotNull String errorMessage, @NotNull Throwable throwable) {
        return new ValueWrapper<>(State.ERROR, null, errorMessage, throwable);
    }

    public static <T> ValueWrapper<T> copyNullWrapper(@NotNull ValueWrapper<?> wrapper) {
        return new ValueWrapper<>(wrapper.state, null, wrapper.errorMessage, wrapper.throwable);
    }

    public boolean isHandled() {
        return state == State.HANDLED;
    }

    public boolean isNull() {
        return value == null;
    }

    public enum State {
        HANDLED,
        ERROR,
        NOT_HANDLED
    }
}
