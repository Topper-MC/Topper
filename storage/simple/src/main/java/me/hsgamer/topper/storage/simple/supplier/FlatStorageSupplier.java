package me.hsgamer.topper.storage.simple.supplier;

import me.hsgamer.hscore.logger.common.LogLevel;
import me.hsgamer.hscore.logger.common.Logger;
import me.hsgamer.hscore.logger.provider.LoggerProvider;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.simple.converter.FlatEntryConverter;
import me.hsgamer.topper.storage.simple.setting.DataStorageSetting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FlatStorageSupplier implements DataStorageSupplier {
    private final Logger logger = LoggerProvider.getLogger(getClass());
    private final File baseFolder;

    public FlatStorageSupplier(File baseFolder) {
        this.baseFolder = baseFolder;
    }

    @Override
    public <K, V> DataStorage<K, V> getStorage(String name, DataStorageSetting<K, V> setting) {
        FlatEntryConverter<K, V> converter = setting.getFlatEntryConverter();
        Properties properties = new Properties();
        File file = new File(baseFolder, name + ".properties");
        Runnable loadRunnable = () -> {
            try {
                if (!file.exists()) {
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    file.createNewFile();
                }
                try (FileInputStream fileOutputStream = new FileInputStream(file)) {
                    properties.load(fileOutputStream);
                }
            } catch (IOException e) {
                logger.log(LogLevel.ERROR, "Failed to load the data", e);
            }
        };
        Runnable saveRunnable = () -> {
            try {
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    properties.store(fileOutputStream, "Data for " + name);
                }
            } catch (IOException e) {
                logger.log(LogLevel.ERROR, "Failed to save the data", e);
            }
        };

        return new DataStorage<K, V>() {
            @Override
            public Map<K, V> load() {
                Map<K, V> map = new HashMap<>();
                properties.forEach((key, value) -> {
                    K k = converter.toKey(key.toString());
                    V v = converter.toValue(value.toString());
                    if (k != null && v != null) {
                        map.put(k, v);
                    }
                });
                return map;
            }

            @Override
            public CompletableFuture<Void> save(Map<K, V> map, boolean urgent) {
                Runnable runnable = () -> {
                    map.forEach((k, v) -> properties.put(converter.toRawKey(k), converter.toRawValue(v)));
                    saveRunnable.run();
                };
                if (urgent) {
                    runnable.run();
                    return CompletableFuture.completedFuture(null);
                } else {
                    return CompletableFuture.runAsync(runnable);
                }
            }

            @Override
            public CompletableFuture<Optional<V>> load(K key, boolean urgent) {
                Supplier<Optional<V>> runnable = () -> Optional.ofNullable(properties.getProperty(converter.toRawKey(key))).map(converter::toValue);
                if (urgent) {
                    return CompletableFuture.completedFuture(runnable.get());
                } else {
                    return CompletableFuture.supplyAsync(runnable);
                }
            }

            @Override
            public CompletableFuture<Void> remove(Collection<K> keys, boolean urgent) {
                Runnable runnable = () -> {
                    keys.forEach(key -> properties.remove(converter.toRawKey(key)));
                    saveRunnable.run();
                };
                if (urgent) {
                    runnable.run();
                    return CompletableFuture.completedFuture(null);
                } else {
                    return CompletableFuture.runAsync(runnable);
                }
            }

            @Override
            public void onRegister() {
                loadRunnable.run();
            }

            @Override
            public void onUnregister() {
                saveRunnable.run();
            }
        };
    }
}
