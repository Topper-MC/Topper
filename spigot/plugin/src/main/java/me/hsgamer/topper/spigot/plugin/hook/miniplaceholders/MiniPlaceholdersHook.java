package me.hsgamer.topper.spigot.plugin.hook.miniplaceholders;

import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.utils.TagsUtils;
import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.builder.ValueProviderBuilder;
import me.hsgamer.topper.spigot.plugin.manager.TopQueryManager;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public class MiniPlaceholdersHook implements Loadable {
    private final TopperPlugin plugin;
    private final Expansion expansion;

    public MiniPlaceholdersHook(TopperPlugin plugin) {
        this.plugin = plugin;

        BiFunction<@Nullable Player, ArgumentQueue, Tag> queryFunction = (player, queue) -> {
            String query = queue.popOr("You need to specify the query").value();
            String result = plugin.get(TopQueryManager.class).get(player, query);
            if (result == null) {
                return TagsUtils.EMPTY_TAG;
            } else {
                return TagsUtils.staticTag(result);
            }
        };

        this.expansion = Expansion.builder("topper")
                .globalPlaceholder("global", (queue, context) -> queryFunction.apply(null, queue))
                .filter(Player.class)
                .audiencePlaceholder("player", (audience, queue, ctx) -> queryFunction.apply((Player) audience, queue))
                .build();
    }

    @Override
    public void load() {
        plugin.get(ValueProviderBuilder.class).register(map -> new MiniPlaceholderValueProvider(plugin, map), "miniplaceholders");
    }

    @Override
    public void enable() {
        expansion.register();
    }

    @Override
    public void disable() {
        expansion.unregister();
    }
}
