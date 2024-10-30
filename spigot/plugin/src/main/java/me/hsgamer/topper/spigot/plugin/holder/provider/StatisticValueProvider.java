package me.hsgamer.topper.spigot.plugin.holder.provider;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StatisticValueProvider implements ValueProvider {
    private final Statistic statistic;
    private final Material material;
    private final EntityType entityType;

    public StatisticValueProvider(Map<String, Object> map) {
        this.statistic = Optional.ofNullable(map.get("statistic"))
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
        this.material = Optional.ofNullable(map.get("material"))
                .map(Objects::toString)
                .map(Material::matchMaterial)
                .orElse(null);
        this.entityType = Optional.ofNullable(map.get("entity"))
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
    }

    @Override
    public CompletableFuture<Optional<Double>> getValue(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return Optional.empty();
            }

            if (statistic == null) {
                return Optional.empty();
            }

            switch (statistic.getType()) {
                case BLOCK:
                    if (material == null || !material.isBlock()) {
                        throw new IllegalArgumentException("Invalid material for BLOCK statistic");
                    }
                    return Optional.of((double) player.getStatistic(statistic, material));
                case ITEM:
                    if (material == null) {
                        throw new IllegalArgumentException("Invalid material for ITEM statistic");
                    }
                    return Optional.of((double) player.getStatistic(statistic, material));
                case ENTITY:
                    if (entityType == null) {
                        throw new IllegalArgumentException("Invalid entity for ENTITY statistic");
                    }
                    return Optional.of((double) player.getStatistic(statistic, entityType));
                default:
                    return Optional.of((double) player.getStatistic(statistic));
            }
        });
    }
}
