package me.hsgamer.topper.spigot.plugin.hook.placeholderapi;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.builder.ValueProviderBuilder;
import me.hsgamer.topper.spigot.plugin.manager.QueryForwardManager;
import me.hsgamer.topper.spigot.value.placeholderapi.PlaceholderValueProvider;
import me.hsgamer.topper.value.string.NumberStringDeformatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlaceholderAPIHook implements Loadable {
    private final TopperPlugin plugin;
    private final List<PlaceholderExpansion> expansions = new ArrayList<>();

    public PlaceholderAPIHook(TopperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        plugin.get(ValueProviderBuilder.class).register(map -> {
            String placeholder = Optional.ofNullable(map.get("placeholder")).map(Object::toString).orElse("");
            boolean isOnlineOnly = Optional.ofNullable(map.get("online"))
                    .map(Object::toString)
                    .map(String::toLowerCase)
                    .map(Boolean::parseBoolean)
                    .orElse(false);
            return new PlaceholderValueProvider(placeholder, isOnlineOnly)
                    .thenApply(NumberStringDeformatter.deformatterOrIdentity(map))
                    .thenApply(Double::parseDouble)
                    .keyMapper(Bukkit::getOfflinePlayer);
        }, "placeholderapi", "placeholder", "papi");
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
