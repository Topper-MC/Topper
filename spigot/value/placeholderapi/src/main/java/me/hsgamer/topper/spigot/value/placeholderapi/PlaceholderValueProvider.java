package me.hsgamer.topper.spigot.value.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import me.hsgamer.topper.value.core.ValueProvider;
import me.hsgamer.topper.value.core.ValueWrapper;
import org.bukkit.OfflinePlayer;

public class PlaceholderValueProvider implements ValueProvider<OfflinePlayer, String> {
    private final String placeholder;
    private final boolean isOnlineOnly;

    public PlaceholderValueProvider(String placeholder, boolean isOnlineOnly) {
        this.placeholder = placeholder;
        this.isOnlineOnly = isOnlineOnly;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    public ValueWrapper<String> apply(OfflinePlayer player) {
        if (isOnlineOnly && !player.isOnline()) {
            return ValueWrapper.notHandled();
        }

        try {
            return ValueWrapper.handled(PlaceholderAPI.setPlaceholders(player, placeholder));
        } catch (Exception e) {
            return ValueWrapper.error("Error while parsing the placeholder: " + placeholder, e);
        }
    }
}
