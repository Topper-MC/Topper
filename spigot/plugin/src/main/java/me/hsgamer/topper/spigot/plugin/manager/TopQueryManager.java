package me.hsgamer.topper.spigot.plugin.manager;

import me.hsgamer.topper.agent.snapshot.SnapshotAgent;
import me.hsgamer.topper.query.core.QueryManager;
import me.hsgamer.topper.query.holder.HolderQuery;
import me.hsgamer.topper.query.simple.SimpleQueryDisplay;
import me.hsgamer.topper.query.snapshot.SnapshotQuery;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.holder.NumberTopHolder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class TopQueryManager extends QueryManager<OfflinePlayer> {
    public TopQueryManager(TopperPlugin instance) {
        addQuery(new HolderQuery<UUID, Double, NumberTopHolder, OfflinePlayer>() {
            @Override
            protected Optional<NumberTopHolder> getHolder(@NotNull String name) {
                return instance.get(TopManager.class).getTopHolder(name);
            }

            @Override
            protected @NotNull SimpleQueryDisplay<UUID, Double> getDisplay(@NotNull NumberTopHolder holder) {
                return holder.getValueDisplay();
            }

            @Override
            protected Optional<UUID> getKey(@NotNull OfflinePlayer actor, @NotNull Context<UUID, Double, NumberTopHolder> context) {
                return Optional.of(actor.getUniqueId());
            }
        });
        addQuery(new SnapshotQuery<UUID, Double, OfflinePlayer>() {
            @Override
            protected Optional<SnapshotAgent<UUID, Double>> getAgent(@NotNull String name) {
                return instance.get(TopManager.class).getTopHolder(name).map(NumberTopHolder::getSnapshotAgent);
            }

            @Override
            protected Optional<SimpleQueryDisplay<UUID, Double>> getDisplay(@NotNull String name) {
                return instance.get(TopManager.class).getTopHolder(name).map(NumberTopHolder::getValueDisplay);
            }

            @Override
            protected Optional<UUID> getKey(@NotNull OfflinePlayer actor, @NotNull Context<UUID, Double> context) {
                return Optional.of(actor.getUniqueId());
            }

            @Override
            protected @NotNull String getDisplayRank(int rank, @NotNull Context<UUID, Double> context) {
                return context.display.getDisplayValue((double) rank, context.parent.args);
            }
        });
    }
}
