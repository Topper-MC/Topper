package me.hsgamer.topper.value.number;

import me.hsgamer.topper.value.core.ValueProvider;
import me.hsgamer.topper.value.core.ValueWrapper;

import java.util.function.Function;

public interface NumberValueProvider {
    static <K, V extends Number, N extends Number> ValueProvider<K, V> converter(ValueProvider<K, N> valueProvider, Function<N, V> converter) {
        return key -> {
            ValueWrapper<N> wrapper = valueProvider.apply(key);
            return !wrapper.isHandled() ? ValueWrapper.copyNullWrapper(wrapper) : ValueWrapper.handled(converter.apply(wrapper.value));
        };
    }

    static <K, V extends Number> ValueProvider<K, Double> toDouble(ValueProvider<K, V> valueProvider) {
        return converter(valueProvider, Number::doubleValue);
    }

    static <K, V extends Number> ValueProvider<K, Float> toFloat(ValueProvider<K, V> valueProvider) {
        return converter(valueProvider, Number::floatValue);
    }

    static <K, V extends Number> ValueProvider<K, Integer> toInteger(ValueProvider<K, V> valueProvider) {
        return converter(valueProvider, Number::intValue);
    }

    static <K, V extends Number> ValueProvider<K, Long> toLong(ValueProvider<K, V> valueProvider) {
        return converter(valueProvider, Number::longValue);
    }
}
