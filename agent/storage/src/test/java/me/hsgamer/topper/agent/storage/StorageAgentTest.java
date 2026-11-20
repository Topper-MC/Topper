package me.hsgamer.topper.agent.storage;

import me.hsgamer.topper.storage.core.DataStorage;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class StorageAgentTest {
    @Test
    public void retainsFailedBatchAndRetries() {
        FakeStorage storage = new FakeStorage();
        StorageAgent<UUID, Double> agent = new StorageAgent<>(storage);
        agent.setRetryDelayMillis(0);
        UUID id = UUID.randomUUID();
        agent.scheduleValue(id, 1.0);

        storage.failCommit = true;
        agent.run();
        assertTrue(storage.saved.isEmpty());
        assertEquals(1, storage.lastBatchSize);
        assertTrue(storage.rolledBack);

        storage.failCommit = false;
        agent.run();
        assertEquals(1, storage.saved.size());
        assertEquals(Double.valueOf(1.0), storage.saved.get(id));
    }

    @Test
    public void doesNotGrowPendingBatchBeyondLimit() {
        FakeStorage storage = new FakeStorage();
        StorageAgent<UUID, Double> agent = new StorageAgent<>(storage);
        agent.setRetryDelayMillis(0);
        agent.setMaxEntryPerCall(2);
        for (int i = 0; i < 3; i++) {
            agent.scheduleValue(UUID.randomUUID(), (double) i);
        }

        storage.failCommit = true;
        agent.run();
        assertEquals(2, storage.lastBatchSize);
        agent.run();
        assertEquals(2, storage.lastBatchSize);

        storage.failCommit = false;
        agent.run();
        assertEquals(2, storage.saved.size());
        agent.run();
        assertEquals(3, storage.saved.size());
    }

    @Test
    public void commitFailureKeepsData() {
        FakeStorage storage = new FakeStorage();
        StorageAgent<UUID, Double> agent = new StorageAgent<>(storage);
        agent.setRetryDelayMillis(0);
        UUID id = UUID.randomUUID();
        agent.scheduleValue(id, 42.0);

        storage.failCommit = true;
        agent.run();
        assertTrue(storage.saved.isEmpty());

        storage.failCommit = false;
        agent.run();
        assertEquals(Double.valueOf(42.0), storage.saved.get(id));
    }

    @Test
    public void backsOffAfterFailure() {
        FakeStorage storage = new FakeStorage();
        StorageAgent<UUID, Double> agent = new StorageAgent<>(storage);
        agent.scheduleValue(UUID.randomUUID(), 1.0);

        storage.failCommit = true;
        agent.run();
        assertEquals(1, storage.modifyCalls);

        agent.run();
        assertEquals(1, storage.modifyCalls);
    }

    private static final class FakeStorage implements DataStorage<UUID, Double> {
        final Map<UUID, Double> saved = new HashMap<>();
        boolean failCommit = false;
        boolean rolledBack = false;
        int lastBatchSize = -1;
        int modifyCalls = 0;

        @Override
        public Map<UUID, Double> load() {
            return new HashMap<>(saved);
        }

        @Override
        public Optional<Double> load(UUID key) {
            return Optional.ofNullable(saved.get(key));
        }

        @Override
        public Optional<Modifier<UUID, Double>> modify() {
            modifyCalls++;
            return Optional.of(new Modifier<UUID, Double>() {
                private final Map<UUID, Double> pending = new HashMap<>();

                @Override
                public void save(Map<UUID, Double> map) {
                    lastBatchSize = map.size();
                    pending.putAll(map);
                }

                @Override
                public void remove(Collection<UUID> keys) {
                    pending.keySet().removeAll(keys);
                }

                @Override
                public void commit() {
                    if (failCommit) {
                        throw new IllegalStateException("commit failed");
                    }
                    saved.putAll(pending);
                }

                @Override
                public void rollback() {
                    rolledBack = true;
                }
            });
        }
    }
}
