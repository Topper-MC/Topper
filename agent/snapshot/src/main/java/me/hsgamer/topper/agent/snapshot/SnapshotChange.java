package me.hsgamer.topper.agent.snapshot;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SnapshotChange<K, V> {
    public final Snapshot<K, V> oldSnapshot;
    public final Snapshot<K, V> newSnapshot;
    private volatile CompletableFuture<Data<K, V>> data;

    public SnapshotChange(Snapshot<K, V> oldSnapshot, Snapshot<K, V> newSnapshot) {
        this.oldSnapshot = oldSnapshot;
        this.newSnapshot = newSnapshot;
    }

    public CompletableFuture<Data<K, V>> getData() {
        CompletableFuture<Data<K, V>> result = data;
        if (result == null) {
            synchronized (this) {
                result = data;
                if (result == null) {
                    result = CompletableFuture.supplyAsync(this::computeData);
                    data = result;
                }
            }
        }
        return result;
    }

    private Data<K, V> computeData() {
        int oldSize = oldSnapshot.size();
        int newSize = newSnapshot.size();

        Map<K, Entry<K, V>> changes = new HashMap<>((int) (Math.max(oldSize, newSize) / 0.75f) + 1);
        int survivors = 0;
        for (Map.Entry<K, Integer> indexEntry : newSnapshot.indexEntries()) {
            K key = indexEntry.getKey();
            int newIndex = indexEntry.getValue();
            int oldIndex = oldSnapshot.getIndex(key);
            if (oldIndex >= 0) survivors++;
            changes.put(key, new Entry<>(
                    key,
                    oldIndex < 0 ? null : oldSnapshot.valueAt(oldIndex),
                    newSnapshot.valueAt(newIndex),
                    oldIndex,
                    newIndex,
                    oldSize,
                    newSize
            ));
        }

        if (survivors < oldSize) {
            for (Map.Entry<K, Integer> indexEntry : oldSnapshot.indexEntries()) {
                K key = indexEntry.getKey();
                if (newSnapshot.getIndex(key) >= 0) continue;
                int oldIndex = indexEntry.getValue();
                changes.put(key, new Entry<>(key, oldSnapshot.valueAt(oldIndex), null, oldIndex, -1, oldSize, newSize));
            }
        }

        if (!changes.isEmpty() && !oldSnapshot.sameOrder(newSnapshot)) {
            computeRelatives(changes.values());
        }

        Map<K, Relative<K, V>> relatives = new HashMap<>((int) (changes.size() / 0.75f) + 1);
        for (Entry<K, V> entry : changes.values()) {
            relatives.put(entry.key, new Relative<>(entry, unmodifiable(entry.overtook), unmodifiable(entry.overtakenBy)));
        }

        return new Data<>(Collections.unmodifiableMap(changes), Collections.unmodifiableMap(relatives));
    }

    private static <K, V> Collection<Entry<K, V>> unmodifiable(List<Entry<K, V>> entries) {
        return entries == null ? Collections.emptyList() : Collections.unmodifiableList(entries);
    }

    private static <K, V> void computeRelatives(Collection<Entry<K, V>> entries) {
        Entry<K, V>[] array = entries.toArray(entryArray(0));
        Arrays.sort(array, SnapshotChange::compareByOldPos);
        mergeSort(array, entryArray(array.length), 0, array.length);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Entry<K, V>[] entryArray(int size) {
        return (Entry<K, V>[]) new Entry[size];
    }

    private static <K, V> int compareByOldPos(Entry<K, V> a, Entry<K, V> b) {
        int compared = Integer.compare(a.oldPos, b.oldPos);
        return compared != 0 ? compared : Integer.compare(a.newPos, b.newPos);
    }

    private static <K, V> void mergeSort(Entry<K, V>[] entries, Entry<K, V>[] buffer, int from, int to) {
        if (to - from < 2) {
            return;
        }

        int mid = (from + to) >>> 1;
        mergeSort(entries, buffer, from, mid);
        mergeSort(entries, buffer, mid, to);

        int i = from;
        int j = mid;
        int k = from;
        while (i < mid && j < to) {
            if (entries[i].newPos <= entries[j].newPos) {
                buffer[k++] = entries[i++];
            } else {
                Entry<K, V> anchor = entries[j];
                for (int x = i; x < mid; x++) {
                    Entry<K, V> overtaker = entries[x];
                    if (overtaker.oldPos < anchor.oldPos) {
                        addInversion(overtaker, anchor);
                    }
                }
                buffer[k++] = entries[j++];
            }
        }
        System.arraycopy(entries, i, buffer, k, mid - i);
        System.arraycopy(entries, j, buffer, k + mid - i, to - j);
        System.arraycopy(buffer, from, entries, from, to - from);
    }

    private static <K, V> void addInversion(Entry<K, V> overtaker, Entry<K, V> anchor) {
        if (anchor.overtook == null) {
            anchor.overtook = new ArrayList<>();
        }
        anchor.overtook.add(overtaker);
        if (overtaker.overtakenBy == null) {
            overtaker.overtakenBy = new ArrayList<>();
        }
        overtaker.overtakenBy.add(anchor);
    }

    public static final class Data<K, V> {
        public final Map<K, Entry<K, V>> changes;
        public final Map<K, Relative<K, V>> relatives;

        Data(Map<K, Entry<K, V>> changes, Map<K, Relative<K, V>> relatives) {
            this.changes = changes;
            this.relatives = relatives;
        }
    }

    public static final class Entry<K, V> {
        public final K key;
        public final V oldValue;
        public final V newValue;
        public final int oldIndex;
        public final int newIndex;
        final int oldPos;
        final int newPos;
        List<Entry<K, V>> overtook;
        List<Entry<K, V>> overtakenBy;

        Entry(K key, V oldValue, V newValue, int oldIndex, int newIndex, int oldSize, int newSize) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.oldIndex = oldIndex;
            this.newIndex = newIndex;
            this.oldPos = oldIndex < 0 ? oldSize : oldIndex;
            this.newPos = newIndex < 0 ? newSize : newIndex;
        }
    }

    public static final class Relative<K, V> {
        public final Entry<K, V> anchor;
        public final Collection<Entry<K, V>> overtook;
        public final Collection<Entry<K, V>> overtakenBy;

        Relative(Entry<K, V> anchor, Collection<Entry<K, V>> overtook, Collection<Entry<K, V>> overtakenBy) {
            this.anchor = anchor;
            this.overtook = overtook;
            this.overtakenBy = overtakenBy;
        }
    }
}
