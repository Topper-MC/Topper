package me.hsgamer.topper.query.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class QueryManager<A> {
    private final List<BiFunction<@Nullable A, @NotNull String, @NotNull QueryResult>> queryList = new ArrayList<>();

    public void addQuery(BiFunction<@Nullable A, @NotNull String, @NotNull QueryResult> queryFunction) {
        queryList.add(queryFunction);
    }

    public void removeQuery(BiFunction<@Nullable A, @NotNull String, @NotNull QueryResult> queryFunction) {
        queryList.remove(queryFunction);
    }

    @Nullable
    public String get(@Nullable A actor, String query) {
        for (BiFunction<@Nullable A, @NotNull String, @NotNull QueryResult> queryFunction : queryList) {
            QueryResult result = queryFunction.apply(actor, query);
            if (result.handled) {
                return result.result;
            }
        }
        return null;
    }
}
