package me.hsgamer.topper.core;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

public final class DataEntry<K, V> {
    private final K key;
    private final DataHolder<K, V> holder;
    private final AtomicReference<V> value;

    public DataEntry(K key, DataHolder<K, V> holder) {
        this.key = key;
        this.holder = holder;
        this.value = new AtomicReference<>(holder.getDefaultValue());
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value.get();
    }

    public void setValue(V value) {
        setValue(value, true);
    }

    public void setValue(UnaryOperator<V> operator) {
        setValue(operator, true);
    }

    public void setValue(V value, boolean notify) {
        setValue(v -> value, notify);
    }

    public void setValue(UnaryOperator<V> operator, boolean notify) {
        this.value.updateAndGet(oldValue -> {
            V newValue = operator.apply(oldValue);
            if (notify && !Objects.equals(oldValue, newValue)) {
                holder.onUpdate(this, oldValue, newValue);
            }
            return newValue;
        });
    }

    public DataHolder<K, V> getHolder() {
        return holder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataEntry<?, ?> dataEntry = (DataEntry<?, ?>) o;
        return Objects.equals(getKey(), dataEntry.getKey()) && Objects.equals(getHolder(), dataEntry.getHolder()) && Objects.equals(getValue(), dataEntry.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getHolder(), getValue());
    }
}
