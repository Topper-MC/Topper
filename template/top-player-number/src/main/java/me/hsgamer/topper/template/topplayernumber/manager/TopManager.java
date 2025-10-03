package me.hsgamer.topper.template.topplayernumber.manager;

import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.flat.converter.NumberFlatValueConverter;
import me.hsgamer.topper.storage.flat.converter.UUIDFlatValueConverter;
import me.hsgamer.topper.storage.sql.converter.NumberSqlValueConverter;
import me.hsgamer.topper.storage.sql.converter.UUIDSqlValueConverter;
import me.hsgamer.topper.template.topplayernumber.TopPlayerNumberTemplate;
import me.hsgamer.topper.template.topplayernumber.holder.NumberTopHolder;
import me.hsgamer.topper.template.topplayernumber.storage.DataStorageSupplier;

import java.util.*;

public class TopManager {
    private final Map<String, NumberTopHolder> holders = new HashMap<>();
    private final TopPlayerNumberTemplate template;
    private DataStorageSupplier storageSupplier;

    public TopManager(TopPlayerNumberTemplate template) {
        this.template = template;
    }

    public void enable() {
        storageSupplier = template.getDataStorageSupplier(template.getSettings().storageType(), template.getSettings().storageSettings());
        template.getSettings().holders().forEach((key, value) -> {
            NumberTopHolder topHolder = new NumberTopHolder(template, key, value);
            topHolder.register();
            holders.put(key, topHolder);
        });
    }

    public void disable() {
        holders.values().forEach(NumberTopHolder::unregister);
        holders.clear();
    }

    public DataStorageSupplier getStorageSupplier() {
        return storageSupplier;
    }

    public DataStorage<UUID, Double> buildStorage(String name) {
        return storageSupplier.getStorage(
                name,
                new UUIDFlatValueConverter(),
                new NumberFlatValueConverter<>(Number::doubleValue),
                new UUIDSqlValueConverter("uuid"),
                new NumberSqlValueConverter<>("value", true, Number::doubleValue)
        );
    }

    public Optional<NumberTopHolder> getHolder(String name) {
        return Optional.ofNullable(holders.get(name));
    }

    public List<String> getHolderNames() {
        return Collections.unmodifiableList(new ArrayList<>(holders.keySet()));
    }

    public void create(UUID uuid) {
        holders.values().forEach(holder -> holder.getOrCreateEntry(uuid));
    }
}
