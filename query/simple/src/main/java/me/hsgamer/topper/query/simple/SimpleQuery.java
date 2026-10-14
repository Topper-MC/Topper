package me.hsgamer.topper.query.simple;

import me.hsgamer.topper.query.core.Query;
import me.hsgamer.topper.query.core.QueryResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public abstract class SimpleQuery<A, C> implements Query<A> {
    private final Map<String, BiFunction<@Nullable A, @NotNull C, @Nullable String>> actions = new HashMap<>();

    protected abstract @Nullable C createContext(@NotNull SimpleQueryContext context);

    protected boolean isSingleName() {
        return false;
    }

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
        SimpleQueryContext parsedContext = SimpleQueryContext.parse(query, isSingleName());
        if (parsedContext == null) return QueryResult.notHandled();

        BiFunction<@Nullable A, @NotNull C, @Nullable String> action = actions.get(parsedContext.actionName);
        if (action == null) return QueryResult.notHandled();

        C context = createContext(parsedContext);
        if (context == null) return QueryResult.notHandled();

        return QueryResult.handled(action.apply(actor, context));
    }
}
