package me.hsgamer.topper.value.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface ValueProvider<K, V> extends Function<K, ValueWrapper<V>> {
    static <K, V> ValueProvider<K, V> empty() {
        return k -> ValueWrapper.notHandled();
    }

    static <K, V> ValueProvider<K, V> ofSimple(Function<@NotNull K, @Nullable V> function) {
        return k -> {
            try {
                return ValueWrapper.handled(function.apply(k));
            } catch (Throwable e) {
                return ValueWrapper.error("An error occurred while getting the value", e);
            }
        };
    }

    @Override
    @NotNull ValueWrapper<V> apply(@NotNull K key);

    default <RK> ValueProvider<RK, V> keyMapper(Function<@NotNull RK, @Nullable K> mapper) {
        return rawKey -> {
            try {
                K key = mapper.apply(rawKey);
                if (key == null) {
                    return ValueWrapper.notHandled();
                }
                return apply(key);
            } catch (Throwable e) {
                return ValueWrapper.error("An error occurred while mapping the key", e);
            }
        };
    }

    default <F> ValueProvider<K, F> thenApply(Function<@NotNull V, @Nullable F> mapper) {
        return key -> {
            ValueWrapper<V> wrapper = apply(key);
            if (!wrapper.isHandled()) {
                return ValueWrapper.copyNullWrapper(wrapper);
            }
            if (wrapper.value == null) {
                return ValueWrapper.error("The raw value is null");
            }
            try {
                return ValueWrapper.handled(mapper.apply(wrapper.value));
            } catch (Throwable e) {
                return ValueWrapper.error("An error occurred while converting the raw value", e);
            }
        };
    }
}
