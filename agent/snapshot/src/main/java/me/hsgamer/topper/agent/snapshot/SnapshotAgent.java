package me.hsgamer.topper.agent.snapshot;

import me.hsgamer.topper.agent.core.AgentHolder;
import me.hsgamer.topper.agent.core.HolderEvent;
import me.hsgamer.topper.agent.core.Notifier;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class SnapshotAgent<K, V> implements Runnable {
    private final AtomicReference<Snapshot<K, V>> snapshot = new AtomicReference<>(Snapshot.empty());
    private final Notifier<SnapshotChange<K, V>> changeNotifier = new Notifier<>();
    private Comparator<V> comparator;

    protected abstract Stream<Map.Entry<K, V>> getDataStream();

    protected abstract boolean needUpdate();

    public void bindTo(AgentHolder<K, V> holder) {
        holder.getHolderNotifier().addListener(e -> {
            if (e != HolderEvent.UNREGISTERED) {
                return;
            }
            snapshot.set(Snapshot.empty());
        });
    }

    public Notifier<SnapshotChange<K, V>> getChangeNotifier() {
        return changeNotifier;
    }

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
        if (!changeNotifier.isEmpty()) {
            changeNotifier.fire(new SnapshotChange<>(currentSnapshot, newSnapshot));
        }

        snapshot.set(newSnapshot);
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
}
