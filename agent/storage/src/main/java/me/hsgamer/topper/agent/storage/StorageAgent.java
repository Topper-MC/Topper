package me.hsgamer.topper.agent.storage;

import me.hsgamer.topper.agent.core.Agent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.core.DataHolder;
import me.hsgamer.topper.storage.core.DataStorage;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageAgent<K, V> implements Agent<K, V>, Runnable {
    private final Logger logger;
    private final DataHolder<K, V> holder;
    private final DataStorage<K, V> storage;
    private final Queue<Map.Entry<K, V>> queue = new ConcurrentLinkedQueue<>(); // Value can be null representing removal
    private final AtomicReference<Map<K, V>> savingMap = new AtomicReference<>();
    private final AtomicBoolean saving = new AtomicBoolean(false);
    private int maxEntryPerCall = 10;

    public StorageAgent(Logger logger, DataHolder<K, V> holder, DataStorage<K, V> storage) {
        this.logger = logger;
        this.holder = holder;
        this.storage = storage;
    }

    private void save(boolean urgent) {
        if (saving.get() && !urgent) return;
        saving.set(true);

        Map<K, V> map = savingMap.get();
        if (map == null) {
            map = new HashMap<>();
        }
        savingMap.set(map);

        for (int i = 0; i < (urgent || maxEntryPerCall <= 0 ? Integer.MAX_VALUE : maxEntryPerCall); i++) {
            Map.Entry<K, V> entry = queue.poll();
            if (entry == null) {
                break;
            }
            map.put(entry.getKey(), entry.getValue());
        }

        if (map.isEmpty()) {
            savingMap.set(null);
            saving.set(false);
            return;
        }

        Map<K, V> finalMap = new HashMap<>();
        Set<K> removeKeys = new HashSet<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                removeKeys.add(entry.getKey());
            } else {
                finalMap.put(entry.getKey(), entry.getValue());
            }
        }

        storage.save(finalMap, urgent)
                .thenCompose(v -> {
                    if (removeKeys.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        return storage.remove(removeKeys, urgent);
                    }
                })
                .whenComplete((v, throwable) -> {
                    if (throwable != null) {
                        logger.log(Level.SEVERE, "Failed to save entries for " + holder.getName(), throwable);
                    } else {
                        savingMap.set(null);
                    }
                    saving.set(false);
                });
    }

    @Override
    public void start() {
        storage.onRegister();
        try {
            storage.load().forEach((uuid, value) -> holder.getOrCreateEntry(uuid).setValue(value, false));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load top entries for " + holder.getName(), e);
        }
    }

    @Override
    public void stop() {
        storage.onUnregister();
    }

    @Override
    public void beforeStop() {
        save(true);
    }

    @Override
    public void onUpdate(DataEntry<K, V> entry) {
        queue.add(new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()));
    }

    @Override
    public void onRemove(DataEntry<K, V> entry) {
        queue.add(new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), null));
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
}
