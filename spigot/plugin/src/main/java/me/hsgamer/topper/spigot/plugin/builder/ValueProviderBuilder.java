package me.hsgamer.topper.spigot.plugin.builder;

import me.hsgamer.hscore.builder.FunctionalMassBuilder;
import me.hsgamer.hscore.common.CollectionUtils;
import me.hsgamer.topper.spigot.value.statistic.StatisticValueProvider;
import me.hsgamer.topper.value.core.ValueProvider;
import org.bukkit.Bukkit;

import java.util.*;

public class ValueProviderBuilder extends FunctionalMassBuilder<Map<String, Object>, ValueProvider<UUID, Double>> {
    public ValueProviderBuilder() {
        register(map -> {
            String statistic = Optional.ofNullable(map.get("statistic")).map(Objects::toString).orElse(null);
            List<String> materials = Optional.ofNullable(map.get("material")).map(CollectionUtils::createStringListFromObject).orElse(Collections.emptyList());
            List<String> entityTypes = Optional.ofNullable(map.get("entity-type")).map(CollectionUtils::createStringListFromObject).orElse(Collections.emptyList());
            return StatisticValueProvider.fromRaw(statistic, materials, entityTypes).thenApply(Integer::doubleValue).keyMapper(Bukkit::getPlayer);
        }, "statistic", "stat");
    }

    @Override
    protected String getType(Map<String, Object> map) {
        return Objects.toString(map.get("type"), "");
    }
}
