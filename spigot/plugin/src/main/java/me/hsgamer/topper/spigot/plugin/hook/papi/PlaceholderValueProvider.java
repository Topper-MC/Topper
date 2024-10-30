package me.hsgamer.topper.spigot.plugin.hook.papi;

import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import io.github.projectunified.minelib.scheduler.global.GlobalScheduler;
import me.clip.placeholderapi.PlaceholderAPI;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.holder.provider.ValueProvider;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlaceholderValueProvider implements ValueProvider {
    private final TopperPlugin plugin;
    private final String placeholder;
    private final boolean isOnlineOnly;
    private final boolean isAsync;
    private final boolean showErrors;

    public PlaceholderValueProvider(TopperPlugin plugin, Map<String, Object> map) {
        this.plugin = plugin;
        placeholder = Optional.ofNullable(map.get("placeholder")).map(Object::toString).orElse("");
        isOnlineOnly = Optional.ofNullable(map.get("online"))
                .map(Object::toString)
                .map(String::toLowerCase)
                .map(Boolean::parseBoolean)
                .orElse(false);
        isAsync = Optional.ofNullable(map.get("async"))
                .map(Object::toString)
                .map(String::toLowerCase)
                .map(Boolean::parseBoolean)
                .orElse(false);
        showErrors = Optional.ofNullable(map.get("show-errors"))
                .map(Object::toString)
                .map(String::toLowerCase)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @Override
    public CompletableFuture<Optional<Double>> getValue(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player;
            if (isOnlineOnly) {
                player = plugin.getServer().getPlayer(uuid);
                if (player == null) {
                    return Optional.empty();
                }
            } else {
                player = plugin.getServer().getOfflinePlayer(uuid);
            }

            try {
                String parsed = PlaceholderAPI.setPlaceholders(player, placeholder).trim();
                if (parsed.isEmpty()) {
                    if (showErrors) {
                        plugin.getLogger().warning("The placeholder " + placeholder + " returns empty");
                    }
                    return Optional.empty();
                }
                return Optional.of(Double.parseDouble(parsed));
            } catch (Exception e) {
                if (showErrors) {
                    plugin.getLogger().log(Level.WARNING, "There is an error while parsing the placeholder: " + placeholder, e);
                }
                return Optional.empty();
            }
        }, (isAsync ? AsyncScheduler.get(plugin) : GlobalScheduler.get(plugin)).getExecutor());
    }
}
