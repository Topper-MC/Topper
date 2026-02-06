package me.hsgamer.topper.value.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ValueProvider<K, V> extends BiConsumer<K, Consumer<ValueWrapper<V>>> {
    static <K, V> ValueProvider<K, V> empty() {
        return (k, callback) -> callback.accept(ValueWrapper.notHandled());
    }

    static <K, V> ValueProvider<K, V> simple(Function<@NotNull K, @Nullable V> function) {
        return (k, callback) -> {
            try {
                callback.accept(ValueWrapper.handled(function.apply(k)));
            } catch (Throwable e) {
                callback.accept(ValueWrapper.error("An error occurred while getting the value", e));
            }
        };
    }

    static <K, V> ValueProvider<K, V> error(@NotNull String errorMessage) {
        return (k, callback) -> callback.accept(ValueWrapper.error(errorMessage));
    }

    @Override
    void accept(K k, Consumer<ValueWrapper<V>> callback);

    default <RK> ValueProvider<RK, V> beforeApply(Function<@NotNull RK, @Nullable K> mapper) {
        return (rawKey, callback) -> {
            try {
                K key = mapper.apply(rawKey);
                if (key == null) {
                    callback.accept(ValueWrapper.notHandled());
                    return;
                }
                accept(key, callback);
            } catch (Throwable e) {
                callback.accept(ValueWrapper.error("An error occurred while mapping the key", e));
            }
        };
    }

    default <F> ValueProvider<K, F> thenApply(Function<@NotNull V, @Nullable F> mapper) {
        return (key, callback) -> accept(key, wrapper -> {
            if (!wrapper.isHandled()) {
                callback.accept(ValueWrapper.copyNullWrapper(wrapper));
                return;
            }
            if (wrapper.value == null) {
                callback.accept(ValueWrapper.error("The raw value is null"));
                return;
            }
            try {
                callback.accept(ValueWrapper.handled(mapper.apply(wrapper.value)));
            } catch (Throwable e) {
                callback.accept(ValueWrapper.error("An error occurred while converting the raw value", e));
            }
        });
    }
}
