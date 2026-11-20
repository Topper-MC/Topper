package me.hsgamer.topper.agent.snapshot;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class SnapshotChangeTest {
    @Test
    public void completesWithChangesAndRelatives() {
        SnapshotChange<String, Integer> change = new SnapshotChange<>(
                snapshot(map("a", 1, "b", 2, "c", 3)),
                snapshot(map("c", 4, "a", 2, "b", 1))
        );

        SnapshotChange.Data<String, Integer> data = change.getData().join();

        assertEquals(3, data.changes.size());
        assertFalse(data.changes.get("c").oldIndex == data.changes.get("c").newIndex);

        SnapshotChange.Relative<String, Integer> c = data.relatives.get("c");
        assertEquals(set("a", "b"), keys(c.overtook));
        assertTrue(c.overtakenBy.isEmpty());

        SnapshotChange.Relative<String, Integer> a = data.relatives.get("a");
        assertEquals(Collections.singleton("c"), keys(a.overtakenBy));
        assertTrue(a.overtook.isEmpty());
    }

    @Test
    public void relativesCoverEveryChangedKey() {
        SnapshotChange<String, Integer> change = new SnapshotChange<>(
                snapshot(map("a", 1, "b", 2, "c", 3)),
                snapshot(map("c", 4, "a", 2, "b", 1))
        );

        SnapshotChange.Data<String, Integer> data = change.getData().join();

        assertEquals(data.changes.keySet(), data.relatives.keySet());
    }

    @Test
    public void sameOrderProducesEmptyRelatives() {
        SnapshotChange<String, Integer> change = new SnapshotChange<>(
                snapshot(map("a", 1, "b", 2)),
                snapshot(map("a", 2, "b", 3))
        );

        SnapshotChange.Data<String, Integer> data = change.getData().join();

        assertEquals(2, data.relatives.size());
        for (SnapshotChange.Relative<String, Integer> relative : data.relatives.values()) {
            assertTrue(relative.overtook.isEmpty());
            assertTrue(relative.overtakenBy.isEmpty());
        }
    }

    @Test
    public void emptySnapshotsProduceEmptyData() {
        SnapshotChange<String, Integer> change = new SnapshotChange<>(Snapshot.empty(), Snapshot.empty());

        SnapshotChange.Data<String, Integer> data = change.getData().join();

        assertTrue(data.changes.isEmpty());
        assertTrue(data.relatives.isEmpty());
    }

    @Test
    public void memoizesTheFuture() {
        SnapshotChange<String, Integer> change = new SnapshotChange<>(
                snapshot(map("a", 1)),
                snapshot(map("a", 2))
        );

        assertSame(change.getData(), change.getData());
    }

    @Test
    public void matchesBruteForceOnRandomChanges() {
        Random random = new Random(1234L);
        for (int round = 0; round < 500; round++) {
            int size = random.nextInt(9);
            List<String> oldKeys = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                oldKeys.add("k" + i);
            }
            Collections.shuffle(oldKeys, random);

            List<String> newKeys = new ArrayList<>(oldKeys);
            int removals = newKeys.isEmpty() ? 0 : random.nextInt(Math.min(3, newKeys.size() + 1));
            for (int i = 0; i < removals; i++) {
                newKeys.remove(random.nextInt(newKeys.size()));
            }
            Collections.shuffle(newKeys, random);
            int additions = random.nextInt(3);
            for (int i = 0; i < additions; i++) {
                newKeys.add(random.nextInt(newKeys.size() + 1), "n" + round + "_" + i);
            }

            assertMatchesBruteForce(oldKeys, newKeys);
        }
    }

    private static void assertMatchesBruteForce(List<String> oldKeys, List<String> newKeys) {
        Map<String, Integer> oldIndexes = indexes(oldKeys);
        Map<String, Integer> newIndexes = indexes(newKeys);
        Set<String> union = new LinkedHashSet<>(oldKeys);
        union.addAll(newKeys);

        SnapshotChange.Data<String, Integer> data = new SnapshotChange<>(
                snapshotOf(oldKeys),
                snapshotOf(newKeys)
        ).getData().join();

        assertEquals(union, data.changes.keySet());
        assertEquals(union, data.relatives.keySet());

        for (String anchor : union) {
            Set<String> expectedOvertook = new HashSet<>();
            Set<String> expectedOvertakenBy = new HashSet<>();
            int anchorOld = oldIndexes.getOrDefault(anchor, -1);
            int anchorNew = newIndexes.getOrDefault(anchor, -1);
            for (String other : union) {
                if (other.equals(anchor)) continue;
                int otherOld = oldIndexes.getOrDefault(other, -1);
                int otherNew = newIndexes.getOrDefault(other, -1);
                boolean wasAbove = otherOld >= 0 && (anchorOld < 0 || otherOld < anchorOld);
                boolean nowBelow = anchorNew >= 0 && (otherNew < 0 || otherNew > anchorNew);
                boolean wasBelow = anchorOld >= 0 && (otherOld < 0 || otherOld > anchorOld);
                boolean nowAbove = otherNew >= 0 && (anchorNew < 0 || otherNew < anchorNew);
                if (wasAbove && nowBelow) {
                    expectedOvertook.add(other);
                } else if (wasBelow && nowAbove) {
                    expectedOvertakenBy.add(other);
                }
            }
            assertEquals(expectedOvertook, keys(data.relatives.get(anchor).overtook), "overtook of " + anchor);
            assertEquals(expectedOvertakenBy, keys(data.relatives.get(anchor).overtakenBy), "overtakenBy of " + anchor);
        }
    }

    private static Map<String, Integer> indexes(List<String> keys) {
        Map<String, Integer> indexes = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            indexes.put(keys.get(i), i);
        }
        return indexes;
    }

    private static Snapshot<String, Integer> snapshotOf(List<String> keys) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        Map<String, Integer> index = new HashMap<>();
        for (String key : keys) {
            index.put(key, list.size());
            list.add(new AbstractMap.SimpleImmutableEntry<>(key, list.size()));
        }
        return new Snapshot<>(list, index);
    }

    private static <K, V> Map<K, V> map(K k1, V v1) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        return map;
    }

    private static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2) {
        Map<K, V> map = map(k1, v1);
        map.put(k2, v2);
        return map;
    }

    private static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = map(k1, v1, k2, v2);
        map.put(k3, v3);
        return map;
    }

    private static <K, V> Snapshot<K, V> snapshot(Map<K, V> values) {
        List<Map.Entry<K, V>> list = new ArrayList<>();
        Map<K, Integer> index = new HashMap<>();
        for (Map.Entry<K, V> entry : values.entrySet()) {
            index.put(entry.getKey(), list.size());
            list.add(new AbstractMap.SimpleImmutableEntry<>(entry));
        }
        return new Snapshot<>(list, index);
    }

    private static <K, V> Set<K> keys(Collection<SnapshotChange.Entry<K, V>> entries) {
        Set<K> keys = new HashSet<>();
        for (SnapshotChange.Entry<K, V> entry : entries) {
            keys.add(entry.key);
        }
        return keys;
    }

    private static <K> Set<K> set(K k1, K k2) {
        Set<K> set = new HashSet<>();
        set.add(k1);
        set.add(k2);
        return set;
    }
}
