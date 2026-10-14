package me.hsgamer.topper.query.simple;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SimpleQueryContext {
    public final @NotNull String name;
    public final @NotNull String args;
    public final @NotNull String actionName;

    public SimpleQueryContext(@NotNull String name, @NotNull String args, @NotNull String actionName) {
        this.name = name;
        this.args = args;
        this.actionName = actionName;
    }

    public static @Nullable SimpleQueryContext parse(@NotNull String query, boolean singleName) {
        int actionSeparator = query.indexOf(';');
        if (singleName) {
            String action = actionSeparator < 0 ? query : query.substring(0, actionSeparator);
            String args = actionSeparator < 0 ? "" : query.substring(actionSeparator + 1);
            return new SimpleQueryContext("", args, action);
        }

        if (actionSeparator < 0) {
            return null;
        }

        String name = query.substring(0, actionSeparator);
        int argsSeparator = query.indexOf(';', actionSeparator + 1);
        if (argsSeparator < 0) {
            return new SimpleQueryContext(name, "", query.substring(actionSeparator + 1));
        }

        String action = query.substring(actionSeparator + 1, argsSeparator);
        String args = query.substring(argsSeparator + 1);
        return new SimpleQueryContext(name, args, action);
    }
}
