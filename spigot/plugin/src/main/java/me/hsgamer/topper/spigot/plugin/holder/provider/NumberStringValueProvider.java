package me.hsgamer.topper.spigot.plugin.holder.provider;

import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import io.github.projectunified.minelib.scheduler.global.GlobalScheduler;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public abstract class NumberStringValueProvider implements ValueProvider {
    protected final TopperPlugin plugin;
    private final boolean isAsync;
    private final boolean showErrors;

    public NumberStringValueProvider(TopperPlugin plugin, Map<String, Object> map) {
        this.plugin = plugin;
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

    protected abstract String getDisplayName();

    protected abstract Optional<String> getString(UUID uuid);

    @Override
    public CompletableFuture<Optional<Double>> getValue(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<String> value = getString(uuid);
                if (!value.isPresent()) {
                    if (showErrors) {
                        plugin.getLogger().warning("The value of " + getDisplayName() + " is empty");
                    }
                    return Optional.empty();
                }
                return Optional.of(Double.parseDouble(value.get()));
            } catch (Exception e) {
                if (showErrors) {
                    plugin.getLogger().log(Level.WARNING, "There is an error while parsing the value of " + getDisplayName(), e);
                }
                return Optional.empty();
            }
        }, (isAsync ? AsyncScheduler.get(plugin) : GlobalScheduler.get(plugin)).getExecutor());
    }
}
