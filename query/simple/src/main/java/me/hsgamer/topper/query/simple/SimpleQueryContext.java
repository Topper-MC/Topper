package me.hsgamer.topper.query.simple;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Pattern;

public final class SimpleQueryContext implements SimpleQuery.Context {
    public final @NotNull String name;
    public final @NotNull String args;
    private final @NotNull String actionName;

    public SimpleQueryContext(@NotNull String name, @NotNull String args, @NotNull String actionName) {
        this.name = name;
        this.args = args;
        this.actionName = actionName;
    }

    public static Optional<SimpleQueryContext> fromQuery(@NotNull String query, boolean singleName, @NotNull String separator) {
        String name;
        String actionName;
        String args;
        if (singleName) {
            String[] split = query.split(Pattern.quote(separator), 2);
            name = "";
            actionName = split[0];
            args = split.length > 1 ? split[1] : "";
        } else {
            String[] split = query.split(Pattern.quote(separator), 3);
            if (split.length < 2) return Optional.empty();
            name = split[0];
            actionName = split[1];
            args = split.length > 2 ? split[2] : "";
        }
        return Optional.of(new SimpleQueryContext(name, args, actionName));
    }

    public static Optional<SimpleQueryContext> fromQuery(@NotNull String query, boolean singleName) {
        return fromQuery(query, singleName, ";");
    }

    @Override
    public @NotNull String getActionName() {
        return actionName;
    }
}
