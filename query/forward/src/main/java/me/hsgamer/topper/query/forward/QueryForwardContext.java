package me.hsgamer.topper.query.forward;

import me.hsgamer.topper.query.core.QueryManager;

public interface QueryForwardContext<A> {
    String getName();

    QueryManager<A> getQueryManager();
}
