package me.hsgamer.topper.agent.snapshot;

import me.hsgamer.topper.agent.core.Agent;
import me.hsgamer.topper.data.core.DataHolder;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class SnapshotAgent<K, V> implements Agent, Runnable {
    private final AtomicReference<List<Map.Entry<K, V>>> entryList = new AtomicReference<>(null);
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(new Snapshot());
    private Predicate<Map.Entry<K, V>> filter = null;
    private Comparator<V> comparator;

    public static <K, V> SnapshotAgent<K, V> create(DataHolder<K, V> holder) {
        return new SnapshotAgent<K, V>() {
            @Override
            protected Stream<Map.Entry<K, V>> getDataStream() {
                return holder.getEntryMap()
                        .entrySet()
                        .stream()
                        .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().getValue()));
            }

            @Override
            protected boolean needUpdate() {
                return true;
            }
        };
    }

    protected abstract Stream<Map.Entry<K, V>> getDataStream();

    protected abstract boolean needUpdate();

    @Override
    public void run() {
        boolean isUpdate = needUpdate();
        List<Map.Entry<K, V>> currentEntryList = entryList.get();
        if (filter == null && !isUpdate && currentEntryList != null) {
            return;
        }

        final List<Map.Entry<K, V>> list;
        if (isUpdate || currentEntryList == null) {
            list = getUrgentSnapshot();
            entryList.set(list);
        } else {
            list = currentEntryList;
        }

        final List<Map.Entry<K, V>> resultList;
        if (filter != null) {
            resultList = list.stream().filter(filter).collect(Collectors.toList());
        } else {
            resultList = list;
        }

        Map<K, Integer> map = IntStream.range(0, resultList.size())
                .boxed()
                .collect(Collectors.toMap(i -> resultList.get(i).getKey(), i -> i));
        snapshot.set(new Snapshot(resultList, map));
    }

    @Override
    public void stop() {
        snapshot.set(new Snapshot());
    }

    public List<Map.Entry<K, V>> getUrgentSnapshot() {
        Stream<Map.Entry<K, V>> stream = getDataStream();
        if (comparator != null) {
            stream = stream.sorted(Map.Entry.comparingByValue(comparator));
        }
        return stream.collect(Collectors.toList());
    }

    public List<Map.Entry<K, V>> getSnapshot() {
        return snapshot.get().entryList;
    }

    public int getSnapshotIndex(K key) {
        return snapshot.get().indexMap.getOrDefault(key, -1);
    }

    public Optional<Map.Entry<K, V>> getSnapshotByIndex(int index) {
        List<Map.Entry<K, V>> list = getSnapshot();
        if (index < 0 || index >= list.size()) return Optional.empty();
        return Optional.of(list.get(index));
    }

    public void setComparator(Comparator<V> comparator) {
        this.comparator = comparator;
    }

    public void setFilter(Predicate<Map.Entry<K, V>> filter) {
        this.filter = filter;
    }

    private final class Snapshot {
        private final List<Map.Entry<K, V>> entryList;
        private final Map<K, Integer> indexMap;

        private Snapshot(List<Map.Entry<K, V>> entryList, Map<K, Integer> indexMap) {
            this.entryList = entryList;
            this.indexMap = indexMap;
        }

        private Snapshot() {
            this(Collections.emptyList(), Collections.emptyMap());
        }
    }
}
