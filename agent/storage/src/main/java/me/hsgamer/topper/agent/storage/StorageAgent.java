package me.hsgamer.topper.agent.storage;

import me.hsgamer.hscore.logger.common.LogLevel;
import me.hsgamer.hscore.logger.common.Logger;
import me.hsgamer.hscore.logger.provider.LoggerProvider;
import me.hsgamer.topper.agent.core.Agent;
import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.core.DataHolder;
import me.hsgamer.topper.storage.core.DataStorage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class StorageAgent<K, V> implements Agent, DataEntryAgent<K, V>, Runnable {
    private static final Logger LOGGER = LoggerProvider.getLogger(StorageAgent.class);

    private final DataHolder<K, V> holder;
    private final DataStorage<K, V> storage;
    private final Queue<Map.Entry<K, V>> queue = new ConcurrentLinkedQueue<>(); // Value can be null representing removal
    private final AtomicReference<Map<K, V>> storeMap = new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicReference<Map<K, V>> savingMap = new AtomicReference<>();
    private final AtomicBoolean saving = new AtomicBoolean(false);
    private int maxEntryPerCall = 10;

    public StorageAgent(DataHolder<K, V> holder, DataStorage<K, V> storage) {
        this.holder = holder;
        this.storage = storage;
    }

    private void save(boolean urgent) {
        if (saving.get() && !urgent) return;
        saving.set(true);

        storeMap.getAndSet(new ConcurrentHashMap<>())
                .forEach((key, value) -> queue.add(new AbstractMap.SimpleEntry<>(key, value)));

        Map<K, V> map = savingMap.updateAndGet(old -> old == null ? new HashMap<>() : old);

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

        Set<K> removeKeys = new HashSet<>();
        Map<K, V> finalMap = map.entrySet()
                .stream()
                .filter(entry -> {
                    if (entry.getValue() == null) {
                        removeKeys.add(entry.getKey());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


        Optional<DataStorage.Modifier<K, V>> optionalModifier = storage.modify();
        if (!optionalModifier.isPresent()) {
            saving.set(false);
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
        } catch (Throwable t) {
            LOGGER.log(LogLevel.ERROR, "Failed to save entries for " + holder.getName(), t);
            modifier.rollback();
        } finally {
            saving.set(false);
        }
    }

    @Override
    public void start() {
        storage.onRegister();
        try {
            storage.load().forEach((uuid, value) -> holder.getOrCreateEntry(uuid).setValue(value, false));
        } catch (Exception e) {
            LOGGER.log(LogLevel.ERROR, "Failed to load top entries for " + holder.getName(), e);
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
    public void onUpdate(DataEntry<K, V> entry, V oldValue, V newValue) {
        storeMap.get().put(entry.getKey(), newValue);
    }

    @Override
    public void onRemove(DataEntry<K, V> entry) {
        storeMap.get().put(entry.getKey(), null);
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
