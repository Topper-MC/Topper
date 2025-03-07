package me.hsgamer.topper.spigot.query.forward.placeholderapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.hsgamer.topper.query.forward.QueryForwardContext;
import me.hsgamer.topper.spigot.query.forward.plugin.PluginContext;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlaceholderQueryForwarder<C extends QueryForwardContext<OfflinePlayer> & PluginContext> implements Consumer<C> {
    private final List<PlaceholderExpansion> expansions = new ArrayList<>();

    @Override
    public void accept(C context) {
        PlaceholderExpansion expansion = new PlaceholderExpansion() {
            @Override
            public @NotNull String getIdentifier() {
                return context.getName();
            }

            @Override
            public @NotNull String getAuthor() {
                return String.join(", ", context.getPlugin().getDescription().getAuthors());
            }

            @Override
            public @NotNull String getVersion() {
                return context.getPlugin().getDescription().getVersion();
            }

            @Override
            public boolean persist() {
                return true;
            }

            @Override
            public String onRequest(OfflinePlayer player, @NotNull String params) {
                return context.getQuery().apply(player, params).result;
            }
        };
        expansion.register();
        expansions.add(expansion);
    }

    public void unregister() {
        expansions.forEach(PlaceholderExpansion::unregister);
        expansions.clear();
    }
}
