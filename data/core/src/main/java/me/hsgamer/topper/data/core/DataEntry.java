package me.hsgamer.topper.data.core;

import java.util.function.UnaryOperator;

public interface DataEntry<K, V> {
    K getKey();

    V getValue();

    default void setValue(UnaryOperator<V> operator) {
        setValue(operator, true);
    }

    default void setValue(V value) {
        setValue(value, true);
    }

    void setValue(UnaryOperator<V> operator, boolean notify);

    DataHolder<K, V> getHolder();

    default void setValue(V value, boolean notify) {
        setValue(v -> value, notify);
    }
}
