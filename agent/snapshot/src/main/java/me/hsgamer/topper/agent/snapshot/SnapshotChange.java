package me.hsgamer.topper.agent.snapshot;

import java.util.*;

public class SnapshotChange<K, V> {
    public final Snapshot<K, V> oldSnapshot;
    public final Snapshot<K, V> newSnapshot;
    private final Map<K, Relative<K, V>> relatives = new HashMap<>();
    private volatile Map<K, Entry<K, V>> changes;
    private volatile List<Entry<K, V>> allEntries;
    private volatile List<Entry<K, V>> movedEntries;

    public SnapshotChange(Snapshot<K, V> oldSnapshot, Snapshot<K, V> newSnapshot) {
        this.oldSnapshot = oldSnapshot;
        this.newSnapshot = newSnapshot;
    }

    private static <K, V> void evaluateCrossing(int anchorPos, int entryPos, List<Entry<K, V>> entries,
                                                List<List<Entry<K, V>>> overtookAccumulators, List<List<Entry<K, V>>> overtakenByAccumulators) {
        Entry<K, V> anchor = entries.get(anchorPos);
        Entry<K, V> entry = entries.get(entryPos);
        int anchorOld = anchor.oldIndex;
        int anchorNew = anchor.newIndex;
        int entryOld = entry.oldIndex;
        int entryNew = entry.newIndex;
        if (entryOld >= 0 && (anchorOld < 0 || entryOld < anchorOld)
                && anchorNew >= 0 && (entryNew < 0 || entryNew > anchorNew)) {
            accumulate(overtookAccumulators, anchorPos, entry);
            accumulate(overtakenByAccumulators, entryPos, anchor);
        } else if (anchorOld >= 0 && (entryOld < 0 || entryOld > anchorOld)
                && entryNew >= 0 && (anchorNew < 0 || entryNew < anchorNew)) {
            accumulate(overtakenByAccumulators, anchorPos, entry);
            accumulate(overtookAccumulators, entryPos, anchor);
        }
    }

    private static <K, V> void accumulate(List<List<Entry<K, V>>> accumulators, int index, Entry<K, V> entry) {
        List<Entry<K, V>> accumulator = accumulators.get(index);
        if (accumulator == null) {
            accumulator = new ArrayList<>();
            accumulators.set(index, accumulator);
        }
        accumulator.add(entry);
    }

    public Map<K, Entry<K, V>> getChanges() {
        Map<K, Entry<K, V>> result = changes;
        if (result == null) {
            synchronized (this) {
                result = changes;
                if (result == null) {
                    result = computeChanges();
                    changes = result;
                }
            }
        }
        return result;
    }

    private Map<K, Entry<K, V>> computeChanges() {
        int oldSize = oldSnapshot.size();
        int newSize = newSnapshot.size();
        if (oldSize == 0 && newSize == 0) {
            return Collections.emptyMap();
        }

        Map<K, Entry<K, V>> changeMap = new HashMap<>((int) (Math.max(oldSize, newSize) / 0.75f) + 1);

        for (Map.Entry<K, Integer> indexEntry : newSnapshot.indexEntries()) {
            K key = indexEntry.getKey();
            int newIndex = indexEntry.getValue();
            int oldIndex = oldSnapshot.getIndex(key);
            changeMap.put(key, new Entry<>(
                    key,
                    oldIndex < 0 ? null : oldSnapshot.valueAt(oldIndex),
                    newSnapshot.valueAt(newIndex),
                    oldIndex,
                    newIndex
            ));
        }

        for (Map.Entry<K, Integer> indexEntry : oldSnapshot.indexEntries()) {
            K key = indexEntry.getKey();
            if (newSnapshot.getIndex(key) >= 0) continue;
            int oldIndex = indexEntry.getValue();
            changeMap.put(key, new Entry<>(key, oldSnapshot.valueAt(oldIndex), null, oldIndex, -1));
        }

        return Collections.unmodifiableMap(changeMap);
    }

    public synchronized Relative<K, V> getRelative(K key) {
        Relative<K, V> cached = relatives.get(key);
        if (cached == null) {
            if (!getChanges().containsKey(key)) {
                return Relative.empty(key);
            }
            cached = computeRelative(key);
            relatives.put(key, cached);
        }
        return cached;
    }

    public synchronized Map<K, Relative<K, V>> getRelatives() {
        Map<K, Entry<K, V>> changeMap = getChanges();
        if (relatives.size() < changeMap.size()) {
            if (oldSnapshot.sameOrder(newSnapshot)) {
                for (Entry<K, V> anchor : changeMap.values()) {
                    relatives.putIfAbsent(anchor.key, new Relative<>(anchor, Collections.emptyList(), Collections.emptyList()));
                }
            } else {
                computeAllRelatives(changeMap);
            }
        }
        return Collections.unmodifiableMap(relatives);
    }

    private Relative<K, V> computeRelative(K key) {
        Map<K, Entry<K, V>> changeMap = getChanges();
        Entry<K, V> anchor = changeMap.get(key);
        if (anchor == null) return Relative.empty(key);

        int anchorOld = anchor.oldIndex;
        int anchorNew = anchor.newIndex;
        List<Entry<K, V>> overtook = null;
        List<Entry<K, V>> overtakenBy = null;
        // An anchor that did not move can only be crossed by entries that did.
        ensureEntryLists();
        List<Entry<K, V>> candidates = anchorOld != anchorNew ? allEntries : movedEntries;
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

    private void computeAllRelatives(Map<K, Entry<K, V>> changeMap) {
        int size = changeMap.size();
        List<Entry<K, V>> entries = new ArrayList<>(changeMap.values());
        List<List<Entry<K, V>>> overtookAccumulators = new ArrayList<>(Collections.nCopies(size, null));
        List<List<Entry<K, V>>> overtakenByAccumulators = new ArrayList<>(Collections.nCopies(size, null));
        // Only pairs with a moved side can cross. Moved-moved pairs go to the
        // lower row; moved-unmoved pairs go to the moved side's row.
        int[] moved = new int[size];
        int[] unmoved = new int[size];
        int movedCount = 0;
        int unmovedCount = 0;
        for (int i = 0; i < size; i++) {
            Entry<K, V> anchor = entries.get(i);
            if (anchor.oldIndex == anchor.newIndex) {
                unmoved[unmovedCount++] = i;
            } else {
                moved[movedCount++] = i;
            }
        }
        for (int a = 0; a < movedCount; a++) {
            int anchorPos = moved[a];
            for (int b = a + 1; b < movedCount; b++) {
                evaluateCrossing(anchorPos, moved[b], entries, overtookAccumulators, overtakenByAccumulators);
            }
            for (int u = 0; u < unmovedCount; u++) {
                evaluateCrossing(anchorPos, unmoved[u], entries, overtookAccumulators, overtakenByAccumulators);
            }
        }

        for (int i = 0; i < size; i++) {
            Entry<K, V> anchor = entries.get(i);
            List<Entry<K, V>> overtook = overtookAccumulators.get(i);
            List<Entry<K, V>> overtakenBy = overtakenByAccumulators.get(i);
            relatives.putIfAbsent(anchor.key, new Relative<>(
                    anchor,
                    overtook == null ? Collections.emptyList() : Collections.unmodifiableCollection(overtook),
                    overtakenBy == null ? Collections.emptyList() : Collections.unmodifiableCollection(overtakenBy)
            ));
        }
    }

    private void ensureEntryLists() {
        if (allEntries == null) {
            synchronized (this) {
                if (allEntries == null) {
                    List<Entry<K, V>> all = new ArrayList<>(getChanges().values());
                    List<Entry<K, V>> moved = new ArrayList<>();
                    for (Entry<K, V> entry : all) {
                        if (entry.oldIndex != entry.newIndex) {
                            moved.add(entry);
                        }
                    }
                    movedEntries = moved;
                    allEntries = all;
                }
            }
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
