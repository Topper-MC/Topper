package me.hsgamer.topper.agent.update;

import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.data.core.DataEntry;
import me.hsgamer.topper.data.core.DataHolder;
import me.hsgamer.topper.value.core.ValueWrapper;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class UpdateAgent<K, V> implements DataEntryAgent<K, V> {
    private final DataHolder<K, V> holder;
    private final Function<K, ValueWrapper<V>> updateFunction;

    private final Map<K, UpdateStatus> map = new ConcurrentHashMap<>();

    private Function<K, FilterResult> filter = null;
    private BiFunction<K, ValueWrapper<V>, ValueWrapper<V>> errorHandler = null;
    private int maxSkips = 1;

    public UpdateAgent(DataHolder<K, V> holder, Function<K, ValueWrapper<V>> updateFunction) {
        this.holder = holder;
        this.updateFunction = updateFunction;
    }

    public void setFilter(Function<K, FilterResult> filter) {
        this.filter = filter;
    }

    public void setErrorHandler(BiFunction<K, ValueWrapper<V>, ValueWrapper<V>> errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setErrorHandler(BiConsumer<K, ValueWrapper<V>> errorHandler) {
        setErrorHandler((k, v) -> {
            errorHandler.accept(k, v);
            return v;
        });
    }

    public void setMaxSkips(int maxSkips) {
        this.maxSkips = maxSkips;
    }

    public Runnable getUpdateRunnable(int maxEntryPerCall) {
        return new Runnable() {
            private final AtomicReference<Iterator<Map.Entry<K, UpdateStatus>>> iteratorRef = new AtomicReference<>();

            @Override
            public void run() {
                Iterator<Map.Entry<K, UpdateStatus>> iterator = iteratorRef.updateAndGet(old -> old == null || !old.hasNext() ? map.entrySet().iterator() : old);
                int count = 0;
                while (count < maxEntryPerCall && iterator.hasNext()) {
                    Map.Entry<K, UpdateStatus> entry;
                    try {
                        entry = iterator.next();
                    } catch (Exception e) {
                        break;
                    }
                    K key = entry.getKey();
                    UpdateStatus updateStatus = entry.getValue();

                    if (updateStatus != UpdateStatus.DEFAULT && !(updateStatus instanceof UpdateStatus.Set)) {
                        continue;
                    }

                    if (filter != null) {
                        FilterResult filterResult = filter.apply(key);
                        switch (filterResult) {
                            case SKIP:
                                entry.setValue(new UpdateStatus.Skip(maxSkips));
                                continue;
                            case RESET:
                                entry.setValue(UpdateStatus.RESET);
                                continue;
                            case CONTINUE:
                                // Do nothing, continue to update
                                break;
                        }
                    }

                    ValueWrapper<V> valueWrapper = updateFunction.apply(key);
                    if (errorHandler != null && valueWrapper.state == ValueWrapper.State.ERROR) {
                        valueWrapper = errorHandler.apply(key, valueWrapper);
                    }
                    switch (valueWrapper.state) {
                        case ERROR:
                        case NOT_HANDLED:
                            entry.setValue(new UpdateStatus.Skip(maxSkips));
                            break;
                        default:
                            entry.setValue(new UpdateStatus.Set(valueWrapper.value));
                            break;
                    }
                    count++;
                }
            }
        };
    }

    public Runnable getSetRunnable() {
        return new Runnable() {
            private final AtomicReference<Iterator<Map.Entry<K, UpdateStatus>>> iteratorRef = new AtomicReference<>();

            @Override
            public void run() {
                Iterator<Map.Entry<K, UpdateStatus>> iterator = iteratorRef.updateAndGet(old -> old == null || !old.hasNext() ? map.entrySet().iterator() : old);
                while (iterator.hasNext()) {
                    Map.Entry<K, UpdateStatus> entry;
                    try {
                        entry = iterator.next();
                    } catch (Exception e) {
                        break;
                    }

                    Optional<DataEntry<K, V>> optionalDataEntry = holder.getEntry(entry.getKey());
                    if (!optionalDataEntry.isPresent()) {
                        iterator.remove();
                        continue;
                    }
                    DataEntry<K, V> dataEntry = optionalDataEntry.get();

                    UpdateStatus updateStatus = entry.getValue();
                    if (updateStatus instanceof UpdateStatus.Skip) {
                        UpdateStatus.Skip skipStatus = (UpdateStatus.Skip) updateStatus;
                        entry.setValue(skipStatus.decrement());
                    } else if (updateStatus == UpdateStatus.RESET) {
                        dataEntry.setValue((V) null);
                        entry.setValue(new UpdateStatus.Skip(maxSkips));
                    } else if (updateStatus instanceof UpdateStatus.Set) {
                        UpdateStatus.Set setStatus = (UpdateStatus.Set) updateStatus;
                        //noinspection unchecked
                        V value = (V) setStatus.getValue();
                        dataEntry.setValue(value);
                        entry.setValue(UpdateStatus.DEFAULT);
                    }
                }
            }
        };
    }


    @Override
    public void onCreate(DataEntry<K, V> entry) {
        map.put(entry.getKey(), UpdateStatus.DEFAULT);
    }

    @Override
    public void onRemove(DataEntry<K, V> entry) {
        map.remove(entry.getKey());
    }

    public enum FilterResult {
        SKIP,
        RESET,
        CONTINUE
    }
}
