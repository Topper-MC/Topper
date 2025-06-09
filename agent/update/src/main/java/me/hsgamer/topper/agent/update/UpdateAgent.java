package me.hsgamer.topper.agent.update;

import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.core.DataHolder;
import me.hsgamer.topper.value.core.ValueWrapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class UpdateAgent<K, V> implements DataEntryAgent<K, V> {
    private final DataHolder<K, V> holder;
    private final Function<K, ValueWrapper<V>> updateFunction;

    private final Map<K, ValueStatus> map = new ConcurrentHashMap<>();
    private final ValueStatus updateMode = new ValueStatus(null, ValueStatus.UPDATE);
    private final ValueStatus notHandledMode = new ValueStatus(null, ValueStatus.NOT_HANDLED);

    private List<Predicate<K>> filters = null;
    private BiConsumer<K, ValueWrapper<V>> errorHandler = null;

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

    private boolean canUpdate(K key) {
        return filters == null || filters.isEmpty() || filters.stream().allMatch(predicate -> predicate.test(key));
    }

    public Runnable getUpdateRunnable(int maxEntryPerCall, int iterationsBeforeHandleAll) {
        return new Runnable() {
            transient Iterator<Map.Entry<K, ValueStatus>> iterator;
            int iteratorCount = 0;

            @Override
            public void run() {
                if (iterator == null || !iterator.hasNext()) {
                    iterator = map.entrySet().iterator();
                    iteratorCount++;
                }

                boolean handleAll = false;
                if (iteratorCount >= iterationsBeforeHandleAll) {
                    handleAll = true;
                    iteratorCount = 0;
                }

                int count = 0;
                while (iterator.hasNext() && count < maxEntryPerCall) {
                    Map.Entry<K, ValueStatus> entry = iterator.next();
                    K key = entry.getKey();
                    ValueStatus valueStatus = entry.getValue();

                    if (!handleAll && valueStatus.mode == ValueStatus.NOT_HANDLED) {
                        continue;
                    }

                    if (!canUpdate(key)) {
                        entry.setValue(notHandledMode);
                        continue;
                    }

                    ValueWrapper<V> valueWrapper = updateFunction.apply(key);
                    switch (valueWrapper.state) {
                        case ERROR:
                            if (errorHandler != null) {
                                errorHandler.accept(key, valueWrapper);
                            }
                            entry.setValue(updateMode);
                            break;
                        case NOT_HANDLED:
                            entry.setValue(notHandledMode);
                            break;
                        default:
                            entry.setValue(new ValueStatus(valueWrapper.value, ValueStatus.SET));
                            break;
                    }
                    count++;
                }
            }
        };
    }

    public Runnable getSetRunnable() {
        return new Runnable() {
            private Iterator<Map.Entry<K, ValueStatus>> iterator;

            @Override
            public void run() {
                if (iterator == null || !iterator.hasNext()) {
                    iterator = map.entrySet().iterator();
                }

                while (iterator.hasNext()) {
                    Map.Entry<K, ValueStatus> entry = iterator.next();
                    ValueStatus valueStatus = entry.getValue();

                    if (valueStatus.mode != ValueStatus.SET) {
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

    private class ValueStatus {
        private static final int UPDATE = 0;
        private static final int SET = 1;
        private static final int NOT_HANDLED = 2;

        private final V value;
        private final int mode;

        private ValueStatus(V value, int mode) {
            this.value = value;
            this.mode = mode;
        }
    }
}
