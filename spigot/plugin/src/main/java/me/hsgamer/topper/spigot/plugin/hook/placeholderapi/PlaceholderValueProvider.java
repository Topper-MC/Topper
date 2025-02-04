package me.hsgamer.topper.spigot.plugin.hook.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.holder.provider.NumberStringValueProvider;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class PlaceholderValueProvider extends NumberStringValueProvider {
    private final String placeholder;
    private final boolean isOnlineOnly;

    PlaceholderValueProvider(TopperPlugin plugin, Map<String, Object> map) {
        super(plugin, map);
        placeholder = Optional.ofNullable(map.get("placeholder")).map(Object::toString).orElse("");
        isOnlineOnly = Optional.ofNullable(map.get("online"))
                .map(Object::toString)
                .map(String::toLowerCase)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @Override
    protected String getDisplayName() {
        return placeholder;
    }

    @Override
    protected ValueState getString(UUID uuid) {
        OfflinePlayer player;
        if (isOnlineOnly) {
            player = plugin.getServer().getPlayer(uuid);
            if (player == null) {
                return ValueState.unhandled();
            }
        } else {
            player = plugin.getServer().getOfflinePlayer(uuid);
        }

        String parsed = PlaceholderAPI.setPlaceholders(player, placeholder).trim();
        return ValueState.handled(parsed.isEmpty() ? null : parsed);
    }
}
