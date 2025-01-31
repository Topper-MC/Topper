package me.hsgamer.topper.spigot.plugin.manager;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.hscore.bukkit.config.BukkitConfig;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.config.MainConfig;
import me.hsgamer.topper.spigot.plugin.holder.NumberTopHolder;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.simple.builder.DataStorageBuilder;
import me.hsgamer.topper.storage.simple.config.DatabaseConfig;
import me.hsgamer.topper.storage.simple.converter.NumberConverter;
import me.hsgamer.topper.storage.simple.converter.UUIDConverter;
import me.hsgamer.topper.storage.simple.setting.DataStorageSetting;
import me.hsgamer.topper.storage.simple.setting.DatabaseSetting;
import me.hsgamer.topper.storage.simple.supplier.DataStorageSupplier;

import java.io.File;
import java.util.*;

public class TopManager implements Loadable {
    private final Map<String, NumberTopHolder> topHolders = new HashMap<>();
    private final TopperPlugin instance;
    private DataStorageSupplier storageSupplier;

    public TopManager(TopperPlugin instance) {
        this.instance = instance;
    }

    @Override
    public void enable() {
        storageSupplier = instance.get(DataStorageBuilder.class).buildSupplier(
                instance.get(MainConfig.class).getStorageType(),
                new DataStorageSetting() {
                    @Override
                    public DatabaseSetting getDatabaseSetting() {
                        return new DatabaseConfig("topper", new BukkitConfig(instance, "database.yml"));
                    }

                    @Override
                    public File getBaseFolder() {
                        return new File(instance.getDataFolder(), "top");
                    }
                }
        );
        storageSupplier.enable();
        instance.get(MainConfig.class).getHolders().forEach((key, value) -> {
            NumberTopHolder topHolder = new NumberTopHolder(instance, key, value);
            topHolder.register();
            topHolders.put(key, topHolder);
        });
    }

    @Override
    public void disable() {
        topHolders.values().forEach(NumberTopHolder::unregister);
        topHolders.clear();
        storageSupplier.disable();
    }

    public DataStorageSupplier getStorageSupplier() {
        return storageSupplier;
    }

    public DataStorage<UUID, Double> buildStorage(String name) {
        return storageSupplier.getStorage(
                name,
                new UUIDConverter("uuid"),
                new NumberConverter<>("value", true, Number::doubleValue)
        );
    }

    public Optional<NumberTopHolder> getTopHolder(String name) {
        return Optional.ofNullable(topHolders.get(name));
    }

    public List<String> getTopHolderNames() {
        return Collections.unmodifiableList(new ArrayList<>(topHolders.keySet()));
    }

    public void create(UUID uuid) {
        topHolders.values().forEach(holder -> holder.getOrCreateEntry(uuid));
    }
}
