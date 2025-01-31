package me.hsgamer.topper.spigot.plugin.hook.papi;

import me.clip.placeholderapi.PlaceholderAPI;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.holder.provider.NumberStringValueProvider;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlaceholderValueProvider extends NumberStringValueProvider {
    private final String placeholder;
    private final boolean isOnlineOnly;

    public PlaceholderValueProvider(TopperPlugin plugin, Map<String, Object> map) {
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
    protected Optional<String> getString(UUID uuid) {
        OfflinePlayer player;
        if (isOnlineOnly) {
            player = plugin.getServer().getPlayer(uuid);
            if (player == null) {
                return Optional.empty();
            }
        } else {
            player = plugin.getServer().getOfflinePlayer(uuid);
        }

        String parsed = PlaceholderAPI.setPlaceholders(player, placeholder).trim();
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(parsed);
    }
}
