package me.hsgamer.topper.query.core;

import me.hsgamer.topper.core.DataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public abstract class QueryManager<K, V, H extends DataHolder<K, V>, A> {
    private final Map<String, QueryAction<K, V, H, A>> actions = new HashMap<>();

    protected abstract Optional<H> getHolder(String name);

    protected boolean isSingleHolder() {
        return false;
    }

    protected void registerAction(String name, QueryAction<K, V, H, A> action) {
        actions.put(name, action);
    }

    protected void registerFunction(String name, BiFunction<@NotNull H, @NotNull String, @Nullable String> function) {
        registerAction(name, (actor, holder, args) -> function.apply(holder, args));
    }

    protected void registerActorFunction(String name, BiFunction<@NotNull A, @NotNull H, @Nullable String> function) {
        registerAction(name, (actor, holder, args) -> {
            if (actor == null) return null;
            return function.apply(actor, holder);
        });
    }

    @Nullable
    public String get(@Nullable A actor, String query) {
        String holderName;
        String actionName;
        String args;
        if (isSingleHolder()) {
            String[] split = query.split(";", 2);
            holderName = "";
            actionName = split[0];
            args = split.length > 1 ? split[1] : "";
        } else {
            String[] split = query.split(";", 3);
            if (split.length < 2) return null;
            holderName = split[0];
            actionName = split[1];
            args = split.length > 2 ? split[2] : "";
        }

        Optional<H> optionalHolder = getHolder(holderName);
        if (!optionalHolder.isPresent()) return null;
        H holder = optionalHolder.get();

        QueryAction<K, V, H, A> action = actions.get(actionName);
        if (action == null) return null;

        return action.get(actor, holder, args);
    }
}
