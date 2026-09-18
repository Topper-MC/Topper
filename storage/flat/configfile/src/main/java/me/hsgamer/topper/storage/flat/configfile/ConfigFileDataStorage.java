package me.hsgamer.topper.storage.flat.configfile;

import io.github.projectunified.craftconfig.common.Config;
import me.hsgamer.topper.storage.flat.core.FlatDataStorage;
import me.hsgamer.topper.storage.flat.core.FlatValueConverter;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class ConfigFileDataStorage<K, V> extends FlatDataStorage<Config, K, V> {
    public ConfigFileDataStorage(File baseFolder, String name, FlatValueConverter<K> keyConverter, FlatValueConverter<V> valueConverter) {
        super(baseFolder, name, keyConverter, valueConverter);
    }

    protected abstract Config getConfig(File file);

    protected abstract String getConfigName(String name);

    @Override
    protected Config setupFile(File baseFolder, String name) {
        File file = new File(baseFolder, getConfigName(name));
        Config config = getConfig(file);
        config.setup();
        return config;
    }

    @Override
    protected Map<String, String> loadFromFile(Config file) {
        return file.getChildren()
                .entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().get(String.class)
                        )
                );
    }

    @Override
    protected Optional<String> loadFromFile(Config file, String key) {
        return Optional.ofNullable(file.get(key)).map(Object::toString);
    }

    @Override
    protected void saveFile(Config file) {
        file.save();
    }

    @Override
    protected void setValue(Config file, String key, String value) {
        file.node(key).set(value);
    }

    @Override
    protected void removeValue(Config file, String key) {
        file.node(key).remove();
    }
}
