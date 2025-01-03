package me.hsgamer.topper.agent.snapshot;

import me.hsgamer.topper.agent.core.Agent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.core.DataHolder;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SnapshotAgent<K, V> implements Agent, Runnable {
    private final AtomicReference<List<Map.Entry<K, V>>> topSnapshot = new AtomicReference<>(Collections.emptyList());
    private final AtomicReference<Map<K, Integer>> indexMap = new AtomicReference<>(Collections.emptyMap());
    private final DataHolder<K, V> holder;
    private final List<Predicate<Map.Entry<K, V>>> filters = new ArrayList<>();
    private Comparator<V> comparator;

    public SnapshotAgent(DataHolder<K, V> holder) {
        this.holder = holder;
    }

    @Override
    public void run() {
        List<Map.Entry<K, V>> list = getUrgentSnapshot();
        topSnapshot.set(getUrgentSnapshot());

        Map<K, Integer> map = IntStream.range(0, list.size())
                .boxed()
                .collect(Collectors.toMap(i -> list.get(i).getKey(), i -> i));
        indexMap.set(map);
    }

    @Override
    public void stop() {
        topSnapshot.set(Collections.emptyList());
        indexMap.set(Collections.emptyMap());
    }

    public List<Map.Entry<K, V>> getUrgentSnapshot() {
        Stream<Map.Entry<K, V>> stream = holder.getEntryMap().entrySet().stream()
                .<Map.Entry<K, V>>map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().getValue()))
                .filter(snapshot -> filters.stream().allMatch(filter -> filter.test(snapshot)));
        if (comparator != null) {
            stream = stream.sorted(Map.Entry.comparingByValue(comparator));
        }
        return stream.collect(Collectors.toList());
    }

    public List<Map.Entry<K, V>> getSnapshot() {
        return topSnapshot.get();
    }

    public int getSnapshotIndex(K key) {
        return indexMap.get().getOrDefault(key, -1);
    }

    public Optional<Map.Entry<K, V>> getSnapshotByIndex(int index) {
        List<Map.Entry<K, V>> list = getSnapshot();
        if (index < 0 || index >= list.size()) return Optional.empty();
        return Optional.of(list.get(index));
    }

    public Optional<DataEntry<K, V>> getEntryByIndex(int index) {
        return getSnapshotByIndex(index).flatMap(snapshot -> holder.getEntry(snapshot.getKey()));
    }

    public void setComparator(Comparator<V> comparator) {
        this.comparator = comparator;
    }

    public void addFilter(Predicate<Map.Entry<K, V>> filter) {
        filters.add(filter);
    }
}
