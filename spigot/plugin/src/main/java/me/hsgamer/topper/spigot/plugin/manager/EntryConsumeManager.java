package me.hsgamer.topper.spigot.plugin.manager;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class EntryConsumeManager implements Loadable {
    private final TopperPlugin plugin;
    private final Table<String, String, List<BiConsumer<UUID, Double>>> consumerTable = HashBasedTable.create();
    private final Map<String, BiFunction<String, UUID, Optional<Double>>> providerMap = new HashMap<>();

    public EntryConsumeManager(TopperPlugin plugin) {
        this.plugin = plugin;
    }

    public Runnable addConsumer(String group, String holder, BiConsumer<UUID, Double> consumer) {
        List<BiConsumer<UUID, Double>> consumerList = consumerTable.get(group, holder);
        if (consumerList == null) {
            consumerList = new ArrayList<>();
            consumerTable.put(group, holder, consumerList);
        }
        consumerList.add(consumer);

        List<BiConsumer<UUID, Double>> finalConsumerList = consumerList;
        return () -> finalConsumerList.remove(consumer);
    }

    public Runnable addProvider(String group, BiFunction<String, UUID, Optional<Double>> provider) {
        providerMap.put(group, provider);
        return () -> providerMap.remove(group);
    }

    public Optional<Double> getValue(String group, String holder, UUID uuid) {
        BiFunction<String, UUID, Optional<Double>> function = providerMap.get(group);
        if (function == null) {
            return Optional.empty();
        }
        return function.apply(holder, uuid);
    }

    @Override
    public void enable() {
        addProvider("topper", (holder, uuid) ->
                plugin.get(TopManager.class)
                        .getTopHolder(holder)
                        .flatMap(h -> h.getEntry(uuid))
                        .map(DataEntry::getValue)
        );
    }

    @Override
    public void disable() {
        consumerTable.clear();
        providerMap.clear();
    }
}
