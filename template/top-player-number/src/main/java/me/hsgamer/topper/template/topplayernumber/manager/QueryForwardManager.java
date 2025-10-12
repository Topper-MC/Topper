package me.hsgamer.topper.template.topplayernumber.manager;

import me.hsgamer.topper.query.core.QueryResult;
import me.hsgamer.topper.query.forward.QueryForward;
import me.hsgamer.topper.query.forward.QueryForwardContext;
import me.hsgamer.topper.template.topplayernumber.TopPlayerNumberTemplate;
import me.hsgamer.topper.template.topplayernumber.holder.NumberTopHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiFunction;

public class QueryForwardManager extends QueryForward<UUID, QueryForwardContext<UUID>> {
    private final TopPlayerNumberTemplate template;

    public QueryForwardManager(TopPlayerNumberTemplate template) {
        this.template = template;
    }

    public void enable() {
        addContext(new QueryForwardContext<UUID>() {
            @Override
            public String getName() {
                return NumberTopHolder.GROUP;
            }

            @Override
            public BiFunction<@Nullable UUID, @NotNull String, @NotNull QueryResult> getQuery() {
                return template.getTopQueryManager();
            }
        });
    }

    public void disable() {
        clearContexts();
        clearForwarders();
    }
}
