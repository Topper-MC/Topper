package me.hsgamer.topper.query.holder;

import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.core.DataHolder;
import me.hsgamer.topper.query.simple.SimpleQuery;
import me.hsgamer.topper.query.simple.SimpleQueryContext;
import me.hsgamer.topper.query.simple.SimpleQueryDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class HolderQuery<K, V, H extends DataHolder<K, V>, A> extends SimpleQuery<A, HolderQuery.Context<K, V, H>> {
    protected HolderQuery() {
        registerActorAction("key", (actor, context) -> {
            K key = getKey(actor, context);
            return getDisplay(context.holder).getDisplayKey(key);
        });
        registerActorAction("name", (actor, context) -> {
            K key = getKey(actor, context);
            return getDisplay(context.holder).getDisplayName(key);
        });
        registerActorAction("value", (actor, context) -> {
            K key = getKey(actor, context);
            if (key == null) return null;
            V value = context.holder.getEntry(key).map(DataEntry::getValue).orElse(null);
            return getDisplay(context.holder).getDisplayValue(value, context.parent.args);
        });
        registerActorAction("value_raw", (actor, context) -> {
            K key = getKey(actor, context);
            if (key == null) return null;
            V value = context.holder.getEntry(key).map(DataEntry::getValue).orElse(null);
            return getDisplay(context.holder).getDisplayValue(value, "raw");
        });
    }

    protected abstract Optional<H> getHolder(@NotNull String name);

    @NotNull
    protected abstract SimpleQueryDisplay<K, V> getDisplay(@NotNull H holder);

    protected boolean isSingleHolder() {
        return false;
    }

    @Nullable
    protected abstract K getKey(@NotNull A actor, @NotNull Context<K, V, H> context);

    @Override
    protected Optional<Context<K, V, H>> getContext(@NotNull String query) {
        return SimpleQueryContext.fromQuery(query, isSingleHolder())
                .map(context -> {
                    Optional<H> optionalHolder = getHolder(context.name);
                    if (!optionalHolder.isPresent()) return null;
                    H holder = optionalHolder.get();
                    return new Context<>(holder, context);
                });
    }

    public static final class Context<K, V, H extends DataHolder<K, V>> implements SimpleQuery.Context {
        public final @NotNull H holder;
        public final @NotNull SimpleQueryContext parent;

        Context(@NotNull H holder, @NotNull SimpleQueryContext parent) {
            this.holder = holder;
            this.parent = parent;
        }

        @Override
        public @NotNull String getActionName() {
            return parent.getActionName();
        }
    }
}
