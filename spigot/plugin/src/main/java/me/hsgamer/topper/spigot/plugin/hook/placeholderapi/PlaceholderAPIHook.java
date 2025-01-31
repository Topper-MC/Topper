package me.hsgamer.topper.spigot.plugin.hook.placeholderapi;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.builder.ValueProviderBuilder;

public class PlaceholderAPIHook implements Loadable {
    private final TopperPlugin plugin;
    private final TopPlaceholderExpansion expansion;

    public PlaceholderAPIHook(TopperPlugin plugin) {
        this.plugin = plugin;
        this.expansion = new TopPlaceholderExpansion(plugin);
    }

    @Override
    public void load() {
        plugin.get(ValueProviderBuilder.class).register(map -> new PlaceholderValueProvider(plugin, map), "placeholderapi", "placeholder", "papi");
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
