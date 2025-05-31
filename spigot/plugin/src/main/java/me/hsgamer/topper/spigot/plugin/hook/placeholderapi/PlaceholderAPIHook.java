package me.hsgamer.topper.spigot.plugin.hook.placeholderapi;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.builder.ValueProviderBuilder;
import me.hsgamer.topper.spigot.plugin.manager.QueryForwardManager;
import me.hsgamer.topper.spigot.plugin.util.ParseUtil;
import me.hsgamer.topper.spigot.query.forward.placeholderapi.PlaceholderQueryForwarder;
import me.hsgamer.topper.spigot.value.placeholderapi.PlaceholderValueProvider;
import me.hsgamer.topper.value.string.StringDeformatters;
import org.bukkit.Bukkit;

import java.util.Optional;

public class PlaceholderAPIHook implements Loadable {
    private final TopperPlugin plugin;
    private final PlaceholderQueryForwarder<QueryForwardManager.ForwardContext> queryForwarder = new PlaceholderQueryForwarder<>();

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
                    .thenApply(StringDeformatters.deformatterOrIdentity(map))
                    .thenApply(ParseUtil::parsePlaceholderNumber)
                    .keyMapper(Bukkit::getOfflinePlayer);
        }, "placeholderapi", "placeholder", "papi");
        plugin.get(QueryForwardManager.class).addForwarder(queryForwarder);
    }

    @Override
    public void disable() {
        queryForwarder.unregister();
    }
}
