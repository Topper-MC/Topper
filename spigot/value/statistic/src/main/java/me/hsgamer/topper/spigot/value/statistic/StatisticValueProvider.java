package me.hsgamer.topper.spigot.value.statistic;

import me.hsgamer.topper.value.core.ValueProvider;
import me.hsgamer.topper.value.core.ValueWrapper;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class StatisticValueProvider implements ValueProvider<Player, Integer> {
    private final Statistic statistic;
    private final Material material;
    private final EntityType entityType;

    public StatisticValueProvider(Statistic statistic, Material material, EntityType entityType) {
        this.statistic = statistic;
        this.material = material;
        this.entityType = entityType;
    }

    public static StatisticValueProvider fromMap(Map<String, Object> map) {
        Statistic statistic = Optional.ofNullable(map.get("statistic"))
                .map(Objects::toString)
                .map(String::toUpperCase)
                .flatMap(s -> {
                    try {
                        return Optional.of(Statistic.valueOf(s));
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                })
                .orElse(null);
        Material material = Optional.ofNullable(map.get("material"))
                .map(Objects::toString)
                .map(Material::matchMaterial)
                .orElse(null);
        EntityType entityType = Optional.ofNullable(map.get("entity"))
                .map(Objects::toString)
                .map(String::toUpperCase)
                .flatMap(s -> {
                    try {
                        return Optional.of(EntityType.valueOf(s));
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                })
                .orElse(null);
        return new StatisticValueProvider(statistic, material, entityType);
    }

    @Override
    public ValueWrapper<Integer> apply(Player player) {
        if (statistic == null) {
            return ValueWrapper.error("Statistic is not set");
        }

        switch (statistic.getType()) {
            case BLOCK:
                if (material == null || !material.isBlock()) {
                    return ValueWrapper.error("Invalid material for BLOCK statistic: " + statistic);
                }
                return ValueWrapper.handled(player.getStatistic(statistic, material));
            case ITEM:
                if (material == null) {
                    return ValueWrapper.error("Invalid material for ITEM statistic: " + statistic);
                }
                return ValueWrapper.handled(player.getStatistic(statistic, material));
            case ENTITY:
                if (entityType == null) {
                    return ValueWrapper.error("Invalid entity for ENTITY statistic: " + statistic);
                }
                return ValueWrapper.handled(player.getStatistic(statistic, entityType));
            default:
                return ValueWrapper.handled(player.getStatistic(statistic));
        }
    }
}
