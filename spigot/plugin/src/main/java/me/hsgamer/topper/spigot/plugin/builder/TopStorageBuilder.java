package me.hsgamer.topper.spigot.plugin.builder;

import me.hsgamer.topper.agent.storage.simple.builder.DataStorageBuilder;
import me.hsgamer.topper.spigot.agent.storage.simple.SpigotDataStorageBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class TopStorageBuilder extends DataStorageBuilder<UUID, Double> {
    public TopStorageBuilder(JavaPlugin plugin) {
        SpigotDataStorageBuilder.register(plugin, this);
    }
}
