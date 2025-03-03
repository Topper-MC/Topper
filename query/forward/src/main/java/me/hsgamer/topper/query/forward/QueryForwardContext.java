package me.hsgamer.topper.query.forward;

import me.hsgamer.topper.query.core.QueryResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public interface QueryForwardContext<A> {
    String getName();

    BiFunction<@Nullable A, @NotNull String, @NotNull QueryResult> getQuery();
}
