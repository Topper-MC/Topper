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
                    return Optional.ofNullable(material).filter(Material::isBlock).map(m -> (double) player.getStatistic(statistic, m));
                case ITEM:
                    return Optional.ofNullable(material).map(m -> (double) player.getStatistic(statistic, m));
                case ENTITY:
                    return Optional.ofNullable(entityType).map(e -> (double) player.getStatistic(statistic, e));
                default:
                    return Optional.of((double) player.getStatistic(statistic));
            }
        });
    }
}
