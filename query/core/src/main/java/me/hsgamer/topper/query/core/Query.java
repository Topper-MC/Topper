package me.hsgamer.topper.query.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public interface Query<A> extends BiFunction<@Nullable A, @NotNull String, @NotNull QueryResult> {
    @Override
    @NotNull QueryResult apply(@Nullable A actor, @NotNull String query);
}
