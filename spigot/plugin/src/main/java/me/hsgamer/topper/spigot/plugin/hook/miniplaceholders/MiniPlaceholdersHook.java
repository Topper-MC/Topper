package me.hsgamer.topper.spigot.plugin.hook.miniplaceholders;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.topper.query.core.QueryResult;
import me.hsgamer.topper.query.forward.QueryForwardContext;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.builder.ValueProviderBuilder;
import me.hsgamer.topper.spigot.plugin.manager.QueryForwardManager;
import me.hsgamer.topper.spigot.plugin.util.ParseUtil;
import me.hsgamer.topper.spigot.query.forward.miniplaceholders.MiniPlaceholdersQueryForwarder;
import me.hsgamer.topper.spigot.value.miniplaceholders.MiniPlaceholderValueProvider;
import me.hsgamer.topper.value.string.StringDeformatters;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;

public class MiniPlaceholdersHook implements Loadable {
    private final TopperPlugin plugin;
    private final MiniPlaceholdersQueryForwarder<QueryForwardContext<Player>> queryForwarder = new MiniPlaceholdersQueryForwarder<>();

    public MiniPlaceholdersHook(TopperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        plugin.get(ValueProviderBuilder.class).register(map -> {
            String placeholder = Optional.ofNullable(map.get("placeholder")).map(Object::toString).orElse("");
            return new MiniPlaceholderValueProvider(placeholder)
                    .thenApply(StringDeformatters.deformatterOrIdentity(map))
                    .thenApply(ParseUtil::parsePlaceholderNumber)
                    .keyMapper(Bukkit::getPlayer);
        }, "miniplaceholders", "miniplaceholder", "mini-placeholders", "mini-placeholder");
        plugin.get(QueryForwardManager.class).addForwarder(context -> queryForwarder.accept(new QueryForwardContext<Player>() {
            @Override
            public String getName() {
                return context.getName();
            }

            @Override
            public BiFunction<@Nullable Player, @NotNull String, @NotNull QueryResult> getQuery() {
                return context.getQuery()::apply;
            }
        }));
    }

    @Override
    public void disable() {
        queryForwarder.unregister();
    }
}
