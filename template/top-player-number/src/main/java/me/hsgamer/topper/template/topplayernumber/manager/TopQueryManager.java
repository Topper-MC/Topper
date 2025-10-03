package me.hsgamer.topper.template.topplayernumber.manager;

import me.hsgamer.topper.agent.snapshot.SnapshotAgent;
import me.hsgamer.topper.query.core.QueryManager;
import me.hsgamer.topper.query.holder.HolderQuery;
import me.hsgamer.topper.query.simple.SimpleQueryDisplay;
import me.hsgamer.topper.query.snapshot.SnapshotQuery;
import me.hsgamer.topper.template.topplayernumber.TopPlayerNumberTemplate;
import me.hsgamer.topper.template.topplayernumber.holder.NumberTopHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class TopQueryManager extends QueryManager<UUID> {
    public TopQueryManager(TopPlayerNumberTemplate template) {
        addQuery(new HolderQuery<UUID, Double, NumberTopHolder, UUID>() {
            @Override
            protected Optional<NumberTopHolder> getHolder(@NotNull String name) {
                return template.getTopManager().getHolder(name);
            }

            @Override
            protected @NotNull SimpleQueryDisplay<UUID, Double> getDisplay(@NotNull NumberTopHolder holder) {
                return holder.getValueDisplay();
            }

            @Override
            protected Optional<UUID> getKey(@NotNull UUID actor, @NotNull Context<UUID, Double, NumberTopHolder> context) {
                return Optional.of(actor);
            }
        });
        addQuery(new SnapshotQuery<UUID, Double, UUID>() {
            @Override
            protected Optional<SnapshotAgent<UUID, Double>> getAgent(@NotNull String name) {
                return template.getTopManager().getHolder(name).map(NumberTopHolder::getSnapshotAgent);
            }

            @Override
            protected Optional<SimpleQueryDisplay<UUID, Double>> getDisplay(@NotNull String name) {
                return template.getTopManager().getHolder(name).map(NumberTopHolder::getValueDisplay);
            }

            @Override
            protected Optional<UUID> getKey(@NotNull UUID actor, @NotNull Context<UUID, Double> context) {
                return Optional.of(actor);
            }

            @Override
            protected @NotNull String getDisplayRank(int rank, @NotNull Context<UUID, Double> context) {
                return context.display.getDisplayValue((double) rank, context.parent.args);
            }
        });
    }
}
