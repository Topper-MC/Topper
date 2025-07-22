package me.hsgamer.topper.storage.flat.core;

import me.hsgamer.topper.storage.core.DataStorage;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public abstract class FlatDataStorage<F, K, V> implements DataStorage<K, V> {
    private final File baseFolder;
    private final String name;
    private final FlatValueConverter<K> keyConverter;
    private final FlatValueConverter<V> valueConverter;
    private F file;

    public FlatDataStorage(File baseFolder, String name, FlatValueConverter<K> keyConverter, FlatValueConverter<V> valueConverter) {
        this.baseFolder = baseFolder;
        this.name = name;
        this.keyConverter = keyConverter;
        this.valueConverter = valueConverter;
    }

    protected abstract F setupFile(File baseFolder, String name);

    protected abstract Map<String, String> loadFromFile(F file);

    protected abstract Optional<String> loadFromFile(F file, String key);

    protected abstract void saveFile(F file);

    protected abstract void setValue(F file, String key, String value);

    protected abstract void removeValue(F file, String key);

    private void checkFile() {
        if (file == null) {
            throw new IllegalStateException("Data storage is not registered");
        }
    }

    @Override
    public final void onRegister() {
        file = setupFile(baseFolder, name);
        if (file == null) {
            throw new IllegalStateException("Failed to get the file for " + name);
        }
    }

    @Override
    public final void onUnregister() {
        if (file != null) {
            saveFile(file);
            file = null;
        }
    }

    @Override
    public final Map<K, V> load() {
        checkFile();
        Map<String, String> rawData = loadFromFile(file);
        return rawData.entrySet()
                .stream()
                .map(entry -> {
                    K key = keyConverter.fromString(entry.getKey());
                    V value = valueConverter.fromString(entry.getValue());
                    return new AbstractMap.SimpleImmutableEntry<>(key, value);
                })
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Optional<V> load(K key) {
        checkFile();
        return loadFromFile(file, keyConverter.toString(key)).map(valueConverter::fromString);
    }

    @Override
    public Optional<Modifier<K, V>> modify() {
        checkFile();
        return Optional.of(new Modifier<K, V>() {
            private final List<Runnable> runnableList = new LinkedList<>();

            @Override
            public void save(Map<K, V> map) {
                if (map.isEmpty()) return;
                runnableList.add(() -> map.forEach((k, v) -> {
                    String key = keyConverter.toString(k);
                    String value = valueConverter.toString(v);
                    setValue(file, key, value);
                }));
            }

            @Override
            public void remove(Collection<K> keys) {
                if (keys.isEmpty()) {
                    return;
                }
                runnableList.add(() -> keys.forEach(key -> {
                    String keyString = keyConverter.toString(key);
                    removeValue(file, keyString);
                }));
            }

            @Override
            public void commit() {
                runnableList.forEach(Runnable::run);
                runnableList.clear();
                saveFile(file);
            }

            @Override
            public void rollback() {
                runnableList.clear();
            }
        });
    }
}
