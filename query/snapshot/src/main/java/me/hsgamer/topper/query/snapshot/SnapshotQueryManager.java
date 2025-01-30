package me.hsgamer.topper.query.snapshot;

import me.hsgamer.topper.agent.snapshot.SnapshotAgent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.core.DataHolder;
import me.hsgamer.topper.query.core.QueryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class SnapshotQueryManager<K, V, H extends DataHolder<K, V>, A> extends QueryManager<K, V, H, A> {
    protected SnapshotQueryManager() {
        registerFunction("top_name", (holder, args) -> {
            int i = 1;
            try {
                i = Integer.parseInt(args);
            } catch (NumberFormatException ignored) {
                // IGNORED
            }
            K key = getSnapshotAgent(holder).getSnapshotByIndex(i - 1).map(Map.Entry::getKey).orElse(null);
            return getDisplayName(key, holder);
        });
        registerFunction("top_key", (holder, args) -> {
            int i = 1;
            try {
                i = Integer.parseInt(args);
            } catch (NumberFormatException ignored) {
                // IGNORED
            }
            K key = getSnapshotAgent(holder).getSnapshotByIndex(i - 1).map(Map.Entry::getKey).orElse(null);
            return getDisplayKey(key, holder);
        });
        registerFunction("top_value", (holder, args) -> {
            String[] split = args.split(";", 2);
            int i = 1;
            try {
                i = Integer.parseInt(split[0]);
            } catch (NumberFormatException ignored) {
                // IGNORED
            }

            String valueArgs = split.length > 1 ? split[1] : "";

            V value = getSnapshotAgent(holder).getSnapshotByIndex(i - 1).map(Map.Entry::getValue).orElse(null);
            return getDisplayValue(value, holder, valueArgs);
        });
        registerFunction("top_value_raw", (holder, args) -> {
            int i = 1;
            try {
                i = Integer.parseInt(args);
            } catch (NumberFormatException ignored) {
                // IGNORED
            }
            V value = getSnapshotAgent(holder).getSnapshotByIndex(i - 1).map(Map.Entry::getValue).orElse(null);
            return getDisplayValue(value, holder, "raw");
        });
        registerActorFunction("top_rank", (actor, holder) -> {
            int index = getSnapshotAgent(holder).getSnapshotIndex(getKeyFromActor(actor));
            return Integer.toString(index + 1);
        });
        registerAction("value", (actor, holder, args) -> {
            if (actor == null) return null;
            V value = holder.getEntry(getKeyFromActor(actor)).map(DataEntry::getValue).orElse(null);
            return getDisplayValue(value, holder, args);
        });
        registerActorFunction("value_raw", (actor, holder) -> {
            V value = holder.getEntry(getKeyFromActor(actor)).map(DataEntry::getValue).orElse(null);
            return getDisplayValue(value, holder, "raw");
        });
    }

    @NotNull
    protected abstract SnapshotAgent<K, V> getSnapshotAgent(@NotNull H holder);

    @NotNull
    protected abstract String getDisplayName(@Nullable K key, @NotNull H holder);

    @NotNull
    protected abstract String getDisplayValue(@Nullable V value, @NotNull H holder, @NotNull String args);

    @NotNull
    protected abstract String getDisplayKey(@Nullable K key, @NotNull H holder);

    @NotNull
    protected abstract K getKeyFromActor(@NotNull A actor);
}
