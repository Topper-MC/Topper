package me.hsgamer.topper.agent.storage;

import me.hsgamer.topper.agent.core.AgentHolder;
import me.hsgamer.topper.storage.core.DataStorage;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StorageAgent<K, V> implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(StorageAgent.class.getSimpleName());

    private final DataStorage<K, V> storage;
    private final Queue<Map.Entry<K, ValueWrapper<V>>> queue = new ConcurrentLinkedQueue<>(); // Value can be null representing removal
    private final AtomicReference<Map<K, ValueWrapper<V>>> storeMap = new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicReference<Map<K, ValueWrapper<V>>> savingMap = new AtomicReference<>();
    private final ReentrantLock saveLock = new ReentrantLock();
    private int maxEntryPerCall = 10;
    private long nextAttemptAt = 0L;
    private long retryDelayMillis = 5_000L;
    private boolean failureLogged = false;

    public StorageAgent(DataStorage<K, V> storage) {
        this.storage = storage;
    }

    public void bindTo(AgentHolder<K, V> holder, boolean loadOnCreate, boolean scheduleOnEntryRemove) {
        holder.getEntryNotifier().addListener(e -> {
            switch (e.kind) {
                case CREATED:
                    if (loadOnCreate) {
                        storage.load(e.entry.getKey()).ifPresent(value -> e.entry.setValue(value, false));
                    }
                    break;
                case UPDATED:
                    scheduleValue(e.entry.getKey(), Objects.equals(e.newValue, e.entry.getHolder().getDefaultValue()) ? null : e.newValue);
                    break;
                case REMOVED:
                    if (scheduleOnEntryRemove) {
                        scheduleValue(e.entry.getKey(), null);
                    }
                    break;
                default:
                    break;
            }
        });
        holder.getHolderNotifier().addListener(e -> {
            switch (e) {
                case REGISTERED:
                    storage.onRegister();
                    if (!loadOnCreate) {
                        try {
                            storage.load().forEach((uuid, value) -> holder.getOrCreateEntry(uuid).setValue(value, false));
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "Failed to load entries", ex);
                        }
                    }
                    break;
                case BEFORE_UNREGISTER:
                    save(true);
                    break;
                case UNREGISTERED:
                    storage.onUnregister();
                    break;
                default:
                    break;
            }
        });
    }

    public void bindTo(AgentHolder<K, V> holder) {
        bindTo(holder, false, true);
    }

    private void save(boolean urgent) {
        if (urgent) {
            saveLock.lock();
        } else if (!saveLock.tryLock()) {
            return;
        }

        try {
            if (!urgent && System.currentTimeMillis() < nextAttemptAt) {
                return;
            }

            storeMap.getAndSet(new ConcurrentHashMap<>())
                    .forEach((key, value) -> queue.add(new AbstractMap.SimpleEntry<>(key, value)));

            Map<K, ValueWrapper<V>> map = savingMap.updateAndGet(old -> old == null ? new HashMap<>() : old);

            while (urgent || maxEntryPerCall <= 0 || map.size() < maxEntryPerCall) {
                Map.Entry<K, ValueWrapper<V>> entry = queue.poll();
                if (entry == null) {
                    break;
                }
                map.put(entry.getKey(), entry.getValue());
            }

            if (map.isEmpty()) {
                savingMap.set(null);
                onSuccess();
                return;
            }

            Set<K> removeKeys = new HashSet<>();
            Map<K, V> finalMap = map.entrySet()
                    .stream()
                    .filter(entry -> {
                        V value = entry.getValue().value;
                        if (value == null) {
                            removeKeys.add(entry.getKey());
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().value));


            Optional<DataStorage.Modifier<K, V>> optionalModifier = storage.modify();
            if (!optionalModifier.isPresent()) {
                onFailure(null);
                return;
            }

            DataStorage.Modifier<K, V> modifier = optionalModifier.get();
            try {
                modifier.save(finalMap);
                if (!removeKeys.isEmpty()) {
                    modifier.remove(removeKeys);
                }
                modifier.commit();
                savingMap.set(null);
                onSuccess();
            } catch (Throwable t) {
                onFailure(t);
                modifier.rollback();
            }
        } finally {
            saveLock.unlock();
        }
    }

    protected void scheduleValue(K key, @Nullable V value) {
        storeMap.get().put(key, new ValueWrapper<>(value));
    }

    private void onFailure(@Nullable Throwable t) {
        nextAttemptAt = System.currentTimeMillis() + retryDelayMillis;
        if (t != null) {
            LOGGER.log(failureLogged ? Level.FINE : Level.SEVERE, "Failed to save entries", t);
            failureLogged = true;
        }
    }

    private void onSuccess() {
        nextAttemptAt = 0L;
        failureLogged = false;
    }

    @Override
    public void run() {
        save(false);
    }

    public DataStorage<K, V> getStorage() {
        return storage;
    }

    public void setMaxEntryPerCall(int taskSaveEntryPerTick) {
        this.maxEntryPerCall = taskSaveEntryPerTick;
    }

    public void setRetryDelayMillis(long retryDelayMillis) {
        this.retryDelayMillis = retryDelayMillis;
    }

    private static final class ValueWrapper<V> {
        private final @Nullable V value;

        private ValueWrapper(@Nullable V value) {
            this.value = value;
        }
    }
}
