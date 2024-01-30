package me.hsgamer.topper.placeholderleaderboard.manager;

import me.hsgamer.topper.core.holder.DataHolder;
import me.hsgamer.topper.core.storage.DataStorage;
import me.hsgamer.topper.placeholderleaderboard.TopperPlaceholderLeaderboard;
import me.hsgamer.topper.placeholderleaderboard.holder.NumberTopHolder;
import me.hsgamer.topper.placeholderleaderboard.holder.PlaceholderTopHolder;
import me.hsgamer.topper.spigot.number.NumberFormatter;

import java.util.*;
import java.util.function.Function;

public class TopManager {
    private final Map<String, NumberTopHolder> topHolders = new HashMap<>();
    private final Map<String, NumberFormatter> topFormatters = new HashMap<>();
    private final NumberFormatter defaultFormatter = new NumberFormatter();
    private final TopperPlaceholderLeaderboard instance;
    private Function<DataHolder<Double>, DataStorage<Double>> storageSupplier;

    public TopManager(TopperPlaceholderLeaderboard instance) {
        this.instance = instance;
    }

    public void register() {
        storageSupplier = instance.getNumberStorageBuilder().buildSupplier(instance.getMainConfig().getStorageType());
        instance.getMainConfig().getPlaceholders().forEach((key, value) -> addTopHolder(key, new PlaceholderTopHolder(instance, key, value)));
        topFormatters.putAll(instance.getMainConfig().getFormatters());
    }

    public void unregister() {
        topHolders.values().forEach(NumberTopHolder::unregister);
        topHolders.clear();
        topFormatters.clear();
    }

    public void addTopHolder(String key, NumberTopHolder topHolder) {
        topHolder.register();
        NumberTopHolder oldHolder = topHolders.put(key, topHolder);
        if (oldHolder != null) oldHolder.unregister();
    }

    public Function<DataHolder<Double>, DataStorage<Double>> getStorageSupplier() {
        return storageSupplier;
    }

    public Optional<NumberTopHolder> getTopHolder(String name) {
        return Optional.ofNullable(topHolders.get(name));
    }

    public List<String> getTopHolderNames() {
        return Collections.unmodifiableList(new ArrayList<>(topHolders.keySet()));
    }

    public NumberFormatter getTopFormatter(String name) {
        return topFormatters.getOrDefault(name, defaultFormatter);
    }

    public void create(UUID uuid) {
        topHolders.values().forEach(holder -> holder.getOrCreateEntry(uuid));
    }
}