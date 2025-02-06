package me.hsgamer.topper.spigot.plugin.builder;

import me.hsgamer.hscore.builder.FunctionalMassBuilder;
import me.hsgamer.topper.spigot.value.statistic.StatisticValueProvider;
import me.hsgamer.topper.value.core.ValueProvider;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ValueProviderBuilder extends FunctionalMassBuilder<Map<String, Object>, ValueProvider<UUID, Double>> {
    public ValueProviderBuilder() {
        register(map -> StatisticValueProvider.fromMap(map).thenApply(Integer::doubleValue).keyMapper(Bukkit::getPlayer), "statistic", "stat");
    }

    @Override
    protected String getType(Map<String, Object> map) {
        return Objects.toString(map.get("type"), "");
    }
}
