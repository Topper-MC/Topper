package me.hsgamer.topper.template.topplayernumber.manager;

import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.template.topplayernumber.TopPlayerNumberTemplate;
import me.hsgamer.topper.template.topplayernumber.holder.NumberTopHolder;

import java.util.*;
import java.util.function.Function;

public class TopManager {
    private final Map<String, NumberTopHolder> holders = new HashMap<>();
    private final TopPlayerNumberTemplate template;
    private Function<String, DataStorage<UUID, Double>> storageSupplier;

    public TopManager(TopPlayerNumberTemplate template) {
        this.template = template;
    }

    public void enable() {
        storageSupplier = template.getStorageSupplier();
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

    public Optional<NumberTopHolder> getHolder(String name) {
        return Optional.ofNullable(holders.get(name));
    }

    public List<String> getHolderNames() {
        return Collections.unmodifiableList(new ArrayList<>(holders.keySet()));
    }

    public void create(UUID uuid) {
        holders.values().forEach(holder -> holder.getOrCreateEntry(uuid));
    }

    public DataStorage<UUID, Double> buildStorage(String name) {
        return storageSupplier.apply(name);
    }
}
