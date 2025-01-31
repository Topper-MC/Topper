package me.hsgamer.topper.spigot.plugin.hook.placeholderapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.manager.TopQueryManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

class TopPlaceholderExpansion extends PlaceholderExpansion {
    private final TopperPlugin instance;

    TopPlaceholderExpansion(TopperPlugin instance) {
        this.instance = instance;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "topper";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", instance.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return instance.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        return instance.get(TopQueryManager.class).get(player, params);
    }
}
