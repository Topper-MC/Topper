package me.hsgamer.topper.spigot.value.statistic;

import me.hsgamer.topper.value.core.ValueProvider;
import me.hsgamer.topper.value.core.ValueWrapper;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StatisticValueProvider implements ValueProvider<Player, Integer> {
    private final Statistic statistic;
    private final List<Material> materials;
    private final List<EntityType> entityTypes;

    public StatisticValueProvider(Statistic statistic, List<Material> materials, List<EntityType> entityTypes) {
        this.statistic = statistic;
        this.materials = materials;
        this.entityTypes = entityTypes;
    }

    public static StatisticValueProvider fromRaw(String statistic, Collection<String> materials, Collection<String> entityTypes) {
        Statistic stat = Optional.ofNullable(statistic)
                .map(String::toUpperCase)
                .flatMap(s -> {
                    try {
                        return Optional.of(Statistic.valueOf(s));
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                })
                .orElse(null);
        List<Material> mat = materials.stream()
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<EntityType> entity = entityTypes.stream()
                .map(String::toUpperCase)
                .flatMap(s -> {
                    try {
                        return Stream.of(EntityType.valueOf(s));
                    } catch (IllegalArgumentException e) {
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());
        return new StatisticValueProvider(stat, mat, entity);
    }

    @Override
    public @NotNull ValueWrapper<Integer> apply(@NotNull Player player) {
        if (statistic == null) {
            return ValueWrapper.error("Statistic is not set");
        }

        switch (statistic.getType()) {
            case BLOCK: {
                if (materials.isEmpty()) {
                    return ValueWrapper.error("No material provided for BLOCK statistic: " + statistic);
                }

                int total = 0;
                for (Material material : materials) {
                    if (material == null || !material.isBlock()) {
                        return ValueWrapper.error("Invalid material for BLOCK statistic: " + statistic + " - " + material);
                    }
                    total += player.getStatistic(statistic, material);
                }

                return ValueWrapper.handled(total);
            }
            case ITEM: {
                if (materials.isEmpty()) {
                    return ValueWrapper.error("No material provided for ITEM statistic: " + statistic);
                }

                int total = 0;
                for (Material material : materials) {
                    if (material == null || !material.isItem()) {
                        return ValueWrapper.error("Invalid material for ITEM statistic: " + statistic + " - " + material);
                    }
                    total += player.getStatistic(statistic, material);
                }

                return ValueWrapper.handled(total);
            }
            case ENTITY: {
                if (entityTypes.isEmpty()) {
                    return ValueWrapper.error("No entity type provided for ENTITY statistic: " + statistic);
                }

                int total = 0;
                for (EntityType entityType : entityTypes) {
                    if (entityType == null || !entityType.isAlive()) {
                        return ValueWrapper.error("Invalid entity type for ENTITY statistic: " + statistic + " - " + entityType);
                    }
                    total += player.getStatistic(statistic, entityType);
                }

                return ValueWrapper.handled(total);
            }
            default:
                return ValueWrapper.handled(player.getStatistic(statistic));
        }
    }
}
