package me.hsgamer.topper.value.delegate;

import me.hsgamer.topper.value.core.ValueProvider;
import me.hsgamer.topper.value.core.ValueWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface DelegateValueProvider<K, R, V> extends ValueProvider<K, V> {
    static <K, R, V> DelegateValueProvider<K, R, V> create(ValueProvider<K, R> rawProvider, Function<@NotNull R, @Nullable V> delegateProvider) {
        return new DelegateValueProvider<K, R, V>() {
            @Override
            public ValueProvider<K, R> getRawProvider() {
                return rawProvider;
            }

            @Override
            public @Nullable V convert(@NotNull R rawValue) {
                return delegateProvider.apply(rawValue);
            }
        };
    }

    static <K, R, V> DelegateValueProvider<K, R, V> create(ValueProvider<K, R> rawProvider, Function<@NotNull R, @Nullable V> delegateProvider, String errorDisplayName) {
        return new DelegateValueProvider<K, R, V>() {
            @Override
            public ValueProvider<K, R> getRawProvider() {
                return rawProvider;
            }

            @Override
            public @Nullable V convert(@NotNull R rawValue) {
                return delegateProvider.apply(rawValue);
            }

            @Override
            public String getErrorDisplayName() {
                return errorDisplayName;
            }
        };
    }

    ValueProvider<K, R> getRawProvider();

    @Nullable V convert(@NotNull R rawValue);

    default String getErrorDisplayName() {
        return getRawProvider().getClass().getSimpleName();
    }

    @Override
    default ValueWrapper<V> apply(K key) {
        ValueWrapper<R> wrapper = getRawProvider().apply(key);
        if (!wrapper.isHandled()) {
            return ValueWrapper.copyNullWrapper(wrapper);
        }
        if (wrapper.value == null) {
            return ValueWrapper.error("The raw value of " + getErrorDisplayName() + " is null");
        }
        try {
            return ValueWrapper.handled(convert(wrapper.value));
        } catch (Throwable e) {
            return ValueWrapper.error("An error occurred while converting the raw value of " + getErrorDisplayName(), e);
        }
    }
}
