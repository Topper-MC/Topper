package me.hsgamer.topper.core;

import java.util.Objects;
import java.util.function.UnaryOperator;

public final class DataEntry<K, V> {
    private final K key;
    private final DataHolder<K, V> holder;
    private volatile V value;

    public DataEntry(K key, DataHolder<K, V> holder) {
        this.key = key;
        this.holder = holder;
        this.value = holder.getDefaultValue();
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        setValue(value, true);
    }

    public void setValue(UnaryOperator<V> operator) {
        setValue(operator, true);
    }

    public void setValue(V value, boolean notify) {
        if (Objects.equals(this.value, value)) return;
        V oldValue = this.value;
        this.value = value;
        if (notify) holder.onUpdate(this, oldValue);
    }

    public void setValue(UnaryOperator<V> operator, boolean notify) {
        setValue(operator.apply(value), notify);
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
