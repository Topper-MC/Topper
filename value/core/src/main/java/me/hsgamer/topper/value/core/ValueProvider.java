package me.hsgamer.topper.value.core;

import java.util.function.Function;

public interface ValueProvider<K, V> extends Function<K, ValueWrapper<V>> {
    static <K, V> ValueProvider<K, V> empty() {
        return k -> ValueWrapper.notHandled();
    }

    @Override
    ValueWrapper<V> apply(K key);
}
