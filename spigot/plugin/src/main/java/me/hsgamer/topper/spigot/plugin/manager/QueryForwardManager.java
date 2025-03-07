package me.hsgamer.topper.spigot.plugin.manager;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.topper.query.core.QueryManager;
import me.hsgamer.topper.query.forward.QueryForward;
import me.hsgamer.topper.query.forward.QueryForwardContext;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.query.forward.plugin.PluginContext;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

public class QueryForwardManager extends QueryForward<OfflinePlayer, QueryForwardManager.ForwardContext> implements Loadable {
    private final TopperPlugin plugin;

    public QueryForwardManager(TopperPlugin plugin) {
        this.plugin = plugin;
    }

    public void addQueryManager(Plugin plugin, String name, QueryManager<OfflinePlayer> queryManager) {
        addContext(new ForwardContext(plugin, name, queryManager));
    }

    @Override
    public void enable() {
        addQueryManager(plugin, TopperPlugin.GROUP, plugin.get(TopQueryManager.class));
    }

    @Override
    public void disable() {
        clearContexts();
        clearForwarders();
    }

    public static final class ForwardContext implements QueryForwardContext<OfflinePlayer>, PluginContext {
        public final Plugin plugin;
        public final String name;
        public final QueryManager<OfflinePlayer> queryManager;

        private ForwardContext(Plugin plugin, String name, QueryManager<OfflinePlayer> queryManager) {
            this.plugin = plugin;
            this.name = name;
            this.queryManager = queryManager;
        }

        @Override
        public Plugin getPlugin() {
            return plugin;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public QueryManager<OfflinePlayer> getQuery() {
            return queryManager;
        }
    }
}
