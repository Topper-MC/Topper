package me.hsgamer.topper.spigot.storage.simple.supplier;

import me.hsgamer.hscore.config.Config;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.simple.converter.ValueConverter;
import me.hsgamer.topper.storage.simple.supplier.DataStorageSupplier;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ConfigStorageSupplier implements DataStorageSupplier {
    private final UnaryOperator<String> configNameProvider;
    private final Function<File, Config> configProvider;
    private final File holderBaseFolder;

    public ConfigStorageSupplier(
            UnaryOperator<String> configNameProvider,
            Function<File, Config> configProvider,
            File holderBaseFolder
    ) {
        this.configNameProvider = configNameProvider;
        this.configProvider = configProvider;
        this.holderBaseFolder = holderBaseFolder;
    }

    @Override
    public <K, V> DataStorage<K, V> getStorage(String name, ValueConverter<K> keyConverter, ValueConverter<V> valueConverter) {
        return new DataStorage<K, V>() {
            private final Config config = configProvider.apply(new File(holderBaseFolder, configNameProvider.apply(name)));

            @Override
            public Map<K, V> load() {
                Map<String[], Object> values = config.getValues(false);
                Map<K, V> map = new HashMap<>();
                values.forEach((path, value) -> {
                    K finalKey = keyConverter.fromRawString(path[0]);
                    V finalValue = valueConverter.fromRawString(String.valueOf(value));
                    if (finalKey != null && finalValue != null) {
                        map.put(finalKey, finalValue);
                    }
                });
                return map;
            }

            @Override
            public Optional<V> load(K key) {
                return Optional.ofNullable(config.get(keyConverter.toRawString(key))).map(String::valueOf).map(valueConverter::fromRawString);
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
                        map.forEach((k, v) -> config.set(valueConverter.toRawString(v), keyConverter.toRawString(k)));
                        removeSet.forEach(key -> config.remove(keyConverter.toRawString(key)));
                        config.save();
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
                config.setup();
            }

            @Override
            public void onUnregister() {
                config.save();
            }
        };
    }
}
