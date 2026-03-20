package me.hsgamer.topper.agent.snapshot;

import me.hsgamer.topper.agent.core.Agent;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SnapshotAgent<K, V> implements Agent, Runnable {
    private final AtomicReference<Snapshot<K, V>> snapshot = new AtomicReference<>(null);
    private Comparator<V> comparator;

    protected abstract Stream<Map.Entry<K, V>> getDataStream();

    protected abstract boolean needUpdate();

    @Override
    public void run() {
        Snapshot<K, V> currentSnapshot = snapshot.get();
        if (currentSnapshot != null && !needUpdate()) {
            return;
        }

        List<Map.Entry<K, V>> list = getUrgentSnapshot();
        Map<K, Integer> map = new HashMap<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            map.put(list.get(i).getKey(), i);
        }
        snapshot.set(new Snapshot<>(list, map));
    }

    @Override
    public void stop() {
        snapshot.set(null);
    }

    public List<Map.Entry<K, V>> getUrgentSnapshot() {
        Stream<Map.Entry<K, V>> stream = getDataStream();
        if (comparator != null) {
            stream = stream.sorted(Map.Entry.comparingByValue(comparator));
        }
        return stream.collect(Collectors.toList());
    }

    public List<Map.Entry<K, V>> getSnapshot() {
        Snapshot<K, V> s = snapshot.get();
        return s == null ? Collections.emptyList() : s.entryList;
    }

    public int getSnapshotIndex(K key) {
        Snapshot<K, V> s = snapshot.get();
        return s == null ? -1 : s.indexMap.getOrDefault(key, -1);
    }

    public Optional<Map.Entry<K, V>> getSnapshotByIndex(int index) {
        List<Map.Entry<K, V>> list = getSnapshot();
        if (index < 0 || index >= list.size()) return Optional.empty();
        return Optional.of(list.get(index));
    }

    public void setComparator(Comparator<V> comparator) {
        this.comparator = comparator;
    }

    private static final class Snapshot<K, V> {
        private final List<Map.Entry<K, V>> entryList;
        private final Map<K, Integer> indexMap;

        private Snapshot(List<Map.Entry<K, V>> entryList, Map<K, Integer> indexMap) {
            this.entryList = entryList;
            this.indexMap = indexMap;
        }
    }
}
