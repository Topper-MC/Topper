package me.hsgamer.topper.spigot.storage.simple;

import me.hsgamer.hscore.bukkit.config.BukkitConfig;
import me.hsgamer.topper.spigot.storage.simple.supplier.ConfigStorageSupplier;
import me.hsgamer.topper.storage.simple.builder.DataStorageBuilder;

public class SpigotDataStorageBuilder {
    public static void register(DataStorageBuilder builder) {
        builder.register(setting -> new ConfigStorageSupplier(name -> name + ".yml", BukkitConfig::new, setting.getBaseFolder()), "config", "yaml", "yml");
        builder.register(setting -> new ConfigStorageSupplier(name -> name + ".json", BukkitConfig::new, setting.getBaseFolder()), "json");
    }
}
