package me.hsgamer.topper.query.snapshot;

import me.hsgamer.topper.agent.snapshot.SnapshotAgent;
import me.hsgamer.topper.query.simple.SimpleQuery;
import me.hsgamer.topper.query.simple.SimpleQueryContext;
import me.hsgamer.topper.query.simple.SimpleQueryDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public abstract class SnapshotQuery<K, V, A> extends SimpleQuery<A, SnapshotQuery.Context<K, V>> {
    protected SnapshotQuery(String prefix) {
        registerAction(prefix + "_name", (actor, context) ->
                context.display.getDisplayName(getKeyAt(context, context.parent.args)));
        registerAction(prefix + "_key", (actor, context) ->
                context.display.getDisplayKey(getKeyAt(context, context.parent.args)));
        registerAction(prefix + "_value", (actor, context) -> {
            String args = context.parent.args;
            int separator = args.indexOf(';');
            V value = getValueAt(context, separator < 0 ? args : args.substring(0, separator));
            return context.display.getDisplayValue(value, separator < 0 ? "" : args.substring(separator + 1));
        });
        registerAction(prefix + "_value_raw", (actor, context) ->
                context.display.getDisplayValue(getValueAt(context, context.parent.args), "raw"));
        registerActorAction(prefix + "_rank", (actor, context) ->
                getKey(actor, context)
                        .map(context.agent::getSnapshotIndex)
                        .map(index -> index + 1)
                        .map(rank -> getDisplayRank(rank, context))
                        .orElse("0"));
        registerAction(prefix + "_size", (actor, context) ->
                Integer.toString(context.agent.getSnapshot().size()));
    }

    protected SnapshotQuery() {
        this("top");
    }

    private static int toIndex(@NotNull String arg) {
        try {
            return Integer.parseInt(arg) - 1;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static <K, V> @Nullable K getKeyAt(@NotNull Context<K, V> context, @NotNull String arg) {
        return context.agent.getSnapshotByIndex(toIndex(arg)).map(Map.Entry::getKey).orElse(null);
    }

    private static <K, V> @Nullable V getValueAt(@NotNull Context<K, V> context, @NotNull String arg) {
        return context.agent.getSnapshotByIndex(toIndex(arg)).map(Map.Entry::getValue).orElse(null);
    }

    protected abstract Optional<View<K, V>> getView(@NotNull String name);

    protected abstract Optional<K> getKey(@NotNull A actor, @NotNull Context<K, V> context);

    protected @NotNull String getDisplayRank(int rank, @NotNull Context<K, V> context) {
        return Integer.toString(rank);
    }

    @Override
    protected @Nullable Context<K, V> createContext(@NotNull SimpleQueryContext context) {
        View<K, V> view = getView(context.name).orElse(null);
        return view == null ? null : new Context<>(view, context);
    }

    public static final class View<K, V> {
        public final @NotNull SnapshotAgent<K, V> agent;
        public final @NotNull SimpleQueryDisplay<K, V> display;

        public View(@NotNull SnapshotAgent<K, V> agent, @NotNull SimpleQueryDisplay<K, V> display) {
            this.agent = agent;
            this.display = display;
        }
    }

    public static final class Context<K, V> {
        public final @NotNull SnapshotAgent<K, V> agent;
        public final @NotNull SimpleQueryDisplay<K, V> display;
        public final @NotNull SimpleQueryContext parent;

        Context(@NotNull View<K, V> view, @NotNull SimpleQueryContext parent) {
            this.agent = view.agent;
            this.display = view.display;
            this.parent = parent;
        }
    }
}
