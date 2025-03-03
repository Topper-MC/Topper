package me.hsgamer.topper.spigot.query.forward.miniplaceholders;

import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.utils.TagsUtils;
import me.hsgamer.topper.query.forward.QueryForwardContext;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class MiniPlaceholdersQueryForwarder<C extends QueryForwardContext<Player>> implements Consumer<C> {
    private final List<Expansion> expansions = new ArrayList<>();

    @Override
    public void accept(C queryContext) {
        BiFunction<@Nullable Player, ArgumentQueue, Tag> queryFunction = (player, queue) -> {
            String query = queue.popOr("You need to specify the query").value();
            String result = queryContext.getQuery().apply(player, query).result;
            if (result == null) {
                return TagsUtils.EMPTY_TAG;
            } else {
                return TagsUtils.staticTag(result);
            }
        };

        Expansion expansion = Expansion.builder("topper")
                .globalPlaceholder("global", (queue, context) -> queryFunction.apply(null, queue))
                .filter(Player.class)
                .audiencePlaceholder("player", (audience, queue, ctx) -> queryFunction.apply((Player) audience, queue))
                .build();

        expansion.register();
        expansions.add(expansion);
    }

    public void unregister() {
        expansions.forEach(Expansion::unregister);
        expansions.clear();
    }
}
