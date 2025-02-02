package me.hsgamer.topper.query.snapshot;

import me.hsgamer.topper.agent.snapshot.SnapshotAgent;
import me.hsgamer.topper.query.simple.SimpleQuery;
import me.hsgamer.topper.query.simple.SimpleQueryContext;
import me.hsgamer.topper.query.simple.SimpleQueryDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public abstract class SnapshotQuery<K, V, A> extends SimpleQuery<A, SnapshotQuery.Context<K, V>> {
    protected SnapshotQuery() {
        registerAction("top_name", (actor, context) -> {
            int i = 1;
            try {
                i = Integer.parseInt(context.parent.args);
            } catch (NumberFormatException ignored) {
                // IGNORED
            }
            K key = context.agent.getSnapshotByIndex(i - 1).map(Map.Entry::getKey).orElse(null);
            return context.display.getDisplayName(key);
        });
        registerAction("top_key", (actor, context) -> {
            int i = 1;
            try {
                i = Integer.parseInt(context.parent.args);
            } catch (NumberFormatException ignored) {
                // IGNORED
            }
            K key = context.agent.getSnapshotByIndex(i - 1).map(Map.Entry::getKey).orElse(null);
            return context.display.getDisplayName(key);
        });
        registerAction("top_value", (actor, context) -> {
            String[] split = context.parent.args.split(";", 2);
            int i = 1;
            try {
                i = Integer.parseInt(split[0]);
            } catch (NumberFormatException ignored) {
                // IGNORED
            }

            String valueArgs = split.length > 1 ? split[1] : "";

            V value = context.agent.getSnapshotByIndex(i - 1).map(Map.Entry::getValue).orElse(null);
            return context.display.getDisplayValue(value, valueArgs);
        });
        registerAction("top_value_raw", (actor, context) -> {
            int i = 1;
            try {
                i = Integer.parseInt(context.parent.args);
            } catch (NumberFormatException ignored) {
                // IGNORED
            }
            V value = context.agent.getSnapshotByIndex(i - 1).map(Map.Entry::getValue).orElse(null);
            return context.display.getDisplayValue(value, "raw");
        });
        registerActorAction("top_rank", (actor, context) ->
                getKey(actor, context)
                        .map(context.agent::getSnapshotIndex)
                        .map(index -> index + 1)
                        .map(Object::toString)
                        .orElse("0"));
    }

    protected abstract Optional<SnapshotAgent<K, V>> getAgent(@NotNull String name);

    protected abstract Optional<SimpleQueryDisplay<K, V>> getDisplay(@NotNull String name);

    protected boolean isSingleAgent() {
        return false;
    }

    protected abstract Optional<K> getKey(@NotNull A actor, @NotNull Context<K, V> context);

    @Override
    protected Optional<Context<K, V>> getContext(@NotNull String query) {
        return SimpleQueryContext.fromQuery(query, isSingleAgent())
                .map(context -> {
                    Optional<SnapshotAgent<K, V>> agent = getAgent(context.name);
                    Optional<SimpleQueryDisplay<K, V>> display = getDisplay(context.name);
                    if (!agent.isPresent() || !display.isPresent()) {
                        return null;
                    }
                    return new Context<>(agent.get(), display.get(), context);
                });
    }

    public static final class Context<K, V> implements SimpleQuery.Context {
        public final @NotNull SnapshotAgent<K, V> agent;
        public final @NotNull SimpleQueryDisplay<K, V> display;
        public final @NotNull SimpleQueryContext parent;

        Context(@NotNull SnapshotAgent<K, V> agent, @NotNull SimpleQueryDisplay<K, V> display, @NotNull SimpleQueryContext parent) {
            this.agent = agent;
            this.display = display;
            this.parent = parent;
        }

        @Override
        public @NotNull String getActionName() {
            return parent.getActionName();
        }
    }
}
