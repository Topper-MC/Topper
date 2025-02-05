package me.hsgamer.topper.spigot.value.player;

import me.hsgamer.topper.value.core.ValueProvider;
import me.hsgamer.topper.value.core.ValueWrapper;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface PlayerValueProvider {
    static <V> ValueProvider<UUID, V> uuidToPlayer(ValueProvider<Player, V> valueProvider) {
        return uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return ValueWrapper.notHandled();
            }
            return valueProvider.apply(player);
        };
    }

    static <V> ValueProvider<UUID, V> uuidToOfflinePlayer(ValueProvider<OfflinePlayer, V> valueProvider) {
        return uuid -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (player == null) {
                return ValueWrapper.notHandled();
            }
            return valueProvider.apply(player);
        };
    }
}
