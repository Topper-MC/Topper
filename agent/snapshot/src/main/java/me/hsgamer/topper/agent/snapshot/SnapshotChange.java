package me.hsgamer.topper.agent.snapshot;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SnapshotChange<K, V> {
    public final Snapshot<K, V> oldSnapshot;
    public final Snapshot<K, V> newSnapshot;
    private final ConcurrentHashMap<K, Relative<K, V>> relatives = new ConcurrentHashMap<>();
    private final Map<K, Relative<K, V>> relativesView = Collections.unmodifiableMap(relatives);
    private volatile ChangeSet<K, V> changes;

    public SnapshotChange(Snapshot<K, V> oldSnapshot, Snapshot<K, V> newSnapshot) {
        this.oldSnapshot = oldSnapshot;
        this.newSnapshot = newSnapshot;
    }

    public Map<K, Entry<K, V>> getChanges() {
        return changeSet().map;
    }

    private ChangeSet<K, V> changeSet() {
        ChangeSet<K, V> result = changes;
        if (result == null) {
            synchronized (this) {
                result = changes;
                if (result == null) {
                    result = computeChangeSet();
                    changes = result;
                }
            }
        }
        return result;
    }

    private ChangeSet<K, V> computeChangeSet() {
        int oldSize = oldSnapshot.size();
        int newSize = newSnapshot.size();
        if (oldSize == 0 && newSize == 0) {
            return new ChangeSet<>(Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
        }

        Map<K, Entry<K, V>> changeMap = new HashMap<>((int) (Math.max(oldSize, newSize) / 0.75f) + 1);
        List<Entry<K, V>> all = new ArrayList<>(Math.max(oldSize, newSize));
        List<Entry<K, V>> moved = new ArrayList<>();

        for (Map.Entry<K, Integer> indexEntry : newSnapshot.indexEntries()) {
            K key = indexEntry.getKey();
            int newIndex = indexEntry.getValue();
            int oldIndex = oldSnapshot.getIndex(key);
            Entry<K, V> entry = new Entry<>(
                    key,
                    oldIndex < 0 ? null : oldSnapshot.valueAt(oldIndex),
                    newSnapshot.valueAt(newIndex),
                    oldIndex,
                    newIndex
            );
            changeMap.put(key, entry);
            all.add(entry);
            if (oldIndex != newIndex) {
                moved.add(entry);
            }
        }

        for (Map.Entry<K, Integer> indexEntry : oldSnapshot.indexEntries()) {
            K key = indexEntry.getKey();
            if (newSnapshot.getIndex(key) >= 0) continue;
            int oldIndex = indexEntry.getValue();
            Entry<K, V> entry = new Entry<>(key, oldSnapshot.valueAt(oldIndex), null, oldIndex, -1);
            changeMap.put(key, entry);
            all.add(entry);
            moved.add(entry);
        }

        return new ChangeSet<>(Collections.unmodifiableMap(changeMap), all, moved);
    }

    public Relative<K, V> getRelative(K key) {
        if (key == null) {
            return computeRelative(null);
        }
        Relative<K, V> cached = relatives.get(key);
        if (cached == null) {
            if (!getChanges().containsKey(key)) {
                return Relative.empty(key);
            }
            cached = relatives.computeIfAbsent(key, this::computeRelative);
        }
        return cached;
    }

    public Map<K, Relative<K, V>> getRelatives() {
        Map<K, Entry<K, V>> changeMap = getChanges();
        if (relatives.size() < changeMap.size()) {
            if (oldSnapshot.sameOrder(newSnapshot)) {
                for (Entry<K, V> anchor : changeMap.values()) {
                    relatives.putIfAbsent(anchor.key, new Relative<>(anchor, Collections.emptyList(), Collections.emptyList()));
                }
            } else {
                for (K key : changeMap.keySet()) {
                    getRelative(key);
                }
            }
        }
        return relativesView;
    }

    private Relative<K, V> computeRelative(K key) {
        ChangeSet<K, V> computed = changeSet();
        Entry<K, V> anchor = computed.map.get(key);
        if (anchor == null) return Relative.empty(key);

        int anchorOld = anchor.oldIndex;
        int anchorNew = anchor.newIndex;

        List<Entry<K, V>> overtook = null;
        List<Entry<K, V>> overtakenBy = null;
        // An anchor that did not move can only be crossed by entries that did.
        List<Entry<K, V>> candidates = anchorOld != anchorNew ? computed.all : computed.moved;
        for (Entry<K, V> entry : candidates) {
            if (entry == anchor) continue;
            int entryOld = entry.oldIndex;
            int entryNew = entry.newIndex;
            boolean wasAboveAnchor = entryOld >= 0 && (anchorOld < 0 || entryOld < anchorOld);
            boolean nowBelowAnchor = anchorNew >= 0 && (entryNew < 0 || entryNew > anchorNew);
            boolean wasBelowAnchor = anchorOld >= 0 && (entryOld < 0 || entryOld > anchorOld);
            boolean nowAboveAnchor = entryNew >= 0 && (anchorNew < 0 || entryNew < anchorNew);
            if (wasAboveAnchor && nowBelowAnchor) {
                if (overtook == null) overtook = new ArrayList<>();
                overtook.add(entry);
            } else if (wasBelowAnchor && nowAboveAnchor) {
                if (overtakenBy == null) overtakenBy = new ArrayList<>();
                overtakenBy.add(entry);
            }
        }
        return new Relative<>(
                anchor,
                overtook == null ? Collections.emptyList() : Collections.unmodifiableCollection(overtook),
                overtakenBy == null ? Collections.emptyList() : Collections.unmodifiableCollection(overtakenBy)
        );
    }

    private static final class ChangeSet<K, V> {
        final Map<K, Entry<K, V>> map;
        final List<Entry<K, V>> all;
        final List<Entry<K, V>> moved;

        ChangeSet(Map<K, Entry<K, V>> map, List<Entry<K, V>> all, List<Entry<K, V>> moved) {
            this.map = map;
            this.all = all;
            this.moved = moved;
        }
    }

    public static final class Entry<K, V> {
        public final K key;
        public final V oldValue;
        public final V newValue;
        public final int oldIndex;
        public final int newIndex;

        Entry(K key, V oldValue, V newValue, int oldIndex, int newIndex) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.oldIndex = oldIndex;
            this.newIndex = newIndex;
        }

        static <K, V> Entry<K, V> empty(K key) {
            return new Entry<>(key, null, null, -1, -1);
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

        static <K, V> Relative<K, V> empty(K key) {
            return new Relative<>(Entry.empty(key), Collections.emptyList(), Collections.emptyList());
        }
    }
}
