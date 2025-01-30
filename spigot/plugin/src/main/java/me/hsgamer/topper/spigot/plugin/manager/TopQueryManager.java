package me.hsgamer.topper.spigot.plugin.manager;

import me.hsgamer.topper.agent.snapshot.SnapshotAgent;
import me.hsgamer.topper.query.snapshot.SnapshotQueryManager;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.holder.NumberTopHolder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class TopQueryManager extends SnapshotQueryManager<UUID, Double, NumberTopHolder, OfflinePlayer> {
    private final TopperPlugin instance;

    public TopQueryManager(TopperPlugin instance) {
        this.instance = instance;
    }

    @Override
    protected Optional<NumberTopHolder> getHolder(String name) {
        return instance.get(TopManager.class).getTopHolder(name);
    }

    @Override
    protected @NotNull SnapshotAgent<UUID, Double> getSnapshotAgent(@NotNull NumberTopHolder holder) {
        return holder.getSnapshotAgent();
    }

    @Override
    protected @NotNull String getDisplayName(@Nullable UUID key, @NotNull NumberTopHolder holder) {
        return holder.getValueDisplay().getDisplayName(key);
    }

    @Override
    protected @NotNull String getDisplayValue(@Nullable Double value, @NotNull NumberTopHolder holder, @NotNull String args) {
        return holder.getValueDisplay().getDisplayValue(value, args);
    }

    @Override
    protected @NotNull String getDisplayKey(@Nullable UUID key, @NotNull NumberTopHolder holder) {
        return holder.getValueDisplay().getDisplayUuid(key);
    }

    @Override
    protected @NotNull UUID getKeyFromActor(@NotNull OfflinePlayer actor, @NotNull NumberTopHolder holder) {
        return actor.getUniqueId();
    }
}
