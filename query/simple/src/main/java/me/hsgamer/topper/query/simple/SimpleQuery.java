package me.hsgamer.topper.query.simple;

import me.hsgamer.topper.query.core.Query;
import me.hsgamer.topper.query.core.QueryResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public abstract class SimpleQuery<A, C extends SimpleQuery.Context> implements Query<A> {
    private final Map<String, BiFunction<@Nullable A, @NotNull C, @Nullable String>> actions = new HashMap<>();

    protected abstract Optional<C> getContext(@NotNull String query);

    protected void registerAction(String name, BiFunction<@Nullable A, @NotNull C, @Nullable String> action) {
        actions.put(name, action);
    }

    protected void registerActorAction(String name, BiFunction<@NotNull A, @NotNull C, @Nullable String> function) {
        registerAction(name, (actor, context) -> {
            if (actor == null) return null;
            return function.apply(actor, context);
        });
    }

    @Override
    public @NotNull QueryResult apply(@Nullable A actor, @NotNull String query) {
        Optional<C> optionalContext = getContext(query);
        if (!optionalContext.isPresent()) return QueryResult.notHandled();
        C context = optionalContext.get();

        String actionName = context.getActionName();
        BiFunction<@Nullable A, @NotNull C, @Nullable String> action = actions.get(actionName);
        if (action == null) return QueryResult.notHandled();

        return QueryResult.handled(action.apply(actor, context));
    }

    public interface Context {
        @NotNull String getActionName();
    }
}
