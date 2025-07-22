package me.hsgamer.topper.storage.flat.properties;

import me.hsgamer.hscore.logger.common.LogLevel;
import me.hsgamer.hscore.logger.common.Logger;
import me.hsgamer.hscore.logger.provider.LoggerProvider;
import me.hsgamer.topper.storage.flat.core.FlatDataStorage;
import me.hsgamer.topper.storage.flat.core.FlatValueConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class PropertiesDataStorage<K, V> extends FlatDataStorage<PropertiesDataStorage.PropertyFile, K, V> {
    private final Logger logger = LoggerProvider.getLogger(getClass());

    public PropertiesDataStorage(File baseFolder, String name, FlatValueConverter<K> keyConverter, FlatValueConverter<V> valueConverter) {
        super(baseFolder, name, keyConverter, valueConverter);
    }

    @Override
    protected PropertyFile setupFile(File baseFolder, String name) {
        File file = new File(baseFolder, name + ".properties");
        Properties properties = new Properties();
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
        return new PropertyFile(file, properties);
    }

    @Override
    protected Map<String, String> loadFromFile(PropertyFile file) {
        return file.properties.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().toString()
                ));
    }

    @Override
    protected Optional<String> loadFromFile(PropertyFile file, String key) {
        return Optional.ofNullable(file.properties.getProperty(key));
    }

    @Override
    protected void saveFile(PropertyFile file) {
        try {
            try (FileOutputStream fileOutputStream = new FileOutputStream(file.file)) {
                file.properties.store(fileOutputStream, null);
            }
        } catch (IOException e) {
            logger.log(LogLevel.ERROR, "Failed to save the data", e);
        }
    }

    @Override
    protected void setValue(PropertyFile file, String key, String value) {
        file.properties.setProperty(key, value);
    }

    @Override
    protected void removeValue(PropertyFile file, String key) {
        file.properties.remove(key);
    }

    public static class PropertyFile {
        private final File file;
        private final Properties properties;

        private PropertyFile(File file, Properties properties) {
            this.file = file;
            this.properties = properties;
        }
    }
}
