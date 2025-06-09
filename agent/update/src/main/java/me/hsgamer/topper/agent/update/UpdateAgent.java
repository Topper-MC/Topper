package me.hsgamer.topper.agent.update;

import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.core.DataHolder;
import me.hsgamer.topper.value.core.ValueWrapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class UpdateAgent<K, V> implements DataEntryAgent<K, V> {
    private final DataHolder<K, V> holder;
    private final Function<K, ValueWrapper<V>> updateFunction;

    private final Map<K, ValueStatus> map = new ConcurrentHashMap<>();
    private final ValueStatus updateMode = new ValueStatus();

    private List<Predicate<K>> filters = null;
    private BiConsumer<K, ValueWrapper<V>> errorHandler = null;
    private int maxSkips = 1;

    public UpdateAgent(DataHolder<K, V> holder, Function<K, ValueWrapper<V>> updateFunction) {
        this.holder = holder;
        this.updateFunction = updateFunction;
    }

    public void addFilter(Predicate<K> filter) {
        if (filters == null) {
            filters = new ArrayList<>();
        }
        filters.add(filter);
    }

    public void setErrorHandler(BiConsumer<K, ValueWrapper<V>> errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setMaxSkips(int maxSkips) {
        this.maxSkips = maxSkips;
    }

    private boolean canUpdate(K key) {
        return filters == null || filters.isEmpty() || filters.stream().allMatch(predicate -> predicate.test(key));
    }

    public Runnable getUpdateRunnable(int maxEntryPerCall) {
        return new Runnable() {
            private final AtomicReference<Iterator<Map.Entry<K, ValueStatus>>> iteratorRef = new AtomicReference<>();

            @Override
            public void run() {
                Iterator<Map.Entry<K, ValueStatus>> iterator = iteratorRef.updateAndGet(old -> old == null || !old.hasNext() ? map.entrySet().iterator() : old);
                int count = 0;
                while (iterator.hasNext() && count < maxEntryPerCall) {
                    Map.Entry<K, ValueStatus> entry = iterator.next();
                    K key = entry.getKey();
                    ValueStatus valueStatus = entry.getValue();

                    if (valueStatus.skip()) {
                        entry.setValue(valueStatus.decrementSkips());
                        continue;
                    }

                    if (!canUpdate(key)) {
                        entry.setValue(new ValueStatus(maxSkips));
                        continue;
                    }

                    ValueWrapper<V> valueWrapper = updateFunction.apply(key);
                    switch (valueWrapper.state) {
                        case ERROR:
                            if (errorHandler != null) {
                                errorHandler.accept(key, valueWrapper);
                            }
                        case NOT_HANDLED:
                            entry.setValue(new ValueStatus(maxSkips));
                            break;
                        default:
                            entry.setValue(new ValueStatus(valueWrapper.value));
                            break;
                    }
                    count++;
                }
            }
        };
    }

    public Runnable getSetRunnable() {
        return new Runnable() {
            private final AtomicReference<Iterator<Map.Entry<K, ValueStatus>>> iteratorRef = new AtomicReference<>();

            @Override
            public void run() {
                Iterator<Map.Entry<K, ValueStatus>> iterator = iteratorRef.updateAndGet(old -> old == null || !old.hasNext() ? map.entrySet().iterator() : old);
                while (iterator.hasNext()) {
                    Map.Entry<K, ValueStatus> entry = iterator.next();
                    ValueStatus valueStatus = entry.getValue();

                    if (!valueStatus.set) {
                        continue;
                    }

                    Optional<DataEntry<K, V>> optionalDataEntry = holder.getEntry(entry.getKey());
                    if (!optionalDataEntry.isPresent()) {
                        iterator.remove();
                        continue;
                    }

                    DataEntry<K, V> dataEntry = optionalDataEntry.get();
                    dataEntry.setValue(valueStatus.value);
                    entry.setValue(updateMode);
                }
            }
        };
    }


    @Override
    public void onCreate(DataEntry<K, V> entry) {
        map.put(entry.getKey(), updateMode);
    }

    @Override
    public void onRemove(DataEntry<K, V> entry) {
        map.remove(entry.getKey());
    }

    private final class ValueStatus {
        private final V value;
        private final int skips;
        private final boolean set;

        // Default constructor for update mode
        private ValueStatus() {
            this.value = null;
            this.skips = 0;
            this.set = false;
        }

        // Constructor for skip mode
        private ValueStatus(int skips) {
            this.value = null;
            this.skips = skips;
            this.set = false;
        }

        // Constructor for set mode
        private ValueStatus(V value) {
            this.value = value;
            this.skips = 0;
            this.set = true;
        }

        private boolean skip() {
            return skips > 0;
        }

        private ValueStatus decrementSkips() {
            return new ValueStatus(skips - 1);
        }
    }
}
