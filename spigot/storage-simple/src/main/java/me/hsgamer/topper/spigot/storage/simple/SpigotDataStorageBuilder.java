package me.hsgamer.topper.spigot.storage.simple;

import io.github.projectunified.minelib.scheduler.global.GlobalScheduler;
import me.hsgamer.hscore.bukkit.config.BukkitConfig;
import me.hsgamer.topper.spigot.storage.simple.supplier.ConfigStorageSupplier;
import me.hsgamer.topper.storage.simple.builder.DataStorageBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executor;

public class SpigotDataStorageBuilder {
    public static void register(JavaPlugin plugin, DataStorageBuilder builder) {
        Executor mainThreadExecutor = runnable -> GlobalScheduler.get(plugin).run(runnable);

        builder.register(setting -> new ConfigStorageSupplier(mainThreadExecutor, name -> name + ".yml", BukkitConfig::new, setting.getBaseFolder()), "config", "yaml", "yml");
        builder.register(setting -> new ConfigStorageSupplier(mainThreadExecutor, name -> name + ".json", BukkitConfig::new, setting.getBaseFolder()), "json");
    }
}
