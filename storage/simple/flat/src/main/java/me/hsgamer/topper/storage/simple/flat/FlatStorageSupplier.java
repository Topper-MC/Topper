package me.hsgamer.topper.storage.simple.flat;

import me.hsgamer.hscore.logger.common.LogLevel;
import me.hsgamer.hscore.logger.common.Logger;
import me.hsgamer.hscore.logger.provider.LoggerProvider;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.simple.converter.ValueConverter;
import me.hsgamer.topper.storage.simple.supplier.DataStorageSupplier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class FlatStorageSupplier implements DataStorageSupplier {
    private final Logger logger = LoggerProvider.getLogger(getClass());
    private final File baseFolder;

    public FlatStorageSupplier(File baseFolder) {
        this.baseFolder = baseFolder;
    }

    @Override
    public <K, V> DataStorage<K, V> getStorage(String name, ValueConverter<K> keyConverter, ValueConverter<V> valueConverter) {
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
                    properties.store(fileOutputStream, null);
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
                    K k = keyConverter.fromRawString(key.toString());
                    V v = valueConverter.fromRawString(value.toString());
                    if (k != null && v != null) {
                        map.put(k, v);
                    }
                });
                return map;
            }

            @Override
            public Optional<V> load(K key) {
                return Optional.ofNullable(properties.getProperty(keyConverter.toRawString(key))).map(valueConverter::fromRawString);
            }

            @Override
            public Optional<Modifier<K, V>> modify() {
                return Optional.of(new Modifier<K, V>() {
                    private final List<Runnable> runnables = new LinkedList<>();

                    @Override
                    public void save(Map<K, V> map) {
                        if (map.isEmpty()) {
                            return;
                        }
                        runnables.add(() -> map.forEach((k, v) -> properties.put(keyConverter.toRawString(k), valueConverter.toRawString(v))));
                    }

                    @Override
                    public void remove(Collection<K> keys) {
                        if (keys.isEmpty()) {
                            return;
                        }
                        runnables.add(() -> keys.forEach(key -> properties.remove(keyConverter.toRawString(key))));
                    }

                    @Override
                    public void commit() {
                        runnables.forEach(Runnable::run);
                        saveRunnable.run();
                    }

                    @Override
                    public void rollback() {
                        runnables.clear();
                    }
                });
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
