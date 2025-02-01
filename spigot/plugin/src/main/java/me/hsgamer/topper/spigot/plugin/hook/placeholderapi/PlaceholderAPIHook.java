package me.hsgamer.topper.spigot.plugin.hook.placeholderapi;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.builder.ValueProviderBuilder;
import me.hsgamer.topper.spigot.plugin.manager.QueryForwardManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderAPIHook implements Loadable {
    private final TopperPlugin plugin;
    private final List<PlaceholderExpansion> expansions = new ArrayList<>();

    public PlaceholderAPIHook(TopperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        plugin.get(ValueProviderBuilder.class).register(map -> new PlaceholderValueProvider(plugin, map), "placeholderapi", "placeholder", "papi");
        plugin.get(QueryForwardManager.class).addQueryForwarder(context -> {
            PlaceholderExpansion expansion = new PlaceholderExpansion() {
                @Override
                public @NotNull String getIdentifier() {
                    return context.name;
                }

                @Override
                public @NotNull String getAuthor() {
                    return String.join(", ", context.plugin.getDescription().getAuthors());
                }

                @Override
                public @NotNull String getVersion() {
                    return plugin.getDescription().getVersion();
                }

                @Override
                public boolean persist() {
                    return true;
                }

                @Override
                public String onRequest(OfflinePlayer player, @NotNull String params) {
                    return context.queryManager.get(player, params);
                }
            };
            expansion.register();
            expansions.add(expansion);
        });
    }

    @Override
    public void disable() {
        for (PlaceholderExpansion expansion : expansions) {
            expansion.unregister();
        }
    }
}
