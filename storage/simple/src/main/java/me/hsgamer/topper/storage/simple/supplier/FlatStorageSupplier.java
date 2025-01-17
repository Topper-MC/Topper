package me.hsgamer.topper.storage.simple.supplier;

import me.hsgamer.hscore.logger.common.LogLevel;
import me.hsgamer.hscore.logger.common.Logger;
import me.hsgamer.hscore.logger.provider.LoggerProvider;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.simple.converter.ValueConverter;

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
                    K k = keyConverter.parseString(key.toString());
                    V v = valueConverter.parseString(value.toString());
                    if (k != null && v != null) {
                        map.put(k, v);
                    }
                });
                return map;
            }

            @Override
            public Optional<V> load(K key) {
                return Optional.ofNullable(properties.getProperty(keyConverter.parseString(key))).map(valueConverter::parseString);
            }

            @Override
            public Optional<Modifier<K, V>> modify() {
                return Optional.of(new Modifier<K, V>() {
                    private final Map<K, V> map = new HashMap<>();
                    private final Set<K> removeSet = new HashSet<>();

                    @Override
                    public void save(Map<K, V> map) {
                        this.map.putAll(map);
                        this.removeSet.removeIf(this.map::containsKey);
                    }

                    @Override
                    public void remove(Collection<K> keys) {
                        this.removeSet.addAll(keys);
                        this.removeSet.forEach(map::remove);
                    }

                    @Override
                    public void commit() {
                        map.forEach((k, v) -> properties.put(keyConverter.parseString(k), valueConverter.parseString(v)));
                        removeSet.forEach(key -> properties.remove(keyConverter.parseString(key)));
                        saveRunnable.run();
                    }

                    @Override
                    public void rollback() {
                        map.clear();
                        removeSet.clear();
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
