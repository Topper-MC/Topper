package me.hsgamer.topper.spigot.plugin.config;

import me.hsgamer.hscore.config.Config;
import me.hsgamer.hscore.config.annotation.Comment;
import me.hsgamer.hscore.config.annotation.ConfigPath;
import me.hsgamer.topper.spigot.plugin.config.converter.StringStringObjectMapConverter;

import java.util.Collections;
import java.util.Map;

public interface MainConfig {
    @ConfigPath(value = "holders", converter = StringStringObjectMapConverter.class, priority = 1)
    @Comment({
            "The settings for the Top Holders",
            "Check the wiki for more information on how to setup Top Holder using Value Provider",
            "https://topper-mc.github.io/Wiki/spigot/provider.html"
    })
    default Map<String, Map<String, Object>> getHolders() {
        return Collections.emptyMap();
    }

    @ConfigPath(value = "load-all-offline-players", priority = 3)
    @Comment("Should the plugin load all offline players when the server starts")
    default boolean isLoadAllOfflinePlayers() {
        return false;
    }

    @ConfigPath(value = {"task", "save", "entry-per-tick"}, priority = 4)
    @Comment("How many entries should be saved per tick")
    default int getTaskSaveEntryPerTick() {
        return 10;
    }

    @ConfigPath(value = {"task", "save", "delay"}, priority = 4)
    @Comment("How many ticks should the plugin wait before saving the leaderboard")
    default int getTaskSaveDelay() {
        return 0;
    }

    @ConfigPath(value = {"task", "update", "entry-per-tick"}, priority = 5)
    @Comment("How many entries should be updated per tick")
    default int getTaskUpdateEntryPerTick() {
        return 10;
    }

    @ConfigPath(value = {"task", "update", "delay"}, priority = 5)
    @Comment("How many ticks should the plugin wait before updating the leaderboard")
    default int getTaskUpdateDelay() {
        return 0;
    }

    @ConfigPath(value = "storage-type")
    @Comment({
            "The type of storage the plugin will use to store the value",
            "Available: FLAT, YAML, JSON, SQLITE, MYSQL"
    })
    default String getStorageType() {
        return "flat";
    }

    void reloadConfig();

    Config getConfig();
}
