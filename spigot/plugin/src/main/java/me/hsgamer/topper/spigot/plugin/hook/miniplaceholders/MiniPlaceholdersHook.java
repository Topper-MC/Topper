package me.hsgamer.topper.spigot.plugin.hook.miniplaceholders;

import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.utils.TagsUtils;
import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.builder.ValueProviderBuilder;
import me.hsgamer.topper.spigot.plugin.manager.TopQueryManager;
import org.bukkit.entity.Player;

public class MiniPlaceholdersHook implements Loadable {
    private final TopperPlugin plugin;
    private final Expansion expansion;

    public MiniPlaceholdersHook(TopperPlugin plugin) {
        this.plugin = plugin;
        this.expansion = Expansion.builder("topper")
                .filter(Player.class)
                .audiencePlaceholder("query", (audience, queue, ctx) -> {
                    Player player = (Player) audience;
                    String query = queue.popOr("You need to specify the query").value();
                    String result = plugin.get(TopQueryManager.class).get(player, query);
                    if (result == null) {
                        return TagsUtils.EMPTY_TAG;
                    } else {
                        return TagsUtils.staticTag(result);
                    }
                })
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
