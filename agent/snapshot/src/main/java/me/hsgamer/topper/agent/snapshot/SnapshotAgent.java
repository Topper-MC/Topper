package me.hsgamer.topper.agent.snapshot;

import me.hsgamer.topper.agent.core.Agent;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class SnapshotAgent<K, V> implements Agent, Runnable {
    private final AtomicReference<Snapshot<K, V>> snapshot = new AtomicReference<>(Snapshot.empty());
    private Comparator<V> comparator;
    private Consumer<SnapshotChange<K, V>> changeConsumer;

    protected abstract Stream<Map.Entry<K, V>> getDataStream();

    protected abstract boolean needUpdate();

    @Override
    public void run() {
        Snapshot<K, V> currentSnapshot = snapshot.get();
        if (!currentSnapshot.isEmpty() && !needUpdate()) {
            return;
        }

        List<Map.Entry<K, V>> list = getUrgentSnapshot();
        Map<K, Integer> map = IntStream.range(0, list.size()).parallel()
                .mapToObj(i -> new AbstractMap.SimpleImmutableEntry<>(list.get(i).getKey(), i))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Snapshot<K, V> newSnapshot = new Snapshot<>(list, map);
        if (changeConsumer != null) {
            changeConsumer.accept(new SnapshotChange<>(currentSnapshot, newSnapshot));
        }

        snapshot.set(newSnapshot);
    }

    @Override
    public void stop() {
        snapshot.set(Snapshot.empty());
    }

    public List<Map.Entry<K, V>> getUrgentSnapshot() {
        Stream<Map.Entry<K, V>> stream = getDataStream();
        if (comparator != null) {
            stream = stream.sorted(Map.Entry.comparingByValue(comparator));
        }
        return stream.collect(Collectors.toList());
    }

    public Snapshot<K, V> getSnapshot() {
        return snapshot.get();
    }

    public int getSnapshotIndex(K key) {
        return snapshot.get().getIndex(key);
    }

    public Optional<Map.Entry<K, V>> getSnapshotByIndex(int index) {
        return snapshot.get().getByIndex(index);
    }

    public void setComparator(Comparator<V> comparator) {
        this.comparator = comparator;
    }

    public void setChangeConsumer(Consumer<SnapshotChange<K, V>> changeConsumer) {
        this.changeConsumer = changeConsumer;
    }
}
