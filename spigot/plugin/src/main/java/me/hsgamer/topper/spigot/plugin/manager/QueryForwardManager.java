package me.hsgamer.topper.spigot.plugin.manager;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.topper.query.core.QueryManager;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class QueryForwardManager implements Loadable {
    private final TopperPlugin plugin;
    private final List<ForwardContext> queryManagers = new ArrayList<>();
    private final List<Consumer<ForwardContext>> queryForwarders = new ArrayList<>();

    public QueryForwardManager(TopperPlugin plugin) {
        this.plugin = plugin;
    }

    public void addQueryManager(Plugin plugin, String name, QueryManager<OfflinePlayer> queryManager) {
        ForwardContext forwardContext = new ForwardContext(plugin, name, queryManager);
        queryManagers.add(forwardContext);
        queryForwarders.forEach(forwarder -> forwarder.accept(forwardContext));
    }

    public void addQueryForwarder(Consumer<ForwardContext> forwarder) {
        queryForwarders.add(forwarder);
        queryManagers.forEach(forwarder);
    }

    @Override
    public void enable() {
        addQueryManager(plugin, "topper", plugin.get(TopQueryManager.class));
    }

    @Override
    public void disable() {
        queryManagers.clear();
        queryForwarders.clear();
    }

    public static final class ForwardContext {
        public final Plugin plugin;
        public final String name;
        public final QueryManager<OfflinePlayer> queryManager;

        private ForwardContext(Plugin plugin, String name, QueryManager<OfflinePlayer> queryManager) {
            this.plugin = plugin;
            this.name = name;
            this.queryManager = queryManager;
        }
    }
}
