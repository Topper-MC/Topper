package me.hsgamer.topper.data.simple;

import me.hsgamer.topper.data.core.DataEntry;
import me.hsgamer.topper.data.core.DataHolder;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

public class SimpleDataEntry<K, V> implements DataEntry<K, V> {
    private final K key;
    private final SimpleDataHolder<K, V> holder;
    private final AtomicReference<V> value;

    public SimpleDataEntry(K key, SimpleDataHolder<K, V> holder) {
        this.key = key;
        this.holder = holder;
        this.value = new AtomicReference<>(holder.getDefaultValue());
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value.get();
    }

    @Override
    public void setValue(UnaryOperator<V> operator, boolean notify) {
        this.value.updateAndGet(oldValue -> {
            V newValue = operator.apply(oldValue);
            if (newValue == null) {
                newValue = holder.getDefaultValue();
            }
            if (notify && !Objects.equals(oldValue, newValue)) {
                holder.onUpdate(this, oldValue, newValue);
            }
            return newValue;
        });
    }

    @Override
    public DataHolder<K, V> getHolder() {
        return holder;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SimpleDataEntry<?, ?> that = (SimpleDataEntry<?, ?>) o;
        return Objects.equals(getKey(), that.getKey()) && Objects.equals(getHolder(), that.getHolder()) && Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getHolder(), getValue());
    }
}
