package me.hsgamer.topper.agent.update;

import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.core.DataHolder;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Predicate;

public class UpdateAgent<K, V> implements DataEntryAgent<K, V> {
    private final Queue<K> updateQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Map.Entry<K, V>> setQueue = new ConcurrentLinkedQueue<>();
    private final DataHolder<K, V> holder;
    private final Function<K, Optional<V>> updateFunction;
    private final List<Predicate<K>> filters = new ArrayList<>();

    public UpdateAgent(DataHolder<K, V> holder, Function<K, Optional<V>> updateFunction) {
        this.holder = holder;
        this.updateFunction = updateFunction;
    }

    public void addFilter(Predicate<K> filter) {
        filters.add(filter);
    }

    private boolean canUpdate(K key) {
        return filters.stream().allMatch(predicate -> predicate.test(key));
    }

    public Runnable getUpdateRunnable(int maxEntryPerCall) {
        return () -> {
            for (int i = 0; i < maxEntryPerCall; i++) {
                K k = updateQueue.poll();
                if (k == null) break;

                if (!canUpdate(k)) {
                    updateQueue.add(k);
                    continue;
                }

                Optional<V> value = updateFunction.apply(k);
                if (!value.isPresent()) {
                    updateQueue.add(k);
                    continue;
                }

                setQueue.add(new AbstractMap.SimpleImmutableEntry<>(k, value.get()));
            }
        };
    }

    public Runnable getSetRunnable() {
        return () -> {
            while (true) {
                Map.Entry<K, V> entry = setQueue.poll();
                if (entry == null) break;
                Optional<DataEntry<K, V>> optionalDataEntry = holder.getEntry(entry.getKey());
                if (!optionalDataEntry.isPresent()) {
                    continue;
                }

                DataEntry<K, V> dataEntry = optionalDataEntry.get();
                dataEntry.setValue(entry.getValue());

                updateQueue.add(entry.getKey());
            }
        };
    }


    @Override
    public void onCreate(DataEntry<K, V> entry) {
        updateQueue.add(entry.getKey());
    }

    @Override
    public void onRemove(DataEntry<K, V> entry) {
        updateQueue.remove(entry.getKey());
    }
}
