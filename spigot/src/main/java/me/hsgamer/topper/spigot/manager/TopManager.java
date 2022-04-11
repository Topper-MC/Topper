package me.hsgamer.topper.spigot.manager;

import me.hsgamer.topper.core.TopFormatter;
import me.hsgamer.topper.core.TopHolder;
import me.hsgamer.topper.core.TopStorage;
import me.hsgamer.topper.spigot.TopperPlugin;
import me.hsgamer.topper.spigot.builder.TopStorageBuilder;
import me.hsgamer.topper.spigot.config.MainConfig;
import me.hsgamer.topper.spigot.holder.PlaceholderTopHolder;
import me.hsgamer.topper.spigot.storage.YamlStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class TopManager {
    private final Map<String, TopHolder> topHolders = new HashMap<>();
    private final Map<String, TopFormatter> topFormatters = new HashMap<>();
    private final TopFormatter defaultFormatter = new TopFormatter();
    private final TopperPlugin instance;

    public TopManager(TopperPlugin instance) {
        this.instance = instance;
    }

    public void register() {
        MainConfig.PLACEHOLDERS.getValue().forEach((key, value) -> {
            TopStorage storage = TopStorageBuilder.INSTANCE.build(MainConfig.STORAGE_TYPE.getValue(), instance).orElseGet(YamlStorage::new);
            addTopHolder(key, new PlaceholderTopHolder(instance, storage, key, value));
        });
        defaultFormatter.addReplacer("name", (uuid, bigDecimal) -> Optional.ofNullable(uuid).map(Bukkit::getOfflinePlayer).map(OfflinePlayer::getName).orElse(""));
        topFormatters.putAll(MainConfig.FORMATTERS.getValue());
        topFormatters.values().forEach(topFormatter -> topFormatter.addReplacer("name", (uuid, bigDecimal) -> Optional.ofNullable(uuid).map(Bukkit::getOfflinePlayer).map(OfflinePlayer::getName).orElse("")));
    }

    public void unregister() {
        topHolders.values().forEach(TopHolder::unregister);
        topHolders.clear();
        topFormatters.clear();
    }

    public void addTopHolder(String key, TopHolder topHolder) {
        if (topHolders.containsKey(key)) {
            topHolders.get(key).unregister();
        }
        topHolder.register();
        topHolders.put(key, topHolder);
    }

    public Optional<TopHolder> getTopHolder(String name) {
        return Optional.ofNullable(topHolders.get(name));
    }

    public List<String> getTopHolderNames() {
        return Collections.unmodifiableList(new ArrayList<>(topHolders.keySet()));
    }

    public TopFormatter getTopFormatter(String name) {
        return topFormatters.getOrDefault(name, defaultFormatter);
    }

    public void create(UUID uuid) {
        topHolders.values().forEach(holder -> holder.getOrCreateEntry(uuid));
    }
}
