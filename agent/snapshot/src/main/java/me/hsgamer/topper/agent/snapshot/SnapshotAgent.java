package me.hsgamer.topper.agent.snapshot;

import me.hsgamer.topper.agent.core.Agent;
import me.hsgamer.topper.core.DataHolder;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class SnapshotAgent<K, V> implements Agent, Runnable {
    private final AtomicReference<List<Map.Entry<K, V>>> entryList = new AtomicReference<>(Collections.emptyList());
    private final AtomicReference<Map<K, Integer>> indexMap = new AtomicReference<>(Collections.emptyMap());
    private final List<Predicate<Map.Entry<K, V>>> filters = new ArrayList<>();
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
        };
    }

    protected abstract Stream<Map.Entry<K, V>> getDataStream();

    @Override
    public void run() {
        List<Map.Entry<K, V>> list = getUrgentSnapshot();
        entryList.set(getUrgentSnapshot());

        Map<K, Integer> map = IntStream.range(0, list.size())
                .boxed()
                .collect(Collectors.toMap(i -> list.get(i).getKey(), i -> i));
        indexMap.set(map);
    }

    @Override
    public void stop() {
        entryList.set(Collections.emptyList());
        indexMap.set(Collections.emptyMap());
    }

    public List<Map.Entry<K, V>> getUrgentSnapshot() {
        Stream<Map.Entry<K, V>> stream = getDataStream().filter(snapshot -> filters.stream().allMatch(filter -> filter.test(snapshot)));
        if (comparator != null) {
            stream = stream.sorted(Map.Entry.comparingByValue(comparator));
        }
        return stream.collect(Collectors.toList());
    }

    public List<Map.Entry<K, V>> getSnapshot() {
        return entryList.get();
    }

    public int getSnapshotIndex(K key) {
        return indexMap.get().getOrDefault(key, -1);
    }

    public Optional<Map.Entry<K, V>> getSnapshotByIndex(int index) {
        List<Map.Entry<K, V>> list = getSnapshot();
        if (index < 0 || index >= list.size()) return Optional.empty();
        return Optional.of(list.get(index));
    }

    public void setComparator(Comparator<V> comparator) {
        this.comparator = comparator;
    }

    public void addFilter(Predicate<Map.Entry<K, V>> filter) {
        filters.add(filter);
    }
}
