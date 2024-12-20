package me.hsgamer.topper.spigot.storage.simple.supplier;

import me.hsgamer.hscore.config.Config;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.simple.converter.FlatEntryConverter;
import me.hsgamer.topper.storage.simple.setting.DataStorageSetting;
import me.hsgamer.topper.storage.simple.supplier.DataStorageSupplier;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ConfigStorageSupplier implements DataStorageSupplier {
    private final Executor mainThreadExecutor;
    private final UnaryOperator<String> configNameProvider;
    private final Function<File, Config> configProvider;
    private final File holderBaseFolder;

    public ConfigStorageSupplier(
            Executor mainThreadExecutor,
            UnaryOperator<String> configNameProvider,
            Function<File, Config> configProvider,
            File holderBaseFolder
    ) {
        this.mainThreadExecutor = mainThreadExecutor;
        this.configNameProvider = configNameProvider;
        this.configProvider = configProvider;
        this.holderBaseFolder = holderBaseFolder;
    }

    @Override
    public <K, V> DataStorage<K, V> getStorage(String name, DataStorageSetting<K, V> setting) {
        FlatEntryConverter<K, V> converter = setting.getFlatEntryConverter();

        return new DataStorage<K, V>() {
            private final Config config = configProvider.apply(new File(holderBaseFolder, configNameProvider.apply(name)));

            @Override
            public Map<K, V> load() {
                Map<String[], Object> values = config.getValues(false);
                Map<K, V> map = new HashMap<>();
                values.forEach((path, value) -> {
                    V finalValue = converter.toValue(String.valueOf(value));
                    if (finalValue != null) {
                        K finalKey = converter.toKey(path[0]);
                        map.put(finalKey, finalValue);
                    }
                });
                return map;
            }

            @Override
            public CompletableFuture<Void> save(Map<K, V> map, boolean urgent) {
                return CompletableFuture.supplyAsync(
                        () -> {
                            map.forEach((key, value) -> config.set(converter.toRawValue(value), converter.toRawKey(key)));
                            config.save();
                            return null;
                        },
                        urgent ? Runnable::run : mainThreadExecutor
                );
            }

            @Override
            public CompletableFuture<Optional<V>> load(K key, boolean urgent) {
                return CompletableFuture.supplyAsync(
                        () -> Optional.ofNullable(config.get(converter.toRawKey(key))).map(String::valueOf).map(converter::toValue),
                        urgent ? Runnable::run : mainThreadExecutor
                );
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
